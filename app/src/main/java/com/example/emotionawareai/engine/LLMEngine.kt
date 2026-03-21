package com.example.emotionawareai.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin facade over the native LLM inference engine.
 *
 * The actual inference is handled by [llm_engine.cpp] via JNI. Swap the stub
 * implementation for llama.cpp by following the integration points in that file.
 */
@Singleton
class LLMEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var nativeHandle: Long = 0L

    val isLoaded: Boolean get() = nativeHandle != 0L

    /**
     * Loads a .gguf model file from the app's files directory.
     * Call this once on a background thread before generating responses.
     */
    suspend fun loadModel(modelFileName: String = DEFAULT_MODEL_FILE): Boolean =
        withContext(Dispatchers.IO) {
            if (isLoaded) return@withContext true

            val modelFile = File(context.filesDir, "models/$modelFileName")
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found at ${modelFile.absolutePath}; using stub mode")
                // Allow stub mode — native code handles a missing file gracefully
            }

            Log.i(TAG, "Loading model: ${modelFile.absolutePath}")
            nativeHandle = nativeLoadModel(modelFile.absolutePath)

            if (nativeHandle == 0L) {
                Log.e(TAG, "Failed to load model")
            } else {
                Log.i(TAG, "Model loaded successfully (handle=$nativeHandle)")
            }
            nativeHandle != 0L
        }

    /**
     * Generates a response token-by-token for [prompt].
     * Each emitted [String] is one or more tokens; collect them to build the
     * full response.
     */
    fun generateResponse(prompt: String): Flow<String> = flow {
        if (!isLoaded) {
            Log.w(TAG, "Model not loaded — returning contextual stub response")
            emit(generateStubResponse(prompt))
            return@flow
        }

        val tokenBuffer = StringBuilder()

        val success = nativeGenerateResponse(nativeHandle, prompt) { token: String ->
            tokenBuffer.append(token)
        }

        if (success) {
            // Re-emit as a single chunk; for true streaming the C++ layer
            // would call the callback per-token and we'd emit inside the lambda.
            // Restructure to Flow<String> emission here once llama.cpp is wired in.
            emit(tokenBuffer.toString())
        } else {
            Log.e(TAG, "Native generation failed")
            emit("Sorry, I encountered an error generating a response.")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generates a varied, contextual stub response when no real model is loaded.
     * Parses the emotion hint and user message from the structured prompt, then
     * selects from emotion-keyed response pools so no two turns feel identical.
     */
    private fun generateStubResponse(prompt: String): String {
        val emotionHint = extractEmotionHint(prompt)
        val userMessage = extractUserMessage(prompt).ifBlank { "hello" }
        val pool = STUB_RESPONSE_POOLS[emotionHint] ?: STUB_RESPONSE_POOLS["NEUTRAL"]!!
        // Use the hash of the user message to deterministically but variedly index
        val index = Math.abs(userMessage.lowercase().hashCode()) % pool.size
        return pool[index]
    }

    /** Extracts the first matched emotion keyword from the [SYSTEM] line. */
    private fun extractEmotionHint(prompt: String): String {
        val systemLine = prompt.lines().firstOrNull { it.startsWith("[SYSTEM]") } ?: return "NEUTRAL"
        return when {
            systemLine.contains("happy", ignoreCase = true) -> "HAPPY"
            systemLine.contains("sad", ignoreCase = true) -> "SAD"
            systemLine.contains("angry", ignoreCase = true) ||
                systemLine.contains("frustrated", ignoreCase = true) -> "ANGRY"
            systemLine.contains("surprised", ignoreCase = true) -> "SURPRISED"
            systemLine.contains("fearful", ignoreCase = true) ||
                systemLine.contains("anxious", ignoreCase = true) -> "FEARFUL"
            systemLine.contains("disgusted", ignoreCase = true) -> "DISGUSTED"
            else -> "NEUTRAL"
        }
    }

    /** Extracts the user's text from the [USER] tag in the prompt. */
    private fun extractUserMessage(prompt: String): String {
        val userLine = prompt.lines().firstOrNull { it.startsWith("[USER]") } ?: return ""
        return userLine.removePrefix("[USER]").trim()
    }

    /**
     * Releases all native resources. Safe to call multiple times.
     */
    fun release() {
        if (nativeHandle != 0L) {
            nativeReleaseModel(nativeHandle)
            nativeHandle = 0L
            Log.i(TAG, "Model released")
        }
    }

    // ── JNI declarations ─────────────────────────────────────────────────────

    private external fun nativeLoadModel(modelPath: String): Long

    private external fun nativeGenerateResponse(
        handle: Long,
        prompt: String,
        tokenCallback: (String) -> Unit
    ): Boolean

    private external fun nativeReleaseModel(handle: Long)

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "LLMEngine"
        const val DEFAULT_MODEL_FILE = "model.gguf"

        /**
         * Emotion-keyed pools of stub responses. Each pool must have at least 5
         * entries so the hash-based index can produce meaningfully varied output.
         */
        private val STUB_RESPONSE_POOLS: Map<String, List<String>> = mapOf(
            "HAPPY" to listOf(
                "That's wonderful! Your positive energy is contagious — keep riding that wave! 🌟",
                "Love the good vibes! It sounds like things are going really well for you right now.",
                "Your happiness really comes through! Moments like these are worth savoring.",
                "That's great to hear! Positivity like yours makes a real difference — keep going!",
                "Awesome! Life's good moments deserve to be celebrated, and this sounds like one of them.",
                "You seem to be in a really good place. That joy in your message is uplifting!"
            ),
            "SAD" to listOf(
                "I hear you, and it's okay to feel this way. You don't have to go through it alone.",
                "That sounds really tough. Remember, it's brave to acknowledge when things feel hard.",
                "I'm sorry you're feeling this way. These feelings are valid — take it one step at a time.",
                "Sometimes life is heavy. It's okay to rest and take things slowly. I'm here for you.",
                "That sounds painful, and your feelings completely make sense. Be kind to yourself today.",
                "Sending you warmth and care. Even small steps forward matter more than you know."
            ),
            "ANGRY" to listOf(
                "That frustration sounds real and valid. Let's take a breath and work through this together.",
                "I can hear how frustrated you are. Your feelings are completely understandable here.",
                "It makes sense to feel angry about that. Let's think about what might help right now.",
                "That sounds genuinely irritating. It's okay to feel strongly — what would help most?",
                "Your frustration comes through clearly. Sometimes just naming it out loud helps a bit.",
                "Strong emotions like this signal something important matters to you. Let's talk through it."
            ),
            "SURPRISED" to listOf(
                "Whoa, that does sound unexpected! How are you processing everything?",
                "Surprises can be a lot to take in! Give yourself a moment to settle.",
                "That's quite a curveball! Take it step by step — you've got this.",
                "Unexpected things can be overwhelming. It's totally normal to need a moment.",
                "That does sound surprising! Once the dust settles, things usually become clearer.",
                "Life throws curveballs sometimes. The good news is you're resilient enough to handle them."
            ),
            "FEARFUL" to listOf(
                "It's okay to feel anxious — these feelings are real, and you're not alone in them.",
                "Take a slow breath with me. Fear is your mind trying to protect you; it doesn't have to control you.",
                "That sounds scary, and your feelings make complete sense. Let's ground ourselves for a moment.",
                "Anxiety can feel overwhelming, but it does pass. You're stronger than you think.",
                "It's okay to feel uncertain. One small step at a time is all anyone ever needs.",
                "I'm here. Fear means you care about something important. Let's take this gently."
            ),
            "DISGUSTED" to listOf(
                "That does sound unpleasant. Your reaction makes total sense given the circumstances.",
                "Ugh, I completely understand why that would bother you. Your feelings are valid here.",
                "Sometimes things just don't sit right with us — and that discomfort is worth listening to.",
                "That sounds really off-putting. It's okay to set boundaries around things that disturb you.",
                "Your reaction is natural. Not everything deserves our tolerance, and instincts exist for a reason.",
                "I hear the discomfort in that. Trust your gut — it's usually pointing at something real."
            ),
            "NEUTRAL" to listOf(
                "That's an interesting thought. I'd love to explore this topic more with you.",
                "Good point! Tell me more — I'm all ears and genuinely curious about your perspective.",
                "Hmm, that's worth thinking about. What's your take on it?",
                "Interesting! There's always more to unpack here. What matters most to you about this?",
                "I hear you. What would be most helpful for you right now?",
                "Thanks for sharing that. Let's dig a little deeper — what's on your mind?",
                "That makes sense to me. Where would you like to take this conversation?"
            )
        )

        init {
            System.loadLibrary("llm_engine")
        }
    }
}
