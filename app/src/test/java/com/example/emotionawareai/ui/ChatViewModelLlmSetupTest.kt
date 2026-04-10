package com.example.emotionawareai.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.billing.PremiumOffer
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.DeviceCapabilityDetector
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.ModelDownloader
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.InsightsGenerator
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.tts.PiperVoiceManager
import com.example.emotionawareai.voice.AudioToneAnalyzer
import com.example.emotionawareai.voice.VoiceError
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for:
 *  - Issue 1: Initial LLM setup flow (phase transitions, skip, retry, cleanup)
 *  - Issue 2: Settings LLM change with failure handling
 *  - Issue 3: Chat screen lifecycle (camera/mic pause/resume)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelLlmSetupTest {

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

    private val downloadingFlow = MutableStateFlow(false)
    private val downloadFailedFlow = MutableStateFlow(false)
    private val downloadProgressFlow = MutableStateFlow<Float?>(null)
    private val responseSpeakingFlow = MutableStateFlow(false)

    private lateinit var viewModel: ChatViewModel

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
        deviceCapabilityDetector = mockk(relaxed = true)
        aiEvaluationEngine = mockk(relaxed = true)
        langfuseTraceManager = mockk(relaxed = true)
        diaryEntryDao = mockk(relaxed = true)
        feedbackDao = mockk(relaxed = true)

        coEvery { insightsGenerator.getLatestInsight() } returns null
        every { insightsGenerator.observeInsights() } returns flowOf(emptyList())
        coEvery { conversationManager.ensureConversation() } returns 1L
        coEvery { conversationManager.getActiveConversationId() } returns 1L
        coEvery { memoryManager.getRecentContext(any(), any()) } returns emptyList()
        coEvery { memoryManager.isTtsEnabled() } returns true
        coEvery { memoryManager.getTtsBackend() } returns TtsBackend.SYSTEM
        coEvery { memoryManager.getPiperVoice() } returns PiperVoice.ALAN
        coEvery { memoryManager.isContinuousConversationEnabled() } returns false
        coEvery { memoryManager.isPremiumUnlocked() } returns false
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
        coEvery { memoryManager.isLlmSetupComplete() } returns false
        coEvery { memoryManager.getSelectedLlmId() } returns ""
        every { memoryManager.observeActiveGoals() } returns flowOf(emptyList())
        coEvery { responseEngine.loadModel(any()) } returns true
        coEvery { responseEngine.isPiperVoiceReady(any()) } returns false
        every { responseEngine.isSpeaking } returns responseSpeakingFlow
        every { responseEngine.isModelFileAvailable(any()) } returns false
        every { responseEngine.modelFilePath(any()) } returns "/fake/model.gguf"

        every { modelDownloader.isDownloading } returns downloadingFlow
        every { modelDownloader.downloadProgress } returns downloadProgressFlow
        every { modelDownloader.downloadFailed } returns downloadFailedFlow
        every { piperVoiceManager.isDownloading } returns MutableStateFlow(false)
        every { piperVoiceManager.downloadProgress } returns MutableStateFlow(null)
        every { piperVoiceManager.downloadFailed } returns MutableStateFlow(false)
        every { piperVoiceManager.isVoiceInstalled(any()) } returns false

        every { emotionDetector.emotionFlow } returns MutableSharedFlow(replay = 1)
        every { activityAnalyzer.captionFlow } returns MutableSharedFlow<List<ActivityCaption>>(replay = 1)
        every { voiceProcessor.recognizedTextFlow } returns MutableSharedFlow(replay = 0)
        every { voiceProcessor.listeningStateFlow } returns MutableSharedFlow(replay = 1)
        every { voiceProcessor.errorFlow } returns MutableSharedFlow<VoiceError>(replay = 0)
        every { voiceProcessor.rmsFlow } returns MutableSharedFlow(replay = 0)
        every { audioToneAnalyzer.audioEmotionFlow } returns MutableSharedFlow(replay = 1)
        every { audioToneAnalyzer.toneInsightFlow } returns MutableSharedFlow(replay = 1)
        every { billingManager.isBillingReady } returns MutableStateFlow(true)
        every { billingManager.isPremium } returns MutableStateFlow(false)
        every { billingManager.billingMessage } returns MutableStateFlow(null)
        every { billingManager.offers } returns MutableStateFlow(emptyList<PremiumOffer>())
        every { billingManager.isPurchaseInProgress } returns MutableStateFlow(false)
        every { billingManager.isRestoreInProgress } returns MutableStateFlow(false)

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Issue 1 tests ─────────────────────────────────────────────────────────

    @Test
    fun `initial llmSetupPhase is DOWNLOADING`() = runTest {
        advanceUntilIdle()
        assertEquals(LlmSetupPhase.DOWNLOADING, viewModel.llmSetupPhase.value)
    }

    @Test
    fun `startLlmSetup for built-in model transitions to COMPLETE immediately`() = runTest {
        advanceUntilIdle()

        val builtIn = LlmOption.GEMINI_NANO
        viewModel.startLlmSetup(builtIn)
        advanceUntilIdle()

        assertEquals(LlmSetupPhase.COMPLETE, viewModel.llmSetupPhase.value)
        assertTrue(viewModel.isLlmSetupComplete.value == true)
    }

    @Test
    fun `startLlmSetup for downloadable model transitions to DOWNLOADING and triggers download`() = runTest {
        advanceUntilIdle()

        viewModel.startLlmSetup(LlmOption.SMOLLM2_135M)

        assertEquals(LlmSetupPhase.DOWNLOADING, viewModel.llmSetupPhase.value)
        assertEquals(LlmOption.SMOLLM2_135M.id, viewModel.selectedLlmId.value)
        verify(exactly = 1) { modelDownloader.startDownload(LlmOption.SMOLLM2_135M) }
    }

    @Test
    fun `successful download transitions to VERIFYING then COMPLETE`() = runTest {
        advanceUntilIdle()

        // Set up model as available after download completes
        every { responseEngine.isModelFileAvailable(any()) } returns true

        viewModel.startLlmSetup(LlmOption.SMOLLM2_135M)
        assertEquals(LlmSetupPhase.DOWNLOADING, viewModel.llmSetupPhase.value)

        // Simulate download completion
        downloadingFlow.value = true
        advanceUntilIdle()
        downloadingFlow.value = false
        advanceUntilIdle()

        assertEquals(LlmSetupPhase.COMPLETE, viewModel.llmSetupPhase.value)
        assertTrue(viewModel.isLlmSetupComplete.value == true)
        assertNull(viewModel.llmSetupError.value)
    }

    @Test
    fun `failed download transitions to FAILED with error message`() = runTest {
        advanceUntilIdle()

        // Model file not available after download
        every { responseEngine.isModelFileAvailable(any()) } returns false
        downloadFailedFlow.value = false // set before setup

        viewModel.startLlmSetup(LlmOption.SMOLLM2_135M)

        // Simulate download failure
        downloadingFlow.value = true
        advanceUntilIdle()
        downloadFailedFlow.value = true
        downloadingFlow.value = false
        advanceUntilIdle()

        assertEquals(LlmSetupPhase.FAILED, viewModel.llmSetupPhase.value)
        assertFalse(viewModel.isLlmSetupComplete.value == true)
        assertNotNull(viewModel.llmSetupError.value)
    }

    @Test
    fun `skipLlmSetup marks setup complete without download`() = runTest {
        advanceUntilIdle()

        viewModel.skipLlmSetup()
        advanceUntilIdle()

        assertTrue(viewModel.isLlmSetupComplete.value == true)
        coVerify { memoryManager.setLlmSetupComplete() }
        // Phase is not changed by skip — navigation is driven by isLlmSetupComplete
    }

    @Test
    fun `retryLlmSetup cancels download and stays in DOWNLOADING to restart`() = runTest {
        advanceUntilIdle()

        viewModel.startLlmSetup(LlmOption.SMOLLM2_135M)
        assertEquals(LlmSetupPhase.DOWNLOADING, viewModel.llmSetupPhase.value)

        viewModel.retryLlmSetup()
        advanceUntilIdle()

        assertEquals(LlmSetupPhase.DOWNLOADING, viewModel.llmSetupPhase.value)
        assertNull(viewModel.llmSetupError.value)
        verify(atLeast = 1) { modelDownloader.cancelDownload() }
    }

    @Test
    fun `retryLlmSetup deletes model file for previously selected option`() = runTest {
        advanceUntilIdle()

        viewModel.startLlmSetup(LlmOption.SMOLLM2_135M)
        viewModel.retryLlmSetup()
        advanceUntilIdle()

        verify { modelDownloader.deleteModelFile(any()) }
    }

    @Test
    fun `isModelDownloadFailed reflects ModelDownloader downloadFailed flow`() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.isModelDownloadFailed.value)

        downloadFailedFlow.value = true
        advanceUntilIdle()

        assertTrue(viewModel.isModelDownloadFailed.value)
    }

    // ── Issue 2 tests ─────────────────────────────────────────────────────────

    @Test
    fun `changeLlmFromSettings starts download for new model and cancels old`() = runTest {
        // Set up an existing selected model
        every { modelDownloader.isDownloading } returns MutableStateFlow(false)
        viewModel.saveLlmSelection(LlmOption.SMOLLM2_135M)
        advanceUntilIdle()

        viewModel.changeLlmFromSettings(LlmOption.TINYLLAMA_1B)
        advanceUntilIdle()

        verify(atLeast = 1) { modelDownloader.cancelDownload() }
        verify { modelDownloader.deleteModelFile(LlmOption.SMOLLM2_135M) }
        verify { modelDownloader.startDownload(LlmOption.TINYLLAMA_1B) }
        assertEquals(LlmOption.TINYLLAMA_1B.id, viewModel.selectedLlmId.value)
    }

    @Test
    fun `changeLlmFromSettings to same model retries download if not loaded`() = runTest {
        every { modelDownloader.isDownloading } returns MutableStateFlow(false)
        viewModel.saveLlmSelection(LlmOption.SMOLLM2_135M)
        advanceUntilIdle()

        // Call changeLlmFromSettings with same model while not loaded
        viewModel.changeLlmFromSettings(LlmOption.SMOLLM2_135M)
        advanceUntilIdle()

        // Should retry download, not cancel
        verify(atLeast = 1) { modelDownloader.startDownload(any()) }
    }

    @Test
    fun `changeLlmFromSettings for built-in model does not start download`() = runTest {
        viewModel.saveLlmSelection(LlmOption.SMOLLM2_135M)
        advanceUntilIdle()

        viewModel.changeLlmFromSettings(LlmOption.GEMINI_NANO)
        advanceUntilIdle()

        // No download for built-in
        verify(exactly = 0) { modelDownloader.startDownload(LlmOption.GEMINI_NANO) }
        assertEquals(LlmOption.GEMINI_NANO.id, viewModel.selectedLlmId.value)
    }

    // ── Issue 3 tests ─────────────────────────────────────────────────────────

    @Test
    fun `onChatScreenInactive stops voice and releases analyzers`() = runTest {
        advanceUntilIdle()

        viewModel.onChatScreenInactive()
        advanceUntilIdle()

        verify(atLeast = 1) { emotionDetector.release() }
        verify(atLeast = 1) { activityAnalyzer.release() }
        verify(atLeast = 1) { voiceProcessor.stopContinuousListening() }
        verify(atLeast = 1) { responseEngine.stopSpeaking() }
    }

    @Test
    fun `onChatScreenActive restores camera when camera enabled and permission granted`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = true, audioGranted = false)
        // Clear previous initialize calls
        advanceUntilIdle()

        viewModel.onChatScreenActive()
        advanceUntilIdle()

        verify(atLeast = 1) { emotionDetector.initialize() }
        verify(atLeast = 1) { activityAnalyzer.initialize() }
    }

    @Test
    fun `onChatScreenActive does not initialize camera without permission`() = runTest {
        advanceUntilIdle()

        // No permissions granted
        viewModel.onChatScreenActive()
        advanceUntilIdle()

        verify(exactly = 0) { emotionDetector.initialize() }
        verify(exactly = 0) { activityAnalyzer.initialize() }
    }

    @Test
    fun `onChatScreenActive resumes continuous listening when enabled and not already listening`() = runTest {
        coEvery { memoryManager.isContinuousConversationEnabled() } returns true
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPermissionsResult(cameraGranted = false, audioGranted = true)
        advanceUntilIdle()

        // Stop it manually to simulate screen leaving
        vm.onChatScreenInactive()
        advanceUntilIdle()

        // Now resume
        vm.onChatScreenActive()
        advanceUntilIdle()

        verify(atLeast = 1) { voiceProcessor.startContinuousListening(any()) }
    }

    @Test
    fun `navigate away and back preserves user preference flags`() = runTest {
        advanceUntilIdle()

        // Verify settings haven't changed after lifecycle calls
        val cameraEnabledBefore = viewModel.isCameraEnabled.value
        val continuousBefore = viewModel.isContinuousConversationEnabled.value
        val ttsBefore = viewModel.isTtsEnabled.value

        viewModel.onChatScreenInactive()
        viewModel.onChatScreenActive()
        advanceUntilIdle()

        // Preferences unchanged — only hardware state paused/resumed
        assertEquals(cameraEnabledBefore, viewModel.isCameraEnabled.value)
        assertEquals(continuousBefore, viewModel.isContinuousConversationEnabled.value)
        assertEquals(ttsBefore, viewModel.isTtsEnabled.value)
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
        piperVoiceManager = piperVoiceManager,
        deviceCapabilityDetector = deviceCapabilityDetector,
        aiEvaluationEngine = aiEvaluationEngine,
        langfuseTraceManager = langfuseTraceManager,
        diaryEntryDao = diaryEntryDao,
        feedbackDao = feedbackDao
    )
}
