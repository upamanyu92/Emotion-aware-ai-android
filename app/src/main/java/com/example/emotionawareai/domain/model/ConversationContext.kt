package com.example.emotionawareai.domain.model

/**
 * Encapsulates all context needed to build an LLM prompt for a single turn.
 *
 * [retrievedMemories] contains long-term memory fragments surfaced by the RAG
 * layer and injected into the prompt under the [LONG-TERM MEMORY] tag.
 */
data class ConversationContext(
    val conversationId: Long,
    val userMessage: String,
    val detectedEmotion: Emotion,
    val audioToneEmotion: Emotion = Emotion.UNKNOWN,
    val recentHistory: List<ChatMessage>,
    val systemStyle: ResponseStyle = ResponseStyle.EMPATHETIC,
    val retrievedMemories: List<MemoryFragment> = emptyList()
) {
    /**
     * Builds the structured prompt fed to the LLM.
     *
     * The system block embeds the mental-health companion persona, real-time
     * emotion signals, and injected long-term memories so the model can
     * provide contextually aware, growth-oriented responses.
     */
    fun buildPrompt(): String = buildString {
        appendLine(SYSTEM_PROMPT)
        appendLine("Current emotional signal: ${detectedEmotion.systemPromptHint}")
        if (audioToneEmotion != Emotion.UNKNOWN && audioToneEmotion != Emotion.NEUTRAL) {
            appendLine("Voice tone hint: user may sound ${audioToneEmotion.displayName.lowercase()}.")
        }
        appendLine("Response style: ${systemStyle.description}")

        if (retrievedMemories.isNotEmpty()) {
            appendLine()
            appendLine("[LONG-TERM MEMORY]")
            retrievedMemories.forEach { memory ->
                appendLine("- [${memory.type.label}] ${memory.content}")
            }
        }

        if (recentHistory.isNotEmpty()) {
            appendLine()
            appendLine("[CONTEXT]")
            recentHistory.takeLast(MAX_HISTORY_TURNS).forEach { msg ->
                val roleTag = if (msg.isFromUser) "User" else "Assistant"
                appendLine("$roleTag: ${msg.content}")
            }
        }

        appendLine()
        appendLine("[USER] $userMessage")
        append("[ASSISTANT]")
    }

    companion object {
        private const val MAX_HISTORY_TURNS = 6

        /**
         * Mental health companion persona injected at the start of every prompt.
         *
         * Governs tone, safety boundaries, and interaction style.
         * Update this constant (rather than [buildPrompt]) to iterate on the persona.
         */
        const val SYSTEM_PROMPT = """[SYSTEM] You are an empathetic, insightful, and zero-judgment AI companion dedicated to the user's personal growth and mental well-being.
Core Directives:
- Tone: Warm, conversational, grounded, and entirely non-judgmental. Speak like a supportive mentor or a thoughtful sounding board.
- Active Listening & Memory: Seamlessly integrate context from past conversations. When appropriate, gently connect current feelings or situations to past patterns you have observed to help the user gain self-awareness.
- Guided Discovery: Do not preach or instantly solve problems. Use the Socratic method—ask thoughtful, open-ended questions that guide the user to their own realizations.
- Accountability: Track the user's stated goals. Check in on their progress gently, celebrating small wins and offering supportive reframing during setbacks.
- Safety & Boundaries: You are a supportive tool, not a licensed therapist. If the user expresses severe distress, crisis, or self-harm, immediately provide appropriate emergency resources and encourage professional help. Never offer medical diagnoses."""
    }
}

enum class ResponseStyle(val description: String) {
    EMPATHETIC("Warm, supportive, and emotionally aware"),
    CONCISE("Brief and to-the-point"),
    DETAILED("Thorough and informative"),
    PLAYFUL("Light-hearted and friendly")
}
