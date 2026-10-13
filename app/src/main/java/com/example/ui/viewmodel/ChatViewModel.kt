package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.FactCategory
import com.example.data.model.AuroraMode
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import com.example.data.model.SourceType
import com.example.data.remote.ApiDiagnosticResult
import com.example.data.remote.ApiDiagnosticStatus
import com.example.data.remote.GeminiRepository
import com.example.data.remote.PersonaSettings
import com.example.data.repository.ChatRepository
import com.example.data.repository.KnowledgeSourceRepository
import com.example.ui.components.VoiceOrbState
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    val chatRepository = ChatRepository(chatDao)
    private val knowledgeDao = database.knowledgeDao()
    private val knowledgeSourceDao = database.knowledgeSourceDao()
    val knowledgeSourceRepository = KnowledgeSourceRepository(knowledgeSourceDao)
    private val geminiRepo = GeminiRepository(knowledgeDao)

    val ttsManager = TextToSpeechManager(application)
    private lateinit var speechManager: SpeechRecognizerManager

    // Active session
    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { id ->
        chatRepository.getMessagesForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeSources: StateFlow<List<KnowledgeSource>> = knowledgeSourceRepository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedFacts: StateFlow<List<LearnedFact>> = knowledgeDao.getAllFacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportMemories: StateFlow<List<LearnedFact>> = knowledgeDao.getFactsByCategoryFlow(FactCategory.SUPPORT_MEMORY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active mode: Free Mode (default) vs Support Mode
    private val _activeMode = MutableStateFlow(AuroraMode.FREE)
    val activeMode: StateFlow<AuroraMode> = _activeMode.asStateFlow()

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

    private val _apiDiagnosticResult = MutableStateFlow<ApiDiagnosticResult?>(null)
    val apiDiagnosticResult: StateFlow<ApiDiagnosticResult?> = _apiDiagnosticResult.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

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

        // Listen to speech manager errors (e.g. microphone permission denied)
        viewModelScope.launch {
            speechManager.errorMessage.collect { err ->
                err?.let {
                    _errorMessage.value = it
                    _voiceOrbState.value = VoiceOrbState.IDLE
                    if (_isLiveModeOpen.value) {
                        _liveSubtitle.value = it
                    }
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

            // Ensure current session exists or select existing one
            val existingSessions = chatRepository.allSessions.first()
            if (existingSessions.isNotEmpty()) {
                _currentSessionId.value = existingSessions.first().sessionId
            } else {
                chatRepository.insertSession(
                    ChatSessionEntity(
                        sessionId = _currentSessionId.value,
                        title = "Rozmowa z Aurora AI"
                    )
                )
            }
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
            chatRepository.insertMessage(userMsg)
            chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())

            // Update session title on first message if generic
            val currentSession = chatRepository.getSession(sId)
            if (currentSession != null && (currentSession.title.startsWith("Rozmowa") || currentSession.title == "Nowa rozmowa")) {
                val cleanTitle = trimmed.take(28).let { if (trimmed.length > 28) "$it..." else it }
                chatRepository.updateSessionTitle(sId, cleanTitle)
            }

            // 2. Prepare past context
            val historyEntities = chatRepository.getRecentMessages(sId, limit = 12).reversed()
            val historyPairs = historyEntities.map { it.role to it.content }

            // 3. Request Gemini
            val result = geminiRepo.generateResponse(historyPairs, _personaSettings.value, _activeMode.value)

            result.onSuccess { responseText ->
                val aiMsg = ChatMessageEntity(
                    sessionId = sId,
                    role = "model",
                    content = responseText,
                    isVoice = isVoice
                )
                val newId = chatRepository.insertMessage(aiMsg)
                chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())

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
            chatRepository.clearMessagesForSession(_currentSessionId.value)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            if (_speakingMessageId.value == messageId) {
                ttsManager.stop()
                _speakingMessageId.value = null
            }
            chatRepository.deleteMessageById(messageId)
        }
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        val currentMsgs = messages.value
        val lastUserMsg = currentMsgs.lastOrNull { it.role == "user" } ?: return
        viewModelScope.launch {
            ttsManager.stop()
            _speakingMessageId.value = null
            val lastMsg = currentMsgs.lastOrNull()
            if (lastMsg?.role == "model") {
                chatRepository.deleteMessageById(lastMsg.id)
            }

            val sId = _currentSessionId.value
            _isGenerating.value = true
            _voiceOrbState.value = VoiceOrbState.THINKING
            _errorMessage.value = null

            val historyEntities = chatRepository.getRecentMessages(sId, limit = 12).reversed()
            val historyPairs = historyEntities.map { it.role to it.content }

            val result = geminiRepo.generateResponse(historyPairs, _personaSettings.value, _activeMode.value)
            result.onSuccess { responseText ->
                val aiMsg = ChatMessageEntity(
                    sessionId = sId,
                    role = "model",
                    content = responseText,
                    isVoice = false
                )
                chatRepository.insertMessage(aiMsg)
                chatRepository.updateSessionTimestamp(sId, System.currentTimeMillis())
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
            }.onFailure { err ->
                _isGenerating.value = false
                _voiceOrbState.value = VoiceOrbState.IDLE
                val errText = err.message ?: "Wystąpił błąd podczas regenerowania odpowiedzi."
                _errorMessage.value = errText
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            ttsManager.stop()
            val newId = UUID.randomUUID().toString()
            chatRepository.insertSession(
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

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            ttsManager.stop()
            chatRepository.deleteSessionWithMessages(sessionId)
            val remaining = chatRepository.allSessions.first()
            if (_currentSessionId.value == sessionId) {
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().sessionId
                } else {
                    startNewSession()
                }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            chatRepository.updateSessionTitle(sessionId, newTitle.trim())
        }
    }

    // Knowledge source management (CRUD)
    fun addKnowledgeSource(title: String, content: String, type: SourceType) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            knowledgeSourceRepository.insertSource(
                KnowledgeSource(
                    title = title.trim(),
                    content = content.trim(),
                    sourceType = type,
                    isEnabled = true
                )
            )
        }
    }

    fun updateKnowledgeSource(id: Long, title: String, content: String, type: SourceType) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            knowledgeSourceRepository.updateSourceContent(
                id = id,
                title = title.trim(),
                content = content.trim(),
                sourceType = type
            )
        }
    }

    fun toggleKnowledgeSource(source: KnowledgeSource, enabled: Boolean) {
        viewModelScope.launch {
            knowledgeSourceRepository.setSourceEnabled(source.id, enabled)
        }
    }

    fun deleteKnowledgeSource(source: KnowledgeSource) {
        viewModelScope.launch {
            knowledgeSourceRepository.deleteSource(source)
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

    fun runApiDiagnostics() {
        viewModelScope.launch {
            _isTestingApi.value = true
            try {
                val result = geminiRepo.testConnection(_personaSettings.value.selectedModel)
                _apiDiagnosticResult.value = result
            } catch (e: Exception) {
                _apiDiagnosticResult.value = ApiDiagnosticResult(
                    isSuccess = false,
                    status = ApiDiagnosticStatus.NETWORK_ERROR,
                    httpCode = null,
                    modelTested = _personaSettings.value.selectedModel,
                    latencyMs = 0L,
                    headline = "Błąd wykonania diagnostyki",
                    details = e.localizedMessage ?: "Wystąpił nieoczekiwany błąd"
                )
            } finally {
                _isTestingApi.value = false
            }
        }
    }

    fun clearApiDiagnostics() {
        _apiDiagnosticResult.value = null
    }

    fun setMode(mode: AuroraMode) {
        _activeMode.value = mode
    }

    fun toggleMode() {
        _activeMode.value = if (_activeMode.value == AuroraMode.FREE) AuroraMode.SUPPORT else AuroraMode.FREE
    }

    fun saveSupportMemoryWithConsent(memoryText: String) {
        if (memoryText.isBlank()) return
        viewModelScope.launch {
            knowledgeDao.insertFact(
                LearnedFact(
                    fact = memoryText.trim(),
                    category = FactCategory.SUPPORT_MEMORY,
                    isActive = true
                )
            )
        }
    }

    fun clearAllSupportMemories() {
        viewModelScope.launch {
            knowledgeDao.clearFactsByCategory(FactCategory.SUPPORT_MEMORY)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
