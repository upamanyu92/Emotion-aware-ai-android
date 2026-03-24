package com.example.emotionawareai.di

import android.content.Context
import androidx.room.Room
import com.example.emotionawareai.data.database.AppDatabase
import com.example.emotionawareai.data.database.ConversationDao
import com.example.emotionawareai.data.database.MemoryFragmentDao
import com.example.emotionawareai.data.database.UserPreferenceDao
import com.example.emotionawareai.data.database.WeeklyInsightDao
import com.example.emotionawareai.domain.repository.ConversationRepository
import com.example.emotionawareai.domain.repository.IMemoryRepository
import com.example.emotionawareai.domain.repository.MemoryFragmentRepository
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.LLMEngine
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
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
        .addMigrations(AppDatabase.MIGRATION_1_2)
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
        @ApplicationContext context: Context,
        llmEngine: LLMEngine
    ): ResponseEngine = ResponseEngine(context, llmEngine)

    @Provides
    @Singleton
    fun provideConversationManager(
        repository: ConversationRepository,
        memoryRepository: IMemoryRepository
    ): ConversationManager = ConversationManager(repository, memoryRepository)

    @Provides
    @Singleton
    fun provideMemoryManager(
        repository: ConversationRepository
    ): MemoryManager = MemoryManager(repository)
}
