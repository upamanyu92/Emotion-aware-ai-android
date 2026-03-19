package com.example.emotionawareai.domain.model

/**
 * A single human-readable caption describing one aspect of the user's
 * currently detected activity (posture, gesture, attention, or inferred
 * behaviour).
 *
 * Captions are produced by [com.example.emotionawareai.engine.ActivityAnalyzer]
 * from MediaPipe PoseLandmarker results and displayed in the
 * [com.example.emotionawareai.ui.component.ActivityCaptionOverlay].
 */
data class ActivityCaption(
    val timestamp: Long = System.currentTimeMillis(),
    val category: CaptionCategory,
    val text: String,
    val confidence: Float = 1f
)

/**
 * Semantic category for an [ActivityCaption], used for colour-coding and
 * grouping in the caption overlay.
 */
enum class CaptionCategory(
    val displayName: String,
    val emoji: String
) {
    POSTURE("Posture", "🧍"),
    GESTURE("Gesture", "🤚"),
    ATTENTION("Attention", "👀"),
    BEHAVIOR("Behavior", "🧠"),
    EMOTION("Emotion", "🎭")
}
