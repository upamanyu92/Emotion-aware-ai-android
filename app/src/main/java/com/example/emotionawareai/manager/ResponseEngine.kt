package com.example.emotionawareai.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.emotionawareai.domain.model.ConversationContext
import com.example.emotionawareai.engine.LLMEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates: LLM inference → token streaming → optional TTS playback.
 *
 * The caller collects the returned [Flow] to receive response tokens as they
 * arrive and to build the full assistant message incrementally.
 */
@Singleton
class ResponseEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmEngine: LLMEngine
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsEnabled = true

    /** True while TTS is actively speaking the assistant response. */
    @Volatile
    private var _isTtsSpeaking = false

    /** Returns true while TTS is actively speaking the assistant response. */
    val isTtsSpeaking: Boolean get() = _isTtsSpeaking

    /**
     * Lazily initialises TextToSpeech on first use.
     */
    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isTtsSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isTtsSpeaking = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isTtsSpeaking = false
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        _isTtsSpeaking = false
                    }
                })
                Log.i(TAG, "TextToSpeech initialized")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed (status=$status)")
            }
        }
    }

    /**
     * Generates a streamed response for the given [context].
     *
     * Returns a [Flow] of token strings. Accumulate them on the collector side
     * to build the full response. TTS speaks the complete response once the
     * flow completes.
     */
    fun generateResponse(context: ConversationContext): Flow<String> {
        val prompt = context.buildPrompt()
        val responseBuffer = StringBuilder()

        return llmEngine.generateResponse(prompt)
            .onEach { token -> responseBuffer.append(token) }
            .onCompletion { cause ->
                if (cause == null && ttsEnabled && responseBuffer.isNotBlank()) {
                    speakResponse(responseBuffer.toString())
                }
            }
    }

    /**
     * Speaks [text] using TTS. Initialises TTS lazily on first call.
     */
    fun speakResponse(text: String) {
        if (!ttsEnabled || text.isBlank()) return
        ensureTts()
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready — skipping speech")
            return
        }
        _isTtsSpeaking = true
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            _isTtsSpeaking = false
            Log.w(TAG, "TTS speak() returned error — resetting isTtsSpeaking")
        }
    }

    fun stopSpeaking() {
        _isTtsSpeaking = false
        tts?.stop()
    }

    fun setTtsEnabled(enabled: Boolean) {
        ttsEnabled = enabled
        if (!enabled) stopSpeaking()
    }

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        llmEngine.loadModel()
    }

    /** Returns `true` if a model is currently loaded and ready for inference. */
    val isModelLoaded: Boolean get() = llmEngine.isLoaded

    /**
     * Copies model data from [inputStream] into the expected on-device location,
     * releases any previously loaded model, and immediately loads the new file.
     *
     * @return `true` if the file was installed and the model loaded successfully.
     */
    suspend fun installAndLoadModel(inputStream: java.io.InputStream): Boolean =
        withContext(Dispatchers.IO) {
            llmEngine.installAndLoadModel(inputStream)
        }

    /**
     * Returns `true` if the model file is present on-disk (without loading it).
     * Delegates to [LLMEngine.isModelFileAvailable].
     */
    fun isModelFileAvailable(): Boolean = llmEngine.isModelFileAvailable()

    /**
     * The expected on-device path for the model file.
     * Expose this to the user for setup instructions.
     */
    fun modelFilePath(): String = llmEngine.modelFilePath()

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        llmEngine.release()
        Log.i(TAG, "ResponseEngine released")
    }

    companion object {
        private const val TAG = "ResponseEngine"
        private const val UTTERANCE_ID = "emotion_aware_utterance"
    }
}
