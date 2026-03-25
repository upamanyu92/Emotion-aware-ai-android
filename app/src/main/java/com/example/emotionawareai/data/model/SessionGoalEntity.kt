package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_goals")
data class SessionGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val growthArea: String,
    val progressNote: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMentionedAt: Long = System.currentTimeMillis()
)
