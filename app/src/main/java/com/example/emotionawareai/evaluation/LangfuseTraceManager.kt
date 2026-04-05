package com.example.emotionawareai.evaluation

import android.util.Log
import com.example.emotionawareai.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends trace and score data to Langfuse for AI evaluation and observability.
 *
 * Uses the Langfuse REST API to create traces, spans, generations, and scores.
 * API keys are read from [BuildConfig] fields populated from GitHub Secrets
 * at build time. When keys are empty (local development), tracing is silently
 * skipped.
 */
@Singleton
class LangfuseTraceManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = BuildConfig.LANGFUSE_BASE_URL.trimEnd('/')
    private val publicKey: String = BuildConfig.LANGFUSE_PUBLIC_KEY
    private val secretKey: String = BuildConfig.LANGFUSE_SECRET_KEY

    val isEnabled: Boolean
        get() = publicKey.isNotBlank() && secretKey.isNotBlank()

    /**
     * Creates a new trace for a conversation turn and returns its ID.
     */
    fun createTrace(
        userId: String,
        sessionId: String,
        input: String,
        metadata: Map<String, Any> = emptyMap()
    ): String {
        val traceId = UUID.randomUUID().toString()
        if (!isEnabled) return traceId

        scope.launch {
            val body = JsonObject().apply {
                addProperty("id", traceId)
                addProperty("name", "chat_turn")
                addProperty("userId", userId)
                addProperty("sessionId", sessionId)
                addProperty("input", input)
                add("metadata", gson.toJsonTree(metadata))
            }
            postToLangfuse("/api/public/traces", body)
        }
        return traceId
    }

    /**
     * Records an LLM generation event within a trace.
     */
    fun recordGeneration(
        traceId: String,
        modelName: String,
        prompt: String,
        completion: String,
        latencyMs: Long,
        tokenCount: Int = 0
    ) {
        if (!isEnabled) return

        scope.launch {
            val body = JsonObject().apply {
                addProperty("traceId", traceId)
                addProperty("name", "llm_generation")
                addProperty("model", modelName)
                addProperty("input", prompt)
                addProperty("output", completion)
                add("usage", JsonObject().apply {
                    addProperty("totalTokens", tokenCount)
                })
                add("metadata", JsonObject().apply {
                    addProperty("latencyMs", latencyMs)
                })
            }
            postToLangfuse("/api/public/generations", body)
        }
    }

    /**
     * Submits evaluation scores (from automated metrics or user feedback)
     * to the Langfuse scoring API.
     */
    fun submitScore(
        traceId: String,
        metricName: String,
        score: Float,
        comment: String = ""
    ) {
        if (!isEnabled) return

        scope.launch {
            val body = JsonObject().apply {
                addProperty("traceId", traceId)
                addProperty("name", metricName)
                addProperty("value", score)
                if (comment.isNotBlank()) addProperty("comment", comment)
            }
            postToLangfuse("/api/public/scores", body)
        }
    }

    /**
     * Submits user feedback (star rating + comment) as a Langfuse score.
     */
    fun submitUserFeedback(
        traceId: String,
        rating: Int,
        comment: String = ""
    ) {
        submitScore(
            traceId = traceId,
            metricName = "user_feedback",
            score = rating / 5.0f,
            comment = comment
        )
    }

    private fun postToLangfuse(path: String, body: JsonObject) {
        try {
            val json = gson.toJson(body)
            val mediaType = "application/json".toMediaType()
            val credentials = okhttp3.Credentials.basic(publicKey, secretKey)

            val request = Request.Builder()
                .url("$baseUrl$path")
                .addHeader("Authorization", credentials)
                .addHeader("Content-Type", "application/json")
                .post(json.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Langfuse API error ${response.code} on $path: ${response.body?.string()?.take(200)}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Langfuse trace failed on $path: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "LangfuseTraceManager"
    }
}
