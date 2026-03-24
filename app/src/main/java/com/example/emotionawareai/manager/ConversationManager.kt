package com.example.emotionawareai.manager

import android.util.Log
import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.ConversationContext
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MemoryFragment
import com.example.emotionawareai.domain.model.MemoryFragmentType
import com.example.emotionawareai.domain.model.ResponseStyle
import com.example.emotionawareai.domain.repository.ConversationRepository
import com.example.emotionawareai.domain.repository.IMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [ConversationContext] from user input, detected emotion, stored history,
 * and RAG-retrieved long-term memories.
 *
 * Acts as the central hub that assembles all information needed before
 * the [ResponseEngine] calls the LLM.
 */
@Singleton
class ConversationManager @Inject constructor(
    private val repository: ConversationRepository,
    private val memoryRepository: IMemoryRepository
) {
    private var activeConversationId: Long = -1L

    /**
     * Ensures the active conversation ID is resolved (creates one if needed).
     */
    suspend fun ensureConversation(): Long = withContext(Dispatchers.IO) {
        if (activeConversationId == -1L) {
            activeConversationId = repository.getOrCreateActiveConversation()
            Log.i(TAG, "Resolved active conversation: id=$activeConversationId")
        }
        activeConversationId
    }

    /**
     * Builds a [ConversationContext] for the given [userMessage] and [emotion].
     *
     * Automatically retrieves relevant long-term memories via RAG and injects
     * them into the context so the LLM can reference past patterns and goals.
     */
    suspend fun buildContext(
        userMessage: String,
        emotion: Emotion,
        audioToneEmotion: Emotion = Emotion.UNKNOWN,
        historyLimit: Int = 6
    ): ConversationContext = withContext(Dispatchers.IO) {
        val convId = ensureConversation()
        val history = repository.getRecentMessages(convId, limit = historyLimit)
        val styleString = repository.getPreference(
            UserPreferenceEntity.KEY_RESPONSE_STYLE,
            ResponseStyle.EMPATHETIC.name
        )
        val style = runCatching { ResponseStyle.valueOf(styleString) }
            .getOrDefault(ResponseStyle.EMPATHETIC)

        val retrievedMemories = memoryRepository.retrieveRelevant(userMessage)

        Log.d(
            TAG,
            "buildContext: convId=$convId, emotion=$emotion, audioEmotion=$audioToneEmotion, " +
                "style=$style, historySize=${history.size}, " +
                "retrievedMemories=${retrievedMemories.size}, messageLength=${userMessage.length}"
        )

        ConversationContext(
            conversationId = convId,
            userMessage = userMessage,
            detectedEmotion = emotion,
            audioToneEmotion = audioToneEmotion,
            recentHistory = history,
            systemStyle = style,
            retrievedMemories = retrievedMemories
        )
    }

    /**
     * Saves a user or assistant message to the active conversation.
     *
     * Additionally extracts and stores goal/pattern fragments from user messages
     * to enrich the long-term memory for future RAG retrieval.
     */
    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val convId = ensureConversation()
        Log.d(TAG, "saveMessage: role=${message.role}, emotion=${message.emotion}, contentLength=${message.content.length}")
        repository.saveMessage(message, convId)

        if (message.isFromUser) {
            extractAndStoreMemoryFragments(message, convId)
        }
    }

    /**
     * Heuristically extracts memory fragments (goals, facts) from a user message
     * and persists them for future RAG retrieval.
     */
    private suspend fun extractAndStoreMemoryFragments(message: ChatMessage, convId: Long) {
        val content = message.content.trim()
        val lower = content.lowercase()

        val goalIndicators = listOf(
            "i want to", "i would like to", "my goal is", "i plan to",
            "i'm trying to", "i hope to", "i need to", "i'm working on",
            "i want", "i'd like", "i wish"
        )
        val patternIndicators = listOf(
            "i always", "i usually", "i tend to", "i often", "every time",
            "whenever i", "i keep", "i never", "i rarely"
        )

        val type = when {
            goalIndicators.any { lower.contains(it) } -> MemoryFragmentType.GOAL
            patternIndicators.any { lower.contains(it) } -> MemoryFragmentType.PATTERN
            else -> null
        }

        if (type != null && content.length >= MIN_FRAGMENT_LENGTH) {
            val keywords = content.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 3 }
                .distinct()
                .take(10)

            val fragment = MemoryFragment(
                conversationId = convId,
                content = content,
                keywords = keywords,
                emotion = message.emotion,
                type = type
            )
            memoryRepository.storeFragment(fragment)
            Log.d(TAG, "Extracted memory fragment: type=$type, keywords=${keywords.take(5)}")
        }
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
            Log.i(TAG, "Started new conversation: id=$activeConversationId, title='$title'")
            activeConversationId
        }

    fun getActiveConversationId(): Long = activeConversationId

    companion object {
        private const val TAG = "ConversationManager"
        private const val MIN_FRAGMENT_LENGTH = 15
    }
}
