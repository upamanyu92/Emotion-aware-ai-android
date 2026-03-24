package com.example.emotionawareai.domain.model

/**
 * Classifies the nature of a stored [MemoryFragment].
 */
enum class MemoryFragmentType(val label: String) {
    GOAL("Goal"),
    PATTERN("Pattern"),
    FACT("Fact"),
    EVENT("Event")
}

/**
 * Domain representation of a long-term memory fragment used for RAG context injection.
 */
data class MemoryFragment(
    val id: Long = 0L,
    /** Source conversation; null for manually created entries. */
    val conversationId: Long? = null,
    /** Human-readable summary of the remembered information. */
    val content: String,
    /** Keywords extracted from [content] for retrieval matching. */
    val keywords: List<String>,
    val emotion: Emotion = Emotion.NEUTRAL,
    val type: MemoryFragmentType,
    val createdAt: Long = System.currentTimeMillis()
)
