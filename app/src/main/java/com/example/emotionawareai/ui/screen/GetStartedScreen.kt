package com.example.emotionawareai.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.emotionawareai.ui.theme.NeonPurple
import kotlinx.coroutines.launch

// ── Page data ────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val assetPath: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

private val pages = listOf(
    OnboardingPage(
        assetPath = "get_started/chat_response.png",
        title = "AI-Powered Conversations",
        subtitle = "Chat with Tara — your emotionally aware companion that understands context, mood, and tone.",
        accent = NeonPurple
    ),
    OnboardingPage(
        assetPath = "get_started/voice_input.png",
        title = "Speak Naturally",
        subtitle = "Use your voice for hands-free conversation. Tara listens, understands, and responds aloud.",
        accent = Color(0xFFFF6B9D)  // NeonRose
    ),
    OnboardingPage(
        assetPath = "get_started/diary.png",
        title = "AI Diary",
        subtitle = "Capture your thoughts and feelings throughout the day. Tara summarises them into meaningful entries.",
        accent = Color(0xFFFFD166)  // NeonGold
    ),
    OnboardingPage(
        assetPath = "get_started/insights.png",
        title = "Weekly Insights",
        subtitle = "Track your emotional patterns and growth with personalised weekly analytics.",
        accent = NeonCyan
    ),
    OnboardingPage(
        assetPath = "get_started/goals.png",
        title = "Personal Growth Goals",
        subtitle = "Set goals, track progress, and let Tara help you stay accountable on your journey.",
        accent = NeonPurple
    )
)

// ── Main composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GetStartedScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    // Animated gradient background
    val gradientTransition = rememberInfiniteTransition(label = "getStartedBg")
    val gradientShift by gradientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradShift"
    )
    val orbPulse by gradientTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    val gradColor0 = lerp(GradStart, GradMid1, gradientShift)
    val gradColor1 = lerp(GradMid1, GradMid2, gradientShift)
    val gradColor2 = lerp(GradMid2, GradEnd, 1f - gradientShift)

    val ctaScale by animateFloatAsState(
        targetValue = if (isLastPage) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ctaScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradColor0, gradColor1, gradColor2)))
    ) {
        // Background accent glow orbs
        Box(
            modifier = Modifier
                .size((240 * orbPulse).dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonPurple.copy(alpha = 0.18f * orbPulse), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size((180 * (1f - orbPulse * 0.2f)).dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonCyan.copy(alpha = 0.12f * orbPulse), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar: Skip button ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan.copy(alpha = 0.8f)
                    )
                }
            }

            // ── Pager ────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(
                    page = pages[pageIndex],
                    pageIndex = pageIndex,
                    isActive = pagerState.currentPage == pageIndex
                )
            }

            // ── Page indicator dots ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, page ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateFloatAsState(
                        targetValue = if (isSelected) 28f else 10f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "dotWidth_$index"
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.35f,
                        animationSpec = tween(300),
                        label = "dotAlpha_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .width(dotWidth.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (isSelected) page.accent.copy(alpha = dotAlpha)
                                else Color.White.copy(alpha = dotAlpha)
                            )
                    )
                }
            }

            // ── CTA button ───────────────────────────────────────────────────
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(56.dp)
                    .scale(ctaScale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(NeonPurple, NeonCyan.copy(alpha = 0.8f))),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLastPage) "Get Started ✨" else "Next →",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Individual page content ──────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    isActive: Boolean
) {
    val context = LocalContext.current

    // Load screenshot from assets
    val bitmap = remember(page.assetPath) {
        runCatching {
            context.assets.open(page.assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    // Entry animation per page
    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.88f) }
    val entryOffsetY = remember { Animatable(40f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            // Animate in
            launch { entryAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            launch { entryScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { entryOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        } else {
            // Reset for next entry
            entryAlpha.snapTo(0f)
            entryScale.snapTo(0.88f)
            entryOffsetY.snapTo(40f)
        }
    }

    // Floating animation for the screenshot
    val floatTransition = rememberInfiniteTransition(label = "float_$pageIndex")
    val floatY by floatTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800 + pageIndex * 300, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY_$pageIndex"
    )
    val glowPulse by floatTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_$pageIndex"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .alpha(entryAlpha.value)
            .offset(y = entryOffsetY.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Screenshot with floating effect + glow shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow behind screenshot
            Box(
                modifier = Modifier
                    .size(260.dp, 440.dp)
                    .offset(y = (floatY + 8).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accent.copy(alpha = 0.15f * glowPulse),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .blur(40.dp)
            )

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = page.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .scale(entryScale.value)
                        .offset(y = floatY.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    page.accent.copy(alpha = 0.6f),
                                    GlassBorder
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                )
            } else {
                // Fallback placeholder if asset fails to load
                Box(
                    modifier = Modifier
                        .size(220.dp, 380.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(GlassCard)
                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📱",
                        fontSize = 48.sp
                    )
                }
            }
        }

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        // Subtitle
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}




