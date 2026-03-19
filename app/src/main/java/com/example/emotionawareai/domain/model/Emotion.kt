package com.example.emotionawareai.domain.model

/**
 * Represents the emotional state detected from the user's face or inferred
 * from conversation context.
 */
enum class Emotion(
    val displayName: String,
    val emoji: String,
    val systemPromptHint: String
) {
    NEUTRAL(
        displayName = "Neutral",
        emoji = "😐",
        systemPromptHint = "The user appears calm and neutral."
    ),
    HAPPY(
        displayName = "Happy",
        emoji = "😊",
        systemPromptHint = "The user appears happy and positive."
    ),
    SAD(
        displayName = "Sad",
        emoji = "😢",
        systemPromptHint = "The user appears sad. Be gentle, empathetic, and supportive."
    ),
    ANGRY(
        displayName = "Angry",
        emoji = "😠",
        systemPromptHint = "The user appears frustrated or angry. Stay calm, validate feelings, and de-escalate."
    ),
    SURPRISED(
        displayName = "Surprised",
        emoji = "😲",
        systemPromptHint = "The user appears surprised. Acknowledge the unexpected and be reassuring."
    ),
    FEARFUL(
        displayName = "Fearful",
        emoji = "😨",
        systemPromptHint = "The user appears fearful or anxious. Be calm, grounding, and reassuring."
    ),
    DISGUSTED(
        displayName = "Disgusted",
        emoji = "😒",
        systemPromptHint = "The user appears disgusted or displeased. Acknowledge their feelings without dismissing them."
    ),
    UNKNOWN(
        displayName = "Unknown",
        emoji = "🤔",
        systemPromptHint = "Emotional state is unclear. Respond empathetically."
    );

    companion object {
        fun fromLabel(label: String): Emotion = entries.firstOrNull {
            it.name.equals(label, ignoreCase = true) ||
                it.displayName.equals(label, ignoreCase = true)
        } ?: UNKNOWN
    }
}
