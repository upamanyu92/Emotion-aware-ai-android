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
    val systemStyle: ResponseStyle = ResponseStyle.EMPATHETIC,
    val userGoals: List<String> = emptyList(),
    val recurringThemes: List<String> = emptyList()
) {
    /**
     * Builds the structured prompt fed to the LLM.
     */
    fun buildPrompt(): String = buildString {
        appendLine("[SYSTEM] You are Ash, a warm and empathetic AI companion focused on personal growth and mental wellness.")
        appendLine("You draw on evidence-based approaches (CBT, motivational interviewing) without being clinical.")
        appendLine("Always respond with compassion, curiosity, and non-judgment. Help users reflect, identify patterns, and take small steps forward.")
        appendLine(detectedEmotion.systemPromptHint)
        if (audioToneEmotion != Emotion.UNKNOWN && audioToneEmotion != Emotion.NEUTRAL) {
            appendLine("Voice tone hint: user may sound ${audioToneEmotion.displayName.lowercase()} — acknowledge this gently if relevant.")
        }
        appendLine("Response style: ${systemStyle.description}")
        if (userGoals.isNotEmpty()) {
            appendLine("User's current growth goals: ${userGoals.joinToString(", ")}.")
        }
        if (recurringThemes.isNotEmpty()) {
            appendLine("Recurring themes in recent conversations: ${recurringThemes.joinToString(", ")}.")
        }
        appendLine("Keep replies concise (2-4 sentences) unless the user asks for more. Never diagnose. Refer to crisis resources if safety is a concern.")

        if (recentHistory.isNotEmpty()) {
            appendLine()
            appendLine("[CONTEXT]")
            recentHistory.takeLast(MAX_HISTORY_TURNS).forEach { msg ->
                val roleTag = if (msg.isFromUser) "User" else "Ash"
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
    EMPATHETIC("Warm, compassionate, and emotionally attuned — validates feelings before advising"),
    CONCISE("Brief and supportive — acknowledges feelings then offers a focused reflection"),
    DETAILED("Thorough and exploratory — digs deeper into patterns and emotions"),
    PLAYFUL("Light-hearted and encouraging — builds on positives with gentle humor")
}
