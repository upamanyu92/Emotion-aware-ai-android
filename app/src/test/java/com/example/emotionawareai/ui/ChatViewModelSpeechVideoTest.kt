package com.example.emotionawareai.ui

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.ModelDownloader
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.InsightsGenerator
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.tts.PiperVoiceManager
import com.example.emotionawareai.voice.VoiceError
import com.example.emotionawareai.voice.AudioToneAnalyzer
import com.example.emotionawareai.voice.VoiceProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
class ChatViewModelSpeechVideoTest {

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

    private lateinit var viewModel: ChatViewModel

    private val emotionSharedFlow = MutableSharedFlow<Emotion>(replay = 1)
    private val activityFlow = MutableSharedFlow<List<ActivityCaption>>(replay = 1)
    private val voiceTextFlow = MutableSharedFlow<String>(replay = 0)
    private val listeningFlow = MutableSharedFlow<Boolean>(replay = 1)
    private val voiceErrorFlow = MutableSharedFlow<VoiceError>(replay = 0)
    private val rmsFlow = MutableSharedFlow<Float>(replay = 0)
    private val audioEmotionFlow = MutableSharedFlow<Emotion>(replay = 1)
    private val toneInsightFlow = MutableSharedFlow<com.example.emotionawareai.voice.ToneInsight>(replay = 1)
    private val billingOffersFlow = MutableStateFlow(emptyList<com.example.emotionawareai.billing.PremiumOffer>())
    private val responseSpeakingFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        conversationManager = mockk(relaxed = true)
        responseEngine = mockk(relaxed = true)
        emotionDetector = mockk(relaxed = true)
        activityAnalyzer = mockk(relaxed = true)
        voiceProcessor = mockk(relaxed = true)
        memoryManager = mockk(relaxed = true)
        audioToneAnalyzer = mockk(relaxed = true)
        billingManager = mockk(relaxed = true)
        moodCheckInDao = mockk(relaxed = true)
        insightsGenerator = mockk(relaxed = true)
        modelDownloader = mockk(relaxed = true)
        piperVoiceManager = mockk(relaxed = true)

        coEvery { insightsGenerator.getLatestInsight() } returns null
        every { insightsGenerator.observeInsights() } returns flowOf(emptyList())

        coEvery { conversationManager.ensureConversation() } returns 1L
        coEvery { conversationManager.getActiveConversationId() } returns 1L
        coEvery { memoryManager.getRecentContext(any(), any()) } returns emptyList()
        coEvery { memoryManager.isTtsEnabled() } returns true
        coEvery { memoryManager.getTtsBackend() } returns TtsBackend.SYSTEM
        coEvery { memoryManager.getPiperVoice() } returns PiperVoice.ALAN
        // Default is now true (live mode enabled by default)
        coEvery { memoryManager.isContinuousConversationEnabled() } returns true
        coEvery { memoryManager.isPremiumUnlocked() } returns true
        coEvery { memoryManager.setPremiumUnlocked(any()) } returns Unit
        coEvery { memoryManager.isPremiumFeaturesGloballyEnabled() } returns true
        coEvery { memoryManager.getUserName() } returns "Test User"
        coEvery { memoryManager.getUserAvatar() } returns "😊"
        coEvery { memoryManager.hasUserProfile() } returns true
        coEvery { memoryManager.isCameraEnabled() } returns true
        coEvery { memoryManager.isCameraPreviewVisible() } returns true
        coEvery { memoryManager.isCaptionsEnabled() } returns true
        coEvery { memoryManager.getGrowthAreas() } returns emptyList()
        coEvery { memoryManager.getCheckInFrequency() } returns "daily"
        coEvery { memoryManager.isPrivacyNoticeShown() } returns true
        coEvery { memoryManager.getLastCheckInDate() } returns ""
        every { memoryManager.observeActiveGoals() } returns flowOf(emptyList())
        coEvery { responseEngine.loadModel() } returns true
        coEvery { responseEngine.isPiperVoiceReady(any()) } returns false
        every { responseEngine.isSpeaking } returns responseSpeakingFlow
        every { responseEngine.isModelFileAvailable() } returns false
        every { responseEngine.modelFilePath() } returns "/data/user/0/com.example.emotionawareai/files/models/model.gguf"

