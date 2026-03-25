package com.example.emotionawareai.domain.model

/**
 * Shared keyword extraction utility used by both the memory storage path
 * ([com.example.emotionawareai.manager.ConversationManager]) and the RAG
 * retrieval path ([com.example.emotionawareai.domain.repository.MemoryFragmentRepository]).
 *
 * Centralising here ensures storage and retrieval use the same tokenisation
 * rules, which is required for keyword-overlap scoring to work correctly.
 */
object KeywordExtractor {

    private val STOP_WORDS = setOf(
        "i", "a", "an", "the", "is", "it", "in", "on", "at", "to", "for",
        "of", "and", "or", "but", "not", "my", "me", "we", "you", "he",
        "she", "they", "this", "that", "was", "are", "be", "been", "being",
        "have", "has", "do", "did", "will", "would", "could", "should",
        "with", "from", "by", "as", "so", "if", "up", "out", "about",
        "just", "like", "what", "how", "when", "where", "who", "which",
        "can", "your", "its", "our", "their", "there", "then", "than"
    )

    /**
     * Tokenises [text] into meaningful lower-case terms by stripping punctuation
     * and common English stop words.
     *
     * @param text     Input text to tokenise.
     * @param minLength Minimum character length for a token to be kept (default 3).
     * @param maxTerms  Maximum number of distinct terms to return (default unlimited).
     */
    fun extract(
        text: String,
        minLength: Int = 3,
        maxTerms: Int = Int.MAX_VALUE
    ): List<String> = text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > minLength && it !in STOP_WORDS }
        .distinct()
        .take(maxTerms)
}
