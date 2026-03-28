package com.example.emotionawareai.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTtsBackend @Inject constructor(
    @ApplicationContext private val context: Context
) : AssistantTtsBackend {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var voiceProfile: TtsVoiceProfile = TtsVoiceProfile.DEFAULT

    @Volatile
    override var isSpeaking: Boolean = false
        private set

    override fun setVoiceProfile(profile: TtsVoiceProfile) {
        voiceProfile = profile
        applyVoiceProfile(profile)
    }

    override fun setPiperVoice(voice: PiperVoice) = Unit

    override fun isReady(): Boolean {
        ensureTts()
        return ready
    }

    override fun speak(text: String): Boolean {
        if (text.isBlank()) return false
        ensureTts()
        if (!ready) return false
        isSpeaking = true
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            isSpeaking = false
            return false
        }
        return true
    }

    override fun stop() {
        isSpeaking = false
        tts?.stop()
    }

    override fun release() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        isSpeaking = false
                    }
                })
                applyVoiceProfile(voiceProfile)
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status=$status")
            }
        }
    }

    private fun applyVoiceProfile(profile: TtsVoiceProfile) {
        val engine = tts ?: return
        if (!ready) return

        engine.setPitch(profile.pitch)
        engine.setSpeechRate(profile.speechRate)

        val hint = profile.genderHint
        if (hint != null) {
            val locale = Locale.getDefault()
            val match = engine.voices
                ?.filter { voice -> !voice.isNetworkConnectionRequired && voice.locale.language == locale.language }
                ?.firstOrNull { voice -> voice.name.contains(hint, ignoreCase = true) }
            if (match != null) {
                engine.voice = match
            }
        }
    }

    companion object {
        private const val TAG = "SystemTtsBackend"
        private const val UTTERANCE_ID = "emotion_aware_utterance"
    }
}
