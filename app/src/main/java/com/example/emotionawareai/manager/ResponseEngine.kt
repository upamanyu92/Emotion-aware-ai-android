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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ttsMonitorJob: Job? = null

    private var ttsEnabled = true
    private var voiceProfile: TtsVoiceProfile = TtsVoiceProfile.DEFAULT
    private var ttsBackend: TtsBackend = TtsBackend.SYSTEM
    private var piperVoice: PiperVoice = PiperVoice.ALAN
    private var ttsFallbackListener: ((String) -> Unit)? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

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

    suspend fun isPiperVoiceReady(voice: PiperVoice = piperVoice): Boolean =
        withContext(Dispatchers.IO) {
            sherpaOnnxTtsBackend.validateVoice(voice)
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
        if (success) {
            startSpeakingMonitor(backend)
            return
        }

        if (backend !== systemTtsBackend) {
            val fallbackMessage = "Neural voice unavailable - using Android system TTS instead."
            Log.w(TAG, fallbackMessage)
            ttsFallbackListener?.invoke(fallbackMessage)
            systemTtsBackend.setVoiceProfile(voiceProfile)
            if (systemTtsBackend.speak(text)) {
                startSpeakingMonitor(systemTtsBackend)
            }
        }
    }

    fun stopSpeaking() {
        ttsMonitorJob?.cancel()
        ttsMonitorJob = null
        _isSpeaking.value = false
        systemTtsBackend.stop()
        sherpaOnnxTtsBackend.stop()
    }

    fun setTtsEnabled(enabled: Boolean) {
        ttsEnabled = enabled
        if (!enabled) stopSpeaking()
    }

    suspend fun loadModel(modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE): Boolean =
        withContext(Dispatchers.IO) {
            llmEngine.loadModel(modelFileName)
        }

    /** Returns `true` if a model is currently loaded and ready for inference. */
    val isModelLoaded: Boolean get() = llmEngine.isLoaded

    /**
     * Copies model data from [inputStream] into the expected on-device location,
     * releases any previously loaded model, and immediately loads the new file.
     *
     * @return `true` if the file was installed and the model loaded successfully.
     */
    suspend fun installAndLoadModel(
        inputStream: java.io.InputStream,
        modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE
    ): Boolean = withContext(Dispatchers.IO) {
        llmEngine.installAndLoadModel(inputStream, modelFileName)
    }

    /**
     * Returns `true` if the model file is present on-disk (without loading it).
     * Delegates to [LLMEngine.isModelFileAvailable].
     */
    fun isModelFileAvailable(modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE): Boolean =
        llmEngine.isModelFileAvailable(modelFileName)

    /**
     * The expected on-device path for the model file.
     * Expose this to the user for setup instructions.
     */
    fun modelFilePath(modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE): String =
        llmEngine.modelFilePath(modelFileName)

    fun release() {
        stopSpeaking()
        systemTtsBackend.release()
        sherpaOnnxTtsBackend.release()
        llmEngine.release()
        scope.cancel()
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

    private fun startSpeakingMonitor(backend: AssistantTtsBackend) {
        ttsMonitorJob?.cancel()
        _isSpeaking.value = true
        ttsMonitorJob = scope.launch {
            while (backend.isSpeaking) {
                delay(TTS_MONITOR_INTERVAL_MS)
            }
            _isSpeaking.value = false
        }
    }

    companion object {
        private const val TAG = "ResponseEngine"
        private const val TTS_MONITOR_INTERVAL_MS = 80L
    }
}
