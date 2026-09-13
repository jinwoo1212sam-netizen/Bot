package com.ultron.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Listening : RecognitionState()
    object Processing : RecognitionState()
    data class Result(val text: String) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    fun startListening(languageTag: String = "hi-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = RecognitionState.Error("Speech recognition is unavailable on this device")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_COMPLETE_PATH, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            _state.value = RecognitionState.Listening
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _state.value = RecognitionState.Error(e.localizedMessage ?: "Unknown speech error")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
            _state.value = RecognitionState.Idle
            _audioRms.value = 0f
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = RecognitionState.Listening
        }

        override fun onBeginningOfSpeech() {
            _state.value = RecognitionState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize RMS from around -2..10 to 0..1
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioRms.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = RecognitionState.Processing
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                else -> "Speech error code $error"
            }
            _state.value = RecognitionState.Error(message)
            _audioRms.value = 0f
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrEmpty()) {
                _state.value = RecognitionState.Result(recognizedText)
                onResult(recognizedText)
            } else {
                _state.value = RecognitionState.Idle
            }
            _audioRms.value = 0f
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()
            if (!partial.isNullOrEmpty()) {
                // Keep listening with state
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        private const val TAG = "SpeechRecognizerMgr"
    }
}
