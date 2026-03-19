package com.example.emotionawareai.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.emotionawareai.domain.model.ConversationContext
import com.example.emotionawareai.engine.LLMEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates: LLM inference → token streaming → optional TTS playback.
 *
 * The caller collects the returned [Flow] to receive response tokens as they
 * arrive and to build the full assistant message incrementally.
 */
@Singleton
class ResponseEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmEngine: LLMEngine
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsEnabled = true

    /**
     * Lazily initialises TextToSpeech on first use.
     */
    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                Log.i(TAG, "TextToSpeech initialized")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed (status=$status)")
            }
        }
    }

    /**
     * Generates a streamed response for the given [context].
     *
     * Returns a [Flow] of token strings. Accumulate them on the collector side
     * to build the full response. TTS speaks the complete response once the
     * flow completes.
     */
    fun generateResponse(context: ConversationContext): Flow<String> {
        val prompt = context.buildPrompt()
        val responseBuffer = StringBuilder()

        return llmEngine.generateResponse(prompt)
            .onEach { token -> responseBuffer.append(token) }
            .onCompletion { cause ->
                if (cause == null && ttsEnabled && responseBuffer.isNotBlank()) {
                    speakResponse(responseBuffer.toString())
                }
            }
    }

    /**
     * Speaks [text] using TTS. Initialises TTS lazily on first call.
     */
    fun speakResponse(text: String) {
        if (!ttsEnabled || text.isBlank()) return
        ensureTts()
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready — skipping speech")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun setTtsEnabled(enabled: Boolean) {
        ttsEnabled = enabled
        if (!enabled) stopSpeaking()
    }

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        llmEngine.loadModel()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        llmEngine.release()
        Log.i(TAG, "ResponseEngine released")
    }

    companion object {
        private const val TAG = "ResponseEngine"
        private const val UTTERANCE_ID = "emotion_aware_utterance"
    }
}
