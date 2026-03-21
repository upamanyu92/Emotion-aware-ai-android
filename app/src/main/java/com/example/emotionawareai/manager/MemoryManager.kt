package com.example.emotionawareai.manager

import android.util.Log
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

    suspend fun isContinuousConversationEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_CONTINUOUS_CONVERSATION_ENABLED, "false").toBoolean()

    suspend fun isPremiumUnlocked(): Boolean =
        getPreference(UserPreferenceEntity.KEY_PREMIUM_UNLOCKED, "false").toBoolean()

    suspend fun isProThemeEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_PRO_THEME_ENABLED, "false").toBoolean()

    suspend fun isExportWithInsightsEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_EXPORT_WITH_INSIGHTS, "true").toBoolean()

    suspend fun setTtsEnabled(enabled: Boolean) {
        Log.i(TAG, "setTtsEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_TTS_ENABLED, enabled.toString())
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        Log.i(TAG, "setCameraEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_CAMERA_ENABLED, enabled.toString())
    }

    suspend fun setContinuousConversationEnabled(enabled: Boolean) {
        Log.i(TAG, "setContinuousConversationEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_CONTINUOUS_CONVERSATION_ENABLED, enabled.toString())
    }

    suspend fun setPremiumUnlocked(enabled: Boolean) {
        Log.i(TAG, "setPremiumUnlocked: $enabled")
        savePreference(UserPreferenceEntity.KEY_PREMIUM_UNLOCKED, enabled.toString())
    }

    suspend fun setProThemeEnabled(enabled: Boolean) {
        Log.i(TAG, "setProThemeEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_PRO_THEME_ENABLED, enabled.toString())
    }

    suspend fun setExportWithInsightsEnabled(enabled: Boolean) {
        Log.i(TAG, "setExportWithInsightsEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_EXPORT_WITH_INSIGHTS, enabled.toString())
    }

    /** Returns whether premium features are globally enabled (remote kill-switch). Defaults true. */
    suspend fun isPremiumFeaturesGloballyEnabled(): Boolean =
        getPreference(
            UserPreferenceEntity.KEY_PREMIUM_FEATURES_GLOBALLY_ENABLED,
            "true"
        ).toBoolean()

    /** Updates the remote kill-switch value locally (e.g. after fetching remote config). */
    suspend fun setPremiumFeaturesGloballyEnabled(enabled: Boolean) {
        Log.i(TAG, "setPremiumFeaturesGloballyEnabled: $enabled")
        savePreference(
            UserPreferenceEntity.KEY_PREMIUM_FEATURES_GLOBALLY_ENABLED,
            enabled.toString()
        )
    }

    // ── User profile helpers ───────────────────────────────────────────────────

    /** Returns the saved user display name, or empty string if not set. */
    suspend fun getUserName(): String =
        getPreference(UserPreferenceEntity.KEY_USER_NAME, "")

    /** Returns the saved user avatar emoji, defaulting to 😊. */
    suspend fun getUserAvatar(): String =
        getPreference(UserPreferenceEntity.KEY_USER_AVATAR, "😊")

    /** Returns true when the user has completed the login/profile setup. */
    suspend fun hasUserProfile(): Boolean = getUserName().isNotBlank()

    /** Persists the user's display name. */
    suspend fun setUserName(name: String) {
        Log.i(TAG, "setUserName")
        savePreference(UserPreferenceEntity.KEY_USER_NAME, name.trim())
    }

    /** Persists the user's emoji avatar. */
    suspend fun setUserAvatar(emoji: String) {
        Log.i(TAG, "setUserAvatar: $emoji")
        savePreference(UserPreferenceEntity.KEY_USER_AVATAR, emoji)
    }

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

    companion object {
        private const val TAG = "MemoryManager"
    }
}
