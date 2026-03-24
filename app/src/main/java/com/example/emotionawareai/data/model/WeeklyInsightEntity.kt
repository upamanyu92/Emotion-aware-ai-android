package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_insights")
data class WeeklyInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val weekStartTimestamp: Long,
    val dominantEmotion: String = "NEUTRAL",
    val moodAverage: Float = 3.0f,
    val checkInCount: Int = 0,
    val topThemesJson: String = "[]",       // JSON array of theme strings
    val narrative: String = "",
    val suggestedNextStepsJson: String = "[]",
    val generatedAt: Long = System.currentTimeMillis()
)
