package com.example.gymrank.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

/**
 * Muscles trained this week card: title, legend, front/back body charts, and footer note.
 * Accepts string-keyed counts for easy wiring from repositories; maps internally to MuscleId.
 */
@Composable
fun MusclesTrainedThisWeekCard(
    frontMuscleCounts: Map<String, Int>,
    backMuscleCounts: Map<String, Int>,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
) {
    val front = frontMuscleCounts.toMuscleIdMap()
    val back = backMuscleCounts.toMuscleIdMap()

    GlassCard(modifier = modifier, glow = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Músculos entrenados esta semana",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Basado en entrenamientos cargados",
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = DesignTokens.Colors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Legend (greens)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendChip(text = "1x")
            LegendChip(text = "2x")
            LegendChip(text = "3x+")
        }

        Spacer(Modifier.height(12.dp))

        MuscleBodyChart(
            frontCounts = front,
            backCounts = back,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "* Basado en entrenamientos completados",
            style = MaterialTheme.typography.bodySmall,
            color = GymRankColors.TextSecondary
        )
    }
}

private fun Map<String, Int>.toMuscleIdMap(): Map<MuscleId, Int> {
    val out = mutableMapOf<MuscleId, Int>()
    this.forEach { (k, v) ->
        when (k.lowercase()) {
            "pecho", "chest" -> out[MuscleId.Chest] = v
            "espalda", "back" -> out[MuscleId.Back] = v
            "piernas", "legs" -> out[MuscleId.Legs] = v
            "hombros", "shoulders" -> out[MuscleId.Shoulders] = v
            "biceps", "bíceps", "biceps" -> out[MuscleId.Biceps] = v
            "triceps", "tríceps" -> out[MuscleId.Triceps] = v
            "abdomen", "abs", "core" -> out[MuscleId.Abs] = v
            "gluteos", "glúteos", "glutes" -> out[MuscleId.Glutes] = v
            "pantorrillas", "gemelos", "calves" -> out[MuscleId.Calves] = v
        }
    }
    return out
}

@Composable
private fun LegendChip(text: String) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .padding(end = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = GymRankColors.PrimaryAccent, fontSize = 12.sp)
    }
}
