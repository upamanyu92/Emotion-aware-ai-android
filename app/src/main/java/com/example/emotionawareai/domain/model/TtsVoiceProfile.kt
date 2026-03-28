package com.example.emotionawareai.domain.model

/**
 * Describes a TTS voice personality.
 *
 * @property displayName Human-readable label shown in the settings UI.
 * @property pitch       Multiplier for TTS engine pitch (1.0 = default).
 * @property speechRate  Multiplier for TTS engine speech rate (1.0 = default).
 * @property genderHint  Optional hint used to search available voices by name
 *                       (e.g. "female", "male"). Null means no gender preference.
 */
enum class TtsVoiceProfile(
    val displayName: String,
    val pitch: Float,
    val speechRate: Float,
    val genderHint: String?
) {
    /** System default voice – no pitch/rate override. */
    DEFAULT("Default", 1.0f, 1.0f, null),

    /** Deeper, calm voice simulating an adult male speaker. */
    MALE("Male", 0.85f, 0.95f, "male"),

    /** Slightly higher-pitched, warm voice simulating an adult female speaker. */
    FEMALE("Female", 1.15f, 1.0f, "female"),

    /** High-pitched, faster voice simulating a child speaker. */
    KID("Kid", 1.6f, 1.15f, null);

    companion object {
        /** Returns the profile matching [name], falling back to [DEFAULT]. */
        fun fromName(name: String): TtsVoiceProfile =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
