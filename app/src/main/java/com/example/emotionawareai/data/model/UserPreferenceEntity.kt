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
        const val KEY_CONVERSATION_ID = "active_conversation_id"
        const val KEY_FONT_SIZE = "font_size"
    }
}
