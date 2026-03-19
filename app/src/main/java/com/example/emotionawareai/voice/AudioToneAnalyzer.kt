package com.example.emotionawareai.voice

import com.example.emotionawareai.domain.model.Emotion
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Lightweight on-device prosody analyzer based on live microphone RMS values.
 *
 * This does not do speech-to-text sentiment; it infers likely arousal/tension
 * from loudness dynamics and exposes it as an emotion hint.
 */
@Singleton
class AudioToneAnalyzer @Inject constructor() {

    private val history = ArrayDeque<Float>()

    private val _audioEmotionFlow = MutableSharedFlow<Emotion>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioEmotionFlow: SharedFlow<Emotion> = _audioEmotionFlow.asSharedFlow()

    private val _toneInsightFlow = MutableSharedFlow<ToneInsight>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val toneInsightFlow: SharedFlow<ToneInsight> = _toneInsightFlow.asSharedFlow()

    fun onRmsSample(rmsDb: Float) {
        val normalized = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
        history.addLast(normalized)
        while (history.size > WINDOW_SIZE) {
            history.removeFirst()
        }

        if (history.size < MIN_SAMPLES) return

        val avg = history.average().toFloat()
        val volatility = computeVolatility(history)

        val inferred = when {
            avg > 0.78f && volatility > 0.2f -> Emotion.ANGRY
            avg > 0.66f && volatility > 0.12f -> Emotion.SURPRISED
            avg < 0.25f -> Emotion.SAD
            avg in 0.32f..0.55f && volatility < 0.06f -> Emotion.NEUTRAL
            else -> Emotion.UNKNOWN
        }

        val toneLabel = when {
            avg < 0.22f -> "Calm and soft"
            avg > 0.72f && volatility > 0.18f -> "High-energy"
            volatility > 0.22f -> "Expressive"
            volatility < 0.05f -> "Steady"
            else -> "Balanced"
        }

        val confidence = (0.45f + (volatility * 1.4f)).coerceIn(0f, 1f)

        _audioEmotionFlow.tryEmit(inferred)
        _toneInsightFlow.tryEmit(
            ToneInsight(
                inferredEmotion = inferred,
                energy = avg,
                volatility = volatility,
                confidence = confidence,
                label = toneLabel
            )
        )
    }

    private fun computeVolatility(values: ArrayDeque<Float>): Float {
        if (values.size < 2) return 0f
        var total = 0f
        var prev = values.first()
        values.drop(1).forEach { current ->
            total += abs(current - prev)
            prev = current
        }
        return total / (values.size - 1)
    }

    companion object {
        private const val WINDOW_SIZE = 10
        private const val MIN_SAMPLES = 4
    }
}

data class ToneInsight(
    val inferredEmotion: Emotion,
    val energy: Float,
    val volatility: Float,
    val confidence: Float,
    val label: String
)

