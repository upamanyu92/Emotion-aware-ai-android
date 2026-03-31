package com.example.emotionawareai.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.ui.LlmSetupPhase
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple

/**
 * Initial setup screen displayed while the pre-configured AI model is being
 * downloaded and installed.
 *
 * The download starts automatically in the background (initiated by
 * [com.example.emotionawareai.EmotionAwareApp]); this screen simply reflects
 * the current [setupPhase]:
 *   - [LlmSetupPhase.DOWNLOADING] / [LlmSetupPhase.VERIFYING]: progress UI
 *     with a "Skip — continue in background" option.
 *   - [LlmSetupPhase.FAILED]: error card with "Retry Download" and "Skip" buttons.
 *   - [LlmSetupPhase.COMPLETE]: brief success state before navigation proceeds.
 */
@Composable
fun LlmSetupScreen(
    setupPhase: LlmSetupPhase,
    downloadProgress: Float?,
    setupError: String?,
    onSkipSetup: () -> Unit,
    onRetrySetup: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GradStart, GradMid1, GradEnd))
            )
    ) {
        AnimatedContent(
            targetState = setupPhase,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(300))
            },
            label = "setupPhaseContent"
        ) { phase ->
            when (phase) {
                LlmSetupPhase.DOWNLOADING, LlmSetupPhase.VERIFYING -> {
                    DownloadingContent(
                        modelName = LlmOption.CONFIGURED_MODEL.name,
                        modelSizeLabel = LlmOption.CONFIGURED_MODEL.sizeLabel,
                        phase = phase,
                        progress = downloadProgress,
                        onSkip = onSkipSetup
                    )
                }
                LlmSetupPhase.FAILED -> {
                    FailedContent(
                        modelName = LlmOption.CONFIGURED_MODEL.name,
                        errorMessage = setupError
                            ?: "Download failed. Please try again or check your connection and available storage.",
                        onRetry = onRetrySetup,
                        onSkip = onSkipSetup
                    )
                }
                LlmSetupPhase.COMPLETE -> {
                    SuccessContent(modelName = LlmOption.CONFIGURED_MODEL.name)
                }
            }
        }
    }
}

// ── Downloading / verifying phase ────────────────────────────────────────────

@Composable
private fun DownloadingContent(
    modelName: String,
    modelSizeLabel: String,
    phase: LlmSetupPhase,
    progress: Float?,
    onSkip: () -> Unit
) {
    val isVerifying = phase == LlmSetupPhase.VERIFYING
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = NeonCyan,
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = if (isVerifying) "Configuring $modelName…" else "Downloading $modelName",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!isVerifying) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCard)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (progress != null && progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonCyan,
                            trackColor = NeonPurple.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "%.0f%% \u2014 $modelSizeLabel".format(progress * 100),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonCyan,
                            trackColor = NeonPurple.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "Connecting\u2026",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Text(
                text = "All data stays on your device.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.15f)
                )
            ) {
                Text("Skip \u2014 continue in background", fontSize = 13.sp)
            }
        }
    }
}

// ── Failure phase ─────────────────────────────────────────────────────────────

@Composable
private fun FailedContent(
    modelName: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Setup failed",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Download Failed",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCard)
                    .border(1.dp, Color(0xFFF44336).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0A0F1E)
                    )
                ) {
                    Text(
                        "Retry Download",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.25f)
                    )
                ) {
                    Text("Skip \u2014 continue in background")
                }
            }
        }
    }
}

// ── Success phase ─────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(modelName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Setup complete",
                tint = NeonCyan,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "AI Model Ready!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$modelName is installed and ready.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
