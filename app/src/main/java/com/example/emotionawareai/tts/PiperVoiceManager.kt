package com.example.emotionawareai.tts

import android.content.Context
import android.util.Log
import com.example.emotionawareai.BuildConfig
import com.example.emotionawareai.domain.model.PiperVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiperVoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class InstalledVoice(
        val modelPath: String,
        val tokensPath: String,
        val dataDir: String,
        val sampleVoice: PiperVoice
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var downloadJob: Job? = null

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadFailed = MutableStateFlow(false)
    val downloadFailed: StateFlow<Boolean> = _downloadFailed.asStateFlow()

    @Volatile
    private var currentVoice: PiperVoice? = null

    val activeVoice: PiperVoice?
        get() = currentVoice

    fun isVoiceInstalled(voice: PiperVoice): Boolean = resolveInstalledVoice(voice) != null

    fun resolveInstalledVoice(voice: PiperVoice): InstalledVoice? {
        val voiceDir = voiceInstallDir(voice)
        if (!voiceDir.exists()) return null

        val model = voiceDir.walkTopDown().firstOrNull {
            it.isFile && it.extension.equals("onnx", ignoreCase = true)
        } ?: return null
        val tokens = voiceDir.walkTopDown().firstOrNull {
            it.isFile && it.name.equals("tokens.txt", ignoreCase = true)
        } ?: return null
        val dataDir = espeakDir()
        if (!dataDir.exists() || !dataDir.isDirectory) return null

        return InstalledVoice(
            modelPath = model.absolutePath,
            tokensPath = tokens.absolutePath,
            dataDir = dataDir.absolutePath,
            sampleVoice = voice
        )
    }

    @Synchronized
    fun startDownloadIfAbsent(voice: PiperVoice) {
        if (_isDownloading.value) return
        if (isVoiceInstalled(voice)) return
        startDownload(voice)
    }

    @Synchronized
    fun startDownload(voice: PiperVoice) {
        if (_isDownloading.value) return
        currentVoice = voice
        downloadJob = scope.launch {
            _downloadFailed.value = false
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val success = downloadAndInstall(voice)
                if (!success) {
                    _downloadFailed.value = true
                }
            } finally {
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }

    @Synchronized
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        currentVoice = null
        _isDownloading.value = false
        _downloadProgress.value = null
        Log.i(TAG, "Cancelled Piper voice download")
    }

    private suspend fun downloadAndInstall(voice: PiperVoice): Boolean = withContext(Dispatchers.IO) {
        val root = piperRootDir().also { it.mkdirs() }
        val tmpRoot = File(root, "tmp/${voice.name.lowercase()}-${System.currentTimeMillis()}").also { it.mkdirs() }
        val voiceArchive = File(tmpRoot, voice.archiveName)
        val espeakArchive = File(tmpRoot, ESPEAK_ARCHIVE_NAME)
        val voiceExtractDir = File(tmpRoot, "voice")
        val espeakExtractDir = File(tmpRoot, "espeak")

        try {
            if (!downloadFile(ESPEAK_DOWNLOAD_URL, espeakArchive, 0f, 0.35f)) return@withContext false
            if (!downloadFile(voice.downloadUrl, voiceArchive, 0.35f, 1f)) return@withContext false

            extractTarBz2(espeakArchive, espeakExtractDir)
            extractTarBz2(voiceArchive, voiceExtractDir)

            val extractedEspeak = locateDirectory(espeakExtractDir, "espeak-ng-data") ?: espeakExtractDir
            val extractedVoiceRoot = locateVoiceRoot(voiceExtractDir)
                ?: throw IllegalStateException("No Piper voice files found in ${voice.archiveName}")

            replaceDirectory(extractedEspeak, espeakDir())
            replaceDirectory(extractedVoiceRoot, voiceInstallDir(voice))
            _downloadProgress.value = 1f
            Log.i(TAG, "Installed Piper voice ${voice.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install Piper voice ${voice.name}", e)
            false
        } finally {
            tmpRoot.deleteRecursively()
        }
    }

    private fun locateVoiceRoot(extractedDir: File): File? {
        return extractedDir.walkTopDown()
            .filter { it.isDirectory }
            .firstOrNull { dir ->
                dir.walkTopDown().any { it.isFile && it.extension.equals("onnx", ignoreCase = true) } &&
                    dir.walkTopDown().any { it.isFile && it.name.equals("tokens.txt", ignoreCase = true) }
            }
    }

    private fun locateDirectory(root: File, name: String): File? =
        root.walkTopDown().firstOrNull { it.isDirectory && it.name == name }

    private suspend fun downloadFile(
        url: String,
        target: File,
        startProgress: Float,
        endProgress: Float
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            var currentUrl = URL(url)
            var redirectsLeft = MAX_REDIRECTS
            while (redirectsLeft-- > 0) {
                if (!currentCoroutineContext().isActive) return@withContext false
                val conn = (currentUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "EmotionAwareAI/${BuildConfig.VERSION_NAME} (Android)")
                }
                connection = conn
                conn.connect()
                when (val code = conn.responseCode) {
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location") ?: return@withContext false
                        conn.disconnect()
                        currentUrl = URL(currentUrl, location)
                    }
                    HttpURLConnection.HTTP_OK -> break
                    else -> {
                        Log.e(TAG, "HTTP $code downloading $url")
                        return@withContext false
                    }
                }
            }

            val conn = connection ?: return@withContext false
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext false

            val contentLength = conn.contentLengthLong
            var bytesRead = 0L
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        if (!currentCoroutineContext().isActive) return@withContext false
                        output.write(buffer, 0, count)
                        bytesRead += count
                        if (contentLength > 0) {
                            val progress = startProgress +
                                ((bytesRead.toFloat() / contentLength.toFloat()) * (endProgress - startProgress))
                            _downloadProgress.value = progress.coerceIn(0f, 1f)
                        } else {
                            _downloadProgress.value = -1f
                        }
                    }
                    output.fd.sync()
                }
            }
            true
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractTarBz2(archive: File, destinationDir: File) {
        destinationDir.mkdirs()
        val destinationPath = destinationDir.canonicalFile.toPath()
        BufferedInputStream(archive.inputStream()).use { fileInput ->
            BZip2CompressorInputStream(fileInput).use { bzIn ->
                TarArchiveInputStream(bzIn).use { tarIn ->
                    var entry = tarIn.nextTarEntry
                    while (entry != null) {
                        val target = try {
                            File(destinationDir, entry.name).canonicalFile
                        } catch (e: Exception) {
                            throw SecurityException("Failed to canonicalize archive path: ${entry.name}", e)
                        }
                        if (!target.toPath().startsWith(destinationPath)) {
                            throw SecurityException("Blocked unsafe archive path: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output ->
                                tarIn.copyTo(output)
                            }
                        }
                        entry = tarIn.nextTarEntry
                    }
                }
            }
        }
    }

    private fun replaceDirectory(sourceDir: File, targetDir: File) {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.parentFile?.mkdirs()
        sourceDir.copyRecursively(targetDir, overwrite = true)
    }

    private fun piperRootDir(): File = File(context.filesDir, "tts/piper")

    private fun voiceInstallDir(voice: PiperVoice): File =
        File(piperRootDir(), "voices/${voice.name.lowercase()}")

    private fun espeakDir(): File = File(piperRootDir(), "espeak-ng-data")

    companion object {
        private const val TAG = "PiperVoiceManager"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val MAX_REDIRECTS = 5
        private const val BUFFER_SIZE = 16 * 1024
        private const val ESPEAK_ARCHIVE_NAME = "espeak-ng-data.tar.bz2"
        private const val ESPEAK_DOWNLOAD_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"
    }
}
