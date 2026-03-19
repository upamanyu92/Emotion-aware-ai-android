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
    }
}
