package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists an individual AI evaluation score for a given metric and message pair.
 * Scores are in the range [0.0, 1.0].
 */
@Entity(tableName = "ai_evaluations")
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** The message ID this evaluation refers to (assistant turn). */
    val messageId: Long,
    /** [com.example.emotionawareai.domain.model.EvaluationMetric] enum name. */
    val metricName: String,
    /** Score in [0.0, 1.0]. */
    val score: Float,
    /** Whether this score came from automated evaluation or user feedback. */
    val isUserFeedback: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
