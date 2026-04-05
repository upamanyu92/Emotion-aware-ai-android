package com.example.emotionawareai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.emotionawareai.data.model.ConversationEntity
import com.example.emotionawareai.data.model.DiaryEntryEntity
import com.example.emotionawareai.data.model.EvaluationEntity
import com.example.emotionawareai.data.model.FeedbackEntity
import com.example.emotionawareai.data.model.MemoryFragmentEntity
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
        MemoryFragmentEntity::class,
        WeeklyInsightEntity::class,
        MoodCheckInEntity::class,
        SessionGoalEntity::class,
        EvaluationEntity::class,
        DiaryEntryEntity::class,
        FeedbackEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun memoryFragmentDao(): MemoryFragmentDao
    abstract fun weeklyInsightDao(): WeeklyInsightDao
    abstract fun moodCheckInDao(): MoodCheckInDao
    abstract fun sessionGoalDao(): SessionGoalDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun feedbackDao(): FeedbackDao

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

        /**
         * Adds [MoodCheckInEntity] and [SessionGoalEntity] tables introduced in v3.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mood_checkins` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `moodScore` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `emotion` TEXT NOT NULL DEFAULT 'NEUTRAL',
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `growthArea` TEXT NOT NULL,
                        `progressNote` TEXT NOT NULL DEFAULT '',
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `lastMentionedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds [EvaluationEntity], [DiaryEntryEntity], and [FeedbackEntity]
         * tables introduced in v4 for AI evaluation, diary, and user feedback.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_evaluations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `metricName` TEXT NOT NULL,
                        `score` REAL NOT NULL,
                        `isUserFeedback` INTEGER NOT NULL DEFAULT 0,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `diary_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rawText` TEXT NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `dailySummary` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `comment` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
