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
            Log.w(TAG, "Model not loaded — returning stub response")
            emit("(Model not loaded. Place a .gguf file in files/models/ to enable inference.)")
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

        init {
            System.loadLibrary("llm_engine")
        }
    }
}
