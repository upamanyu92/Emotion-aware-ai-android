package com.example.emotionawareai.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.ui.theme.EmotionAngry
import com.example.emotionawareai.ui.theme.EmotionDisgusted
import com.example.emotionawareai.ui.theme.EmotionFearful
import com.example.emotionawareai.ui.theme.EmotionHappy
import com.example.emotionawareai.ui.theme.EmotionNeutral
import com.example.emotionawareai.ui.theme.EmotionSad
import com.example.emotionawareai.ui.theme.EmotionSurprised
import com.example.emotionawareai.ui.theme.EmotionUnknown
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.voice.ToneInsight

/**
 * A rich, real-time emotional and expressive telemetry panel.
 *
 * Designed to be immediately understandable by anyone aged 18+:
 * - Large emoji for instant emotional recognition
 * - Simple bar visualisations for energy and intensity
 * - Plain-English labels for every metric
 *
 * @param faceEmotion   Emotion detected from the front camera.
 * @param voiceEmotion  Emotion inferred from voice tone.
 * @param toneInsight   Detailed prosody breakdown (energy, volatility, label).
 * @param userName      Optional user name to personalise the greeting.
 * @param isListening   Whether the microphone is currently active.
 */
@Composable
fun TelemetryDashboard(
    faceEmotion: Emotion,
    voiceEmotion: Emotion,
    toneInsight: ToneInsight?,
    userName: String = "",
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val faceColor by animateColorAsState(
        targetValue = emotionColor(faceEmotion),
        animationSpec = tween(durationMillis = 600),
        label = "faceColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(GlassCard, Color(0x08FFFFFF))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonPurple.copy(alpha = 0.4f),
                        NeonCyan.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveDot(active = isListening, color = NeonCyan)
                    Text(
                        text = if (userName.isNotBlank()) "Hey $userName 👋" else "Live Vibes",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = NeonCyan
                    )
                }
                Text(
                    text = "LIVE MOOD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 9.sp
                )
            }

            // ── Face emotion row ──────────────────────────────────────────────
            TelemetryRow(
                label = "Face",
                icon = faceEmotion.emoji,
                valueText = faceEmotion.displayName,
                subText = faceEmotion.friendlyHint,
                accentColor = faceColor
            )

            // ── Voice tone row ────────────────────────────────────────────────
            val voiceToneLabel = toneInsight?.label ?: if (isListening) "Listening…" else "—"
            val voiceColor by animateColorAsState(
                targetValue = if (toneInsight != null) toneColor(toneInsight) else NeonPurple.copy(alpha = 0.6f),
                animationSpec = tween(600),
                label = "voiceColor"
            )
            TelemetryRow(
                label = "Voice",
                icon = if (isListening) "🎤" else "🔇",
                valueText = voiceEmotion.takeUnless { it == Emotion.UNKNOWN }?.displayName
                    ?: voiceToneLabel,
                subText = toneInsight?.let { "Tone: ${it.label}" },
                accentColor = voiceColor
            )

            // ── Energy bar ────────────────────────────────────────────────────
            val energy = toneInsight?.energy ?: 0f
            EnergyBar(
                energy = energy,
                label = energyLabel(energy),
                color = energyBarColor(energy)
            )

            // ── Stress / calm indicator ────────────────────────────────────────
            val volatility = toneInsight?.volatility ?: 0f
            val stressEmoji = stressEmoji(volatility)
            val stressText = stressLabel(volatility)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stressEmoji, fontSize = 18.sp)
                Column {
                    Text(
                        text = "Inner State",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = stressText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// ── Sub-components ─────────────────────────────────────────────────────────────

@Composable
private fun TelemetryRow(
    label: String,
    icon: String,
    valueText: String,
    subText: String? = null,
    accentColor: Color
) {
    val rowScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rowScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Emotion icon with colored backdrop
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.75f),
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EnergyBar(
    energy: Float,
    label: String,
    color: Color
) {
    val animatedEnergy by animateFloatAsState(
        targetValue = energy.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "energyAnim"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = energyEmoji(energy), fontSize = 14.sp)
                Text(
                    text = "Energy",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = color,
                fontSize = 10.sp
            )
        }

        // Segmented bar — 10 blocks, easy to read
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val filledBlocks = (animatedEnergy * 10).toInt().coerceIn(0, 10)
            repeat(10) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < filledBlocks) color.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                )
            }
        }
    }
}

/** Animated pulsing dot indicating live microphone or data state. */
@Composable
private fun LiveDot(active: Boolean, color: Color) {
    val pulseTransition = rememberInfiniteTransition(label = "liveDotPulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(if (active) scale else 1f)
            .background(
                if (active) color else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
    )
}

// ── Helper functions ───────────────────────────────────────────────────────────

private fun emotionColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY -> EmotionHappy
    Emotion.SAD -> EmotionSad
    Emotion.ANGRY -> EmotionAngry
    Emotion.SURPRISED -> EmotionSurprised
    Emotion.FEARFUL -> EmotionFearful
    Emotion.DISGUSTED -> EmotionDisgusted
    Emotion.NEUTRAL -> EmotionNeutral
    Emotion.UNKNOWN -> EmotionUnknown
}

private fun toneColor(tone: ToneInsight): Color = when {
    tone.energy > 0.72f && tone.volatility > 0.18f -> EmotionAngry
    tone.energy > 0.55f -> NeonGold
    tone.energy < 0.25f -> EmotionSad
    else -> NeonCyan
}

private fun energyBarColor(energy: Float): Color = when {
    energy > 0.75f -> EmotionAngry.copy(alpha = 0.9f)
    energy > 0.55f -> NeonGold
    energy > 0.3f -> NeonCyan
    else -> EmotionSad.copy(alpha = 0.8f)
}

private fun energyEmoji(energy: Float): String = when {
    energy > 0.75f -> "🔥"
    energy > 0.55f -> "⚡"
    energy > 0.3f -> "💚"
    else -> "💤"
}

private fun energyLabel(energy: Float): String = when {
    energy > 0.75f -> "Very High"
    energy > 0.55f -> "High"
    energy > 0.35f -> "Moderate"
    energy > 0.15f -> "Low"
    else -> "Very Low"
}

private fun stressEmoji(volatility: Float): String = when {
    volatility > 0.22f -> "😤"
    volatility > 0.12f -> "😅"
    volatility > 0.06f -> "🙂"
    else -> "😌"
}

private fun stressLabel(volatility: Float): String = when {
    volatility > 0.22f -> "Quite stimulated — deep breath helps"
    volatility > 0.12f -> "A bit excited — that's okay"
    volatility > 0.06f -> "Feeling balanced"
    else -> "Calm and relaxed ✨"
}

/** Short, plain-language hint shown below the emotion name. */
private val Emotion.friendlyHint: String
    get() = when (this) {
        Emotion.HAPPY -> "You seem to be in a great mood!"
        Emotion.SAD -> "Feeling low? I'm here for you."
        Emotion.ANGRY -> "Sensing some tension — let's talk."
        Emotion.SURPRISED -> "Something unexpected happened?"
        Emotion.FEARFUL -> "Feeling anxious? Take it easy."
        Emotion.DISGUSTED -> "Something bothering you?"
        Emotion.NEUTRAL -> "Looking calm and steady."
        Emotion.UNKNOWN -> "Checking your vibes…"
    }
