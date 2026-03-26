package com.example.emotionawareai.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the Microsoft BitNet b1.58 2B GGUF model from HuggingFace and
 * saves it to the app-private model directory so [LLMEngine] can load it.
 *
 * This class owns its own process-lifetime [CoroutineScope] so downloads can
 * be started from [Application.onCreate] before any Activity or ViewModel
 * exists. Progress and downloading state are exposed as [StateFlow]s that any
 * observer (e.g. [ChatViewModel]) can collect.
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Process-lifetime scope. Lives as long as the app process. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var downloadJob: Job? = null

    private val _isDownloading = MutableStateFlow(false)
    /** `true` while the BitNet model is being fetched from the network. */
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    /**
     * Download progress in `[0, 1]`, `-1f` when content-length is unknown,
     * or `null` when no download is in progress.
     */
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadFailed = MutableStateFlow(false)
    /** Becomes `true` when the last download attempt ended in failure. */
    val downloadFailed: StateFlow<Boolean> = _downloadFailed.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts a background download if the model is **not** already present.
     *
     * Safe to call from [Application.onCreate]: it is non-blocking and
     * idempotent — subsequent calls while a download is already running are
     * ignored, and calls when the file is present return immediately.
     */
    @Synchronized
    fun startDownloadIfAbsent(
        modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE
    ) {
        if (_isDownloading.value) return
        if (ModelFileLocator.isAvailable(context.filesDir, modelFileName)) {
            Log.i(TAG, "Model already present — skipping download")
            return
        }
        launchDownload(modelFileName)
    }

    /**
     * Starts a background download unconditionally, overwriting any existing
     * file. No-op if a download is already running.
     */
    @Synchronized
    fun startDownload(
        modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE
    ) {
        if (_isDownloading.value) return
        launchDownload(modelFileName)
    }

    /** Cancels any in-progress download. */
    @Synchronized
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _isDownloading.value = false
        _downloadProgress.value = null
        Log.i(TAG, "BitNet model download cancelled")
    }

    private fun launchDownload(modelFileName: String) {
        downloadJob = scope.launch {
            _downloadFailed.value = false
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val success = downloadBlocking(modelFileName) { progress ->
                    _downloadProgress.value = progress
                }
                if (!success) {
                    _downloadFailed.value = true
                    Log.e(TAG, "BitNet model download failed")
                }
            } finally {
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }

    // ── Suspend helpers (also usable from callers with their own scope) ───────

    /**
     * Downloads the BitNet model only when it is not already present on-device.
     *
     * @param modelFileName destination filename under `<filesDir>/models/`.
     * @param onProgress    called on the IO thread with values in `[0, 1]`,
     *                      or `-1f` when content-length is unknown.
     * @return `true` if the model is ready to load (was already present or was
     *         downloaded successfully), `false` on failure or cancellation.
     */
    suspend fun downloadIfAbsent(
        modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (ModelFileLocator.isAvailable(context.filesDir, modelFileName)) {
            Log.i(TAG, "Model already present — skipping download")
            return@withContext true
        }
        downloadBlocking(modelFileName, onProgress)
    }

    /**
     * Unconditionally downloads the BitNet model, overwriting any existing file.
     *
     * Redirects from HuggingFace (up to [MAX_REDIRECTS]) are followed manually
     * so that each hop uses the same timeout settings and can be cancelled.
     */
    suspend fun download(
        modelFileName: String = LLMEngine.DEFAULT_MODEL_FILE,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        downloadBlocking(modelFileName, onProgress)
    }

    // ── Private implementation ────────────────────────────────────────────────

    private suspend fun downloadBlocking(
        modelFileName: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
        val target = File(modelsDir, modelFileName)
        val tmpFile = File(modelsDir, "$modelFileName.tmp")

        var connection: HttpURLConnection? = null
        try {
            // Resolve redirects manually (HuggingFace uses HTTP 302 → CDN URL).
            var currentUrl = URL(BITNET_MODEL_URL)
            var redirectsLeft = MAX_REDIRECTS
            while (redirectsLeft-- > 0) {
                if (!isActive) {
                    Log.i(TAG, "Download cancelled before connecting")
                    return@withContext false
                }

                val conn = currentUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.instanceFollowRedirects = false
                conn.requestMethod = "GET"
                // Add user agent to avoid bot detection
                conn.setRequestProperty("User-Agent", "EmotionAwareAI/1.0 (Android)")
                connection = conn

                try {
                    conn.connect()
                    val code = conn.responseCode
                    Log.d(TAG, "HTTP response code: $code for URL: $currentUrl")

                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location")
                        if (location == null) {
                            Log.e(TAG, "Redirect response but no Location header")
                            return@withContext false
                        }
                        Log.d(TAG, "Following redirect to: $location")
                        conn.disconnect()
                        connection = null
                        currentUrl = URL(location)
                    } else if (code == HttpURLConnection.HTTP_OK) {
                        break
                    } else {
                        Log.e(TAG, "HTTP $code downloading BitNet model from $currentUrl")
                        val errorStream = conn.errorStream
                        if (errorStream != null) {
                            val errorBody = errorStream.bufferedReader().use { it.readText() }
                            Log.e(TAG, "Error response body: ${errorBody.take(500)}")
                        }
                        return@withContext false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to $currentUrl", e)
                    throw e
                }
            }

            val conn = connection ?: run {
                Log.e(TAG, "Too many redirects for $BITNET_MODEL_URL")
                return@withContext false
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP $responseCode downloading BitNet model")
                return@withContext false
            }

            val contentLength = conn.contentLengthLong
            Log.i(TAG, "Starting download, content length: $contentLength bytes")
            var bytesRead = 0L
            var downloadComplete = false

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        if (!isActive) {
                            Log.i(TAG, "Download cancelled by coroutine")
                            // downloadComplete stays false; tmpFile is cleaned up below.
                            return@use
                        }
                        output.write(buf, 0, n)
                        bytesRead += n
                        val progress = if (contentLength > 0) {
                            bytesRead.toFloat() / contentLength
                        } else {
                            -1f
                        }
                        onProgress(progress)

                        // Log progress periodically
                        if (bytesRead % (10 * 1024 * 1024) == 0L) {
                            Log.d(TAG, "Downloaded $bytesRead bytes...")
                        }
                    }
                    downloadComplete = true
                }
            }

            if (!downloadComplete) {
                tmpFile.delete()
                return@withContext false
            }

            // renameTo is atomic but may fail across mount points; fall back to copy+delete.
            if (!tmpFile.renameTo(target)) {
                try {
                    tmpFile.copyTo(target, overwrite = true)
                    tmpFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Rename fallback copy failed", e)
                    tmpFile.delete()
                    return@withContext false
                }
            }
            Log.i(TAG, "BitNet model downloaded to ${target.absolutePath} ($bytesRead bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "BitNet model download failed", e)
            tmpFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "ModelDownloader"

        /**
         * Official Microsoft BitNet b1.58 2B 4T model in IQ1_S GGUF format (~500 MB).
         * The filename `ggml-model-i2_s.gguf` is the HuggingFace distribution name for
         * the 1-bit IQ1_S quantization.
         *
         * Source: https://huggingface.co/microsoft/BitNet-b1.58-2B-4T-GGUF
         */
        const val BITNET_MODEL_URL =
            "https://huggingface.co/microsoft/BitNet-b1.58-2B-4T-GGUF/resolve/main/ggml-model-i2_s.gguf"

        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS    = 60_000
        private const val BUFFER_SIZE        = 32 * 1024 // 32 KB chunks
        private const val MAX_REDIRECTS      = 10
    }
}
