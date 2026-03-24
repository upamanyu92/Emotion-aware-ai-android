package com.example.emotionawareai.engine

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for [ModelFileLocator].
 *
 * These verify model file availability detection without triggering the JNI
 * `System.loadLibrary` call in [LLMEngine].
 */
class LLMEngineTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = createTempDir("llm_test")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `isAvailable returns false when model file is absent`() {
        assertFalse(
            "isAvailable should be false when no model file exists",
            ModelFileLocator.isAvailable(tempDir, LLMEngine.DEFAULT_MODEL_FILE)
        )
    }

    @Test
    fun `isAvailable returns true when model file is present`() {
        val modelsDir = File(tempDir, "models").also { it.mkdirs() }
        File(modelsDir, LLMEngine.DEFAULT_MODEL_FILE).createNewFile()

        assertTrue(
            "isAvailable should be true when model file is present",
            ModelFileLocator.isAvailable(tempDir, LLMEngine.DEFAULT_MODEL_FILE)
        )
    }

    @Test
    fun `path points to filesDir models subfolder`() {
        val path = ModelFileLocator.path(tempDir, LLMEngine.DEFAULT_MODEL_FILE)

        assertTrue(
            "path should be under filesDir/models/",
            path.startsWith(tempDir.absolutePath) && path.contains("models")
        )
        assertTrue(
            "path should reference the default .gguf filename",
            path.endsWith(LLMEngine.DEFAULT_MODEL_FILE)
        )
    }

    @Test
    fun `isAvailable uses provided fileName argument`() {
        val modelsDir = File(tempDir, "models").also { it.mkdirs() }
        File(modelsDir, "custom_model.gguf").createNewFile()

        assertTrue(ModelFileLocator.isAvailable(tempDir, "custom_model.gguf"))
        assertFalse(ModelFileLocator.isAvailable(tempDir, "other_model.gguf"))
    }
}
