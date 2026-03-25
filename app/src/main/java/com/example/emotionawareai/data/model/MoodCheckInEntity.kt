package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_checkins")
data class MoodCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val moodScore: Int,
    val note: String = "",
    val emotion: String = "NEUTRAL",
    val timestamp: Long = System.currentTimeMillis()
)
