package com.example.emotionawareai.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
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
 * The download is co-routine-cancellable: if the calling coroutine is cancelled
 * the connection is closed and any partial file is deleted.
 *
 * Progress is reported as a [Float] in `[0, 1]` via the [onProgress] callback.
 * When the server does not report a Content-Length the value is `-1f`
 * (indeterminate).
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

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
        download(modelFileName, onProgress)
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
        val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
        val target = File(modelsDir, modelFileName)
        val tmpFile = File(modelsDir, "$modelFileName.tmp")

        var connection: HttpURLConnection? = null
        try {
            // Resolve redirects manually (HuggingFace uses HTTP 302 → CDN URL).
            var currentUrl = URL(BITNET_MODEL_URL)
            var redirectsLeft = MAX_REDIRECTS
            while (redirectsLeft-- > 0) {
                val conn = currentUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.instanceFollowRedirects = false
                connection = conn

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    connection = null
                    currentUrl = URL(location)
                } else {
                    break
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
