package com.example.emotionawareai.engine

import android.content.Context
import android.util.Log
import com.example.emotionawareai.domain.model.LlmOption
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
 * Downloads the selected GGUF model from HuggingFace and saves it to the
 * app-private model directory so [LLMEngine] can load it.
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
    @Volatile private var activeOption: LlmOption? = null

    private val _isDownloading = MutableStateFlow(false)
    /** `true` while the selected model is being fetched from the network. */
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
     * idempotent — subsequent calls while a download for the same option is
     * already running are ignored, and calls when the file is present return
     * immediately.
     */
    @Synchronized
    fun startDownloadIfAbsent(option: LlmOption = LlmOption.BITNET_2B) {
        if (!option.isDownloadable()) {
            Log.i(TAG, "Skipping download for built-in option ${option.name}")
            return
        }
        if (_isDownloading.value) {
            if (activeOption?.id == option.id) return
            cancelDownload()
        }
        if (ModelFileLocator.isAvailable(context.filesDir, option.modelFileName)) {
            Log.i(TAG, "${option.name} already present — skipping download")
            return
        }
        launchDownload(option)
    }

    /**
     * Starts a background download unconditionally, overwriting any existing
     * file. If a different model is already downloading, it is cancelled first.
     */
    @Synchronized
    fun startDownload(option: LlmOption = LlmOption.BITNET_2B) {
        if (!option.isDownloadable()) {
            Log.i(TAG, "Skipping download for built-in option ${option.name}")
            return
        }
        if (_isDownloading.value) {
            if (activeOption?.id == option.id) return
            cancelDownload()
        }
        launchDownload(option)
    }

    /** Cancels any in-progress download. */
    @Synchronized
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        activeOption = null
        _isDownloading.value = false
        _downloadProgress.value = null
        Log.i(TAG, "Model download cancelled")
    }

    private fun launchDownload(option: LlmOption) {
        downloadJob = scope.launch {
            activeOption = option
            _downloadFailed.value = false
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val success = downloadBlocking(option) { progress ->
                    _downloadProgress.value = progress
                }
                if (!success) {
                    _downloadFailed.value = true
                    Log.e(TAG, "${option.name} download failed")
                }
            } finally {
                activeOption = null
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }

    // ── Suspend helpers (also usable from callers with their own scope) ───────

    /**
     * Downloads the model only when it is not already present on-device.
     *
     * @param option selected model metadata.
     * @param onProgress called on the IO thread with values in `[0, 1]`,
     * or `-1f` when content-length is unknown.
     */
    suspend fun downloadIfAbsent(
        option: LlmOption = LlmOption.BITNET_2B,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (!option.isDownloadable()) return@withContext true
        if (ModelFileLocator.isAvailable(context.filesDir, option.modelFileName)) {
            Log.i(TAG, "${option.name} already present — skipping download")
            return@withContext true
        }
        downloadBlocking(option, onProgress)
    }

    /**
     * Unconditionally downloads the selected model, overwriting any existing file.
     *
     * Redirects from HuggingFace (up to [MAX_REDIRECTS]) are followed manually
     * so that each hop uses the same timeout settings and can be cancelled.
     */
    suspend fun download(
        option: LlmOption = LlmOption.BITNET_2B,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (!option.isDownloadable()) return@withContext true
        downloadBlocking(option, onProgress)
    }

    // ── Private implementation ────────────────────────────────────────────────

    private suspend fun downloadBlocking(
        option: LlmOption,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadUrl = option.downloadUrl ?: run {
            Log.w(TAG, "No download URL configured for ${option.name}")
            return@withContext false
        }
        val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
        val target = File(modelsDir, option.modelFileName)
        val tmpFile = File(modelsDir, "${option.modelFileName}.tmp")

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = URL(downloadUrl)
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
                        currentUrl = URL(currentUrl, location)
                    } else if (code == HttpURLConnection.HTTP_OK) {
                        break
                    } else {
                        Log.e(TAG, "HTTP $code downloading ${option.name} from $currentUrl")
                        conn.errorStream?.bufferedReader()?.use { reader ->
                            Log.e(TAG, "Error response body: ${reader.readText().take(500)}")
                        }
                        return@withContext false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to $currentUrl", e)
                    throw e
                }
            }

            val conn = connection ?: run {
                Log.e(TAG, "Too many redirects for $downloadUrl")
                return@withContext false
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP $responseCode downloading ${option.name}")
                return@withContext false
            }

            val contentLength = conn.contentLengthLong
            Log.i(TAG, "Starting download of ${option.name}, content length: $contentLength bytes")
            var bytesRead = 0L
            var downloadComplete = false

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        if (!isActive) {
                            Log.i(TAG, "Download cancelled by coroutine")
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
            Log.i(TAG, "${option.name} downloaded to ${target.absolutePath} ($bytesRead bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "${option.name} download failed", e)
            tmpFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "ModelDownloader"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 32 * 1024
        private const val MAX_REDIRECTS = 10
    }

    /** Returns true when the option is not built-in and exposes a downloadable GGUF URL. */
    private fun LlmOption.isDownloadable(): Boolean = !isBuiltIn && !downloadUrl.isNullOrBlank()
}
