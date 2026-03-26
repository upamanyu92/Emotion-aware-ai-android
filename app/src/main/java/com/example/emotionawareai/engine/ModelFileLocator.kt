package com.example.emotionawareai.engine

import android.util.Log
import java.io.File
import java.io.InputStream

/**
 * Utility helpers for locating and installing the LLM model file on-device.
 *
 * Extracted from [LLMEngine] so the file-presence logic can be tested without
 * triggering the JNI `System.loadLibrary` call in that class.
 */
object ModelFileLocator {

    private const val TAG = "ModelFileLocator"

    /**
     * Returns `true` if a model file with [modelFileName] exists under
     * `<filesDir>/models/`.
     */
    fun isAvailable(filesDir: File, modelFileName: String): Boolean =
        File(filesDir, "models/$modelFileName").exists()

    /**
     * Returns the absolute path where the model file is expected.
     */
    fun path(filesDir: File, modelFileName: String): String =
        File(filesDir, "models/$modelFileName").absolutePath

    /**
     * Copies model data from [inputStream] into `<filesDir>/models/[modelFileName]`,
     * creating parent directories as needed.
     *
     * The destination file is deleted on failure to avoid leaving a partial file.
     *
     * @return `true` on success, `false` if an I/O error occurs.
     */
    fun installFromInputStream(
        filesDir: File,
        inputStream: InputStream,
        modelFileName: String
    ): Boolean {
        val modelsDir = File(filesDir, "models")
        modelsDir.mkdirs()
        val target = File(modelsDir, modelFileName)
        return try {
            target.outputStream().use { out -> inputStream.copyTo(out) }
            Log.i(TAG, "Model installed at ${target.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install model at ${target.absolutePath}", e)
            target.delete()
            false
        }
    }
}
