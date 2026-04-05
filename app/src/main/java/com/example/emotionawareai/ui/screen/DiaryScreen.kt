package com.example.emotionawareai.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.ui.theme.NeonRose

/**
 * AI Companion Diary screen.
 *
 * The user can leave this screen open and the app will continuously listen
 * to ambient speech, transcribe it to text, and store each fragment as a
 * diary entry. At the end of the day (or on demand) the AI generates a
 * comprehensive summary from all captured text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(viewModel: ChatViewModel) {
    val isDiaryListening by viewModel.isDiaryListening.collectAsStateWithLifecycle()
    val diaryTranscripts by viewModel.diaryTranscripts.collectAsStateWithLifecycle()
    val diarySummary by viewModel.diarySummary.collectAsStateWithLifecycle()
    val diaryDates by viewModel.diaryDates.collectAsStateWithLifecycle()
    val isSummaryGenerating by viewModel.isDiarySummaryGenerating.collectAsStateWithLifecycle()

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micColor by animateColorAsState(
        targetValue = if (isDiaryListening) NeonRose else NeonCyan,
        animationSpec = tween(500),
        label = "micColor"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Diary", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117).copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary FAB
                if (diaryTranscripts.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { viewModel.generateDiarySummary() },
                        containerColor = NeonPurple,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Summarize,
                            contentDescription = "Generate Summary",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Mic FAB
                FloatingActionButton(
                    onClick = {
                        if (isDiaryListening) viewModel.stopDiaryListening()
                        else viewModel.startDiaryListening()
                    },
                    containerColor = micColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .then(
                            if (isDiaryListening) Modifier.scale(pulseScale) else Modifier
                        )
                ) {
                    Icon(
                        imageVector = if (isDiaryListening) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (isDiaryListening) "Stop Listening" else "Start Listening",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Listening Status ─────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ListeningStatusBanner(isDiaryListening)
            }

            // ── Daily Summary ────────────────────────────────────────
            if (diarySummary.isNotBlank() || isSummaryGenerating) {
                item {
                    DiarySummaryCard(diarySummary, isSummaryGenerating)
                }
            }

            // ── Past Diary Dates ─────────────────────────────────────
            if (diaryDates.size > 1) {
                item {
                    Text(
                        text = "Past Entries",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ── Today's Transcripts ─────────────────────────────────
            item {
                if (diaryTranscripts.isNotEmpty()) {
                    Text(
                        text = "Today's Transcripts (${diaryTranscripts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            items(diaryTranscripts) { transcript ->
                TranscriptBubble(text = transcript)
            }

            if (diaryTranscripts.isEmpty() && !isDiaryListening) {
                item {
                    EmptyDiaryPlaceholder()
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun ListeningStatusBanner(isListening: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isListening) listOf(
                        NeonRose.copy(alpha = 0.2f),
                        NeonPurple.copy(alpha = 0.15f)
                    ) else listOf(
                        GlassCard,
                        GlassCard
                    )
                )
            )
            .border(
                1.dp,
                if (isListening) NeonRose.copy(alpha = 0.4f) else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isListening) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    tint = NeonRose,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isListening) "Listening… Speak naturally, your words are being captured."
                else "Tap the microphone to start capturing your thoughts.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = if (isListening) 0.9f else 0.6f)
            )
        }
    }
}

@Composable
private fun DiarySummaryCard(summary: String, isGenerating: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = NeonPurple.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = NeonGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isGenerating) "Generating summary…" else summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = if (isGenerating) 0.5f else 0.85f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TranscriptBubble(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GlassCard)
            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyDiaryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = NeonCyan.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your AI Diary",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Leave this screen open and speak naturally.\nThe AI will capture your thoughts and create\na comprehensive daily summary.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
