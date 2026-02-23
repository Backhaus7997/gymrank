@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.model.WorkoutExercise
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    onGoToCreate: () -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    val uiState by viewModel.uiState.collectAsState()
    val workouts = uiState.workouts
    val totalWorkouts = workouts.size

    var query by remember { mutableStateOf("") }
    val filteredWorkouts by remember(workouts, query) {
        derivedStateOf {
            val q = query.trim().lowercase(Locale.getDefault())
            if (q.isBlank()) workouts
            else workouts.filter { w ->
                val t = (w.type ?: w.title).lowercase(Locale.getDefault())
                val intensity = (w.intensity ?: "").lowercase(Locale.getDefault())
                val notes = (w.notes ?: "").lowercase(Locale.getDefault())
                val muscles = w.muscles.joinToString(" ").lowercase(Locale.getDefault())
                val ex = w.exercises.joinToString(" ") { it.name }.lowercase(Locale.getDefault())
                t.contains(q) || intensity.contains(q) || muscles.contains(q) || notes.contains(q) || ex.contains(q)
            }
        }
    }

    // --- Bottom sheet selection ---
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (selectedWorkout != null) {
        WorkoutDetailsBottomSheet(
            workout = selectedWorkout!!,
            onDismiss = { selectedWorkout = null },
            sheetState = sheetState,
            bg = bg,
            surface = surface,
            input = input,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Historial de entrenamientos",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                total = totalWorkouts,
                accent = accent,
                surface = surface,
                input = input,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            SearchBarPro(
                query = query,
                onQueryChange = { query = it },
                input = input,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Text(
                text = "Tus entrenamientos",
                color = textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            when {
                uiState.isLoading -> {
                    GlassCard {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator(color = accent) }
                    }
                }

                uiState.error != null -> {
                    GlassCard {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ocurrió un error",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.error ?: "Error",
                                color = GymRankColors.Error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                workouts.isEmpty() -> {
                    EmptyStateCard(
                        onGoToCreate = onGoToCreate,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                filteredWorkouts.isEmpty() -> {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sin resultados",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Probá con otro texto (nombre, músculo, ejercicio o intensidad).",
                                color = textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 14.dp)
                    ) {
                        items(filteredWorkouts.sortedByDescending { it.timestampMillis ?: it.createdAt ?: 0L }) { w ->
                            WorkoutCardRow(
                                workout = w,
                                onDetails = { selectedWorkout = w },
                                accent = accent,
                                surface = surface,
                                input = input,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------- Cards (list) ----------------------------- */

@Composable
private fun WorkoutCardRow(
    workout: Workout,
    onDetails: () -> Unit,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val fmtDate = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")) }
    val ts = workout.timestampMillis ?: workout.createdAt ?: 0L
    val dateText = remember(ts) { if (ts == 0L) "—" else fmtDate.format(Date(ts)) }

    val title = (workout.type?.takeIf { it.isNotBlank() } ?: workout.title)
        .ifBlank { "Entrenamiento" }

    val previewExercises = workout.exercises.take(3)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        // ✅ un poco más de padding y más aire entre bloques
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,            // ✅ antes 16
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))    // ✅ micro aire
                    Text(
                        text = dateText,
                        color = textSecondary,
                        fontSize = 11.sp,             // ✅ antes 12
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = input,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
                ) {
                    TextButton(
                        onClick = onDetails,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Ver Detalles",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp          // ✅ antes 12
                        )
                    }
                }
            }

            if (previewExercises.isEmpty()) {
                Text(
                    text = "Sin ejercicios cargados",
                    color = textSecondary,
                    fontSize = 11.sp,               // ✅ antes 12
                    modifier = Modifier.alpha(0.95f)
                )
            } else {
                // ✅ más espacio entre líneas de ejercicios
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    previewExercises.forEach { ex ->
                        ExerciseLineCompact(
                            ex = ex,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLineCompact(
    ex: WorkoutExercise,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• ${ex.name.ifBlank { "Ejercicio" }}",
            color = textSecondary,
            fontSize = 11.sp, // ✅ antes 12
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp)) // ✅ un toque más de aire
        Text(
            text = "${max(0, ex.sets)}x${max(0, ex.reps)}",
            color = textSecondary,
            fontSize = 11.sp // ✅ antes 12
        )
    }
}

/* ----------------------------- Bottom sheet ----------------------------- */

@Composable
private fun WorkoutDetailsBottomSheet(
    workout: Workout,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    bg: Color,
    surface: Color,
    input: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val fmtDay = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "AR")) }
    val ts = workout.timestampMillis ?: workout.createdAt ?: 0L
    val dateText = remember(ts) { if (ts == 0L) "—" else fmtDay.format(Date(ts)) }

    val title = (workout.type?.takeIf { it.isNotBlank() } ?: workout.title)
        .ifBlank { "Entrenamiento" }

    val exercises = workout.exercises
    val minutes = workout.durationMinutes ?: 0
    val intensity = workout.intensity?.trim().orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp)
                ) { Box(Modifier.size(width = 44.dp, height = 5.dp)) }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Ver Detalles",
                    color = textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = textSecondary)
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = surface,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChipPill(text = "${exercises.size} ej.", input = input, textColor = textSecondary)
                        ChipPill(text = dateText, input = input, textColor = textSecondary)

                        if (minutes > 0) {
                            MetaPillSimple(
                                text = "${minutes} min",
                                input = input,
                                accent = accent,
                                textPrimary = textPrimary
                            )
                        }
                        if (intensity.isNotBlank()) {
                            MetaPillSimple(
                                text = intensity,
                                input = input,
                                accent = accent,
                                textPrimary = textPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Ejercicios",
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (exercises.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = input,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sin ejercicios",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exercises.forEach { ex ->
                        ExerciseCard(
                            ex = ex,
                            surface = surface,
                            input = input,
                            accent = accent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            workout.notes?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Notas",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = input,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note,
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    ex: WorkoutExercise,
    surface: Color,
    input: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ex.name.ifBlank { "Ejercicio" },
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val weightText = when {
                    ex.usesBodyweight -> "Peso corporal"
                    ex.weightKg != null -> "Peso: ${ex.weightKg} kg"
                    else -> "Peso: —"
                }

                Text(
                    text = weightText,
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = input,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
            ) {
                Text(
                    text = "${max(0, ex.sets)}x${max(0, ex.reps)}",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/* ----------------------------- Small UI bits ----------------------------- */

@Composable
private fun SearchBarPro(
    query: String,
    onQueryChange: (String) -> Unit,
    input: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Buscar por nombre, músculo o intensidad", color = textSecondary) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.copy(alpha = 0.55f),
            unfocusedBorderColor = accent.copy(alpha = 0.20f),
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary,
            cursorColor = accent,
            focusedContainerColor = input,
            unfocusedContainerColor = input
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SummaryCard(
    total: Int,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val glow = Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(glow)
            .padding(1.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Surface(
            color = surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Resumen", color = textSecondary, fontSize = 12.sp)

                    Text(
                        text = "Total de entrenamientos",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = total.toString(),
                            color = accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "entrenamientos",
                            color = textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Surface(
                    color = input,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Historial",
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    onGoToCreate: () -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(46.dp)
            )

            Text(
                text = "Todavía no registraste entrenamientos",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Cuando cargues uno, acá vas a ver el detalle.",
                color = textSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .alpha(0.95f)
            )

            Button(
                onClick = onGoToCreate,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cargar entrenamiento", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChipPill(
    text: String,
    input: Color,
    textColor: Color
) {
    Surface(
        color = input,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MetaPillSimple(
    text: String,
    input: Color,
    accent: Color,
    textPrimary: Color
) {
    Surface(
        color = input,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
    ) {
        Text(
            text = text,
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}