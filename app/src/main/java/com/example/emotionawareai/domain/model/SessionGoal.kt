package com.example.emotionawareai.domain.model

/**
 * A user-defined growth goal.
 */
data class SessionGoal(
    val id: Long = 0L,
    val title: String,
    val growthArea: GrowthArea,
    val progressNote: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMentionedAt: Long = System.currentTimeMillis()
)
