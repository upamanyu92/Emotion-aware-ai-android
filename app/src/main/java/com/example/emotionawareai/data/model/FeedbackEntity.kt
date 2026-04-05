package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user feedback on an individual AI response.
 *
 * Users can submit a 1–5 star rating plus optional free-text comment
 * from the feedback sheet attached to each assistant message bubble.
 */
@Entity(tableName = "user_feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** The assistant message ID the feedback refers to. */
    val messageId: Long,
    /** User rating 1–5 stars. */
    val rating: Int,
    /** Optional freeform comment from user. */
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
