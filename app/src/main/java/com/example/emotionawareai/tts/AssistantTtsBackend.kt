package com.example.emotionawareai.tts

import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsVoiceProfile

interface AssistantTtsBackend {
    val isSpeaking: Boolean

    fun setVoiceProfile(profile: TtsVoiceProfile)

    fun setPiperVoice(voice: PiperVoice)

    fun isReady(): Boolean

    fun speak(text: String): Boolean

    fun stop()

    fun release()
}
