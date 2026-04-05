package com.example.emotionawareai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonGold
import com.example.emotionawareai.ui.theme.NeonPurple

/**
 * Bottom sheet that lets users rate an AI response (1–5 stars) with an
 * optional freeform comment. Attached to individual assistant message bubbles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIFeedbackSheet(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D1117),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rate This Response",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your feedback helps improve AI performance",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Star rating row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (star in 1..5) {
                    Icon(
                        imageVector = if (star <= selectedRating) Icons.Filled.Star
                        else Icons.Filled.StarBorder,
                        contentDescription = "$star star",
                        tint = if (star <= selectedRating) NeonGold else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { selectedRating = star }
                    )
                }
            }

            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (selectedRating) {
                        1 -> "Poor"
                        2 -> "Below Average"
                        3 -> "Average"
                        4 -> "Good"
                        5 -> "Excellent"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGold.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comment field
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Additional feedback (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = NeonCyan,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }

                Button(
                    onClick = {
                        if (selectedRating > 0) {
                            onSubmit(selectedRating, comment)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonPurple, NeonCyan)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    enabled = selectedRating > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = GlassCard,
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Submit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
