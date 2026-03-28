package com.example.emotionawareai.di

import android.content.Context
import androidx.room.Room
import com.example.emotionawareai.data.database.AppDatabase
import com.example.emotionawareai.data.database.ConversationDao
import com.example.emotionawareai.data.database.MemoryFragmentDao
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.data.database.SessionGoalDao
import com.example.emotionawareai.data.database.UserPreferenceDao
import com.example.emotionawareai.data.database.WeeklyInsightDao
import com.example.emotionawareai.domain.repository.ConversationRepository
import com.example.emotionawareai.domain.repository.IMemoryRepository
import com.example.emotionawareai.domain.repository.MemoryFragmentRepository
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.LLMEngine
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.InsightsGenerator
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.tts.PiperVoiceManager
import com.example.emotionawareai.tts.SherpaOnnxTtsBackend
import com.example.emotionawareai.tts.SystemTtsBackend
import com.example.emotionawareai.voice.VoiceProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideConversationDao(db: AppDatabase): ConversationDao =
        db.conversationDao()

    @Provides
    @Singleton
    fun provideUserPreferenceDao(db: AppDatabase): UserPreferenceDao =
        db.userPreferenceDao()

    @Provides
    @Singleton
    fun provideMemoryFragmentDao(db: AppDatabase): MemoryFragmentDao =
        db.memoryFragmentDao()

    @Provides
    @Singleton
    fun provideWeeklyInsightDao(db: AppDatabase): WeeklyInsightDao =
        db.weeklyInsightDao()

    @Provides
    @Singleton
    fun provideMoodCheckInDao(db: AppDatabase): MoodCheckInDao =
        db.moodCheckInDao()

    @Provides
    @Singleton
    fun provideSessionGoalDao(db: AppDatabase): SessionGoalDao =
        db.sessionGoalDao()

    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        userPreferenceDao: UserPreferenceDao
    ): ConversationRepository = ConversationRepository(conversationDao, userPreferenceDao)

    @Provides
    @Singleton
    fun provideMemoryRepository(
        fragmentDao: MemoryFragmentDao,
        insightDao: WeeklyInsightDao
    ): IMemoryRepository = MemoryFragmentRepository(fragmentDao, insightDao)

    @Provides
    @Singleton
    fun provideLLMEngine(
        @ApplicationContext context: Context
    ): LLMEngine = LLMEngine(context)

    @Provides
    @Singleton
    fun provideEmotionDetector(
        @ApplicationContext context: Context
    ): EmotionDetector = EmotionDetector(context)

    @Provides
    @Singleton
    fun provideActivityAnalyzer(
        @ApplicationContext context: Context
    ): ActivityAnalyzer = ActivityAnalyzer(context)

    @Provides
    @Singleton
    fun provideVoiceProcessor(
        @ApplicationContext context: Context
    ): VoiceProcessor = VoiceProcessor(context)

    @Provides
    @Singleton
    fun provideResponseEngine(
        llmEngine: LLMEngine,
        systemTtsBackend: SystemTtsBackend,
        sherpaOnnxTtsBackend: SherpaOnnxTtsBackend
    ): ResponseEngine = ResponseEngine(llmEngine, systemTtsBackend, sherpaOnnxTtsBackend)

    @Provides
    @Singleton
    fun provideConversationManager(
        repository: ConversationRepository,
        memoryRepository: IMemoryRepository
    ): ConversationManager = ConversationManager(repository, memoryRepository)

    @Provides
    @Singleton
    fun provideMemoryManager(
        repository: ConversationRepository,
        sessionGoalDao: SessionGoalDao
    ): MemoryManager = MemoryManager(repository, sessionGoalDao)

    @Provides
    @Singleton
    fun provideInsightsGenerator(
        moodCheckInDao: MoodCheckInDao,
        sessionGoalDao: SessionGoalDao,
        weeklyInsightDao: WeeklyInsightDao,
        repository: ConversationRepository
    ): InsightsGenerator = InsightsGenerator(moodCheckInDao, sessionGoalDao, weeklyInsightDao, repository)
}
