package com.example.emotionawareai.manager

import android.util.Log
import com.example.emotionawareai.data.database.SessionGoalDao
import com.example.emotionawareai.data.model.SessionGoalEntity
import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.GrowthArea
import com.example.emotionawareai.domain.model.SessionGoal
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import com.example.emotionawareai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val repository: ConversationRepository,
    private val sessionGoalDao: SessionGoalDao
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

    suspend fun isCaptionsEnabled(): Boolean =
        getPreference(UserPreferenceEntity.KEY_CAPTIONS_ENABLED, "true").toBoolean()

    suspend fun isCameraPreviewVisible(): Boolean =
        getPreference(UserPreferenceEntity.KEY_CAMERA_PREVIEW_VISIBLE, "true").toBoolean()

    suspend fun setTtsEnabled(enabled: Boolean) {
        Log.i(TAG, "setTtsEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_TTS_ENABLED, enabled.toString())
    }

    suspend fun getTtsVoiceProfile(): TtsVoiceProfile =
        TtsVoiceProfile.fromName(
            getPreference(UserPreferenceEntity.KEY_TTS_VOICE_PROFILE, TtsVoiceProfile.DEFAULT.name)
        )

    suspend fun setTtsVoiceProfile(profile: TtsVoiceProfile) {
        Log.i(TAG, "setTtsVoiceProfile: ${profile.name}")
        savePreference(UserPreferenceEntity.KEY_TTS_VOICE_PROFILE, profile.name)
    }

    suspend fun getTtsBackend(): TtsBackend =
        TtsBackend.fromName(
            getPreference(UserPreferenceEntity.KEY_TTS_BACKEND, TtsBackend.SYSTEM.name)
        )

    suspend fun setTtsBackend(backend: TtsBackend) {
        Log.i(TAG, "setTtsBackend: ${backend.name}")
        savePreference(UserPreferenceEntity.KEY_TTS_BACKEND, backend.name)
    }

    suspend fun getPiperVoice(): PiperVoice =
        PiperVoice.fromName(
            getPreference(UserPreferenceEntity.KEY_PIPER_VOICE, PiperVoice.ALAN.name)
        )

    suspend fun setPiperVoice(voice: PiperVoice) {
        Log.i(TAG, "setPiperVoice: ${voice.name}")
        savePreference(UserPreferenceEntity.KEY_PIPER_VOICE, voice.name)
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

    suspend fun setCaptionsEnabled(enabled: Boolean) {
        Log.i(TAG, "setCaptionsEnabled: $enabled")
        savePreference(UserPreferenceEntity.KEY_CAPTIONS_ENABLED, enabled.toString())
    }

    suspend fun setCameraPreviewVisible(visible: Boolean) {
        Log.i(TAG, "setCameraPreviewVisible: $visible")
        savePreference(UserPreferenceEntity.KEY_CAMERA_PREVIEW_VISIBLE, visible.toString())
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

    // ── Growth goals helpers ──────────────────────────────────────────────────

    fun observeActiveGoals(): Flow<List<SessionGoal>> =
        sessionGoalDao.observeActiveGoals()
            .map { list -> list.map { it.toGoal() } }
            .flowOn(Dispatchers.IO)

    suspend fun getActiveGoals(): List<SessionGoal> = withContext(Dispatchers.IO) {
        sessionGoalDao.getActiveGoals().map { it.toGoal() }
    }

    suspend fun addGoal(title: String, area: GrowthArea): Long = withContext(Dispatchers.IO) {
        sessionGoalDao.insert(SessionGoalEntity(title = title, growthArea = area.name))
    }

    suspend fun archiveGoal(id: Long) = withContext(Dispatchers.IO) {
        sessionGoalDao.archiveGoal(id)
    }

    suspend fun updateGoalProgress(id: Long, note: String) = withContext(Dispatchers.IO) {
        sessionGoalDao.updateProgress(id, note)
    }

    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        sessionGoalDao.delete(id)
    }

    private fun SessionGoalEntity.toGoal() = SessionGoal(
        id = id,
        title = title,
        growthArea = GrowthArea.entries.firstOrNull { it.name == growthArea }
            ?: run {
                Log.w(TAG, "Unknown growthArea value '$growthArea' for goal id=$id; defaulting to MOTIVATION")
                GrowthArea.MOTIVATION
            },
        progressNote = progressNote,
        isActive = isActive,
        createdAt = createdAt,
        lastMentionedAt = lastMentionedAt
    )

    // ── Onboarding/preference helpers ─────────────────────────────────────────

    suspend fun getGrowthAreas(): List<GrowthArea> {
        val raw = getPreference(UserPreferenceEntity.KEY_GROWTH_AREAS, "")
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { name ->
            GrowthArea.entries.firstOrNull { it.name == name.trim() }
        }
    }

    suspend fun setGrowthAreas(areas: List<GrowthArea>) {
        savePreference(UserPreferenceEntity.KEY_GROWTH_AREAS, areas.joinToString(",") { it.name })
    }

    suspend fun getCheckInFrequency(): String =
        getPreference(UserPreferenceEntity.KEY_CHECKIN_FREQUENCY, "daily")

    suspend fun setCheckInFrequency(frequency: String) {
        savePreference(UserPreferenceEntity.KEY_CHECKIN_FREQUENCY, frequency)
    }

    suspend fun isPrivacyNoticeShown(): Boolean =
        getPreference(UserPreferenceEntity.KEY_PRIVACY_NOTICE_SHOWN, "false").toBoolean()

    suspend fun setPrivacyNoticeShown() {
        savePreference(UserPreferenceEntity.KEY_PRIVACY_NOTICE_SHOWN, "true")
    }

    suspend fun isOnboardingComplete(): Boolean =
        getPreference(UserPreferenceEntity.KEY_ONBOARDING_COMPLETE, "false").toBoolean()

    suspend fun setOnboardingComplete() {
        savePreference(UserPreferenceEntity.KEY_ONBOARDING_COMPLETE, "true")
    }

    suspend fun getLastCheckInDate(): String =
        getPreference(UserPreferenceEntity.KEY_LAST_CHECKIN_DATE, "")

    suspend fun setLastCheckInDate(isoDate: String) {
        savePreference(UserPreferenceEntity.KEY_LAST_CHECKIN_DATE, isoDate)
    }

    companion object {
        private const val TAG = "MemoryManager"
    }
}
