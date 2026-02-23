package com.example.gymrank.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymrank.ui.theme.GymRankColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    minHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    val borderStroke: BorderStroke = if (glow) {
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    GymRankColors.PrimaryAccent.copy(alpha = 0.35f),
                    GymRankColors.PrimaryAccent.copy(alpha = 0.10f)
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            color = GymRankColors.PrimaryAccent.copy(alpha = 0.14f)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (minHeight != null) Modifier.heightIn(min = minHeight) else Modifier)
            .border(borderStroke, shape)
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212).copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // ✅ Padding responsive: en cards angostas (grid 2 columnas) reduce padding
        BoxWithConstraints {
            val isNarrow = maxWidth < 180.dp

            val resolvedPadding = contentPadding ?: if (isNarrow) {
                PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            } else {
                PaddingValues(horizontal = 14.dp, vertical = 16.dp)
            }

            Column(
                modifier = Modifier.padding(resolvedPadding)
            ) {
                content()
            }
        }
    }
}
