@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.gymrank.ui.screens.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
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
fun WorkoutScreen(
    onExploreClick: () -> Unit = {},
    onCoachClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCreateRoutineClick: () -> Unit = {},
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    // =========================
    // Workouts (últimos 3)
    // =========================
    val repo = remember { WorkoutRepositoryFirestoreImpl() }
    val allWorkouts by remember { repo.getWorkouts() }.collectAsState(initial = emptyList())

    val last3 = remember(allWorkouts) {
        allWorkouts
            .sortedByDescending { it.timestampMillis ?: it.createdAt ?: 0L }
            .take(3)
    }

    // =========================
    // Bottom sheet (idéntico a historial)
    // =========================
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

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Entrenar", color = textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* TODO menu */ }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            FeatureGrid(
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface,
                onExploreClick = onExploreClick,
                onCoachClick = onCoachClick,
                onHistoryClick = onHistoryClick,
                onProgressClick = onProgressClick
            )

            Spacer(Modifier.height(20.dp))

            RecoverySection(
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent
            )

            Spacer(Modifier.height(20.dp))

            // ✅ Mini historial (últimos 3) + mismo popup de historial
            MyWorkoutsSection(
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                surface = surface,
                input = input,
                items = last3,
                onCreateWorkout = onCreateRoutineClick,
                onOpenHistory = onHistoryClick,
                onOpenDetails = { w -> selectedWorkout = w }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ============================================
   FEATURE GRID (igual que tu screen)
   ============================================ */

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
private fun FeatureGrid(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    onExploreClick: () -> Unit,
    onCoachClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProgressClick: () -> Unit,
) {
    val items = listOf(
        FeatureItem("Explorar", "Rutinas y programas", Icons.Filled.Search),
        FeatureItem("Coach IA", "Plan inteligente", Icons.Outlined.AutoAwesome),
        FeatureItem("Historial", "Tus entrenamientos", Icons.Filled.History),
        FeatureItem("Progreso", "Evolución y stats", Icons.Filled.BarChart),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[0], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onExploreClick() }
            FeatureButton(items[1], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onCoachClick() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[2], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onHistoryClick() }
            FeatureButton(items[3], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onProgressClick() }
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    color: Color,
    baseFontSizeSp: Float,
    minFontSizeSp: Float,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    lineHeightSp: Float? = null,
) {
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        var chosen = baseFontSizeSp

        while (chosen > minFontSizeSp) {
            val style = TextStyle(
                fontSize = chosen.sp,
                fontWeight = fontWeight,
                lineHeight = (lineHeightSp ?: (chosen + 2f)).sp
            )
            val result = measurer.measure(
                text = text,
                style = style,
                maxLines = maxLines
            )
            if (!result.hasVisualOverflow && result.size.width <= maxWidthPx) break
            chosen -= 1f
        }

        Text(
            text = text,
            color = color,
            fontSize = chosen.sp,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = (lineHeightSp ?: (chosen + 2f)).sp
        )
    }
}

@Composable
private fun FeatureButton(
    item: FeatureItem,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val gradient = Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.86f)))

    GlassCard(
        modifier = modifier,
        minHeight = 84.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 78.dp)
                    .background(gradient, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    AutoSizeText(
                        text = item.title,
                        color = textPrimary,
                        baseFontSizeSp = 15f,
                        minFontSizeSp = 12f,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeightSp = 16f
                    )

                    Spacer(Modifier.height(2.dp))

                    AutoSizeText(
                        text = item.subtitle,
                        color = textSecondary,
                        baseFontSizeSp = 12f,
                        minFontSizeSp = 10f,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeightSp = 13f
                    )
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        }
    }
}

/* ============================================
   RECOVERY (dejé tu versión simple)
   ============================================ */

@Composable
private fun RecoverySection(
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    GlassCard(glow = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recuperación muscular", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { /* TODO */ }) {
                Text("Detalles", color = accent, fontSize = 14.sp)
            }
        }

        Text("Estimado en base a tus entrenamientos", color = textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        // Placeholder
        val muscles = remember {
            listOf(
                "Abductores" to 100,
                "Abdominales" to 100,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            muscles.forEach { (name, percent) ->
                RecoveryMuscleCard(
                    name = name,
                    percent = percent,
                    accent = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class MuscleGroup { LEG, ARM, TORSO, OTHER }

private fun muscleGroupFor(name: String): MuscleGroup {
    val n = name.lowercase().trim()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u")
    return when (n) {
        "cuadriceps", "isquiotibiales", "pantorrillas", "gemelos",
        "gluteos", "aductores", "abductores", "piernas" -> MuscleGroup.LEG
        "biceps", "triceps", "antebrazos", "hombros", "deltoides" -> MuscleGroup.ARM
        "pecho", "espalda", "abdominales", "abs", "lumbares", "trapecios", "core" -> MuscleGroup.TORSO
        else -> MuscleGroup.OTHER
    }
}

@Composable
private fun MuscleIcon(group: MuscleGroup, tint: Color, modifier: Modifier = Modifier) {
    val icon: ImageVector = when (group) {
        MuscleGroup.LEG -> Icons.Filled.DirectionsRun
        MuscleGroup.ARM -> Icons.Filled.FitnessCenter
        MuscleGroup.TORSO -> Icons.Filled.Accessibility
        MuscleGroup.OTHER -> Icons.Filled.FitnessCenter
    }
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier)
}

@Composable
private fun RecoveryMuscleCard(
    name: String,
    percent: Int,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    MuscleIcon(
                        group = muscleGroupFor(name),
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$percent%",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val p = (percent.coerceIn(0, 100)) / 100f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(p)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.75f))
                )
            }
        }
    }
}

/* ============================================
   ✅ MIS ENTRENAMIENTOS: últimos 3 + detalles sheet
   ============================================ */

@Composable
private fun MyWorkoutsSection(
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    surface: Color,
    input: Color,
    items: List<Workout>,
    onCreateWorkout: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDetails: (Workout) -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Mis Entrenamientos", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Últimos entrenamientos cargados", color = textSecondary, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                    .clickable { onCreateWorkout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear entrenamiento", tint = accent)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCreateWorkout() }
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📝", fontSize = 52.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tocá + para crear tu rutina",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "O encontrá programas en Explorar y Coach IA",
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { w ->
                    WorkoutMiniRow(
                        workout = w,
                        onDetails = { onOpenDetails(w) },
                        accent = accent,
                        surface = surface,
                        input = input,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ver historial completo",
                color = accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenHistory() }
            )
        }
    }
}

@Composable
private fun WorkoutMiniRow(
    workout: Workout,
    onDetails: () -> Unit,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    // ✅ Mostramos nombre real (title/type) arriba
    val title = (workout.type?.takeIf { it.isNotBlank() } ?: workout.title).ifBlank { "Entrenamiento" }

    val musclesText = workout.muscles
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString(" · ")
        .ifBlank { "—" }

    val exCount = workout.exercises.size
    val setsCount = workout.exercises.sumOf { max(0, it.sets) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = musclesText,
                    color = textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "$exCount ejercicios • $setsCount sets",
                    color = textSecondary,
                    fontSize = 11.sp
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
                        "Ver detalles",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/* ============================================
   ✅ BOTTOM SHEET COPIADO 1:1 DE HISTORIAL
   ============================================ */

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
                    text = "Detalles",
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