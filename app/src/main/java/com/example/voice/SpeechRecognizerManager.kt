package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorRecovery: (() -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(locale: Locale = Locale.forLanguageTag("pl-PL")) {
        mainHandler.post {
            internalStartListening(locale)
        }
    }

    private fun internalStartListening(locale: Locale) {
        internalStopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Rozpoznawanie mowy nie jest dostępne na tym urządzeniu."
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                        _errorMessage.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                        _rmsLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nie rozpoznano mowy."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Przekroczono limit czasu oczekiwania na głos."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Błąd sieci podczas rozpoznawania mowy."
                            SpeechRecognizer.ERROR_AUDIO -> "Błąd nagrywania dźwięku."
                            SpeechRecognizer.ERROR_CLIENT -> "Operacja anulowana."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Brak uprawnień mikrofonu."
                            else -> "Błąd rozpoznawania mowy (kod: $error)"
                        }
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && 
                            error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT && 
                            error != SpeechRecognizer.ERROR_CLIENT) {
                            _errorMessage.value = msg
                        }
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onErrorRecovery?.invoke()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _partialText.value = text
                            onResult(text)
                        } else {
                            onErrorRecovery?.invoke()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _partialText.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = "Nie udało się uruchomić mikrofonu: ${e.message}"
            Log.e("SpeechRecognizer", "startListening error", e)
        }
    }

    fun stopListening() {
        mainHandler.post {
            internalStopListening()
        }
    }

    private fun internalStopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("SpeechRecognizer", "destroy error", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }
}
