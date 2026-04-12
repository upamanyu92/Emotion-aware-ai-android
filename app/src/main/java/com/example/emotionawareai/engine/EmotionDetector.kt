package com.example.emotionawareai.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.emotionawareai.domain.model.Emotion
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects the user's facial emotion using MediaPipe FaceLandmarker.
 *
 * Frames are processed at up to [MAX_FPS] to balance responsiveness and
 * battery usage. The detected [Emotion] is broadcast on [emotionFlow].
 */
@Singleton
class EmotionDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var faceLandmarker: FaceLandmarker? = null
    // `var` so it can be recreated after a `release()` call in the same singleton lifetime.
    private var analysisExecutor = Executors.newSingleThreadExecutor()

    private val _emotionFlow = MutableSharedFlow<Emotion>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val emotionFlow: SharedFlow<Emotion> = _emotionFlow.asSharedFlow()

    /** Tracks the timestamp of the last processed frame. */
    private var lastProcessedMs = 0L

    val isInitialized: Boolean get() = faceLandmarker != null

    /**
     * Initialises the MediaPipe FaceLandmarker. Must be called before
     * [processFrame]. Safe to call multiple times (no-op if already init).
     */
    fun initialize() {
        if (isInitialized) return

        // Recreate the executor if a prior release() shut it down.
        if (analysisExecutor.isShutdown) {
            analysisExecutor = Executors.newSingleThreadExecutor()
        }

        runCatching {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(FACE_LANDMARKER_MODEL)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(CONFIDENCE_THRESHOLD)
                .setMinFacePresenceConfidence(CONFIDENCE_THRESHOLD)
                .setMinTrackingConfidence(CONFIDENCE_THRESHOLD)
                .setOutputFaceBlendshapes(true)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.i(TAG, "FaceLandmarker initialized")
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize FaceLandmarker: ${e.message}")
        }
    }

    /**
     * Processes a CameraX [ImageProxy] frame. Converts to [Bitmap] and
     * delegates to [processBitmapFrame]. The proxy is closed by this method.
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastProcessedMs < FRAME_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessedMs = nowMs

        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        imageProxy.close()
        processBitmapFrameInternal(bitmap, landmarker, recycleAfter = true)
    }

    /**
     * Processes a [Bitmap] that is shared with other analyzers (e.g.
     * [ActivityAnalyzer]). The caller owns the bitmap lifecycle — this method
     * does **not** recycle it.
     */
    fun processBitmapFrame(bitmap: Bitmap) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastProcessedMs < FRAME_INTERVAL_MS) return
        lastProcessedMs = nowMs
        val landmarker = faceLandmarker ?: return
        processBitmapFrameInternal(bitmap, landmarker, recycleAfter = false)
    }

    private fun processBitmapFrameInternal(
        bitmap: Bitmap,
        landmarker: FaceLandmarker,
        recycleAfter: Boolean
    ) {
        analysisExecutor.execute {
            runCatching {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result: FaceLandmarkerResult = landmarker.detect(mpImage)
                val emotion = classifyEmotion(result)
                _emotionFlow.tryEmit(emotion)
            }.onFailure { e ->
                Log.e(TAG, "Frame processing error: ${e.message}")
            }
            if (recycleAfter && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    /**
     * Classifies emotion from FaceLandmarker blendshape scores.
     * Scores map to AU (Action Unit) proxies from the blendshape list.
     */
    private fun classifyEmotion(result: FaceLandmarkerResult): Emotion {
        val faceBlendshapes = result.faceBlendshapes().orElse(null)
            ?.firstOrNull() ?: return Emotion.UNKNOWN

        val scores = faceBlendshapes.associate { it.categoryName() to it.score() }

        val mouthSmileLeft  = scores["mouthSmileLeft"]  ?: 0f
        val mouthSmileRight = scores["mouthSmileRight"] ?: 0f
        val browDownLeft    = scores["browDownLeft"]    ?: 0f
        val browDownRight   = scores["browDownRight"]   ?: 0f
        val browInnerUp     = scores["browInnerUp"]     ?: 0f
        val jawOpen         = scores["jawOpen"]         ?: 0f
        val eyeWideLeft     = scores["eyeWideLeft"]     ?: 0f
        val eyeWideRight    = scores["eyeWideRight"]    ?: 0f
        val mouthFrownLeft  = scores["mouthFrownLeft"]  ?: 0f
        val mouthFrownRight = scores["mouthFrownRight"] ?: 0f
        val noseSneerLeft   = scores["noseSneerLeft"]   ?: 0f
        val noseSneerRight  = scores["noseSneerRight"]  ?: 0f

        val smile    = (mouthSmileLeft + mouthSmileRight) / 2f
        val frown    = (mouthFrownLeft + mouthFrownRight) / 2f
        val browDown = (browDownLeft + browDownRight) / 2f
        val eyeWide  = (eyeWideLeft + eyeWideRight) / 2f
        val sneer    = (noseSneerLeft + noseSneerRight) / 2f

        return when {
            smile > 0.5f                                -> Emotion.HAPPY
            frown > 0.4f && browInnerUp > 0.3f         -> Emotion.SAD
            browDown > 0.5f && sneer > 0.3f            -> Emotion.ANGRY
            jawOpen > 0.5f && eyeWide > 0.4f           -> Emotion.SURPRISED
            browInnerUp > 0.5f && eyeWide > 0.5f       -> Emotion.FEARFUL
            sneer > 0.5f                                -> Emotion.DISGUSTED
            else                                        -> Emotion.NEUTRAL
        }
    }

    fun release() {
        runCatching { faceLandmarker?.close() }
        faceLandmarker = null
        analysisExecutor.shutdown()
        analysisExecutor.awaitTermination(2, TimeUnit.SECONDS)
        Log.i(TAG, "EmotionDetector released")
    }

    companion object {
        private const val TAG = "EmotionDetector"
        private const val FACE_LANDMARKER_MODEL = "face_landmarker.task"
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val MAX_FPS = 10
        private const val FRAME_INTERVAL_MS = 1000L / MAX_FPS
    }
}
