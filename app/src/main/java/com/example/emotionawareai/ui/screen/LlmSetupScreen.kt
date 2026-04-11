package com.example.emotionawareai.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.emotionawareai.engine.DeviceCapabilityDetector
import com.example.emotionawareai.ui.LlmSetupPhase
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple

private val NeonOrange = Color(0xFFFF9800)
private val NeonRed = Color(0xFFF44336)

/**
 * Initial setup screen shown before the first model is downloaded.
 *
 * - [LlmSetupPhase.SELECTING]: Device-aware model picker with compatibility scores,
 *   recommended badge, and heat/stability warnings for heavy models.
 * - [LlmSetupPhase.DOWNLOADING] / [LlmSetupPhase.VERIFYING]: Progress UI with a
 *   "Skip" option.
 * - [LlmSetupPhase.FAILED]: Error card with Retry and Skip buttons.
 * - [LlmSetupPhase.COMPLETE]: Brief success state before navigation proceeds.
 */
@Composable
fun LlmSetupScreen(
    setupPhase: LlmSetupPhase,
    downloadProgress: Float?,
    setupError: String?,
    availableModels: List<Pair<LlmOption, DeviceCapabilityDetector.Compatibility>>,
    deviceRamMb: Int,
    deviceModel: String,
    deviceChipset: String,
    selectedModelForDownload: LlmOption,
    onModelSelected: (LlmOption) -> Unit,
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
                LlmSetupPhase.SELECTING -> {
                    SelectingContent(
                        availableModels = availableModels,
                        deviceRamMb = deviceRamMb,
                        deviceModel = deviceModel,
                        deviceChipset = deviceChipset,
                        onModelSelected = onModelSelected,
                        onSkip = onSkipSetup
                    )
                }
                LlmSetupPhase.DOWNLOADING, LlmSetupPhase.VERIFYING -> {
                    DownloadingContent(
                        modelName = selectedModelForDownload.name,
                        modelSizeLabel = selectedModelForDownload.sizeLabel,
                        phase = phase,
                        progress = downloadProgress,
                        onSkip = onSkipSetup
                    )
                }
                LlmSetupPhase.FAILED -> {
                    FailedContent(
                        modelName = selectedModelForDownload.name,
                        errorMessage = setupError
                            ?: "Download failed. Please check your connection and available storage.",
                        onRetry = onRetrySetup,
                        onSkip = onSkipSetup
                    )
                }
                LlmSetupPhase.COMPLETE -> {
                    SuccessContent(modelName = selectedModelForDownload.name)
                }
            }
        }
    }
}

// ── Model selection phase ─────────────────────────────────────────────────────

@Composable
private fun SelectingContent(
    availableModels: List<Pair<LlmOption, DeviceCapabilityDetector.Compatibility>>,
    deviceRamMb: Int,
    deviceModel: String,
    deviceChipset: String,
    onModelSelected: (LlmOption) -> Unit,
    onSkip: () -> Unit
) {
    // Pending confirmation for a model that requires a warning dialog
    var pendingWarningModel by remember { mutableStateOf<LlmOption?>(null) }

    pendingWarningModel?.let { model ->
        AlertDialog(
            onDismissRequest = { pendingWarningModel = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "High Resource Warning",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "\"${model.name}\" (${model.technicalName}) requires at least " +
                    "${model.minRamMb / 1024} GB of RAM and downloads " +
                    "${model.sizeLabel} of data.\n\n" +
                    "Running this model on a device with insufficient RAM may cause " +
                    "severe slowdowns, overheating, or application crashes.\n\n" +
                    "Only proceed if your device has ${model.minRamMb / 1024}+ GB RAM " +
                    "and adequate cooling.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingWarningModel = null
                        onModelSelected(model)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
                ) {
                    Text("I Understand — Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingWarningModel = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1A1F35),
            shape = RoundedCornerShape(20.dp)
        )
    }

    val deviceRamGb = deviceRamMb / 1024

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }

        // ── Header ──────────────────────────────────────────────────────────
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Choose Your AI Brain",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick the AI model that powers your companion.\nAll data stays on your device.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // ── Device info card ─────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Your Device",
                        color = NeonPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))
                DeviceInfoRow("Model", deviceModel.ifBlank { "Android Device" })
                DeviceInfoRow("RAM", "$deviceRamGb GB")
                DeviceInfoRow("Chip", deviceChipset.ifBlank { "Unknown" })
            }
        }

        // ── Recommended badge ────────────────────────────────────────────────
        val recommendedEntry = availableModels.firstOrNull { it.second.isRecommended }
        if (recommendedEntry != null) {
            item {
                Text(
                    "⭐  Best for Your Device",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                ModelCard(
                    option = recommendedEntry.first,
                    compatibility = recommendedEntry.second,
                    isRecommended = true,
                    onClick = {
                        if (recommendedEntry.first.requiresWarning) {
                            pendingWarningModel = recommendedEntry.first
                        } else {
                            onModelSelected(recommendedEntry.first)
                        }
                    }
                )
            }
        }

        // ── All options ───────────────────────────────────────────────────────
        item {
            Text(
                "All AI Options",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(availableModels.filter { !it.second.isRecommended }) { (option, compat) ->
            ModelCard(
                option = option,
                compatibility = compat,
                isRecommended = false,
                onClick = {
                    if (option.requiresWarning) {
                        pendingWarningModel = option
                    } else {
                        onModelSelected(option)
                    }
                }
            )
        }

        // ── Skip button ───────────────────────────────────────────────────────
        item {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.12f)
                )
            ) {
                Text("Skip for now — set up later in Settings", fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ModelCard(
    option: LlmOption,
    compatibility: DeviceCapabilityDetector.Compatibility,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isRecommended -> NeonCyan.copy(alpha = 0.6f)
        option.requiresWarning && compatibility.score < 50 -> NeonRed.copy(alpha = 0.4f)
        option.requiresWarning -> NeonOrange.copy(alpha = 0.4f)
        else -> GlassBorder
    }
    val compatColor = when (compatibility.label) {
        "Excellent" -> NeonCyan
        "Good" -> Color(0xFF81C784)
        "Fair" -> NeonOrange
        else -> NeonRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isRecommended) NeonCyan.copy(alpha = 0.07f) else GlassCard
            )
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRecommended) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (option.requiresWarning) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "High resource usage",
                    tint = if (compatibility.score < 50) NeonRed else NeonOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                option.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Compatibility badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(compatColor.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    compatibility.label,
                    color = compatColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            option.description,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        // Specs row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.parameterLabel.isNotBlank()) {
                SpecChip(option.parameterLabel)
            }
            if (!option.isBuiltIn) {
                SpecChip(option.sizeLabel)
                SpecChip("${option.minRamMb / 1024} GB RAM min")
            }
        }

        // Quality stars
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { i ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i < option.qualityRating) NeonCyan else Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Quality",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }

        if (option.idealUseCase.isNotBlank()) {
            Text(
                "Best for: ${option.idealUseCase}",
                color = NeonPurple.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Heat warning note for risky models
        if (option.requiresWarning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (compatibility.score < 50) NeonRed.copy(alpha = 0.12f)
                        else NeonOrange.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (compatibility.score < 50) NeonRed else NeonOrange,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    if (compatibility.score < 50)
                        "Not recommended — may cause overheating or crashes on this device"
                    else
                        "Heavy model — may cause warmth or slower responses",
                    color = if (compatibility.score < 50) NeonRed else NeonOrange,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SpecChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
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

