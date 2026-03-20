package com.example.emotionawareai.domain.repository

import android.util.Log
import com.example.emotionawareai.data.database.ConversationDao
import com.example.emotionawareai.data.database.UserPreferenceDao
import com.example.emotionawareai.data.model.ConversationEntity
import com.example.emotionawareai.data.model.MessageEntity
import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val preferenceDao: UserPreferenceDao
) {

    // ── Conversations ────────────────────────────────────────────────────────

    suspend fun createConversation(title: String = "New Conversation"): Long {
        val id = conversationDao.insertConversation(ConversationEntity(title = title))
        Log.i(TAG, "Created conversation: id=$id, title='$title'")
        return id
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    suspend fun getOrCreateActiveConversation(): Long {
        val pref = preferenceDao.getByKey(UserPreferenceEntity.KEY_CONVERSATION_ID)
        val storedId = pref?.value?.toLongOrNull()
        if (storedId != null && conversationDao.getConversationById(storedId) != null) {
            Log.d(TAG, "Using existing active conversation: id=$storedId")
            return storedId
        }
        val newId = createConversation()
        savePreference(UserPreferenceEntity.KEY_CONVERSATION_ID, newId.toString())
        Log.i(TAG, "Created new active conversation: id=$newId")
        return newId
    }

    // ── Messages ─────────────────────────────────────────────────────────────

    suspend fun saveMessage(message: ChatMessage, conversationId: Long): Long {
        val entity = MessageEntity(
            conversationId = conversationId,
            content = message.content,
            role = message.role.name,
            emotion = message.emotion.name,
            timestamp = message.timestamp
        )
        val messageId = conversationDao.insertMessage(entity)
        Log.d(TAG, "Saved message: id=$messageId, conversationId=$conversationId, role=${message.role}")

        // Update conversation's updatedAt timestamp
        conversationDao.getConversationById(conversationId)?.let { conv ->
            conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
        return messageId
    }

    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> =
        conversationDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getRecentMessages(
        conversationId: Long,
        limit: Int = 10
    ): List<ChatMessage> =
        conversationDao.getRecentMessages(conversationId, limit)
            .reversed()
            .map { it.toDomain() }

    // ── Preferences ──────────────────────────────────────────────────────────

    suspend fun savePreference(key: String, value: String) {
        preferenceDao.upsert(UserPreferenceEntity(key = key, value = value))
    }

    suspend fun getPreference(key: String, default: String = ""): String =
        preferenceDao.getByKey(key)?.value ?: default

    fun observePreference(key: String): Flow<String?> =
        preferenceDao.observeByKey(key).map { it?.value }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        content = content,
        role = MessageRole.valueOf(role),
        emotion = Emotion.fromLabel(emotion),
        timestamp = timestamp
    )

    companion object {
        private const val TAG = "ConversationRepository"
    }
}
