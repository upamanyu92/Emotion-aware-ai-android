package com.example.emotionawareai

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.DeviceCapabilityDetector
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.ModelDownloader
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.InsightsGenerator
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.tts.PiperVoiceManager
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.voice.AudioToneAnalyzer
import com.example.emotionawareai.voice.VoiceProcessor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var conversationManager: ConversationManager
    private lateinit var responseEngine: ResponseEngine
    private lateinit var emotionDetector: EmotionDetector
    private lateinit var activityAnalyzer: ActivityAnalyzer
    private lateinit var voiceProcessor: VoiceProcessor
    private lateinit var memoryManager: MemoryManager
    private lateinit var audioToneAnalyzer: AudioToneAnalyzer
    private lateinit var billingManager: BillingManager
    private lateinit var moodCheckInDao: MoodCheckInDao
    private lateinit var insightsGenerator: InsightsGenerator
    private lateinit var modelDownloader: ModelDownloader
    private lateinit var piperVoiceManager: PiperVoiceManager
    private lateinit var deviceCapabilityDetector: DeviceCapabilityDetector
    private lateinit var aiEvaluationEngine: com.example.emotionawareai.evaluation.AIEvaluationEngine
    private lateinit var langfuseTraceManager: com.example.emotionawareai.evaluation.LangfuseTraceManager
    private lateinit var diaryEntryDao: com.example.emotionawareai.data.database.DiaryEntryDao
    private lateinit var feedbackDao: com.example.emotionawareai.data.database.FeedbackDao
    private lateinit var viewModel: ChatViewModel

    private val emotionSharedFlow = MutableSharedFlow<Emotion>(replay = 1)
    private val activityCaptionFlow = MutableSharedFlow<List<ActivityCaption>>(replay = 1)
    private val voiceTextFlow     = MutableSharedFlow<String>(replay = 0)
    private val listeningFlow     = MutableSharedFlow<Boolean>(replay = 1)
    private val voiceErrorFlow    = MutableSharedFlow<com.example.emotionawareai.voice.VoiceError>(replay = 0)
    private val rmsFlow = MutableSharedFlow<Float>(replay = 0)
    private val audioEmotionFlow = MutableSharedFlow<Emotion>(replay = 1)
    private val toneInsightFlow = MutableSharedFlow<com.example.emotionawareai.voice.ToneInsight>(replay = 1)
    private val billingOffersFlow = MutableStateFlow(emptyList<com.example.emotionawareai.billing.PremiumOffer>())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        conversationManager = mockk(relaxed = true)
        responseEngine      = mockk(relaxed = true)
        emotionDetector     = mockk(relaxed = true)
        activityAnalyzer    = mockk(relaxed = true)
        voiceProcessor      = mockk(relaxed = true)
        memoryManager       = mockk(relaxed = true)
        audioToneAnalyzer   = mockk(relaxed = true)
        billingManager      = mockk(relaxed = true)
        moodCheckInDao      = mockk(relaxed = true)
        insightsGenerator   = mockk(relaxed = true)
        modelDownloader     = mockk(relaxed = true)
        piperVoiceManager   = mockk(relaxed = true)
        deviceCapabilityDetector = mockk(relaxed = true)
        aiEvaluationEngine  = mockk(relaxed = true)
        langfuseTraceManager = mockk(relaxed = true)
        diaryEntryDao       = mockk(relaxed = true)
        feedbackDao         = mockk(relaxed = true)

        coEvery { conversationManager.ensureConversation() } returns 1L
        coEvery { conversationManager.getActiveConversationId() } returns 1L
        coEvery { memoryManager.getRecentContext(any(), any()) } returns emptyList()
        coEvery { memoryManager.isTtsEnabled() } returns true
        coEvery { memoryManager.getTtsBackend() } returns TtsBackend.SYSTEM
        coEvery { memoryManager.getPiperVoice() } returns PiperVoice.ALAN
        coEvery { memoryManager.isContinuousConversationEnabled() } returns false
        coEvery { memoryManager.isPremiumUnlocked() } returns false
        coEvery { memoryManager.isProThemeEnabled() } returns false
        coEvery { memoryManager.isExportWithInsightsEnabled() } returns true
        coEvery { memoryManager.setPremiumUnlocked(any()) } returns Unit
        coEvery { memoryManager.isPremiumFeaturesGloballyEnabled() } returns true
        coEvery { responseEngine.loadModel() } returns true
        every { responseEngine.isSpeaking } returns MutableStateFlow(false)
        every { responseEngine.isModelFileAvailable() } returns false
        every { responseEngine.modelFilePath() } returns "/data/user/0/com.example.emotionawareai/files/models/model.gguf"

        coEvery { insightsGenerator.getLatestInsight() } returns null
        every { insightsGenerator.observeInsights() } returns flowOf(emptyList())

        every { modelDownloader.isDownloading } returns MutableStateFlow(false)
        every { modelDownloader.downloadProgress } returns MutableStateFlow(null)
        every { modelDownloader.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isDownloading } returns MutableStateFlow(false)
        every { piperVoiceManager.downloadProgress } returns MutableStateFlow(null)
        every { piperVoiceManager.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isVoiceInstalled(any()) } returns false

        every { emotionDetector.emotionFlow }   returns emotionSharedFlow
        every { activityAnalyzer.captionFlow } returns activityCaptionFlow
        every { voiceProcessor.recognizedTextFlow } returns voiceTextFlow
        every { voiceProcessor.listeningStateFlow } returns listeningFlow
        every { voiceProcessor.errorFlow }       returns voiceErrorFlow
        every { voiceProcessor.rmsFlow } returns rmsFlow
        every { audioToneAnalyzer.audioEmotionFlow } returns audioEmotionFlow
        every { audioToneAnalyzer.toneInsightFlow } returns toneInsightFlow
        every { billingManager.isBillingReady } returns MutableStateFlow(true)
        every { billingManager.isPremium } returns MutableStateFlow(false)
        every { billingManager.billingMessage } returns MutableStateFlow(null)
        every { billingManager.offers } returns billingOffersFlow
        every { billingManager.isPurchaseInProgress } returns MutableStateFlow(false)
        every { billingManager.isRestoreInProgress } returns MutableStateFlow(false)

        viewModel = ChatViewModel(
            conversationManager = conversationManager,
            responseEngine      = responseEngine,
            emotionDetector     = emotionDetector,
            activityAnalyzer    = activityAnalyzer,
            voiceProcessor      = voiceProcessor,
            memoryManager       = memoryManager,
            audioToneAnalyzer   = audioToneAnalyzer,
            billingManager      = billingManager,
            moodCheckInDao      = moodCheckInDao,
            insightsGenerator   = insightsGenerator,
            modelDownloader     = modelDownloader,
            piperVoiceManager   = piperVoiceManager,
            deviceCapabilityDetector = deviceCapabilityDetector,
            aiEvaluationEngine  = aiEvaluationEngine,
            langfuseTraceManager = langfuseTraceManager,
            diaryEntryDao       = diaryEntryDao,
            feedbackDao         = feedbackDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyMessages_andNeutralEmotion() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.messages.value.isEmpty())
        assertEquals(Emotion.NEUTRAL, viewModel.currentEmotion.value)
        assertFalse(viewModel.isListening.value)
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun sendMessage_addsUserMessageImmediately() = runTest {
        val responseTokens = "Great to hear from you!"
        coEvery { conversationManager.buildContext(any(), any(), any(), any()) } returns mockk(relaxed = true) {
            every { buildPrompt() } returns "prompt"
        }
        every { responseEngine.generateResponse(any()) } returns flowOf(responseTokens)

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        val msgs = viewModel.messages.value
        assertTrue(msgs.any { it.role == MessageRole.USER && it.content == "Hello" })
    }

    @Test
    fun sendMessage_addsAssistantResponse_afterGeneration() = runTest {
        val assistantText = "I'm doing well, thank you!"
        coEvery { conversationManager.buildContext(any(), any(), any(), any()) } returns mockk(relaxed = true) {
            every { buildPrompt() } returns "prompt"
        }
        every { responseEngine.generateResponse(any()) } returns flowOf(assistantText)

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendMessage("How are you?")
        testDispatcher.scheduler.advanceUntilIdle()

        val msgs = viewModel.messages.value
        assertTrue(msgs.any { it.role == MessageRole.ASSISTANT && it.content == assistantText })
    }

    @Test
    fun sendMessage_withBlankText_isIgnored() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendMessage("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun clearError_resetsErrorMessage_toNull() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun startNewConversation_clearsMessages() = runTest {
        coEvery { conversationManager.buildContext(any(), any(), any(), any()) } returns mockk(relaxed = true) {
            every { buildPrompt() } returns "prompt"
        }
        every { responseEngine.generateResponse(any()) } returns flowOf("response")

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendMessage("msg")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startNewConversation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.messages.value.isEmpty())
    }
}
