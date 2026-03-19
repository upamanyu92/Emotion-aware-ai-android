package com.example.emotionawareai

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.voice.VoiceProcessor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private lateinit var voiceProcessor: VoiceProcessor
    private lateinit var memoryManager: MemoryManager
    private lateinit var viewModel: ChatViewModel

    private val emotionSharedFlow = MutableSharedFlow<Emotion>(replay = 1)
    private val voiceTextFlow     = MutableSharedFlow<String>(replay = 0)
    private val listeningFlow     = MutableSharedFlow<Boolean>(replay = 1)
    private val voiceErrorFlow    = MutableSharedFlow<com.example.emotionawareai.voice.VoiceError>(replay = 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        conversationManager = mockk(relaxed = true)
        responseEngine      = mockk(relaxed = true)
        emotionDetector     = mockk(relaxed = true)
        voiceProcessor      = mockk(relaxed = true)
        memoryManager       = mockk(relaxed = true)

        coEvery { conversationManager.ensureConversation() } returns 1L
        coEvery { conversationManager.getActiveConversationId() } returns 1L
        coEvery { memoryManager.getRecentContext(any(), any()) } returns emptyList()
        coEvery { memoryManager.isTtsEnabled() } returns true
        coEvery { responseEngine.loadModel() } returns true

        every { emotionDetector.emotionFlow }   returns emotionSharedFlow
        every { voiceProcessor.recognizedTextFlow } returns voiceTextFlow
        every { voiceProcessor.listeningStateFlow } returns listeningFlow
        every { voiceProcessor.errorFlow }       returns voiceErrorFlow

        viewModel = ChatViewModel(
            conversationManager = conversationManager,
            responseEngine      = responseEngine,
            emotionDetector     = emotionDetector,
            voiceProcessor      = voiceProcessor,
            memoryManager       = memoryManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty messages and neutral emotion`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.messages.value.isEmpty())
        assertEquals(Emotion.NEUTRAL, viewModel.currentEmotion.value)
        assertFalse(viewModel.isListening.value)
        assertFalse(viewModel.isGenerating.value)
    }

    @Test
    fun `sendMessage adds user message immediately`() = runTest {
        val responseTokens = "Great to hear from you!"
        coEvery { conversationManager.buildContext(any(), any()) } returns mockk(relaxed = true) {
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
    fun `sendMessage adds assistant response after generation`() = runTest {
        val assistantText = "I'm doing well, thank you!"
        coEvery { conversationManager.buildContext(any(), any()) } returns mockk(relaxed = true) {
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
    fun `sendMessage with blank text is ignored`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendMessage("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `startNewConversation clears messages`() = runTest {
        coEvery { conversationManager.buildContext(any(), any()) } returns mockk(relaxed = true) {
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
