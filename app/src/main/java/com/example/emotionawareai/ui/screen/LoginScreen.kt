// Suppress file-level import warnings for the deprecated Google Sign-In classes.
// The actual usages are isolated in googleSignInIntent / resolveGoogleDisplayName helpers
// (both annotated @Suppress("DEPRECATION")); see the TODO there for the migration path.
@file:Suppress("DEPRECATION")

package com.example.emotionawareai.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.GrowthArea
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradMid2
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private val AVATAR_OPTIONS = listOf("😊", "😎", "🧘", "🎯", "💡", "🌟", "🦁", "🐼", "🚀", "🎵")

// Google Sign-In (legacy API) helpers.
// TODO: Migrate to androidx.credentials CredentialManager + GetGoogleIdOption once
//       a Google Cloud OAuth web-client ID is available for this project.
//       See: https://developer.android.com/identity/sign-in/legacy-gsi-migration
@Suppress("DEPRECATION")
private fun googleSignInIntent(context: android.content.Context): Intent {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()
    return GoogleSignIn.getClient(context, gso).signInIntent
}

@Suppress("DEPRECATION")
private fun resolveGoogleDisplayName(intent: Intent?): String? {
    return try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent)
            .getResult(ApiException::class.java)
        account?.displayName?.ifBlank { null }
            ?: account?.email?.substringBefore('@')
    } catch (_: ApiException) {
        null
    }
}

/**
 * Onboarding / login screen shown on first launch.
 *
 * Offers Google Sign-In, Apple Sign-In (via browser OAuth), and a local
 * "continue without account" path. The selected display name and avatar are
 * stored locally — no remote account data is retained by the app.
 *
 * @param onProfileCreated Called with (name, avatar) once the user proceeds.
 * @param onAppleSignIn    Called when the user taps "Continue with Apple"; the
 *                         caller is responsible for launching the Custom-Tabs
 *                         flow and calling [onProfileCreated] on success.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onProfileCreated: (name: String, avatar: String) -> Unit,
    onOnboardingComplete: (areas: List<GrowthArea>, frequency: String) -> Unit = { _, _ -> },
    onAppleSignIn: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AVATAR_OPTIONS.first()) }
    var showContent by remember { mutableStateOf(false) }
    var showLocalForm by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableStateOf(0) }
    var selectedAreas by remember { mutableStateOf(setOf<GrowthArea>()) }
    var selectedFrequency by remember { mutableStateOf("daily") }
    var pendingName by remember { mutableStateOf("") }
    var pendingAvatar by remember { mutableStateOf(AVATAR_OPTIONS.first()) }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    // Google Sign-In launcher (calls into suppress-annotated helpers above)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val displayName = resolveGoogleDisplayName(result.data) ?: "User"
            pendingName = displayName
            pendingAvatar = "😊"
            onboardingStep = 1
        } else {
            showLocalForm = true
        }
    }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
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

                Spacer(Modifier.height(36.dp))

                when (onboardingStep) {
                    0 -> {
                if (!showLocalForm) {
                    // ── Social sign-in buttons ───────────────────────────────
                    // Google Sign-In
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            googleSignInLauncher.launch(googleSignInIntent(context))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, NeonCyan.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = "🔵  Continue with Google",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Apple Sign-In
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAppleSignIn()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "🍎  Continue with Apple",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Continue without account
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLocalForm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassCard,
                            contentColor = Color.White.copy(alpha = 0.75f)
                        )
                    ) {
                        Text(
                            text = "Continue without account",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Your data stays on your device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // ── Local profile form ───────────────────────────────────
                    // Avatar picker
                    Text(
                        text = "Choose your avatar",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(14.dp))

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
                                val trimmedName = name.trim()
                                if (trimmedName.isNotBlank()) {
                                    pendingName = trimmedName
                                    pendingAvatar = selectedAvatar
                                    onboardingStep = 1
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

                    Button(
                        onClick = {
                            keyboard?.hide()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val trimmedName = name.trim()
                            if (trimmedName.isNotBlank()) {
                                pendingName = trimmedName
                                pendingAvatar = selectedAvatar
                                onboardingStep = 1
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

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "← Back",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { showLocalForm = false }
                    )
                } // end if-else (step 0)
                    } // end case 0

                    1 -> {
                        // ── Step 1: Growth area selection ────────────────────
                        Text(
                            text = "What do you want to work on?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Pick the areas most relevant to you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GrowthArea.entries.forEach { area ->
                                val isSelected = area in selectedAreas
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAreas = if (isSelected) {
                                            selectedAreas - area
                                        } else {
                                            selectedAreas + area
                                        }
                                    },
                                    label = {
                                        Text(
                                            "${area.emoji} ${area.displayName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Transparent,
                                        selectedContainerColor = NeonPurple.copy(alpha = 0.35f),
                                        labelColor = Color.White,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = GlassBorder,
                                        selectedBorderColor = NeonCyan
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = { onboardingStep = 2 },
                            enabled = selectedAreas.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple,
                                disabledContainerColor = NeonPurple.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                "Continue →",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    } // end case 1

                    2 -> {
                        // ── Step 2: Check-in frequency + privacy ─────────────
                        Text(
                            text = "How often would you like to check in?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        listOf(
                            Triple("daily", "Daily", "A check-in every day"),
                            Triple("weekly", "Weekly", "Once a week review"),
                            Triple("as_needed", "As needed", "Whenever you feel like it")
                        ).forEach { (value, label, subtitle) ->
                            val isSelected = selectedFrequency == value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) NeonPurple.copy(alpha = 0.25f) else GlassCard
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan.copy(alpha = 0.7f) else GlassBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedFrequency = value }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        "✓",
                                        color = NeonCyan,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "🔒 Your data stays on your device. Nothing is sent to external servers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                "ℹ️ Tara is for personal growth, not medical care. If you're in crisis, call 988.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                onOnboardingComplete(selectedAreas.toList(), selectedFrequency)
                                onProfileCreated(pendingName, pendingAvatar)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text(
                                "Get Started ✨",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    } // end case 2
                } // end when
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

