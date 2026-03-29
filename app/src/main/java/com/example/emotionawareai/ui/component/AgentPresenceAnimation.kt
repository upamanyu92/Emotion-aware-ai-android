package com.example.emotionawareai.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.ui.SpeechCaption
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.ui.theme.NeonRose

private const val ASSISTANT_DISPLAY_NAME = "Tara"

private enum class AgentVisualState(
    val title: String,
    val subtitle: String,
    val accent: Color
) {
    LISTENING(
        title = "Listening",
        subtitle = "Your voice becomes the next caption",
        accent = NeonCyan
    ),
    THINKING(
        title = "Thinking",
        subtitle = "Tara is composing the next response",
        accent = NeonPurple
    ),
    SPEAKING(
        title = "Speaking",
        subtitle = "Tara is reading the response aloud",
        accent = NeonRose
    ),
    IDLE(
        title = "Ready",
        subtitle = "Start speaking or type to continue",
        accent = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
fun AgentPresenceAnimation(
    isListening: Boolean,
    isGenerating: Boolean,
    isSpeaking: Boolean,
    speechCaption: SpeechCaption?,
    captionsVisible: Boolean,
    userName: String,
    modifier: Modifier = Modifier
) {
    val state = when {
        isListening -> AgentVisualState.LISTENING
        isGenerating -> AgentVisualState.THINKING
        isSpeaking -> AgentVisualState.SPEAKING
        else -> AgentVisualState.IDLE
    }

    val transition = rememberInfiniteTransition(label = "agent_presence")
    val orbScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val ringScale by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val barAnim by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barAnim"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(208.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(196.dp)
                    .scale(ringScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                state.accent.copy(alpha = if (state == AgentVisualState.IDLE) 0.12f else 0.24f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                state.accent.copy(alpha = 0.9f),
                                NeonPurple.copy(alpha = 0.8f),
                                NeonCyan.copy(alpha = 0.8f),
                                state.accent.copy(alpha = 0.9f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .scale(if (state == AgentVisualState.IDLE) 1f else orbScale)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    state.accent.copy(alpha = 0.28f),
                                    GlassCard,
                                    Color.Black.copy(alpha = 0.36f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            AgentVisualState.LISTENING -> Icons.Filled.Mic
                            AgentVisualState.THINKING -> Icons.Filled.AutoAwesome
                            AgentVisualState.SPEAKING -> Icons.Filled.GraphicEq
                            AgentVisualState.IDLE -> Icons.Filled.Hearing
                        },
                        contentDescription = null,
                        tint = state.accent,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(5) { index ->
                val multiplier = 0.55f + (index % 3) * 0.18f
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height((18 + (32 * barAnim * multiplier)).dp)
                        .background(
                            state.accent.copy(alpha = if (state == AgentVisualState.IDLE) 0.25f else 0.85f),
                            RoundedCornerShape(100)
                        )
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )
        }

        AnimatedVisibility(
            visible = captionsVisible && speechCaption != null,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically { it / 3 },
            exit = fadeOut(animationSpec = tween(220)) + slideOutVertically { it / 4 }
        ) {
            val captionTurnId = speechCaption?.turnId
            AnimatedContent(
                targetState = captionTurnId,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + slideInVertically { it / 5 }) togetherWith
                        (fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 6 })
                },
                label = "speech_caption_turn"
            ) { currentTurnId ->
                val currentCaption = speechCaption?.takeIf { it.turnId == currentTurnId }
                if (currentCaption != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.Black.copy(alpha = 0.46f),
                                RoundedCornerShape(22.dp)
                            )
                            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (currentCaption.speaker == MessageRole.USER) {
                                userName.ifBlank { "You" }
                            } else {
                                ASSISTANT_DISPLAY_NAME
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (currentCaption.speaker == MessageRole.USER) NeonCyan else NeonRose
                        )
                        Text(
                            text = currentCaption.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}
