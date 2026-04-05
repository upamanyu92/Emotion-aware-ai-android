package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A diary entry captured by continuous speech-to-text listening.
 *
 * Each entry is a raw text fragment recognised from ambient speech during
 * a diary session. At the end of the day these fragments are aggregated
 * and summarised by the AI agent.
 */
@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Raw transcribed text from the speech recognizer. */
    val rawText: String,
    /** ISO-8601 date string (yyyy-MM-dd) grouping entries by day. */
    val dateKey: String,
    /** AI-generated summary for the day; empty until summarisation runs. */
    val dailySummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
