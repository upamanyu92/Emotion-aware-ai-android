package com.example.emotionawareai.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.engine.DeviceCapabilityDetector
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import kotlinx.coroutines.delay

/**
 * Initial setup screen where the user chooses their on-device LLM.
 *
 * If the device is a Pixel 8+ with Google AI Core installed, the built-in
 * Gemini Nano option appears at the top and is pre-selected as the default.
 * Otherwise the screen shows downloadable models ranked by compatibility
 * with the target device, highlighting the recommended option.
 */
@Composable
fun LlmSetupScreen(
    detector: DeviceCapabilityDetector,
    onModelSelected: (LlmOption) -> Unit
) {
    val optionsWithCompat = remember { detector.allOptionsWithCompatibility() }
    val recommended = remember { detector.recommendedOption() }
    var selectedId by remember { mutableStateOf(recommended.id) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GradStart, GradMid1, GradEnd)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }
                ) {
                    Column {
                        Text(
                            text = "Choose Your AI Model",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select the best on-device LLM for your experience. " +
                                "Models run entirely on your phone \u2014 your data never leaves the device.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ── Device info card ─────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 150)) +
                        slideInVertically(tween(600, delayMillis = 150)) { 30 }
                ) {
                    DeviceInfoCard(detector)
                }
            }

            // ── Model list ───────────────────────────────────────────────────
            itemsIndexed(optionsWithCompat) { index, (option, compat) ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 250 + index * 80)) +
                        slideInVertically(tween(500, delayMillis = 250 + index * 80)) { 40 }
                ) {
                    LlmOptionCard(
                        option = option,
                        compatibility = compat,
                        isSelected = selectedId == option.id,
                        onSelect = { selectedId = option.id }
                    )
                }
            }

            // ── Continue button ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 600))
                ) {
                    Button(
                        onClick = {
                            val selected = optionsWithCompat
                                .map { it.first }
                                .firstOrNull { it.id == selectedId }
                                ?: recommended
                            onModelSelected(selected)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF0A0F1E)
                        )
                    ) {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Device info card ─────────────────────────────────────────────────────────

@Composable
private fun DeviceInfoCard(detector: DeviceCapabilityDetector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Your Device",
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${detector.manufacturer} ${detector.model}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chipset: ${detector.chipset}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RAM: ${detector.totalRamMb} MB",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }

            if (detector.hasBuiltInLlm) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Built-in AI (Google AI Core) detected",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Individual model option card ─────────────────────────────────────────────

@Composable
private fun LlmOptionCard(
    option: LlmOption,
    compatibility: DeviceCapabilityDetector.Compatibility,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else GlassBorder,
        animationSpec = tween(250),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GlassCard.copy(alpha = 0.9f) else GlassCard,
        animationSpec = tween(250),
        label = "bg"
    )

    val compatColor = when {
        compatibility.score >= 85 -> Color(0xFF4CAF50)
        compatibility.score >= 70 -> Color(0xFF8BC34A)
        compatibility.score >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Column {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Selection indicator
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .border(
                                2.dp,
                                if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color(0xFF0A0F1E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = option.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (compatibility.isRecommended) {
                        Badge(text = "Recommended", color = NeonCyan)
                    }
                    if (option.isBuiltIn) {
                        Badge(text = "Built-in", color = Color(0xFF4CAF50))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = option.description,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Size
                Text(
                    text = option.sizeLabel,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                // Quality stars
                Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < option.qualityRating) Icons.Default.Star
                            else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i < option.qualityRating) Color(0xFFFFD54F)
                            else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Compatibility badge
                Text(
                    text = compatibility.label,
                    color = compatColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            // RAM headroom for downloadable models
            if (!option.isBuiltIn) {
                Spacer(modifier = Modifier.height(4.dp))
                val headroomText = if (compatibility.ramHeadroomMb >= 0)
                    "+${compatibility.ramHeadroomMb} MB headroom"
                else
                    "${compatibility.ramHeadroomMb} MB short"
                Text(
                    text = "Min ${option.minRamMb} MB RAM \u2022 $headroomText",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
