package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import com.example.data.model.SourceType
import com.example.data.remote.GeminiRepository
import com.example.data.remote.PersonaSettings
import com.example.ui.components.VoiceOrbState
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val knowledgeDao = database.knowledgeDao()
    private val geminiRepo = GeminiRepository(knowledgeDao)

    val ttsManager = TextToSpeechManager(application)
    private lateinit var speechManager: SpeechRecognizerManager

    // Active session
    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        chatDao.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeSources: StateFlow<List<KnowledgeSource>> = knowledgeDao.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedFacts: StateFlow<List<LearnedFact>> = knowledgeDao.getAllFacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Persona settings
    private val _personaSettings = MutableStateFlow(PersonaSettings())
    val personaSettings: StateFlow<PersonaSettings> = _personaSettings.asStateFlow()

    // Live Voice state
    private val _isLiveModeOpen = MutableStateFlow(false)
    val isLiveModeOpen: StateFlow<Boolean> = _isLiveModeOpen.asStateFlow()

    private val _voiceOrbState = MutableStateFlow(VoiceOrbState.IDLE)
    val voiceOrbState: StateFlow<VoiceOrbState> = _voiceOrbState.asStateFlow()

    private val _liveSubtitle = MutableStateFlow("")
    val liveSubtitle: StateFlow<String> = _liveSubtitle.asStateFlow()

    private val _handsFreeLoop = MutableStateFlow(true)
    val handsFreeLoop: StateFlow<Boolean> = _handsFreeLoop.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val speechAmplitude: StateFlow<Float>

    init {
        speechManager = SpeechRecognizerManager(
            context = application,
            onResult = { recognizedText -> handleSpeechRecognized(recognizedText) },
            onErrorRecovery = {
                if (_isLiveModeOpen.value && _handsFreeLoop.value && !_isGenerating.value) {
                    _voiceOrbState.value = VoiceOrbState.IDLE
                    _liveSubtitle.value = "Dotknij kuli lub mów dalej..."
                } else if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
                    _voiceOrbState.value = VoiceOrbState.IDLE
                }
            }
        )
        speechAmplitude = speechManager.rmsLevel

        // Listen to TTS state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _voiceOrbState.value = VoiceOrbState.SPEAKING
                } else {
                    _speakingMessageId.value = null
                    if (_isLiveModeOpen.value) {
                        if (_handsFreeLoop.value) {
                            startLiveListening()
                        } else {
                            _voiceOrbState.value = VoiceOrbState.IDLE
                        }
                    } else if (_voiceOrbState.value == VoiceOrbState.SPEAKING) {
                        _voiceOrbState.value = VoiceOrbState.IDLE
                    }
                }
            }
        }

        // Listen to partial speech text
        viewModelScope.launch {
            speechManager.partialText.collect { partial ->
                if (partial.isNotBlank() && _voiceOrbState.value == VoiceOrbState.LISTENING) {
                    _liveSubtitle.value = partial
                }
            }
        }

        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            val sources = knowledgeDao.getActiveSources()
            if (sources.isEmpty()) {
                knowledgeDao.insertSource(
                    KnowledgeSource(
                        title = "Mój Profil & Preferencje",
                        content = "Użytkownik ceni bezpośrednią komunikację, wysoki poziom merytoryczny, poczucie humoru i cięte riposty. Asystent powinien odpowiadać konkretnie i bez owijania w bawełnę.",
                        sourceType = SourceType.USER_PROFILE,
                        isEnabled = true
                    )
                )
                knowledgeDao.insertSource(
                    KnowledgeSource(
                        title = "Specyfikacja Projektu i Wiedza Bazowa",
                        content = "Aplikacja Android Aurora AI z dynamicznym trybem rozmowy na żywo (Live Voice), rozpoznawaniem mowy i syntezą mowy (TTS) oraz bazą wiedzy umożliwiającą douczanie modelu.",
                        sourceType = SourceType.DOCUMENT,
                        isEnabled = true
                    )
                )
            }

            val facts = knowledgeDao.getActiveFacts()
            if (facts.isEmpty()) {
                knowledgeDao.insertFact(
                    LearnedFact(
                        fact = "Preferuje dynamiczny, dowcipny styl rozmowy bez zbędnych formalności",
                        category = FactCategory.STYLE,
                        isActive = true
                    )
                )
                knowledgeDao.insertFact(
                    LearnedFact(
                        fact = "Ceni natychmiastowe, konkretne odpowiedzi podsumowane jednym trafnym wnioskiem",
                        category = FactCategory.PREFERENCE,
                        isActive = true
                    )
                )
            }

            // Ensure current session exists
            chatDao.insertSession(
                ChatSessionEntity(
                    sessionId = _currentSessionId.value,
                    title = "Rozmowa z Aurora AI"
                )
            )
        }
    }

    fun setLiveModeOpen(isOpen: Boolean) {
        _isLiveModeOpen.value = isOpen
        if (isOpen) {
            _liveSubtitle.value = "Dotknij kuli lub mów, aby rozpocząć..."
            if (_handsFreeLoop.value) {
                startLiveListening()
            }
        } else {
            stopLiveListening()
            ttsManager.stop()
            _voiceOrbState.value = VoiceOrbState.IDLE
        }
    }

    fun toggleHandsFreeLoop() {
        _handsFreeLoop.value = !_handsFreeLoop.value
    }

    fun startLiveListening() {
        ttsManager.stop()
        _voiceOrbState.value = VoiceOrbState.LISTENING
        _liveSubtitle.value = "Słucham cię..."
        speechManager.startListening()
    }

    fun stopLiveListening() {
        speechManager.stopListening()
        if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
            _voiceOrbState.value = VoiceOrbState.IDLE
        }
    }

    fun toggleLiveListening() {
        if (_voiceOrbState.value == VoiceOrbState.LISTENING) {
            stopLiveListening()
        } else {
            startLiveListening()
        }
    }

    private fun handleSpeechRecognized(text: String) {
        if (text.isBlank()) {
            if (_isLiveModeOpen.value && _handsFreeLoop.value) {
                startLiveListening()
            }
            return
        }
        _liveSubtitle.value = text
        sendMessage(text, isVoice = true)
    }

    fun sendMessage(text: String, isVoice: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        val sId = _currentSessionId.value
        viewModelScope.launch {
            _isGenerating.value = true
            _voiceOrbState.value = VoiceOrbState.THINKING
            _errorMessage.value = null

            // 1. Insert user message
            val userMsg = ChatMessageEntity(
                sessionId = sId,
                role = "user",
                content = trimmed,
                isVoice = isVoice
            )
            chatDao.insertMessage(userMsg)
            chatDao.updateSessionTimestamp(sId, System.currentTimeMillis())

            // 2. Prepare past context
            val historyEntities = chatDao.getRecentMessages(sId, limit = 12).reversed()
            val historyPairs = historyEntities.map { it.role to it.content }

            // 3. Request Gemini
            val result = geminiRepo.generateResponse(historyPairs, _personaSettings.value)

            result.onSuccess { responseText ->
                val aiMsg = ChatMessageEntity(
                    sessionId = sId,
                    role = "model",
                    content = responseText,
                    isVoice = isVoice
                )
                val newId = chatDao.insertMessage(aiMsg)
                chatDao.updateSessionTimestamp(sId, System.currentTimeMillis())

                _isGenerating.value = false

                // If in live mode or voice message, play via TTS
                if (_isLiveModeOpen.value || isVoice) {
                    _liveSubtitle.value = responseText
                    _speakingMessageId.value = newId
                    _voiceOrbState.value = VoiceOrbState.SPEAKING
                    ttsManager.speak(responseText)
                } else {
                    _voiceOrbState.value = VoiceOrbState.IDLE
                }
            }.onFailure { err ->
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
                val errText = err.message ?: "Wystąpił nieoczekiwany błąd podczas komunikacji z Gemini."
                _errorMessage.value = errText
                _liveSubtitle.value = "Błąd: $errText"
            }
        }
    }

    fun toggleSpeakMessage(message: ChatMessageEntity) {
        if (_speakingMessageId.value == message.id && ttsManager.isSpeaking.value) {
            ttsManager.stop()
            _speakingMessageId.value = null
        } else {
            ttsManager.stop()
            _speakingMessageId.value = message.id
            ttsManager.speak(message.content)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            ttsManager.stop()
            chatDao.clearMessagesForSession(_currentSessionId.value)
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            ttsManager.stop()
            val newId = UUID.randomUUID().toString()
            chatDao.insertSession(
                ChatSessionEntity(
                    sessionId = newId,
                    title = "Rozmowa ${System.currentTimeMillis() % 10000}"
                )
            )
            _currentSessionId.value = newId
        }
    }

    fun switchSession(sessionId: String) {
        ttsManager.stop()
        _currentSessionId.value = sessionId
    }

    // Knowledge source management
    fun addKnowledgeSource(title: String, content: String, type: SourceType) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            knowledgeDao.insertSource(
                KnowledgeSource(
                    title = title.trim(),
                    content = content.trim(),
                    sourceType = type,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleKnowledgeSource(source: KnowledgeSource, enabled: Boolean) {
        viewModelScope.launch {
            knowledgeDao.setSourceEnabled(source.id, enabled)
        }
    }

    fun deleteKnowledgeSource(source: KnowledgeSource) {
        viewModelScope.launch {
            knowledgeDao.deleteSource(source)
        }
    }

    // Learned fact management
    fun addLearnedFact(fact: String, category: FactCategory) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            knowledgeDao.insertFact(
                LearnedFact(
                    fact = fact.trim(),
                    category = category,
                    isActive = true
                )
            )
        }
    }

    fun toggleLearnedFact(fact: LearnedFact, active: Boolean) {
        viewModelScope.launch {
            knowledgeDao.setFactActive(fact.id, active)
        }
    }

    fun deleteLearnedFact(fact: LearnedFact) {
        viewModelScope.launch {
            knowledgeDao.deleteFact(fact)
        }
    }

    // Persona settings updates
    fun updatePersonaSettings(
        humor: Float? = null,
        boldness: Float? = null,
        temperature: Float? = null,
        model: String? = null,
        autoLearn: Boolean? = null
    ) {
        val curr = _personaSettings.value
        _personaSettings.value = curr.copy(
            humorLevel = humor ?: curr.humorLevel,
            boldnessLevel = boldness ?: curr.boldnessLevel,
            temperature = temperature ?: curr.temperature,
            selectedModel = model ?: curr.selectedModel,
            autoLearn = autoLearn ?: curr.autoLearn
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
