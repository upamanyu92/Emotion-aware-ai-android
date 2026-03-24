package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists an extracted memory fragment for long-term RAG-based recall.
 *
 * [keywords] is a comma-separated list of lower-case terms used for fast
 * LIKE-based retrieval until a real embedding model is available.
 */
@Entity(tableName = "memory_fragments")
data class MemoryFragmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** The conversation this fragment was extracted from. May be null for manually added entries. */
    val conversationId: Long? = null,
    /** Human-readable summary of the remembered fact, goal, or pattern. */
    val content: String,
    /** Comma-separated lower-case keywords for retrieval. */
    val keywords: String,
    /** Dominant emotion at extraction time. */
    val emotionTag: String = "NEUTRAL",
    /** GOAL | PATTERN | FACT | EVENT */
    val fragmentType: String,
    val createdAt: Long = System.currentTimeMillis()
)
