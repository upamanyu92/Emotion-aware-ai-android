package com.example.emotionawareai.domain.model

/**
 * Domain representation of a weekly emotional pattern summary.
 *
 * Generated once per week by aggregating [MemoryFragment]s and message history.
 */
data class WeeklyInsight(
    val id: Long = 0L,
    /** Epoch millis for the Monday that begins this week. */
    val weekStartTimestamp: Long,
    val dominantEmotion: Emotion,
    /** Occurrence count per detected emotion over the week. */
    val emotionFrequencies: Map<Emotion, Int>,
    /** AI-generated narrative summary of the week's patterns. */
    val summary: String,
    /** Goals mentioned or tracked during this week. */
    val trackedGoals: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)
