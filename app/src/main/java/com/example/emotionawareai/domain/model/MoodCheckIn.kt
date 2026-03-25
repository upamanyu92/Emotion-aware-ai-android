package com.example.emotionawareai.domain.model

/**
 * A daily mood check-in snapshot.
 */
data class MoodCheckIn(
    val id: Long = 0L,
    val moodScore: Int,           // 1–5
    val note: String = "",
    val emotion: Emotion = Emotion.NEUTRAL,
    val timestamp: Long = System.currentTimeMillis()
)
