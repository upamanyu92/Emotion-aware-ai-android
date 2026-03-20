package com.example.emotionawareai.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromUser
    val horizontalArrangement = if (isUser) Alignment.End else Alignment.Start

    val accentColor by animateColorAsState(
        targetValue = if (isUser) NeonPurple else NeonCyan,
        animationSpec = tween(durationMillis = 300),
        label = "bubbleAccent"
    )

    val bubbleShape = RoundedCornerShape(
        topStart = if (isUser) 22.dp else 6.dp,
        topEnd = if (isUser) 6.dp else 22.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp
    )

    val bgBrush = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                NeonPurple.copy(alpha = 0.32f),
                NeonPurple.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassCard,
                Color(0x10FFFFFF)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = horizontalArrangement
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(bgBrush)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = if (isUser) 0.55f else 0.25f),
                    shape = bubbleShape
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!isUser) {
                Text(
                    text = "MoodMitra AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = if (message.isStreaming && message.content.isEmpty()) "● ● ●"
                       else message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = if (isUser) 0.95f else 0.88f),
                modifier = Modifier.padding(top = if (!isUser) 2.dp else 0.dp)
            )

            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accentColor.copy(alpha = 0.55f),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        // Emotion tag for user messages
        if (isUser && message.emotion.name != "NEUTRAL" && message.emotion.name != "UNKNOWN") {
            Text(
                text = message.emotion.emoji,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun Spacer4dp() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(vertical = 2.dp))
}
