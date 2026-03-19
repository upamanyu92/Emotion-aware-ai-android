package com.example.emotionawareai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's [SpeechRecognizer] in a coroutine-friendly interface.
 *
 * Emits recognised text via [recognizedTextFlow] and errors via [errorFlow].
 * The caller is responsible for holding RECORD_AUDIO permission before calling
 * [startListening].
 */
@Singleton
class VoiceProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _recognizedTextFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val recognizedTextFlow: SharedFlow<String> = _recognizedTextFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<VoiceError>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errorFlow: SharedFlow<VoiceError> = _errorFlow.asSharedFlow()

    private val _listeningStateFlow = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val listeningStateFlow: SharedFlow<Boolean> = _listeningStateFlow.asSharedFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _listeningStateFlow.tryEmit(true)
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech begun")
        }

        override fun onRmsChanged(rmsdB: Float) { /* ignored */ }

        override fun onBufferReceived(buffer: ByteArray?) { /* ignored */ }

        override fun onEndOfSpeech() {
            _listeningStateFlow.tryEmit(false)
            Log.d(TAG, "Speech ended")
        }

        override fun onError(error: Int) {
            _listeningStateFlow.tryEmit(false)
            val voiceError = VoiceError.fromCode(error)
            Log.w(TAG, "Recognition error: $voiceError (code=$error)")
            _errorFlow.tryEmit(voiceError)
        }

        override fun onResults(results: Bundle?) {
            _listeningStateFlow.tryEmit(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topResult = matches?.firstOrNull()
            if (!topResult.isNullOrBlank()) {
                Log.d(TAG, "Recognised: $topResult")
                _recognizedTextFlow.tryEmit(topResult)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                Log.d(TAG, "Partial: $partial")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) { /* ignored */ }
    }

    /**
     * Starts listening for speech. Must be called from the main thread.
     * No-op if already listening or if SpeechRecognizer is unavailable.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun startListening(locale: Locale = Locale.getDefault()) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            _errorFlow.tryEmit(VoiceError.NOT_AVAILABLE)
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        speechRecognizer?.startListening(intent)
        Log.i(TAG, "Started listening")
    }

    /**
     * Stops the current recognition session.
     */
    fun stopListening() {
        speechRecognizer?.let {
            it.stopListening()
            it.destroy()
            Log.i(TAG, "Stopped listening")
        }
        speechRecognizer = null
        _listeningStateFlow.tryEmit(false)
    }

    fun release() {
        stopListening()
    }

    companion object {
        private const val TAG = "VoiceProcessor"
    }
}

enum class VoiceError(val message: String) {
    AUDIO_ERROR("Audio recording error"),
    CLIENT_ERROR("Client-side error"),
    INSUFFICIENT_PERMISSIONS("Insufficient permissions"),
    NETWORK_ERROR("Network error"),
    NO_MATCH("No speech recognised"),
    RECOGNIZER_BUSY("Recogniser busy"),
    SERVER_ERROR("Server error"),
    SPEECH_TIMEOUT("No speech detected"),
    NOT_AVAILABLE("Speech recognition not available");

    companion object {
        fun fromCode(code: Int): VoiceError = when (code) {
            SpeechRecognizer.ERROR_AUDIO                -> AUDIO_ERROR
            SpeechRecognizer.ERROR_CLIENT               -> CLIENT_ERROR
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> INSUFFICIENT_PERMISSIONS
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT      -> NETWORK_ERROR
            SpeechRecognizer.ERROR_NO_MATCH             -> NO_MATCH
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY      -> RECOGNIZER_BUSY
            SpeechRecognizer.ERROR_SERVER               -> SERVER_ERROR
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT       -> SPEECH_TIMEOUT
            else                                        -> CLIENT_ERROR
        }
    }
}
