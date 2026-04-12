package com.example.emotionawareai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
 *
 * When [isContinuousMode] is true the recognizer automatically restarts after
 * each result or benign error (timeout / no-match) so the microphone stays
 * live without the user having to press a button.
 */
@Singleton
class VoiceProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var activeLocale: Locale = Locale.getDefault()

    /** When true the recognizer restarts automatically after each session. */
    @Volatile
    var isContinuousMode: Boolean = false
        private set

    /** Prevents re-entrancy during destroy/recreate cycles. */
    @Volatile
    private var isRestarting = false

    /** Last known recognizer readiness state (set by recognition callbacks). */
    @Volatile
    private var isRecognizerActive = false

    @Volatile
    private var lastStartRequestElapsedMs = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

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

    private val _rmsFlow = MutableSharedFlow<Float>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val rmsFlow: SharedFlow<Float> = _rmsFlow.asSharedFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isRestarting = false
            isRecognizerActive = true
            _listeningStateFlow.tryEmit(true)
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech begun")
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsFlow.tryEmit(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) { /* ignored */ }

        override fun onEndOfSpeech() {
            isRecognizerActive = false
            _listeningStateFlow.tryEmit(false)
            Log.d(TAG, "Speech ended")
        }

        override fun onError(error: Int) {
            isRecognizerActive = false
            _listeningStateFlow.tryEmit(false)
            val voiceError = VoiceError.fromCode(error)

            // During continuous-mode recovery Android may emit a transient client error
            // from the recognizer instance that is being replaced. Retry quietly instead
            // of surfacing a user-visible failure for that expected hand-off.
            if (isContinuousMode && voiceError.shouldSilentlyRecoverInContinuousMode(isRestarting)) {
                Log.w(TAG, "Recognition error recovered in continuous mode: $voiceError (code=$error)")
                isRestarting = false
                val delayMs = if (voiceError == VoiceError.CLIENT_ERROR) {
                    CLIENT_ERROR_RESTART_DELAY_MS
                } else if (voiceError == VoiceError.RECOGNIZER_BUSY) {
                    BUSY_ERROR_RESTART_DELAY_MS
                } else {
                    RESTART_DELAY_MS
                }
                scheduleRestart(delayMs)
                return
            }

            isRestarting = false
            Log.w(TAG, "Recognition error: $voiceError (code=$error)")
            _errorFlow.tryEmit(voiceError)
        }

        override fun onResults(results: Bundle?) {
            isRestarting = false
            isRecognizerActive = false
            _listeningStateFlow.tryEmit(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topResult = matches?.firstOrNull()
            if (!topResult.isNullOrBlank()) {
                Log.d(TAG, "Recognised: $topResult")
                _recognizedTextFlow.tryEmit(topResult)
            }
            // In continuous mode, automatically restart after delivering results.
            // The ViewModel will call startListening() again once it has finished
            // generating a response, so we only restart here when NOT generating.
            if (isContinuousMode && topResult.isNullOrBlank()) {
                // No text recognised — restart immediately to keep mic live
                scheduleRestart()
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
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startListeningInternal(locale, forceRestart = false)
        } else {
            mainHandler.post { startListeningInternal(locale, forceRestart = false) }
        }
    }

    /**
     * Starts listening in continuous mode: the mic stays live by automatically
     * restarting after each result or benign error until [stopContinuousListening]
     * is called.
     */
    fun startContinuousListening(locale: Locale = Locale.getDefault()) {
        isContinuousMode = true
        startListening(locale)
    }

    /**
     * Stops continuous listening and silences auto-restarts.
     */
    fun stopContinuousListening() {
        isContinuousMode = false
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        stopListening()
    }

    /**
     * Stops the current recognition session. Does not affect [isContinuousMode].
     */
    fun stopListening() {
        isRestarting = false
        isRecognizerActive = false
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        destroyCurrentRecognizer()
        _listeningStateFlow.tryEmit(false)
    }

    fun release() {
        isContinuousMode = false
        isRestarting = false
        isRecognizerActive = false
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        destroyCurrentRecognizer()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun destroyCurrentRecognizer() {
        speechRecognizer?.let {
            runCatching {
                it.stopListening()
                it.destroy()
            }
            Log.d(TAG, "SpeechRecognizer destroyed")
        }
        speechRecognizer = null
    }

    private fun buildRecognitionIntent(locale: Locale): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

    /**
     * Posts a restart runnable on the main thread after a short delay.
     * Only one restart is ever queued at a time. [isRestarting] remains true
     * until the replacement recognizer becomes ready or fails again, which
     * lets us suppress the transient client error emitted during recognizer
     * hand-off on some devices.
     */
    private fun scheduleRestart(delayMs: Long = RESTART_DELAY_MS) {
        if (!isContinuousMode || isRestarting) return
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        isRestarting = true
        mainHandler.postDelayed({
            if (!isContinuousMode) {
                isRestarting = false
            } else {
                startListeningInternal(activeLocale, forceRestart = true)
            }
        }, RESTART_TOKEN, delayMs)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startListeningInternal(locale: Locale, forceRestart: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val sameLocale = activeLocale == locale
        if (!forceRestart && sameLocale && speechRecognizer != null && (isRecognizerActive || isRestarting)) {
            Log.d(TAG, "Ignoring duplicate start request while recognizer is already active")
            return
        }
        if (!forceRestart && now - lastStartRequestElapsedMs < START_DEBOUNCE_MS) {
            Log.d(TAG, "Ignoring rapid start request (${now - lastStartRequestElapsedMs}ms since previous)")
            return
        }

        lastStartRequestElapsedMs = now
        activeLocale = locale
        mainHandler.removeCallbacksAndMessages(RESTART_TOKEN)
        isRecognizerActive = false
        destroyCurrentRecognizer()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = buildRecognitionIntent(locale)
        speechRecognizer?.startListening(intent)
        Log.i(TAG, "Started listening (continuous=$isContinuousMode, forced=$forceRestart)")
    }

    companion object {
        private const val TAG = "VoiceProcessor"
        private const val RESTART_DELAY_MS = 300L
        private const val CLIENT_ERROR_RESTART_DELAY_MS = 600L
        private const val BUSY_ERROR_RESTART_DELAY_MS = 900L
        private const val START_DEBOUNCE_MS = 450L
        private val RESTART_TOKEN = Any()
    }
}

/** Returns true for errors that should silently trigger a restart in continuous mode. */
internal fun VoiceError.shouldSilentlyRecoverInContinuousMode(isRestarting: Boolean): Boolean =
    this == VoiceError.NO_MATCH ||
        this == VoiceError.SPEECH_TIMEOUT ||
        this == VoiceError.CLIENT_ERROR ||
        this == VoiceError.RECOGNIZER_BUSY

val VoiceError.isBenignForContinuousMode: Boolean
    get() = this == VoiceError.NO_MATCH ||
        this == VoiceError.SPEECH_TIMEOUT ||
        this == VoiceError.CLIENT_ERROR ||
        this == VoiceError.RECOGNIZER_BUSY

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
