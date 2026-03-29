package com.example.emotionawareai.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.R
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated boot splash screen displayed for ~3 seconds on app launch.
 *
 * Features:
 * - Deep gradient background with floating particle animation
 * - Banner image (splash_banner) that scales up with spring-like easing
 * - Pulsating glow ring evoking a heartbeat / emotional-awareness theme
 * - Tagline that fades in gracefully
 *
 * After the hold duration [onFinished] is invoked to advance navigation.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Animatable values ────────────────────────────────────────────────────

    val bannerAlpha = remember { Animatable(0f) }
    val bannerScale = remember { Animatable(0.7f) }
    val taglineAlpha = remember { Animatable(0f) }
    val bannerOffsetY = remember { Animatable(40f) }

    // Infinite pulsating glow
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Floating particles
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    // ── Animation timeline ───────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        // Banner entrance
        bannerAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        bannerScale.animateTo(1f, tween(1000, easing = EaseOutBack))
    }
    LaunchedEffect(Unit) {
        bannerOffsetY.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(600)
        taglineAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050A18),
                        Color(0xFF0D1B2A),
                        Color(0xFF0A0F1E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val particleCount = 18
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((particlePhase + i * (360.0 / particleCount)) % 360.0)
                val radiusX = w * 0.3f + (i % 3) * w * 0.08f
                val radiusY = h * 0.25f + (i % 4) * h * 0.06f
                val cx = w / 2 + radiusX * cos(angle).toFloat()
                val cy = h / 2 + radiusY * sin(angle).toFloat()
                val particleAlpha = 0.12f + (i % 5) * 0.06f
                val particleRadius = 1.5f + (i % 4) * 1.2f
                val color = if (i % 2 == 0) NeonCyan else NeonPurple
                drawCircle(
                    color = color.copy(alpha = particleAlpha),
                    radius = particleRadius.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Pulsating glow ring behind the banner
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(200.dp)
                .scale(pulseScale)
                .alpha(glowAlpha)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.5f),
                        NeonPurple.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.45f
                ),
                center = center,
                radius = size.width * 0.45f
            )
        }

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .offset(y = bannerOffsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Banner image with scale + fade animation
            Image(
                painter = painterResource(id = R.drawable.splash_banner),
                contentDescription = "SensEAI Labs banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(bannerAlpha.value)
                    .scale(bannerScale.value),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tagline
            Text(
                text = "Your Emotional Wellness Companion",
                color = NeonCyan.copy(alpha = taglineAlpha.value),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "powered by on-device AI",
                color = Color.White.copy(alpha = taglineAlpha.value * 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }

        // Heartbeat line at the bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(bannerAlpha.value)
        ) {
            val w = size.width
            val midY = size.height / 2
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, midY)
                // flat line
                lineTo(w * 0.25f, midY)
                // heartbeat spike
                lineTo(w * 0.32f, midY - size.height * 0.35f)
                lineTo(w * 0.38f, midY + size.height * 0.25f)
                lineTo(w * 0.44f, midY - size.height * 0.15f)
                lineTo(w * 0.50f, midY)
                // flat line continuation
                lineTo(w, midY)
            }
            drawPath(
                path = path,
                color = NeonCyan.copy(alpha = 0.6f * bannerAlpha.value),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