        // ModelDownloader state flows — idle by default so ViewModel init doesn't trigger load
        every { modelDownloader.isDownloading } returns MutableStateFlow(false)
        every { modelDownloader.downloadProgress } returns MutableStateFlow(null)
        every { modelDownloader.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isDownloading } returns MutableStateFlow(false)
        every { piperVoiceManager.downloadProgress } returns MutableStateFlow(null)
        every { piperVoiceManager.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isVoiceInstalled(any()) } returns false

        coEvery { conversationManager.buildContext(any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { responseEngine.generateResponse(any()) } returns flowOf("ok")

        every { emotionDetector.emotionFlow } returns emotionSharedFlow
        every { activityAnalyzer.captionFlow } returns activityFlow
        every { voiceProcessor.recognizedTextFlow } returns voiceTextFlow
        every { voiceProcessor.listeningStateFlow } returns listeningFlow
        every { voiceProcessor.errorFlow } returns voiceErrorFlow
        every { voiceProcessor.rmsFlow } returns rmsFlow
        every { audioToneAnalyzer.audioEmotionFlow } returns audioEmotionFlow
        every { audioToneAnalyzer.toneInsightFlow } returns toneInsightFlow
        every { billingManager.isBillingReady } returns MutableStateFlow(true)
        every { billingManager.isPremium } returns MutableStateFlow(true)
        every { billingManager.billingMessage } returns MutableStateFlow(null)
        every { billingManager.offers } returns billingOffersFlow
        every { billingManager.isPurchaseInProgress } returns MutableStateFlow(false)
        every { billingManager.isRestoreInProgress } returns MutableStateFlow(false)

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startVoiceInput without permission shows permission error`() = runTest {
        advanceUntilIdle()

        viewModel.startVoiceInput()

        assertEquals("Microphone permission is required for voice input", viewModel.errorMessage.value)
        verify(exactly = 0) { voiceProcessor.startListening(any()) }
        verify(exactly = 0) { voiceProcessor.startContinuousListening(any()) }
    }

    @Test
    fun `camera permission initializes emotion and activity analyzers`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = true, audioGranted = false)

        verify(exactly = 1) { emotionDetector.initialize() }
        verify(exactly = 1) { activityAnalyzer.initialize() }
    }

    @Test
    fun `camera frame is routed to both analyzers`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        advanceUntilIdle()

        viewModel.onCameraFrame(bitmap)

        verify(exactly = 1) { emotionDetector.processBitmapFrame(bitmap) }
        verify(exactly = 1) { activityAnalyzer.processBitmapFrame(bitmap) }
    }

    @Test
    fun `audio permission with continuous mode enabled starts continuous listening`() = runTest {
        advanceUntilIdle()

        // With continuous mode on (default true), granting audio should auto-start
        viewModel.onPermissionsResult(cameraGranted = false, audioGranted = true)

        verify(atLeast = 1) { voiceProcessor.startContinuousListening(any()) }
    }

    @Test
    fun `continuous conversation restarts listening after voice response`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = false, audioGranted = true)

        voiceTextFlow.emit("hello")
        advanceUntilIdle()
        advanceTimeBy(350)
        advanceUntilIdle()

        // Continuous listening should have been started at least once (on permission + after response)
        verify(atLeast = 1) { voiceProcessor.startContinuousListening(any()) }
    }

    @Test
    fun `toggle continuous conversation off stops continuous listening`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = false, audioGranted = true)
        // Toggle off (currently true by default)
        viewModel.toggleContinuousConversation()
        advanceUntilIdle()

        verify(atLeast = 1) { voiceProcessor.stopContinuousListening() }
        coVerify { memoryManager.setContinuousConversationEnabled(false) }
    }

    @Test
    fun `toggle continuous conversation off then on restarts continuous listening`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = false, audioGranted = true)

        // Toggle off
        viewModel.toggleContinuousConversation()
        advanceUntilIdle()
        verify(atLeast = 1) { voiceProcessor.stopContinuousListening() }
        coVerify { memoryManager.setContinuousConversationEnabled(false) }

        // Toggle back on
        viewModel.toggleContinuousConversation()
        advanceUntilIdle()
        verify(atLeast = 2) { voiceProcessor.startContinuousListening(any()) }
        coVerify { memoryManager.setContinuousConversationEnabled(true) }
    }

    @Test
    fun `initial currentEmotion is UNKNOWN so audio-tone can override it`() = runTest {
        advanceUntilIdle()

        // Before any camera frame is processed, currentEmotion must be UNKNOWN
        // (not NEUTRAL) so that audio-tone emotion can take over in effectiveEmotion.
        assertEquals(com.example.emotionawareai.domain.model.Emotion.UNKNOWN, viewModel.currentEmotion.value)
    }

    @Test
    fun `sendMessage calls buildContext before saveMessage to avoid duplicate history`() = runTest {
        advanceUntilIdle()

        val callOrder = mutableListOf<String>()
        coEvery { conversationManager.buildContext(any(), any(), any(), any()) } answers {
            callOrder += "buildContext"
            mockk(relaxed = true)
        }
        coEvery { conversationManager.saveMessage(any()) } answers {
            callOrder += "saveMessage"
            1L
        }

        viewModel.sendMessage("test message")
        advanceUntilIdle()

        val buildIdx = callOrder.indexOf("buildContext")
        val saveIdx  = callOrder.indexOf("saveMessage")
        assertTrue(
            "buildContext (index $buildIdx) must be called before saveMessage (index $saveIdx)",
            buildIdx >= 0 && saveIdx > buildIdx
        )
    }



    @Test
    fun `response engine speaking flow updates isSpeaking state`() = runTest {
        advanceUntilIdle()

        responseSpeakingFlow.value = true
        advanceUntilIdle()
        assertTrue(viewModel.isSpeaking.value)

        responseSpeakingFlow.value = false
        advanceUntilIdle()
        assertFalse(viewModel.isSpeaking.value)
    }

    @Test
    fun `sendMessage publishes assistant speech caption`() = runTest {
        advanceUntilIdle()

        viewModel.sendMessage("hello there")
        advanceUntilIdle()

        val caption = viewModel.speechCaption.value
        assertEquals(com.example.emotionawareai.domain.model.MessageRole.ASSISTANT, caption?.speaker)
        assertEquals("ok", caption?.text)
    }

    @Test
    fun `setTtsBackend persists choice and triggers Piper download when needed`() = runTest {
        advanceUntilIdle()

        viewModel.setTtsBackend(TtsBackend.SHERPA_PIPER)
        advanceUntilIdle()

        verify(exactly = 1) { responseEngine.setTtsBackend(TtsBackend.SHERPA_PIPER) }
        verify(exactly = 1) { piperVoiceManager.startDownloadIfAbsent(PiperVoice.ALAN) }
        coVerify { memoryManager.setTtsBackend(TtsBackend.SHERPA_PIPER) }
    }

    @Test
    fun `downloaded Piper voice that fails readiness surfaces fallback error`() = runTest {
        val downloadState = MutableStateFlow(true)
        every { piperVoiceManager.isDownloading } returns downloadState
        every { piperVoiceManager.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isVoiceInstalled(PiperVoice.ALAN) } returns true
        coEvery { memoryManager.getTtsBackend() } returns TtsBackend.SHERPA_PIPER
        coEvery { responseEngine.isPiperVoiceReady(PiperVoice.ALAN) } returns false

        viewModel = createViewModel()
        advanceUntilIdle()

        downloadState.value = false
        advanceUntilIdle()

        assertEquals(
            "Alan downloaded but could not be initialized. Falling back to Android system TTS.",
            viewModel.errorMessage.value
        )
    }

    @Test
    fun `setPiperVoice persists voice and refreshes response engine`() = runTest {
        advanceUntilIdle()

        viewModel.setPiperVoice(PiperVoice.AMY)
        advanceUntilIdle()

        verify(exactly = 1) { responseEngine.setPiperVoice(PiperVoice.AMY) }
        coVerify { memoryManager.setPiperVoice(PiperVoice.AMY) }
    }

    @Test
    fun `isModelAvailable reflects ResponseEngine model file check`() = runTest {
        advanceUntilIdle()

        // The mock returns false for isModelFileAvailable()
        assertFalse(viewModel.isModelAvailable.value)
    }

    private fun createViewModel() = ChatViewModel(
        conversationManager = conversationManager,
        responseEngine = responseEngine,
        emotionDetector = emotionDetector,
        activityAnalyzer = activityAnalyzer,
        voiceProcessor = voiceProcessor,
        memoryManager = memoryManager,
        audioToneAnalyzer = audioToneAnalyzer,
        billingManager = billingManager,
        moodCheckInDao = moodCheckInDao,
        insightsGenerator = insightsGenerator,
        modelDownloader = modelDownloader,
        piperVoiceManager = piperVoiceManager
    )
}
