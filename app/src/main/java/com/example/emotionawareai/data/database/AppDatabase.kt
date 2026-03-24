package com.example.emotionawareai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.emotionawareai.data.model.ConversationEntity
import com.example.emotionawareai.data.model.MessageEntity
import com.example.emotionawareai.data.model.MoodCheckInEntity
import com.example.emotionawareai.data.model.SessionGoalEntity
import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.data.model.WeeklyInsightEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UserPreferenceEntity::class,
        MoodCheckInEntity::class,
        SessionGoalEntity::class,
        WeeklyInsightEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun moodCheckInDao(): MoodCheckInDao
    abstract fun sessionGoalDao(): SessionGoalDao
    abstract fun weeklyInsightDao(): WeeklyInsightDao

    companion object {
        const val DATABASE_NAME = "emotion_aware_ai.db"
    }
}
