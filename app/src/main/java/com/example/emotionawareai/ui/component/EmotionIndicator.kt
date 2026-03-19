package com.example.emotionawareai.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

private fun emotionColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY     -> EmotionHappy
    Emotion.SAD       -> EmotionSad
    Emotion.ANGRY     -> EmotionAngry
    Emotion.SURPRISED -> EmotionSurprised
    Emotion.FEARFUL   -> EmotionFearful
    Emotion.DISGUSTED -> EmotionDisgusted
    Emotion.NEUTRAL   -> EmotionNeutral
    Emotion.UNKNOWN   -> EmotionUnknown
}

@Composable
fun EmotionIndicator(
    emotion: Emotion,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val color by animateColorAsState(
        targetValue = emotionColor(emotion),
        animationSpec = tween(durationMillis = 400),
        label = "emotionColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (emotion != Emotion.NEUTRAL && emotion != Emotion.UNKNOWN) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "emotionScale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = emotion.emoji,
            fontSize = 18.sp
        )
        if (showLabel) {
            Column {
                Text(
                    text = emotion.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
        }
    }
}
