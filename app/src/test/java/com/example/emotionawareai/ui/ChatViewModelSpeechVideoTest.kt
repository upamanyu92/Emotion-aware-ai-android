package com.example.emotionawareai.ui

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
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

        coEvery { conversationManager.ensureConversation() } returns 1L
        coEvery { conversationManager.getActiveConversationId() } returns 1L
        coEvery { memoryManager.getRecentContext(any(), any()) } returns emptyList()
        coEvery { memoryManager.isTtsEnabled() } returns true
        coEvery { memoryManager.isContinuousConversationEnabled() } returns false
        coEvery { memoryManager.isPremiumUnlocked() } returns true
        coEvery { memoryManager.setPremiumUnlocked(any()) } returns Unit
        coEvery { memoryManager.isPremiumFeaturesGloballyEnabled() } returns true
        coEvery { responseEngine.loadModel() } returns true

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

        viewModel = ChatViewModel(
            conversationManager = conversationManager,
            responseEngine = responseEngine,
            emotionDetector = emotionDetector,
            activityAnalyzer = activityAnalyzer,
            voiceProcessor = voiceProcessor,
            memoryManager = memoryManager,
            audioToneAnalyzer = audioToneAnalyzer,
            billingManager = billingManager
        )
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
    fun `continuous conversation restarts listening after voice response`() = runTest {
        advanceUntilIdle()

        viewModel.onPermissionsResult(cameraGranted = false, audioGranted = true)
        viewModel.toggleContinuousConversation()
        verify(exactly = 1) { voiceProcessor.startListening(any()) }

        voiceTextFlow.emit("hello")
        advanceUntilIdle()
        advanceTimeBy(350)
        advanceUntilIdle()

        verify(exactly = 2) { voiceProcessor.startListening(any()) }
        coVerify(exactly = 1) { memoryManager.setContinuousConversationEnabled(true) }
    }
}


