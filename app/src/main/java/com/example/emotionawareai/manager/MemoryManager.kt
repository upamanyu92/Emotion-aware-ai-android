package com.example.emotionawareai.manager

import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level memory interface backed by Room.
 *
 * Provides convenience methods used by [ConversationManager] and the ViewModel
 * to read/write conversation history and user preferences without touching
 * Room entities directly.
 */
@Singleton
class MemoryManager @Inject constructor(
    private val repository: ConversationRepository
) {
    /**
     * Retrieves the [limit] most recent [ChatMessage]s for [conversationId],
     * returned in chronological order.
     */
    suspend fun getRecentContext(
        conversationId: Long,
        limit: Int = 8
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        repository.getRecentMessages(conversationId, limit)
    }

    /**
     * Observes live message updates for [conversationId].
     */
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        repository.getMessagesForConversation(conversationId).flowOn(Dispatchers.IO)

    /**
     * Persists a [preference] key-value pair.
     */
    suspend fun savePreference(key: String, value: String) = withContext(Dispatchers.IO) {
        repository.savePreference(key, value)
    }

    /**
     * Retrieves a preference value, returning [default] if not found.
     */
    suspend fun getPreference(key: String, default: String = ""): String =
        withContext(Dispatchers.IO) {
            repository.getPreference(key, default)
        }

    /**
     * Observes live updates for a single preference key.
     */
    fun observePreference(key: String): Flow<String?> =
        repository.observePreference(key).flowOn(Dispatchers.IO)

    // ── Typed convenience helpers ─────────────────────────────────────────────

    suspend fun isTtsEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_TTS_ENABLED, "true").toBoolean()

    suspend fun isCameraEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_CAMERA_ENABLED, "true").toBoolean()

    suspend fun setTtsEnabled(enabled: Boolean) =
        savePreference(UserPreferenceEntity.KEY_TTS_ENABLED, enabled.toString())

    suspend fun setCameraEnabled(enabled: Boolean) =
        savePreference(UserPreferenceEntity.KEY_CAMERA_ENABLED, enabled.toString())

    /**
     * Returns the dominant emotion across the last [limit] user messages.
     */
    suspend fun inferDominantEmotion(
        conversationId: Long,
        limit: Int = 10
    ): Emotion = withContext(Dispatchers.IO) {
        val messages = repository.getRecentMessages(conversationId, limit)
        messages
            .filter { it.isFromUser }
            .groupingBy { it.emotion }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: Emotion.NEUTRAL
    }
}
