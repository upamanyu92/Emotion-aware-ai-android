package com.example.emotionawareai.ui.component

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.ui.SpeechCaption
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ── Constants ─────────────────────────────────────────────────────────────────

private const val PARTICLE_N = 2000
private const val PARTICLE_FOV = 550f
private const val PARTICLE_CAMERA_Z = 600f
private const val PARTICLE_DISPLAY_NAME = "Tara"

// ── Particle state held in flat FloatArrays for GC-free updates ──────────────

private class ParticleState {
    val px = FloatArray(PARTICLE_N)
    val py = FloatArray(PARTICLE_N)
    val pz = FloatArray(PARTICLE_N)
    val vx = FloatArray(PARTICLE_N)
    val vy = FloatArray(PARTICLE_N)
    val vz = FloatArray(PARTICLE_N)
    val tx = FloatArray(PARTICLE_N)
    val ty = FloatArray(PARTICLE_N)
    val tz = FloatArray(PARTICLE_N)
    val ox = FloatArray(PARTICLE_N)   // sphere home – unit-sphere coords
    val oy = FloatArray(PARTICLE_N)
    val oz = FloatArray(PARTICLE_N)
    val hue = FloatArray(PARTICLE_N)
    val phase = FloatArray(PARTICLE_N)
    var rotY = 0f
    var appState = 0          // 0 = rotating sphere, 1 = text formation
    var sphereRadius = 0f
    var initialized = false
}

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * 3-D particle engine that renders a rotating Fibonacci sphere in the idle
 * state and morphs the particles into the current AI-state label ("Listening",
 * "Thinking", "Speaking", "Ready") when active.
 *
 * Drop-in replacement for [AgentPresenceAnimation] in [ChatScreen].
 * The Canvas sits in the same layout slot (below the status row, above the
 * input bar) so it never overlays the camera wallpaper.
 */
