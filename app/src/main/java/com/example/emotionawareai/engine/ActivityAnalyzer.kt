package com.example.emotionawareai.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.CaptionCategory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Analyzes live camera frames using MediaPipe [PoseLandmarker] to detect:
 * - Body posture (upright, slouching, leaning)
 * - Head orientation (facing camera, looking away, tilted)
 * - Arm / hand positions (arms crossed, raised, hand near face, gesturing)
 * - High-level inferred behaviour (attentive, thinking, distracted, etc.)
 *
 * Results are emitted as a list of [ActivityCaption] objects on [captionFlow].
 * Frames are capped at [MAX_FPS] to save battery.
 *
 * MediaPipe model asset required: **pose_landmarker_lite.task** placed in
 * `src/main/assets/`.
 */
@Singleton
class ActivityAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var poseLandmarker: PoseLandmarker? = null
    // `var` so it can be recreated after a `release()` call in the same singleton lifetime.
    private var analysisExecutor = Executors.newSingleThreadExecutor()

    private val _captionFlow = MutableSharedFlow<List<ActivityCaption>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val captionFlow: SharedFlow<List<ActivityCaption>> = _captionFlow.asSharedFlow()

    private var lastProcessedMs = 0L

    // Previous wrist Y-positions for movement/gesture detection.
    private var prevLeftWristY = 0f
    private var prevRightWristY = 0f

    val isInitialized: Boolean get() = poseLandmarker != null

    /**
     * Initialises the MediaPipe PoseLandmarker. Safe to call multiple times
     * (no-op if already initialised).
     */
    fun initialize() {
        if (isInitialized) return

        // Recreate the executor if a prior release() shut it down.
        if (analysisExecutor.isShutdown) {
            analysisExecutor = Executors.newSingleThreadExecutor()
        }

        runCatching {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(POSE_LANDMARKER_MODEL)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(CONFIDENCE_THRESHOLD)
                .setMinPosePresenceConfidence(CONFIDENCE_THRESHOLD)
                .setMinTrackingConfidence(CONFIDENCE_THRESHOLD)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i(TAG, "PoseLandmarker initialized")
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize PoseLandmarker: ${e.message}")
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
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()
        processBitmapFrameInternal(bitmap, recycleAfter = true)
    }

    /**
     * Processes a [Bitmap] that is shared with other analyzers (e.g.
     * [EmotionDetector]). The caller owns the bitmap lifecycle — this method
     * does **not** recycle it.
     */
    fun processBitmapFrame(bitmap: Bitmap) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastProcessedMs < FRAME_INTERVAL_MS) return
        lastProcessedMs = nowMs
        processBitmapFrameInternal(bitmap, recycleAfter = false)
    }

    private fun processBitmapFrameInternal(bitmap: Bitmap, recycleAfter: Boolean) {
        val landmarker = poseLandmarker ?: return
        analysisExecutor.execute {
            runCatching {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result: PoseLandmarkerResult = landmarker.detect(mpImage)
                val captions = analyzePose(result)
                _captionFlow.tryEmit(captions)
            }.onFailure { e ->
                Log.e(TAG, "Frame processing error: ${e.message}")
            }
            if (recycleAfter && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    // ── Pose analysis ─────────────────────────────────────────────────────────

    private fun analyzePose(result: PoseLandmarkerResult): List<ActivityCaption> {
        val captions = mutableListOf<ActivityCaption>()

        val landmarks = result.landmarks().firstOrNull()
            ?: return listOf(
                ActivityCaption(
                    category = CaptionCategory.ATTENTION,
                    text = "No person detected",
                    confidence = 1f
                )
            )

        val nose          = landmarks.getOrNull(NOSE)
        val leftShoulder  = landmarks.getOrNull(LEFT_SHOULDER)
        val rightShoulder = landmarks.getOrNull(RIGHT_SHOULDER)
        val leftElbow     = landmarks.getOrNull(LEFT_ELBOW)
        val rightElbow    = landmarks.getOrNull(RIGHT_ELBOW)
        val leftWrist     = landmarks.getOrNull(LEFT_WRIST)
        val rightWrist    = landmarks.getOrNull(RIGHT_WRIST)
        val leftHip       = landmarks.getOrNull(LEFT_HIP)
        val rightHip      = landmarks.getOrNull(RIGHT_HIP)

        // ── Attention: is the person looking at the screen? ───────────────────
        captions += detectAttention(nose)

        // ── Posture: upright, slouching, leaning ──────────────────────────────
        if (leftShoulder != null && rightShoulder != null &&
            leftHip != null && rightHip != null
        ) {
            captions += detectPosture(leftShoulder, rightShoulder, leftHip, rightHip)
        }

        // ── Head tilt (derived from shoulder symmetry) ────────────────────────
        if (leftShoulder != null && rightShoulder != null) {
            detectHeadTilt(leftShoulder, rightShoulder)?.let { captions += it }
        }

        // ── Arm / hand gestures ───────────────────────────────────────────────
        if (leftWrist != null && rightWrist != null &&
            leftShoulder != null && rightShoulder != null && nose != null
        ) {
            captions += detectGesture(
                nose, leftShoulder, rightShoulder,
                leftElbow, rightElbow, leftWrist, rightWrist
            )
        }

        // ── High-level behaviour inference ────────────────────────────────────
        inferBehavior(captions)?.let { captions += it }

        return captions
    }

    private fun detectAttention(
        nose: com.google.mediapipe.tasks.components.containers.NormalizedLandmark?
    ): ActivityCaption {
        if (nose == null) {
            return ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Face not visible",
                confidence = 1f
            )
        }
        val visibility = nose.visibility().orElse(0f)
        return when {
            visibility < 0.3f -> ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Looking away",
                confidence = 1f - visibility
            )
            nose.x() < 0.2f -> ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Glancing far left",
                confidence = visibility
            )
            nose.x() > 0.8f -> ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Glancing far right",
                confidence = visibility
            )
            nose.x() < 0.35f || nose.x() > 0.65f -> ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Glancing sideways",
                confidence = visibility
            )
            else -> ActivityCaption(
                category = CaptionCategory.ATTENTION,
                text = "Facing camera",
                confidence = visibility
            )
        }
    }

    private fun detectPosture(
        leftShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        rightShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        leftHip: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        rightHip: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): ActivityCaption {
        val shoulderMidX = (leftShoulder.x() + rightShoulder.x()) / 2f
        val shoulderMidY = (leftShoulder.y() + rightShoulder.y()) / 2f
        val hipMidX      = (leftHip.x() + rightHip.x()) / 2f
        val hipMidY      = (leftHip.y() + rightHip.y()) / 2f
        val shoulderWidth = abs(leftShoulder.x() - rightShoulder.x())

        // Ratio of shoulder width to torso height: low ratio → slouching
        val torsoHeight = hipMidY - shoulderMidY
        val verticalRatio = if (torsoHeight > 0f) shoulderWidth / torsoHeight else 1f

        val lateralOffset = shoulderMidX - hipMidX

        val postureText = when {
            verticalRatio < 0.35f           -> "Slouching"
            lateralOffset < -0.08f          -> "Leaning left"
            lateralOffset >  0.08f          -> "Leaning right"
            shoulderMidY < hipMidY - 0.35f  -> "Leaning forward"
            verticalRatio > 0.8f            -> "Upright posture"
            else                            -> "Relaxed posture"
        }
        return ActivityCaption(
            category = CaptionCategory.POSTURE,
            text = postureText,
            confidence = 0.8f
        )
    }

    private fun detectHeadTilt(
        leftShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        rightShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): ActivityCaption? {
        val tilt = abs(leftShoulder.y() - rightShoulder.y())
        if (tilt < 0.04f) return null
        val direction = if (leftShoulder.y() > rightShoulder.y()) "right" else "left"
        return ActivityCaption(
            category = CaptionCategory.POSTURE,
            text = "Head tilted $direction",
            confidence = minOf(tilt * 6f, 1f)
        )
    }

    private fun detectGesture(
        nose: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        leftShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        rightShoulder: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        leftElbow: com.google.mediapipe.tasks.components.containers.NormalizedLandmark?,
        rightElbow: com.google.mediapipe.tasks.components.containers.NormalizedLandmark?,
        leftWrist: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        rightWrist: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): ActivityCaption {
        val noseY    = nose.y()
        val noseX    = nose.x()
        val centerX  = (leftShoulder.x() + rightShoulder.x()) / 2f

        val leftWristNearFace  = leftWrist.y()  < noseY + 0.15f && abs(leftWrist.x()  - noseX) < 0.22f
        val rightWristNearFace = rightWrist.y() < noseY + 0.15f && abs(rightWrist.x() - noseX) < 0.22f

        // Chin-resting / thinking gesture: one wrist just below nose height
        val leftWristAtChin  = leftWrist.y()  in (noseY + 0.05f)..(noseY + 0.20f) &&
                abs(leftWrist.x()  - noseX) < 0.15f
        val rightWristAtChin = rightWrist.y() in (noseY + 0.05f)..(noseY + 0.20f) &&
                abs(rightWrist.x() - noseX) < 0.15f

        val leftArmRaised  = leftWrist.y()  < leftShoulder.y()  - 0.05f
        val rightArmRaised = rightWrist.y() < rightShoulder.y() - 0.05f

        // Arms crossed: wrists on opposite sides of the body's centre line
        val armsCrossed = leftWrist.x() > centerX + 0.04f && rightWrist.x() < centerX - 0.04f

        // Fidgeting: significant wrist Y-movement since last frame
        val leftDelta  = abs(leftWrist.y()  - prevLeftWristY)
        val rightDelta = abs(rightWrist.y() - prevRightWristY)
        prevLeftWristY  = leftWrist.y()
        prevRightWristY = rightWrist.y()
        val isGesturing = leftDelta > 0.04f || rightDelta > 0.04f

        val text = when {
            leftWristNearFace && rightWristNearFace -> "Hands covering face"
            leftWristAtChin || rightWristAtChin     -> "Hand on chin (thinking)"
            leftWristNearFace                       -> "Left hand near face"
            rightWristNearFace                      -> "Right hand near face"
            armsCrossed                             -> "Arms crossed"
            leftArmRaised && rightArmRaised         -> "Both arms raised"
            leftArmRaised                           -> "Left arm raised"
            rightArmRaised                          -> "Right arm raised"
            isGesturing                             -> "Gesturing / moving hands"
            else                                    -> "Arms at rest"
        }

        return ActivityCaption(
            category = CaptionCategory.GESTURE,
            text = text,
            confidence = 0.75f
        )
    }

    /**
     * Infers a high-level behavioural label from the lower-level signal captions.
     */
    private fun inferBehavior(signals: List<ActivityCaption>): ActivityCaption? {
        val attention = signals.find { it.category == CaptionCategory.ATTENTION }?.text ?: ""
        val posture   = signals.find { it.category == CaptionCategory.POSTURE   }?.text ?: ""
        val gesture   = signals.find { it.category == CaptionCategory.GESTURE   }?.text ?: ""

        val behavior = when {
            attention.contains("away",     ignoreCase = true) ||
            attention.contains("sideways", ignoreCase = true) -> "Distracted"

            gesture.contains("chin",    ignoreCase = true) ||
            gesture.contains("face",    ignoreCase = true) -> "Thinking / contemplating"

            gesture == "Arms crossed" && posture.contains("slouch", ignoreCase = true) ->
                "Defensive / disengaged"

            gesture == "Arms crossed" -> "Reserved / guarded"

            gesture.contains("Gesturing",   ignoreCase = true) &&
            attention == "Facing camera"    -> "Actively engaged"

            gesture.contains("arms raised", ignoreCase = true) -> "Excited or emphasizing"

            posture.contains("Leaning forward", ignoreCase = true) &&
            attention == "Facing camera" -> "Leaning in / interested"

            posture == "Upright posture" && attention == "Facing camera" -> "Attentive"

            posture == "Slouching" -> "Tired or relaxed"

            posture.contains("Leaning", ignoreCase = true) &&
            attention == "Facing camera" -> "Casually engaged"

            else -> null
        } ?: return null

        return ActivityCaption(
            category = CaptionCategory.BEHAVIOR,
            text = behavior,
            confidence = 0.7f
        )
    }

    fun release() {
        runCatching { poseLandmarker?.close() }
        poseLandmarker = null
        analysisExecutor.shutdown()
        analysisExecutor.awaitTermination(2, TimeUnit.SECONDS)
        Log.i(TAG, "ActivityAnalyzer released")
    }

    companion object {
        private const val TAG = "ActivityAnalyzer"
        private const val POSE_LANDMARKER_MODEL = "pose_landmarker_lite.task"
        private const val CONFIDENCE_THRESHOLD  = 0.5f
        private const val MAX_FPS               = 5
        private const val FRAME_INTERVAL_MS     = 1000L / MAX_FPS

        // MediaPipe Pose landmark indices
        private const val NOSE          =  0
        private const val LEFT_SHOULDER = 11
        private const val RIGHT_SHOULDER = 12
        private const val LEFT_ELBOW    = 13
        private const val RIGHT_ELBOW   = 14
        private const val LEFT_WRIST    = 15
        private const val RIGHT_WRIST   = 16
        private const val LEFT_HIP      = 23
        private const val RIGHT_HIP     = 24
    }
}
