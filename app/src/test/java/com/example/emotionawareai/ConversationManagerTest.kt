package com.example.emotionawareai

import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.domain.repository.ConversationRepository
import com.example.emotionawareai.domain.repository.IMemoryRepository
import com.example.emotionawareai.manager.ConversationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConversationManagerTest {

    private lateinit var repository: ConversationRepository
    private lateinit var memoryRepository: IMemoryRepository
    private lateinit var manager: ConversationManager

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        memoryRepository = mockk(relaxed = true)
        coEvery { memoryRepository.retrieveRelevant(any(), any()) } returns emptyList()
        manager = ConversationManager(repository, memoryRepository)
    }

    @Test
    fun `ensureConversation returns existing id when already initialised`() = runTest {
        coEvery { repository.getOrCreateActiveConversation() } returns 42L

        val first = manager.ensureConversation()
        val second = manager.ensureConversation()

        assertEquals(first, second)
        coVerify(exactly = 1) { repository.getOrCreateActiveConversation() }
    }

    @Test
    fun `buildContext includes correct emotion and recent history`() = runTest {
        val convId = 1L
        coEvery { repository.getOrCreateActiveConversation() } returns convId
        coEvery { repository.getRecentMessages(convId, any()) } returns listOf(
            ChatMessage(id = 1, content = "Hello", role = MessageRole.USER, emotion = Emotion.HAPPY),
            ChatMessage(id = 2, content = "Hi there!", role = MessageRole.ASSISTANT)
        )
        coEvery { repository.getPreference(any(), any()) } returns "EMPATHETIC"

        val context = manager.buildContext("How are you?", Emotion.HAPPY)

        assertEquals(convId, context.conversationId)
        assertEquals(Emotion.HAPPY, context.detectedEmotion)
        assertEquals("How are you?", context.userMessage)
        assertEquals(2, context.recentHistory.size)
    }

    @Test
    fun `buildContext prompt contains emotion hint`() = runTest {
        coEvery { repository.getOrCreateActiveConversation() } returns 1L
        coEvery { repository.getRecentMessages(any(), any()) } returns emptyList()
        coEvery { repository.getPreference(any(), any()) } returns "EMPATHETIC"

        val context = manager.buildContext("I'm feeling low", Emotion.SAD)
        val prompt = context.buildPrompt()

        assert(prompt.contains(Emotion.SAD.systemPromptHint)) {
            "Prompt should contain sad emotion hint"
        }
        assert(prompt.contains("I'm feeling low")) {
            "Prompt should contain user message"
        }
    }

    @Test
    fun `saveMessage delegates to repository`() = runTest {
        val convId = 5L
        coEvery { repository.getOrCreateActiveConversation() } returns convId
        val message = ChatMessage(
            content = "Test",
            role = MessageRole.USER,
            emotion = Emotion.NEUTRAL
        )

        manager.saveMessage(message)

        coVerify { repository.saveMessage(message, convId) }
    }

    @Test
    fun `startNewConversation resets active conversation id`() = runTest {
        val firstId = 1L
        val newId = 2L
        coEvery { repository.getOrCreateActiveConversation() } returns firstId
        coEvery { repository.createConversation(any()) } returns newId
        coEvery { repository.savePreference(any(), any()) } returns Unit

        manager.ensureConversation()
        manager.startNewConversation("Test Conv")

        assertEquals(newId, manager.getActiveConversationId())
    }

    @Test
    fun `buildPrompt format is correct`() = runTest {
        coEvery { repository.getOrCreateActiveConversation() } returns 1L
        coEvery { repository.getRecentMessages(any(), any()) } returns emptyList()
        coEvery { repository.getPreference(any(), any()) } returns "EMPATHETIC"

        val context = manager.buildContext("Hello AI", Emotion.NEUTRAL)
        val prompt = context.buildPrompt()

        assert(prompt.contains("[SYSTEM]")) { "Prompt must have [SYSTEM] tag" }
        assert(prompt.contains("[USER]"))   { "Prompt must have [USER] tag" }
        assert(prompt.contains("[ASSISTANT]")) { "Prompt must have [ASSISTANT] tag" }
    }

    @Test
    fun `buildContext history does not include the message being sent`() = runTest {
        val convId = 1L
        val previousMessages = listOf(
            ChatMessage(id = 1, content = "Hi there", role = MessageRole.USER),
            ChatMessage(id = 2, content = "Hello!", role = MessageRole.ASSISTANT)
        )
        coEvery { repository.getOrCreateActiveConversation() } returns convId
        coEvery { repository.getRecentMessages(convId, any()) } returns previousMessages
        coEvery { repository.getPreference(any(), any()) } returns "EMPATHETIC"

        val currentUserMessage = "What can you help me with?"
        val context = manager.buildContext(currentUserMessage, Emotion.NEUTRAL)
        val prompt = context.buildPrompt()

        val userTagCount = prompt.split("[USER]").size - 1
        assertEquals("Current user message must appear exactly once under [USER]", 1, userTagCount)

        assertEquals(2, context.recentHistory.size)
        assert(context.recentHistory.none { it.content == currentUserMessage }) {
            "Current user message must not appear in recentHistory"
        }
    }
}
