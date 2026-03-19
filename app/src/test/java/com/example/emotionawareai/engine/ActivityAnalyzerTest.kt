package com.example.emotionawareai.engine

import android.content.Context
import com.example.emotionawareai.domain.model.CaptionCategory
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * Unit tests for [ActivityAnalyzer] logic.
 *
 * Since ActivityAnalyzer uses MediaPipe and Android-specific Bitmap/ImageProxy,
 * we use reflection to test the private analysis methods that handle pure data.
 */
class ActivityAnalyzerTest {

    private lateinit var analyzer: ActivityAnalyzer
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        analyzer = ActivityAnalyzer(context)
    }

    @Test
    fun `detectAttention returns Facing camera when nose is centered`() {
        val nose = mockLandmark(0.5f, 0.5f, 0.9f)
        val method = getPrivateMethod("detectAttention", com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java)
        
        val result = method.invoke(analyzer, nose) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals(CaptionCategory.ATTENTION, result.category)
        assertEquals("Facing camera", result.text)
    }

    @Test
    fun `detectAttention returns Looking away when visibility is low`() {
        val nose = mockLandmark(0.5f, 0.5f, 0.1f)
        val method = getPrivateMethod("detectAttention", com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java)
        
        val result = method.invoke(analyzer, nose) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals("Looking away", result.text)
    }

    @Test
    fun `detectPosture returns Slouching when shoulders are narrow relative to torso`() {
        val leftShoulder = mockLandmark(0.48f, 0.2f)
        val rightShoulder = mockLandmark(0.52f, 0.2f)
        val leftHip = mockLandmark(0.48f, 0.8f)
        val rightHip = mockLandmark(0.52f, 0.8f)
        
        val method = getPrivateMethod("detectPosture", 
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java
        )
        
        val result = method.invoke(analyzer, leftShoulder, rightShoulder, leftHip, rightHip) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals("Slouching", result.text)
    }

    @Test
    fun `detectPosture returns Leaning left when shoulders are offset left of hips`() {
        // shoulderWidth = 0.3, center = 0.3
        val leftShoulder = mockLandmark(0.15f, 0.2f)
        val rightShoulder = mockLandmark(0.45f, 0.2f) 
        // hipWidth = 0.3, center = 0.5
        val leftHip = mockLandmark(0.35f, 0.6f)
        val rightHip = mockLandmark(0.65f, 0.6f) 
        
        // torsoHeight = 0.6 - 0.2 = 0.4
        // verticalRatio = 0.3 / 0.4 = 0.75 (> 0.35, not Slouching)
        // lateralOffset = 0.3 - 0.5 = -0.2 (< -0.08, Leaning left)
        
        val method = getPrivateMethod("detectPosture", 
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java
        )
        
        val result = method.invoke(analyzer, leftShoulder, rightShoulder, leftHip, rightHip) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals("Leaning left", result.text)
    }

    @Test
    fun `detectGesture returns Hand on chin when wrist is near nose`() {
        val nose = mockLandmark(0.5f, 0.4f)
        val leftShoulder = mockLandmark(0.3f, 0.5f)
        val rightShoulder = mockLandmark(0.7f, 0.5f)
        val leftWrist = mockLandmark(0.52f, 0.5f) // Near nose Y (0.4 + 0.1)
        val rightWrist = mockLandmark(0.8f, 0.8f)
        
        val method = getPrivateMethod("detectGesture",
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java
        )
        
        val result = method.invoke(analyzer, nose, leftShoulder, rightShoulder, null, null, leftWrist, rightWrist) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals("Hand on chin (thinking)", result.text)
    }

    @Test
    fun `detectGesture returns Arms crossed when wrists cross center line`() {
        val nose = mockLandmark(0.5f, 0.4f)
        val leftShoulder = mockLandmark(0.3f, 0.5f)
        val rightShoulder = mockLandmark(0.7f, 0.5f) // center 0.5
        val leftWrist = mockLandmark(0.6f, 0.7f)  // left wrist on right side
        val rightWrist = mockLandmark(0.4f, 0.7f) // right wrist on left side
        
        val method = getPrivateMethod("detectGesture",
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java,
            com.google.mediapipe.tasks.components.containers.NormalizedLandmark::class.java
        )
        
        val result = method.invoke(analyzer, nose, leftShoulder, rightShoulder, null, null, leftWrist, rightWrist) as com.example.emotionawareai.domain.model.ActivityCaption
        
        assertEquals("Arms crossed", result.text)
    }

    private fun mockLandmark(x: Float, y: Float, visibility: Float = 1.0f): com.google.mediapipe.tasks.components.containers.NormalizedLandmark {
        val lm = mockk<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>()
        io.mockk.every { lm.x() } returns x
        io.mockk.every { lm.y() } returns y
        io.mockk.every { lm.visibility() } returns java.util.Optional.of(visibility)
        return lm
    }

    private fun getPrivateMethod(name: String, vararg parameterTypes: Class<*>): Method {
        val method = ActivityAnalyzer::class.java.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        return method
    }
}
