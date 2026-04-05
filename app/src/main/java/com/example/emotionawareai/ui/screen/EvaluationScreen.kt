package com.example.emotionawareai.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.domain.model.EvaluationMetric
import com.example.emotionawareai.domain.model.MetricCategory
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.ui.theme.NeonRose

/**
 * Dashboard screen showing the 22 AI evaluation metrics grouped by category,
 * overall score, and recent user feedback summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(viewModel: ChatViewModel) {
    val metricScores by viewModel.evaluationScores.collectAsStateWithLifecycle()
    val overallScore by viewModel.overallEvaluationScore.collectAsStateWithLifecycle()
    val averageFeedbackRating by viewModel.averageFeedbackRating.collectAsStateWithLifecycle()
    val feedbackCount by viewModel.feedbackCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Evaluation", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117).copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Overall Score Card ──────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OverallScoreCard(overallScore, averageFeedbackRating, feedbackCount)
            }

            // ── Metrics by Category ─────────────────────────────────────
            val categories = MetricCategory.entries
            categories.forEach { category ->
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                val metrics = EvaluationMetric.byCategory(category)
                items(metrics, key = { it.name }) { metric ->
                    MetricRow(
                        metric = metric,
                        score = metricScores[metric.name] ?: 0f
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OverallScoreCard(
    overallScore: Float,
    avgFeedback: Float,
    feedbackCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.25f),
                        NeonCyan.copy(alpha = 0.15f)
                    )
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Overall AI Score",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Automated score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(overallScore * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = scoreColor(overallScore),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // User feedback
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (feedbackCount > 0) String.format("%.1f", avgFeedback) else "–",
                            style = MaterialTheme.typography.headlineLarge,
                            color = NeonGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (feedbackCount > 0) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = NeonGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = "$feedbackCount ratings",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { overallScore.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = scoreColor(overallScore),
                trackColor = GlassCard,
            )
        }
    }
}

@Composable
private fun MetricRow(metric: EvaluationMetric, score: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GlassCard)
            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = metric.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = scoreColor(score),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = scoreColor(score),
            trackColor = GlassCard,
        )
    }
}

@Composable
private fun scoreColor(score: Float): Color = when {
    score >= 0.8f -> NeonCyan
    score >= 0.6f -> NeonGold
    score >= 0.4f -> Color(0xFFFF9800)
    else -> NeonRose
}
