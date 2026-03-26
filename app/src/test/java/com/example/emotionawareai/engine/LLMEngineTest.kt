package com.example.emotionawareai.engine

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
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

    @Test
    fun `installFromInputStream creates model file with correct content`() {
        val content = "fake gguf model bytes".toByteArray()
        val result = ModelFileLocator.installFromInputStream(
            tempDir,
            ByteArrayInputStream(content),
            LLMEngine.DEFAULT_MODEL_FILE
        )

        assertTrue("installFromInputStream should return true on success", result)
        assertTrue(
            "Model file should exist after installation",
            ModelFileLocator.isAvailable(tempDir, LLMEngine.DEFAULT_MODEL_FILE)
        )
        val written = File(tempDir, "models/${LLMEngine.DEFAULT_MODEL_FILE}").readBytes()
        assertArrayEquals("Written bytes should match input stream content", content, written)
    }

    @Test
    fun `installFromInputStream creates models directory if absent`() {
        // tempDir has no 'models' subdirectory yet
        val result = ModelFileLocator.installFromInputStream(
            tempDir,
            ByteArrayInputStream(ByteArray(0)),
            LLMEngine.DEFAULT_MODEL_FILE
        )

        assertTrue("Should succeed even when models dir does not exist", result)
        assertTrue(File(tempDir, "models").isDirectory)
    }

    @Test
    fun `installFromInputStream overwrites existing model file`() {
        // Install a first version
        ModelFileLocator.installFromInputStream(
            tempDir,
            ByteArrayInputStream("version1".toByteArray()),
            LLMEngine.DEFAULT_MODEL_FILE
        )

        // Overwrite with a second version
        val result = ModelFileLocator.installFromInputStream(
            tempDir,
            ByteArrayInputStream("version2".toByteArray()),
            LLMEngine.DEFAULT_MODEL_FILE
        )

        assertTrue("Second install should succeed", result)
        val content = File(tempDir, "models/${LLMEngine.DEFAULT_MODEL_FILE}").readText()
        assertEquals("File should contain the latest content", "version2", content)
    }
}
