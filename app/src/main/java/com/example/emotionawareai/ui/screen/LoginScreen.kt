package com.example.emotionawareai.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradMid2
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple

private val AVATAR_OPTIONS = listOf("😊", "😎", "🧘", "🎯", "💡", "🌟", "🦁", "🐼", "🚀", "🎵")

/**
 * Onboarding / login screen shown on first launch.
 *
 * Collects the user's display name and an emoji avatar.  Everything is stored
 * locally — no network call is ever made.
 *
 * @param onProfileCreated Called with (name, avatar) once the user taps "Get Started".
 */
@Composable
fun LoginScreen(onProfileCreated: (name: String, avatar: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AVATAR_OPTIONS.first()) }
    var showContent by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current

    // Trigger content animation shortly after composition
    LaunchedEffect(Unit) { showContent = true }

    // Animated gradient background (same system as ChatScreen)
    val gradientTransition = rememberInfiniteTransition(label = "loginBgGradient")
    val gradientShift by gradientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loginGradShift"
    )
    val orbPulse by gradientTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loginOrbPulse"
    )

    val gradColor0 = lerp(GradStart, GradMid1, gradientShift)
    val gradColor1 = lerp(GradMid1, GradMid2, gradientShift)
    val gradColor2 = lerp(GradMid2, GradEnd, 1f - gradientShift)

    val ctaScale by animateFloatAsState(
        targetValue = if (name.trim().isNotBlank()) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ctaScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(gradColor0, gradColor1, gradColor2))
            )
    ) {
        // Accent glow orbs
        Box(
            modifier = Modifier
                .size((260 * orbPulse).dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonPurple.copy(alpha = 0.22f * orbPulse), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size((200 * (1f - orbPulse * 0.2f)).dp)
                .align(Alignment.BottomCenter)
                .offset(y = 80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonCyan.copy(alpha = 0.14f * orbPulse), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(60.dp)
        )

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600)) + slideInVertically(
                tween(700),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // ── App logo / title ─────────────────────────────────────────
                Text(
                    text = "MoodMitra AI",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your emotionally aware AI companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                // ── Avatar picker ────────────────────────────────────────────
                Text(
                    text = "Choose your avatar",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(14.dp))

                // Selected avatar display
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NeonPurple.copy(alpha = 0.35f),
                                    NeonCyan.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(2.dp, NeonPurple.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedAvatar, fontSize = 38.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Avatar grid
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVATAR_OPTIONS.take(5).forEach { emoji ->
                        AvatarChip(
                            emoji = emoji,
                            selected = selectedAvatar == emoji,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAvatar = emoji
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVATAR_OPTIONS.drop(5).forEach { emoji ->
                        AvatarChip(
                            emoji = emoji,
                            selected = selectedAvatar == emoji,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAvatar = emoji
                            }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Name input ───────────────────────────────────────────────
                Text(
                    text = "What should I call you?",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    placeholder = {
                        Text(
                            "Your first name",
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            if (name.trim().isNotBlank()) {
                                onProfileCreated(name.trim(), selectedAvatar)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple.copy(alpha = 0.8f),
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                        cursorColor = NeonCyan,
                        focusedContainerColor = GlassCard,
                        unfocusedContainerColor = Color(0x0DFFFFFF)
                    )
                )

                Spacer(Modifier.height(28.dp))

                // ── Get Started CTA ──────────────────────────────────────────
                Button(
                    onClick = {
                        keyboard?.hide()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (name.trim().isNotBlank()) {
                            onProfileCreated(name.trim(), selectedAvatar)
                        }
                    },
                    enabled = name.trim().isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .scale(ctaScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (name.trim().isNotBlank())
                                    Brush.horizontalGradient(
                                        listOf(NeonPurple, NeonCyan.copy(alpha = 0.8f))
                                    )
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            NeonPurple.copy(alpha = 0.25f),
                                            NeonCyan.copy(alpha = 0.15f)
                                        )
                                    ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Get Started ✨",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Everything stays on your device. No account needed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (selected)
                    Brush.radialGradient(
                        listOf(NeonPurple.copy(alpha = 0.45f), NeonCyan.copy(alpha = 0.2f))
                    )
                else
                    Brush.linearGradient(listOf(GlassCard, GlassCard))
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonGold.copy(alpha = 0.9f) else GlassBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}
