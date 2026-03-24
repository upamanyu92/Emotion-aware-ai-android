package com.example.emotionawareai.engine

import java.io.File

/**
 * Utility helpers for locating the LLM model file on-device.
 *
 * Extracted from [LLMEngine] so the file-presence logic can be tested without
 * triggering the JNI `System.loadLibrary` call in that class.
 */
object ModelFileLocator {

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
}
