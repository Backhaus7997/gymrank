package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    // ✅ Accent no existe → usamos el verde que ya venís usando
    val accent = Color(0xFF2EF2A0)

    val totalWorkouts = 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de entrenamientos", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                }
            )
        },
        containerColor = bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Resumen", color = textSecondary, fontSize = 12.sp)

                    Text(
                        text = "Total de entrenamientos",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = totalWorkouts.toString(),
                            color = accent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "entrenamientos",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(44.dp)
                    )

                    Text(
                        text = "Todavía no registraste entrenamientos",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Cuando cargues uno, acá vas a ver el detalle con ejercicios, series y pesos.",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .alpha(0.95f)
                    )

                    Button(
                        onClick = { /* TODO: navegar a cargar entrenamiento */ },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("Cargar entrenamiento", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
