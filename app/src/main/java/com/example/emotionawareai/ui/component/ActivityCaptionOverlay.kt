package com.example.emotionawareai.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.CaptionCategory

// ── Per-category accent colours ───────────────────────────────────────────────

private val CategoryColorPosture   = Color(0xFF64B5F6) // blue
private val CategoryColorGesture   = Color(0xFF81C784) // green
private val CategoryColorAttention = Color(0xFFFFD54F) // amber
private val CategoryColorBehavior  = Color(0xFFCE93D8) // purple
private val CategoryColorEmotion   = Color(0xFFFF8A65) // orange

private fun categoryColor(category: CaptionCategory): Color = when (category) {
    CaptionCategory.POSTURE   -> CategoryColorPosture
    CaptionCategory.GESTURE   -> CategoryColorGesture
    CaptionCategory.ATTENTION -> CategoryColorAttention
    CaptionCategory.BEHAVIOR  -> CategoryColorBehavior
    CaptionCategory.EMOTION   -> CategoryColorEmotion
}

/**
 * A full-width live-caption strip that displays the most recent
 * [ActivityCaption] values produced by
 * [com.example.emotionawareai.engine.ActivityAnalyzer].
 *
 * The strip is styled like television closed-captions: dark semi-transparent
 * background with colour-coded category labels. Each category is shown as a
 * single row with its emoji, display name, and caption text. The BEHAVIOR
 * row is visually emphasised as the high-level summary.
 *
 * @param captions The current list of captions, one per [CaptionCategory].
 * @param modifier  Optional [Modifier] applied to the outer surface.
 */
@Composable
fun ActivityCaptionOverlay(
    captions: List<ActivityCaption>,
    modifier: Modifier = Modifier
) {
    if (captions.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📷  LIVE ACTIVITY",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            // One row per caption in the defined display order
            val displayOrder = listOf(
                CaptionCategory.BEHAVIOR,
                CaptionCategory.ATTENTION,
                CaptionCategory.POSTURE,
                CaptionCategory.GESTURE
            )

            displayOrder.forEach { category ->
                val caption = captions.find { it.category == category }
                if (caption != null) {
                    CaptionRow(caption = caption)
                }
            }
        }
    }
}

@Composable
private fun CaptionRow(caption: ActivityCaption) {
    val color = categoryColor(caption.category)
    val isBehavior = caption.category == CaptionCategory.BEHAVIOR

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji
        Text(
            text = caption.category.emoji,
            fontSize = if (isBehavior) 15.sp else 12.sp
        )
        Spacer(modifier = Modifier.width(6.dp))

        // Category label
        Text(
            text = caption.category.displayName,
            color = color,
            fontSize = if (isBehavior) 12.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(64.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        // Animated caption text
        AnimatedContent(
            targetState = caption.text,
            transitionSpec = {
                (slideInVertically { h -> h } + fadeIn()) togetherWith
                    (slideOutVertically { h -> -h } + fadeOut()) using
                    SizeTransform(clip = false)
            },
            label = "captionText_${caption.category.name}"
        ) { text ->
            Text(
                text = text,
                color = if (isBehavior) Color.White else Color.White.copy(alpha = 0.85f),
                fontSize = if (isBehavior) 13.sp else 11.sp,
                fontWeight = if (isBehavior) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
