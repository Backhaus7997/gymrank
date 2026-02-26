package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.domain.model.WorkoutTemplateDay
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    templateId: String,
    onBack: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val glass = Color(0xFF1C1C1E)
    val stroke = Color(0xFF2C2C2E)
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = Color(0xFF2EF2A0)

    val vm: ProgramDetailViewModel = viewModel()
    val ui by vm.state.collectAsState()

    LaunchedEffect(templateId) {
        vm.load(templateId)
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Programa", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { inner ->
        when {
            ui.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent)
                }
            }

            ui.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No se pudo cargar el programa", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(ui.error ?: "", color = textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.load(templateId) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Reintentar", fontWeight = FontWeight.Bold) }
                }
            }

            else -> {
                val template = ui.template

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .background(bg),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Header del programa
                    item {
                        GlassCard {
                            val cover = template?.coverUrl?.trim().orEmpty()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                if (cover.startsWith("http")) {
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = "Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1F2322), Color(0xFF0F0F0F))
                                            )
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.15f),
                                                Color.Black.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                                )

                                Column(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                                ) {
                                    Text(
                                        text = template?.title?.ifBlank { templateId } ?: templateId,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = template?.description.orEmpty(),
                                        color = textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Chips resumen
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallChip(text = if (template?.isPro == true) "PRO" else "Comunidad", accent, stroke, textSecondary)
                                if ((template?.weeks ?: 0) > 0) SmallChip(text = "${template?.weeks} Semanas", accent, stroke, textSecondary)
                                if (!template?.level.isNullOrBlank()) SmallChip(text = template?.level ?: "", accent, stroke, textSecondary)
                                if ((template?.frequencyPerWeek ?: 0) > 0) SmallChip(text = "${template?.frequencyPerWeek}x/sem", accent, stroke, textSecondary)
                            }
                        }
                    }

                    // Lista de días
                    items(ui.days) { day ->
                        DayCard(day = day, textPrimary = textPrimary, textSecondary = textSecondary, stroke = stroke)
                    }

                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, accent: Color, stroke: Color, textSecondary: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF1C1C1E),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(listOf(stroke, stroke)))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (text.equals("PRO", true)) accent else textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun DayCard(
    day: WorkoutTemplateDay,
    textPrimary: Color,
    textSecondary: Color,
    stroke: Color
) {
    GlassCard {
        Text(day.title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (day.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(day.description, color = textSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))

        day.exercises.forEach { ex ->
            Text(
                text = "• ${ex.name}  ·  ${ex.sets}x${ex.reps}",
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}