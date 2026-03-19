package com.example.emotionawareai.manager

import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.ConversationContext
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.ResponseStyle
import com.example.emotionawareai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [ConversationContext] from user input, detected emotion, and stored history.
 *
 * Acts as the central hub that assembles all information needed before
 * the [ResponseEngine] calls the LLM.
 */
@Singleton
class ConversationManager @Inject constructor(
    private val repository: ConversationRepository
) {
    private var activeConversationId: Long = -1L

    /**
     * Ensures the active conversation ID is resolved (creates one if needed).
     */
    suspend fun ensureConversation(): Long = withContext(Dispatchers.IO) {
        if (activeConversationId == -1L) {
            activeConversationId = repository.getOrCreateActiveConversation()
        }
        activeConversationId
    }

    /**
     * Builds a [ConversationContext] for the given [userMessage] and [emotion].
     */
    suspend fun buildContext(
        userMessage: String,
        emotion: Emotion
    ): ConversationContext = withContext(Dispatchers.IO) {
        val convId = ensureConversation()
        val history = repository.getRecentMessages(convId, limit = 6)
        val styleString = repository.getPreference(
            UserPreferenceEntity.KEY_RESPONSE_STYLE,
            ResponseStyle.EMPATHETIC.name
        )
        val style = runCatching { ResponseStyle.valueOf(styleString) }
            .getOrDefault(ResponseStyle.EMPATHETIC)

        ConversationContext(
            conversationId = convId,
            userMessage = userMessage,
            detectedEmotion = emotion,
            recentHistory = history,
            systemStyle = style
        )
    }

    /**
     * Saves a user or assistant message to the active conversation.
     */
    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val convId = ensureConversation()
        repository.saveMessage(message, convId)
    }

    /**
     * Starts a new conversation, replacing the current one.
     */
    suspend fun startNewConversation(title: String = "New Conversation"): Long =
        withContext(Dispatchers.IO) {
            activeConversationId = repository.createConversation(title)
            repository.savePreference(
                UserPreferenceEntity.KEY_CONVERSATION_ID,
                activeConversationId.toString()
            )
            activeConversationId
        }

    fun getActiveConversationId(): Long = activeConversationId
}
