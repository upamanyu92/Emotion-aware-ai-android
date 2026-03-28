package com.example.emotionawareai.domain.model

/**
 * Selects which on-device speech synthesizer should be used for assistant output.
 */
enum class TtsBackend(
    val displayName: String,
    val subtitle: String
) {
    SYSTEM(
        displayName = "System",
        subtitle = "Android TextToSpeech with tiny footprint"
    ),
    SHERPA_PIPER(
        displayName = "Neural",
        subtitle = "Sherpa-ONNX + Piper voice model (fully offline)"
    );

    companion object {
        fun fromName(name: String): TtsBackend =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
