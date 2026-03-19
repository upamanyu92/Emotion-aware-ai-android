package com.example.emotionawareai.domain.model

/**
 * Represents a single chat message in the conversation.
 */
data class ChatMessage(
    val id: Long = 0L,
    val content: String,
    val role: MessageRole,
    val emotion: Emotion = Emotion.NEUTRAL,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
) {
    val isFromUser: Boolean get() = role == MessageRole.USER
    val isFromAssistant: Boolean get() = role == MessageRole.ASSISTANT
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
