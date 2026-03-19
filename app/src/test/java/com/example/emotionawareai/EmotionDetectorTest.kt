package com.example.emotionawareai

import com.example.emotionawareai.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [Emotion] classification logic and helpers.
 *
 * The actual EmotionDetector uses MediaPipe and requires a real Android context,
 * so these tests focus on the domain model and the fromLabel factory.
 */
class EmotionDetectorTest {

    @Test
    fun `Emotion fromLabel returns correct enum for exact name match`() {
        assertEquals(Emotion.HAPPY,     Emotion.fromLabel("HAPPY"))
        assertEquals(Emotion.SAD,       Emotion.fromLabel("SAD"))
        assertEquals(Emotion.ANGRY,     Emotion.fromLabel("ANGRY"))
        assertEquals(Emotion.SURPRISED, Emotion.fromLabel("SURPRISED"))
        assertEquals(Emotion.FEARFUL,   Emotion.fromLabel("FEARFUL"))
        assertEquals(Emotion.DISGUSTED, Emotion.fromLabel("DISGUSTED"))
        assertEquals(Emotion.NEUTRAL,   Emotion.fromLabel("NEUTRAL"))
    }

    @Test
    fun `Emotion fromLabel is case insensitive`() {
        assertEquals(Emotion.HAPPY, Emotion.fromLabel("happy"))
        assertEquals(Emotion.SAD,   Emotion.fromLabel("Sad"))
        assertEquals(Emotion.ANGRY, Emotion.fromLabel("anGRY"))
    }

    @Test
    fun `Emotion fromLabel matches display name`() {
        assertEquals(Emotion.HAPPY,     Emotion.fromLabel("Happy"))
        assertEquals(Emotion.SAD,       Emotion.fromLabel("Sad"))
        assertEquals(Emotion.SURPRISED, Emotion.fromLabel("Surprised"))
        assertEquals(Emotion.NEUTRAL,   Emotion.fromLabel("Neutral"))
    }

    @Test
    fun `Emotion fromLabel returns UNKNOWN for unrecognised label`() {
        assertEquals(Emotion.UNKNOWN, Emotion.fromLabel(""))
        assertEquals(Emotion.UNKNOWN, Emotion.fromLabel("confused"))
        assertEquals(Emotion.UNKNOWN, Emotion.fromLabel("xyz123"))
    }

    @Test
    fun `all Emotion entries have non-blank display name, emoji and hint`() {
        for (emotion in Emotion.entries) {
            assert(emotion.displayName.isNotBlank()) {
                "${emotion.name} has blank displayName"
            }
            assert(emotion.emoji.isNotBlank()) {
                "${emotion.name} has blank emoji"
            }
            assert(emotion.systemPromptHint.isNotBlank()) {
                "${emotion.name} has blank systemPromptHint"
            }
        }
    }

    @Test
    fun `Emotion HAPPY and SAD have opposite sentiment hints`() {
        val happyHint  = Emotion.HAPPY.systemPromptHint.lowercase()
        val sadHint    = Emotion.SAD.systemPromptHint.lowercase()

        assert(happyHint.contains("happy") || happyHint.contains("positive")) {
            "HAPPY hint should mention happy or positive"
        }
        assert(sadHint.contains("sad") || sadHint.contains("support")) {
            "SAD hint should mention sad or support"
        }
    }

    @Test
    fun `Emotion isFromUser and isFromAssistant flags are correct`() {
        val userMsg = com.example.emotionawareai.domain.model.ChatMessage(
            content = "hi",
            role = com.example.emotionawareai.domain.model.MessageRole.USER
        )
        val assistMsg = com.example.emotionawareai.domain.model.ChatMessage(
            content = "hello",
            role = com.example.emotionawareai.domain.model.MessageRole.ASSISTANT
        )
        assert(userMsg.isFromUser)
        assert(!userMsg.isFromAssistant)
        assert(assistMsg.isFromAssistant)
        assert(!assistMsg.isFromUser)
    }
}
