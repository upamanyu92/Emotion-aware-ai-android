package com.example.emotionawareai.domain.model

/**
 * An AI-generated weekly growth insight report.
 */
data class WeeklyInsight(
    val id: Long = 0L,
    val weekStartTimestamp: Long,
    val dominantEmotion: Emotion,
    val moodAverage: Float,          // 1.0–5.0
    val checkInCount: Int,
    val topThemes: List<String>,     // e.g. ["work stress", "sleep"]
    val narrative: String,           // AI-generated summary paragraph
    val suggestedNextSteps: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)