@Composable
fun ParticleTextAnimation(
    isListening: Boolean,
    isGenerating: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    isCameraActive: Boolean = false,
    speechCaption: SpeechCaption?,
    captionsVisible: Boolean,
    userName: String
) {
    val displayText = when {
        isListening -> "Listening"
        isGenerating -> "Thinking"
        isSpeaking -> "Speaking"
        else -> "Ready"
    }

    val state = remember { ParticleState() }
    var tick by remember { mutableLongStateOf(0L) }
    var canvasSizeW by remember { mutableStateOf(0) }
    var canvasSizeH by remember { mutableStateOf(0) }

    // ── Sphere initialisation (once, when canvas size is first available) ─────
    LaunchedEffect(canvasSizeW, canvasSizeH) {
        if (canvasSizeW <= 0 || canvasSizeH <= 0 || state.initialized) return@LaunchedEffect
        val sphereR = minOf(canvasSizeW, canvasSizeH).toFloat() * 0.42f
        state.sphereRadius = sphereR
        val phi = (sqrt(5.0) + 1.0) / 2.0
        for (i in 0 until PARTICLE_N) {
            val yy = 1.0 - (i.toDouble() / (PARTICLE_N - 1)) * 2.0
            val rr = sqrt(1.0 - yy * yy)
            val theta = 2.0 * PI * i.toDouble() / phi
            val ix = (rr * cos(theta)).toFloat()
            val iy = yy.toFloat()
            val iz = (rr * sin(theta)).toFloat()
            state.ox[i] = ix; state.oy[i] = iy; state.oz[i] = iz
            state.px[i] = ix * sphereR; state.py[i] = iy * sphereR; state.pz[i] = iz * sphereR
            state.tx[i] = state.px[i]; state.ty[i] = state.py[i]; state.tz[i] = state.pz[i]
            state.hue[i] = (i.toFloat() / PARTICLE_N) * 360f
            state.phase[i] = (i.toFloat() / PARTICLE_N) * (2.0 * PI).toFloat()
        }
        state.initialized = true
    }

    // ── Target positions: sphere home or sampled text pixels ─────────────────
    LaunchedEffect(displayText, canvasSizeW, canvasSizeH) {
        if (!state.initialized || canvasSizeW <= 0 || canvasSizeH <= 0) return@LaunchedEffect
        if (displayText == "Ready") {
            // Return every particle to its sphere home position
            val r = state.sphereRadius
            for (i in 0 until PARTICLE_N) {
                state.tx[i] = state.ox[i] * r
                state.ty[i] = state.oy[i] * r
                state.tz[i] = state.oz[i] * r
            }
            state.appState = 0
        } else {
            // Sample text on a background thread to avoid blocking the UI
            val targets = withContext(Dispatchers.Default) {
                sampleTextTargets(displayText, canvasSizeW, canvasSizeH, PARTICLE_N)
            }
            // Write targets and flip appState together on the main thread
            for (i in 0 until PARTICLE_N) {
                state.tx[i] = targets[i * 2]
                state.ty[i] = targets[i * 2 + 1]
                state.tz[i] = 0f
            }
            state.appState = 1
        }
    }

    // ── Physics loop: spring + friction, executed once per frame ─────────────
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos ->
                if (!state.initialized) return@withFrameNanos
                val t = (frameNanos / 1_000_000_000.0).toFloat()
                if (state.appState == 0) state.rotY += 0.006f
                val cosRY = cos(state.rotY)
                val sinRY = sin(state.rotY)
                val r = state.sphereRadius
                val sp = if (state.appState == 0) 0.02f else 0.022f
                for (i in 0 until PARTICLE_N) {
                    if (state.appState == 0) {
                        // Orbit + wobble – each particle floats around its sphere home
                        val wobble = sin(t * 1.2f + state.phase[i]) * 1.8f
                        val bx = state.ox[i] * r
                        val by = state.oy[i] * r
                        val bz = state.oz[i] * r
                        state.tx[i] = bx * cosRY + bz * sinRY
                        state.ty[i] = by + wobble * 0.3f
                        state.tz[i] = -bx * sinRY + bz * cosRY
                    }
                    // Spring toward target
                    state.vx[i] += (state.tx[i] - state.px[i]) * sp
                    state.vy[i] += (state.ty[i] - state.py[i]) * sp
                    state.vz[i] += (state.tz[i] - state.pz[i]) * sp
                    // Friction
                    state.vx[i] *= 0.82f
                    state.vy[i] *= 0.82f
                    state.vz[i] *= 0.82f
                    state.px[i] += state.vx[i]
                    state.py[i] += state.vy[i]
                    state.pz[i] += state.vz[i]
                }
                tick = frameNanos
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .onSizeChanged { size ->
                    canvasSizeW = size.width
                    canvasSizeH = size.height
                }
        ) {
            // Subscribes this draw scope to frame-tick updates so it redraws each frame
            tick.let { }
            if (!state.initialized) return@Canvas

            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val t = (tick / 1_000_000_000.0).toFloat()

            for (i in 0 until PARTICLE_N) {
                val worldZ = state.pz[i] + PARTICLE_CAMERA_Z
                if (worldZ <= 0f) continue
                val scale = PARTICLE_FOV / worldZ
                val sx = state.px[i] * scale + cx
                val sy = state.py[i] * scale + cy
                if (sx < -5f || sx > w + 5f || sy < -5f || sy > h + 5f) continue
                val pSize = (1.5f * scale).coerceIn(0.5f, 4f)
                val color = if (state.appState == 0) {
                    particleHslToColor((state.hue[i] + t * 25f) % 360f, 80f, 65f)
                } else {
                    particleHslToColor(190f, 90f, 85f)   // neon-cyan for text mode
                }
                drawCircle(color = color, radius = pSize, center = Offset(sx, sy))
            }
        }

        // ── Speech captions (preserved from AgentPresenceAnimation) ──────────
        AnimatedVisibility(
            visible = captionsVisible && speechCaption != null,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically { it / 3 },
            exit = fadeOut(animationSpec = tween(220)) + slideOutVertically { it / 4 }
        ) {
            val captionTurnId = speechCaption?.turnId
            AnimatedContent(
                targetState = captionTurnId,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + slideInVertically { it / 5 }) togetherWith
                        (fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 6 })
                },
                label = "speech_caption_turn"
            ) { currentTurnId ->
                val currentCaption = speechCaption?.takeIf { it.turnId == currentTurnId }
                if (currentCaption != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.46f), RoundedCornerShape(22.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (currentCaption.speaker == MessageRole.USER) {
                                userName.ifBlank { "You" }
                            } else {
                                PARTICLE_DISPLAY_NAME
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (currentCaption.speaker == MessageRole.USER) NeonCyan else NeonRose
                        )
                        Text(
                            text = currentCaption.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Renders [text] onto an off-screen [Bitmap] of [w]×[h] pixels, samples every
 * opaque pixel, shuffles the result (Fisher-Yates), and fills [n] target slots
 * (wrapping if there are fewer sampled pixels than particles).
 *
 * Returns a [FloatArray] of size [n]×2: `[x0, y0, x1, y1, …]` where each
 * coordinate is centred at the origin (i.e. relative to the canvas centre).
 */
private fun sampleTextTargets(text: String, w: Int, h: Int, n: Int): FloatArray {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val aCanvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
    }
    paint.textSize = (w * 0.22f).coerceAtMost(h * 0.55f)
    val textX = w / 2f
    val textY = h / 2f - (paint.descent() + paint.ascent()) / 2f
    aCanvas.drawText(text, textX, textY, paint)

    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    bitmap.recycle()

    // Collect sampled points as a flat [x0, y0, x1, y1, …] array
    val raw = ArrayList<Float>(n * 2)
    val step = 3   // sample every 3rd pixel to control density
    for (y in 0 until h step step) {
        for (x in 0 until w step step) {
            if ((pixels[y * w + x] ushr 24) and 0xFF > 120) {
                raw.add(x - w / 2f + (Random.nextFloat() * 0.8f - 0.4f))
                raw.add(y - h / 2f + (Random.nextFloat() * 0.8f - 0.4f))
            }
        }
    }

    // Fisher-Yates shuffle on pairs
    val pairCount = raw.size / 2
    val arr = raw.toFloatArray()
    for (i in pairCount - 1 downTo 1) {
        val j = Random.nextInt(i + 1)
        val tmpX = arr[i * 2]; val tmpY = arr[i * 2 + 1]
        arr[i * 2] = arr[j * 2]; arr[i * 2 + 1] = arr[j * 2 + 1]
        arr[j * 2] = tmpX; arr[j * 2 + 1] = tmpY
    }

    // Fill the result array, wrapping if needed
    val result = FloatArray(n * 2)
    if (pairCount > 0) {
        for (i in 0 until n) {
            val idx = (i % pairCount) * 2
            result[i * 2] = arr[idx]
            result[i * 2 + 1] = arr[idx + 1]
        }
    }
    return result
}

/** Fast HSL → Compose [Color] without any object allocation in the hot path. */
private fun particleHslToColor(h: Float, s: Float, l: Float): Color {
    val ss = s / 100f
    val ll = l / 100f
    val c = (1f - abs(2f * ll - 1f)) * ss
    val hh = h / 60f
    val x = c * (1f - abs(hh % 2f - 1f))
    val m = ll - c / 2f
    val (r, g, b) = when {
        hh < 1f -> Triple(c, x, 0f)
        hh < 2f -> Triple(x, c, 0f)
        hh < 3f -> Triple(0f, c, x)
        hh < 4f -> Triple(0f, x, c)
        hh < 5f -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
