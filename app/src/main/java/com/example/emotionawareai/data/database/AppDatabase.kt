package com.example.emotionawareai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.emotionawareai.data.model.ConversationEntity
import com.example.emotionawareai.data.model.MessageEntity
import com.example.emotionawareai.data.model.UserPreferenceEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UserPreferenceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        const val DATABASE_NAME = "emotion_aware_ai.db"
    }
}
