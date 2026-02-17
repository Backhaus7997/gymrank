package com.example.gymrank.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymrank.ui.theme.GymRankColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderStroke: BorderStroke = if (glow) {
        // Subtle gradient border to simulate glow
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    GymRankColors.PrimaryAccent.copy(alpha = 0.35f),
                    GymRankColors.PrimaryAccent.copy(alpha = 0.12f)
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            color = GymRankColors.PrimaryAccent.copy(alpha = 0.18f)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(borderStroke, shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212).copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            content()
        }
    }
}
