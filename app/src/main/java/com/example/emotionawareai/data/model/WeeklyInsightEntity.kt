package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a weekly aggregated insight snapshot used for the "Weekly Insights" dashboard.
 *
 * [emotionFrequencies] and [trackedGoals] are stored as JSON strings to avoid
 * additional join tables while keeping the schema simple.
 */
@Entity(tableName = "weekly_insights")
data class WeeklyInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Epoch millis for the Monday that starts this insight week. */
    val weekStartTimestamp: Long,
    /** Most frequent emotion during this week (Emotion enum name). */
    val dominantEmotion: String,
    /** JSON object mapping Emotion name → occurrence count, e.g. {"HAPPY":5,"SAD":2}. */
    val emotionFrequencies: String = "{}",
    /** AI-generated narrative summary of the week. */
    val summary: String,
    /** JSON array of goal strings mentioned or tracked this week. */
    val trackedGoals: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)
