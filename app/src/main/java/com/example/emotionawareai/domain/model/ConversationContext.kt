package com.example.emotionawareai.domain.model

/**
 * Encapsulates all context needed to build an LLM prompt for a single turn.
 */
data class ConversationContext(
    val conversationId: Long,
    val userMessage: String,
    val detectedEmotion: Emotion,
    val audioToneEmotion: Emotion = Emotion.UNKNOWN,
    val recentHistory: List<ChatMessage>,
    val systemStyle: ResponseStyle = ResponseStyle.EMPATHETIC
) {
    /**
     * Builds the structured prompt fed to the LLM.
     */
    fun buildPrompt(): String = buildString {
        appendLine("[SYSTEM] You are an empathetic AI assistant. ${detectedEmotion.systemPromptHint}")
        if (audioToneEmotion != Emotion.UNKNOWN && audioToneEmotion != Emotion.NEUTRAL) {
            appendLine("Voice tone hint: user may sound ${audioToneEmotion.displayName.lowercase()}.")
        }
        appendLine("Response style: ${systemStyle.description}")
        appendLine("Be concise (2-3 sentences). Do not repeat the user's words verbatim.")

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
    }
}

enum class ResponseStyle(val description: String) {
    EMPATHETIC("Warm, supportive, and emotionally aware"),
    CONCISE("Brief and to-the-point"),
    DETAILED("Thorough and informative"),
    PLAYFUL("Light-hearted and friendly")
}
