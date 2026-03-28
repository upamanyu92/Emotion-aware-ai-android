package com.example.emotionawareai.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.emotionawareai.BuildConfig
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SherpaOnnxTtsBackend @Inject constructor(
    private val piperVoiceManager: PiperVoiceManager
) : AssistantTtsBackend {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engineMutex = Mutex()

    @Volatile
    private var selectedVoice: PiperVoice = PiperVoice.ALAN

    @Volatile
    private var synthesisJob: Job? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var offlineTts: OfflineTts? = null

    @Volatile
    override var isSpeaking: Boolean = false
        private set

    override fun setVoiceProfile(profile: TtsVoiceProfile) = Unit

    override fun setPiperVoice(voice: PiperVoice) {
        if (selectedVoice == voice) return
        selectedVoice = voice
        runBlocking {
            engineMutex.withLock {
                offlineTts?.release()
                offlineTts = null
            }
        }
    }

    override fun isReady(): Boolean = piperVoiceManager.resolveInstalledVoice(selectedVoice) != null

    override fun speak(text: String): Boolean {
        if (text.isBlank()) return false
        val installed = piperVoiceManager.resolveInstalledVoice(selectedVoice) ?: return false
        synthesisJob?.cancel()
        stopTrack()
        synthesisJob = scope.launch {
            try {
                val tts = engineMutex.withLock {
                    offlineTts ?: createEngine(installed).also { offlineTts = it }
                }
                val audio = tts.generate(text, 0, 1.0f)
                playAudio(audio.samples, audio.sampleRate)
            } catch (e: Exception) {
                Log.e(TAG, "Neural TTS synthesis failed", e)
                isSpeaking = false
                stopTrack()
            }
        }
        return true
    }

    override fun stop() {
        isSpeaking = false
        synthesisJob?.cancel()
        synthesisJob = null
        stopTrack()
    }

    override fun release() {
        stop()
        runBlocking {
            engineMutex.withLock {
                offlineTts?.release()
                offlineTts = null
            }
        }
    }

    private fun createEngine(installed: PiperVoiceManager.InstalledVoice): OfflineTts {
        val vitsConfig = OfflineTtsVitsModelConfig.builder()
            .setModel(installed.modelPath)
            .setTokens(installed.tokensPath)
            .setDataDir(installed.dataDir)
            .build()
        val modelConfig = OfflineTtsModelConfig.builder()
            .setVits(vitsConfig)
            .setNumThreads(2)
            .setDebug(BuildConfig.DEBUG)
            .build()
        val config = OfflineTtsConfig.builder()
            .setModel(modelConfig)
            .build()
        return OfflineTts(config)
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        val pcm = samples.toPcm16()
        if (pcm.isEmpty()) {
            isSpeaking = false
            return
        }

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val track = AudioTrack(
            attributes,
            format,
            pcm.size,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack = track
        isSpeaking = true
        track.write(pcm, 0, pcm.size)
        track.notificationMarkerPosition = samples.size
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                isSpeaking = false
                stopTrack()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
        track.play()
    }

    private fun stopTrack() {
        audioTrack?.runCatching {
            pause()
            flush()
            release()
        }
        audioTrack = null
    }

    private fun FloatArray.toPcm16(): ByteArray {
        val output = ByteArray(size * 2)
        forEachIndexed { index, sample ->
            val clamped = sample.coerceIn(-1f, 1f)
            val shortValue = (clamped * Short.MAX_VALUE).toInt().toShort()
            output[index * 2] = (shortValue.toInt() and 0xFF).toByte()
            output[index * 2 + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
        }
        return output
    }

    companion object {
        private const val TAG = "SherpaOnnxTtsBackend"
    }
}
