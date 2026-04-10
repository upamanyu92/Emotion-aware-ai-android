package com.example.emotionawareai.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin facade over the native LLM inference engine (llama.cpp via JNI).
 *
 * The actual inference is handled by [llm_engine.cpp] which uses the llama.cpp
 * library to run any GGUF model downloaded by [ModelDownloader].
 *
 * Key improvements over the original stub implementation:
 *  - True per-token streaming via [callbackFlow] — each token emitted as it is
 *    produced by the C++ sampling loop, enabling real-time UI updates.
 *  - Unique per-model file names prevent models from overwriting each other.
 *  - Thread count is auto-tuned to device CPU core count.
 *  - Load/inference failures surface a human-readable error via [lastError].
 *  - [countTokens] allows callers to check prompt budget against [N_CTX].
 */
@Singleton
class LLMEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var nativeHandle: Long = 0L
    private val stubResponseCounter = AtomicInteger(0)

    /** Last error message from the native layer, or empty string if no error. */
    var lastError: String = ""
        private set

    val isLoaded: Boolean get() = nativeHandle != 0L

    /**
     * Returns `true` if the model file is present on-disk (without loading it).
     * Use this to detect "stub mode" before calling [loadModel].
     */
    fun isModelFileAvailable(modelFileName: String = DEFAULT_MODEL_FILE): Boolean =
        ModelFileLocator.isAvailable(context.filesDir, modelFileName)

    /**
     * The absolute path where the model file is expected.
     * Display this to the user so they know where to place the .gguf file.
     */
    fun modelFilePath(modelFileName: String = DEFAULT_MODEL_FILE): String =
        ModelFileLocator.path(context.filesDir, modelFileName)

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
                return@withContext false
            }

            Log.i(TAG, "Loading model: ${modelFile.absolutePath}")
            nativeHandle = nativeLoadModel(modelFile.absolutePath, recommendedThreadCount())

            if (nativeHandle == 0L) {
                lastError = nativeGetLastError()
                Log.e(TAG, "Failed to load model: $lastError")
            } else {
                lastError = ""
                Log.i(TAG, "Model loaded successfully (handle=$nativeHandle)")
            }
            nativeHandle != 0L
        }

    /**
     * Copies model data from [inputStream] into the expected on-device location and
     * immediately loads the installed model. If a model is already loaded it is
     * released first so the new file takes effect.
     *
     * @return `true` if the file was installed and loaded successfully.
     */
    suspend fun installAndLoadModel(
        inputStream: java.io.InputStream,
        modelFileName: String = DEFAULT_MODEL_FILE
    ): Boolean = withContext(Dispatchers.IO) {
        release()
        val installed = ModelFileLocator.installFromInputStream(
            context.filesDir, inputStream, modelFileName
        )
        if (!installed) return@withContext false
        loadModel(modelFileName)
    }

    /**
     * Returns the number of tokens the loaded model's tokenizer would produce
     * for [text]. Returns -1 if the model is not loaded. Useful for checking
     * whether a prompt would exceed the context window before inference.
     */
    fun countTokens(text: String): Int {
        if (!isLoaded) return -1
        return nativeCountTokens(nativeHandle, text)
    }

    /**
     * Generates a response token-by-token for [prompt].
     *
     * Each emitted [String] is a single text piece as produced by the llama.cpp
     * token-to-piece conversion. Collect the stream to assemble the full response.
     *
     * When the model is not loaded (stub mode) the flow emits a single contextual
     * response string from the [STUB_RESPONSE_POOLS] and completes.
     */
    fun generateResponse(prompt: String): Flow<String> = callbackFlow {
        if (!isLoaded) {
            Log.w(TAG, "Model not loaded — returning contextual stub response")
            trySend(generateStubResponse(prompt))
            close()
            return@callbackFlow
        }

        // Launch inference on IO thread. nativeGenerateResponse blocks until
        // the full response is produced, calling the lambda for each token piece.
        val inferenceJob = launch(Dispatchers.IO) {
            val success = nativeGenerateResponse(nativeHandle, prompt) { token: String ->
                trySend(token)
            }
            if (!success) {
                val err = nativeGetLastError()
                lastError = err.ifBlank { "Unknown inference error" }
                Log.e(TAG, "Native generation failed: $lastError")
                trySend("Sorry, I encountered an error generating a response.")
            }
            close()
        }

        awaitClose { inferenceJob.cancel() }
    }.buffer(Channel.UNLIMITED) // prevent backpressure drops during slow consumers

    /**
     * Generates a varied, contextual stub response when no real model is loaded.
     * Parses the emotion hint and user message from the structured prompt, then
     * selects from emotion-keyed response pools so no two turns feel identical.
     */
    private fun generateStubResponse(prompt: String): String {
        val emotionHint = extractEmotionHint(prompt)
        val userMessage = extractUserMessage(prompt).ifBlank { "hello" }
        val pool = STUB_RESPONSE_POOLS[emotionHint] ?: STUB_RESPONSE_POOLS["NEUTRAL"]!!
        val turnOffset = stubResponseCounter.getAndIncrement()
        val mixedSeed = (userMessage.lowercase().hashCode() + turnOffset) and 0x7FFFFFFF
        val index = mixedSeed % pool.size
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

    /** Loads the GGUF model at [modelPath] with [nThreads] inference threads.
     *  Returns a native handle on success, 0L on failure. */
    private external fun nativeLoadModel(modelPath: String, nThreads: Int): Long

    private external fun nativeGenerateResponse(
        handle: Long,
        prompt: String,
        tokenCallback: (String) -> Unit
    ): Boolean

    /** Returns the token count for [text] using the loaded model's tokenizer. */
    private external fun nativeCountTokens(handle: Long, text: String): Int

    /** Returns the last error message set by the native layer, or empty string. */
    private external fun nativeGetLastError(): String

    private external fun nativeReleaseModel(handle: Long)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a thread count tuned to the device's CPU core count.
     * Uses slightly fewer than all cores to leave headroom for the UI thread and
     * other Android system services running concurrently:
     *   - 8+ cores (e.g. Pixel 8 / Snapdragon 8 Gen 3): use 6 — leaves 2 for the OS.
     *   - 6–7 cores (mid-range): use 4 — keeps UI responsive.
     *   - < 6 cores (budget/legacy): use 2 — avoids thermal throttling on weak CPUs.
     */
    private fun recommendedThreadCount(): Int =
        when (Runtime.getRuntime().availableProcessors()) {
            in 8..Int.MAX_VALUE -> 6
            in 6..7 -> 4
            else -> 2
        }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "LLMEngine"

        /** Context window in tokens (matches N_CTX in llm_engine.cpp). */
        const val N_CTX = 2048

        /**
         * Default model file name — matches [LlmOption.CONFIGURED_MODEL.modelFileName].
         * Callers should always prefer passing the specific option's [modelFileName]
         * rather than relying on this constant to avoid silent file collisions.
         */
        const val DEFAULT_MODEL_FILE = "smollm2_135m_q4.gguf"

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
