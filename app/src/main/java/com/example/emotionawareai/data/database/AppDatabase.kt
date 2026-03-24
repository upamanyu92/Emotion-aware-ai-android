package com.example.emotionawareai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.emotionawareai.data.model.ConversationEntity
import com.example.emotionawareai.data.model.MemoryFragmentEntity
import com.example.emotionawareai.data.model.MessageEntity
import com.example.emotionawareai.data.model.UserPreferenceEntity
import com.example.emotionawareai.data.model.WeeklyInsightEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UserPreferenceEntity::class,
        MemoryFragmentEntity::class,
        WeeklyInsightEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun memoryFragmentDao(): MemoryFragmentDao
    abstract fun weeklyInsightDao(): WeeklyInsightDao

    companion object {
        const val DATABASE_NAME = "emotion_aware_ai.db"

        /**
         * Adds the [MemoryFragmentEntity] and [WeeklyInsightEntity] tables introduced in v2.
         * Existing data in conversations, messages and user_preferences is preserved.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_fragments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER,
                        `content` TEXT NOT NULL,
                        `keywords` TEXT NOT NULL,
                        `emotionTag` TEXT NOT NULL DEFAULT 'NEUTRAL',
                        `fragmentType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_insights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `weekStartTimestamp` INTEGER NOT NULL,
                        `dominantEmotion` TEXT NOT NULL,
                        `emotionFrequencies` TEXT NOT NULL DEFAULT '{}',
                        `summary` TEXT NOT NULL,
                        `trackedGoals` TEXT NOT NULL DEFAULT '[]',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
