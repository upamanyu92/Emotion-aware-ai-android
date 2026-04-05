package com.example.emotionawareai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val KEY_RESPONSE_STYLE = "preferred_emotion_response_style"
        const val KEY_TTS_ENABLED = "tts_enabled"
        const val KEY_CAMERA_ENABLED = "camera_enabled"
        const val KEY_CONTINUOUS_CONVERSATION_ENABLED = "continuous_conversation_enabled"
        const val KEY_PREMIUM_UNLOCKED = "premium_unlocked"
        const val KEY_PREMIUM_NUDGE_SHOWN = "premium_nudge_shown"
        const val KEY_PRO_THEME_ENABLED = "pro_theme_enabled"
        const val KEY_EXPORT_WITH_INSIGHTS = "export_with_insights"
        const val KEY_CONVERSATION_ID = "active_conversation_id"
        const val KEY_FONT_SIZE = "font_size"
        /** Remote-config kill-switch: when false, all premium features are disabled. */
        const val KEY_PREMIUM_FEATURES_GLOBALLY_ENABLED = "premium_features_globally_enabled"
        /** User profile: display name entered at login. */
        const val KEY_USER_NAME = "user_name"
        /** User profile: emoji avatar chosen at login. */
        const val KEY_USER_AVATAR = "user_avatar"
        /** Whether activity captions overlay is visible. */
        const val KEY_CAPTIONS_ENABLED = "captions_enabled"
        /** Whether the camera preview feed is shown to the user (camera may still be running). */
        const val KEY_CAMERA_PREVIEW_VISIBLE = "camera_preview_visible"
        /** Selected growth areas from onboarding (comma-separated GrowthArea names). */
        const val KEY_GROWTH_AREAS = "growth_areas"
        /** Check-in frequency preference: "daily", "weekly", or "as_needed". */
        const val KEY_CHECKIN_FREQUENCY = "checkin_frequency"
        /** Whether the privacy notice has been shown on first launch. */
        const val KEY_PRIVACY_NOTICE_SHOWN = "privacy_notice_shown"
        /** Whether the onboarding flow has been fully completed (including goal selection). */
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        /** Whether the daily check-in sheet has been shown today (ISO date string). */
        const val KEY_LAST_CHECKIN_DATE = "last_checkin_date"
        /** Selected TTS voice profile name (matches TtsVoiceProfile enum name). */
        const val KEY_TTS_VOICE_PROFILE = "tts_voice_profile"
        /** Active speech synthesis backend. */
        const val KEY_TTS_BACKEND = "tts_backend"
        /** Selected Piper neural voice name. */
        const val KEY_PIPER_VOICE = "piper_voice"
        /** Whether the LLM setup has been completed. */
        const val KEY_LLM_SETUP_COMPLETE = "llm_setup_complete"
        /** ID of the selected LLM option (matches [LlmOption.id]). */
        const val KEY_SELECTED_LLM_ID = "selected_llm_id"
        // NOTE: The HuggingFace access token is NOT stored here.
        // It is kept in SecureTokenStorage (EncryptedSharedPreferences + Android Keystore).
    }
}
