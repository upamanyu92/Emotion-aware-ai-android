package com.example.emotionawareai.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.ChatMessage
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

    val bubbleColor by animateColorAsState(
        targetValue = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(durationMillis = 200),
        label = "bubbleColor"
    )

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = horizontalArrangement
    ) {
        Surface(
            color = bubbleColor.copy(alpha = if (isUser) 0.9f else 0.28f),
            shape = RoundedCornerShape(
                topStart = if (isUser) 22.dp else 10.dp,
                topEnd = if (isUser) 10.dp else 22.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 22.dp else 10.dp,
                        topEnd = if (isUser) 10.dp else 22.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!isUser) {
                    Text(
                        text = "MoodMitra AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (message.isStreaming && message.content.isEmpty()) "●●●"
                           else message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(top = if (!isUser) 4.dp else 0.dp)
                )

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
