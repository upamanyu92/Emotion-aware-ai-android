package com.example.emotionawareai.domain.model

/**
 * Curated Piper voices that can be downloaded for the neural TTS backend.
 */
enum class PiperVoice(
    val displayName: String,
    val localeLabel: String,
    val description: String,
    val archiveName: String
) {
    ALAN(
        displayName = "Alan",
        localeLabel = "English (UK)",
        description = "Warm, natural British male voice",
        archiveName = "vits-piper-en_GB-alan-low.tar.bz2"
    ),
    AMY(
        displayName = "Amy",
        localeLabel = "English (US)",
        description = "Friendly, expressive American female voice",
        archiveName = "vits-piper-en_US-amy-low.tar.bz2"
    );

    val downloadUrl: String
        get() = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$archiveName"

    companion object {
        fun fromName(name: String): PiperVoice =
            entries.firstOrNull { it.name == name } ?: ALAN
    }
}
