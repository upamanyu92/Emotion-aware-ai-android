package com.example.emotionawareai.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.CornerRadius
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
 * Uses only `boot_background.png` plus lightweight Compose-drawn overlays.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }

    // Gentle cinematic movement for the background image.
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val bgScale by infiniteTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale"
    )
    val bgOffsetX by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_offset_x"
    )
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
        titleOffsetY.animateTo(0f, tween(820, easing = FastOutSlowInEasing))
        delay(180)
        subtitleAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        delay(3000)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.boot_background),
            contentDescription = "Splash background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(bgScale)
                .offset(x = bgOffsetX.dp)
        )

        // Soft dark gradient to keep text legible on bright background regions.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.42f),
                        Color.Black.copy(alpha = 0.28f),
                        Color.Black.copy(alpha = 0.58f)
                    )
                )
            )
        }

        // Floating particles overlay for motion depth.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val particleCount = 20
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((particlePhase + i * (360.0 / particleCount)) % 360.0)
                val radiusX = w * 0.28f + (i % 4) * w * 0.06f
                val radiusY = h * 0.22f + (i % 3) * h * 0.08f
                val cx = w / 2 + radiusX * cos(angle).toFloat()
                val cy = h / 2 + radiusY * sin(angle).toFloat()
                val particleAlpha = 0.08f + (i % 5) * 0.04f
                val particleRadius = 1.4f + (i % 4) * 1.1f
                val color = if (i % 2 == 0) NeonCyan else NeonPurple
                drawCircle(
                    color = color.copy(alpha = particleAlpha),
                    radius = particleRadius.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Glow beam behind title.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(glowAlpha)
        ) {
            val center = Offset(size.width / 2, size.height * 0.58f)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.34f),
                        NeonPurple.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.62f
                ),
                topLeft = Offset(size.width * 0.12f, size.height * 0.45f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.26f),
                cornerRadius = CornerRadius(size.width * 0.18f, size.width * 0.18f)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .offset(y = titleOffsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Emotion Aware AI",
                color = Color.White.copy(alpha = titleAlpha.value),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "offline emotional assistant",
                color = NeonCyan.copy(alpha = subtitleAlpha.value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}
