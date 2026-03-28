package com.example.emotionawareai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.emotionawareai.ui.SpeechCaption
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradStart

@Composable
fun VoiceModeOverlay(
    isListening: Boolean,
    isGenerating: Boolean,
    isSpeaking: Boolean,
    speechCaption: SpeechCaption?,
    captionsVisible: Boolean,
    userName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(GradMid1, GradStart, GradEnd)
                )
            )
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Exit voice mode",
                tint = Color.White.copy(alpha = 0.72f)
            )
        }

        AgentPresenceAnimation(
            isListening = isListening,
            isGenerating = isGenerating,
            isSpeaking = isSpeaking,
            speechCaption = speechCaption,
            captionsVisible = captionsVisible,
            userName = userName,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        )
    }
}
