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
        systemPromptHint = "The user appears calm. Create space for them to explore what's on their mind."
    ),
    HAPPY(
        displayName = "Happy",
        emoji = "😊",
        systemPromptHint = "The user appears happy and positive. Celebrate this with them and explore what's working well."
    ),
    SAD(
        displayName = "Sad",
        emoji = "😢",
        systemPromptHint = "The user appears sad. Lead with empathy, validate their feelings, and hold space before offering perspectives."
    ),
    ANGRY(
        displayName = "Angry",
        emoji = "😠",
        systemPromptHint = "The user appears frustrated or angry. Acknowledge and validate their feelings first; don't rush to solutions."
    ),
    SURPRISED(
        displayName = "Surprised",
        emoji = "😲",
        systemPromptHint = "The user appears surprised. Help them process the unexpected with curiosity rather than anxiety."
    ),
    FEARFUL(
        displayName = "Fearful",
        emoji = "😨",
        systemPromptHint = "The user appears anxious or fearful. Offer grounding, reassurance, and calm presence."
    ),
    DISGUSTED(
        displayName = "Disgusted",
        emoji = "😒",
        systemPromptHint = "The user appears displeased or disgusted. Acknowledge their discomfort without dismissing it."
    ),
    UNKNOWN(
        displayName = "Unknown",
        emoji = "🤔",
        systemPromptHint = "Emotional state is unclear. Open with a gentle, open-ended check-in."
    );

    companion object {
        fun fromLabel(label: String): Emotion = entries.firstOrNull {
            it.name.equals(label, ignoreCase = true) ||
                it.displayName.equals(label, ignoreCase = true)
        } ?: UNKNOWN
    }
}
