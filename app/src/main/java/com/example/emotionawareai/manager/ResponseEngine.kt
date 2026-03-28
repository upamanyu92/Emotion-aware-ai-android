package com.example.emotionawareai.manager

import android.util.Log
import com.example.emotionawareai.domain.model.ConversationContext
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import com.example.emotionawareai.engine.LLMEngine
import com.example.emotionawareai.tts.AssistantTtsBackend
import com.example.emotionawareai.tts.SherpaOnnxTtsBackend
import com.example.emotionawareai.tts.SystemTtsBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
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
    private val llmEngine: LLMEngine,
    private val systemTtsBackend: SystemTtsBackend,
    private val sherpaOnnxTtsBackend: SherpaOnnxTtsBackend
) {
    private var ttsEnabled = true
    private var voiceProfile: TtsVoiceProfile = TtsVoiceProfile.DEFAULT
    private var ttsBackend: TtsBackend = TtsBackend.SYSTEM
    private var piperVoice: PiperVoice = PiperVoice.ALAN
    private var ttsFallbackListener: ((String) -> Unit)? = null

    /** Returns true while any TTS backend is actively speaking the response. */
    val isTtsSpeaking: Boolean
        get() = systemTtsBackend.isSpeaking || sherpaOnnxTtsBackend.isSpeaking

    fun setVoiceProfile(profile: TtsVoiceProfile) {
        voiceProfile = profile
        systemTtsBackend.setVoiceProfile(profile)
    }

    fun setTtsBackend(backend: TtsBackend) {
        if (ttsBackend == backend) return
        stopSpeaking()
        ttsBackend = backend
    }

    fun setPiperVoice(voice: PiperVoice) {
        piperVoice = voice
        sherpaOnnxTtsBackend.setPiperVoice(voice)
    }

    fun setTtsFallbackListener(listener: ((String) -> Unit)?) {
        ttsFallbackListener = listener
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

    /** Speaks [text] using the selected backend, with system-TTS fallback. */
    fun speakResponse(text: String) {
        if (!ttsEnabled || text.isBlank()) return
        val backend = activeBackend()
        val success = backend.speak(text)
        if (!success && backend !== systemTtsBackend) {
            val fallbackMessage = "Neural voice unavailable — using Android system TTS instead."
            Log.w(TAG, fallbackMessage)
            ttsFallbackListener?.invoke(fallbackMessage)
            systemTtsBackend.setVoiceProfile(voiceProfile)
            systemTtsBackend.speak(text)
        }
    }

    fun stopSpeaking() {
        systemTtsBackend.stop()
        sherpaOnnxTtsBackend.stop()
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
        systemTtsBackend.release()
        sherpaOnnxTtsBackend.release()
        llmEngine.release()
        Log.i(TAG, "ResponseEngine released")
    }

    private fun activeBackend(): AssistantTtsBackend {
        if (ttsBackend == TtsBackend.SHERPA_PIPER) {
            sherpaOnnxTtsBackend.setPiperVoice(piperVoice)
            if (sherpaOnnxTtsBackend.isReady()) {
                return sherpaOnnxTtsBackend
            }
        }
        systemTtsBackend.setVoiceProfile(voiceProfile)
        return systemTtsBackend
    }

    companion object {
        private const val TAG = "ResponseEngine"
    }
}
