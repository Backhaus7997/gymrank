package com.example.gymrank.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.R
import com.example.gymrank.ui.components.BodySvg
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.components.MuscleId
import com.example.gymrank.ui.session.SessionViewModel
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale


@Composable
fun HomeScreen(
    sessionViewModel: SessionViewModel,
    viewModel: HomeViewModel = viewModel(),
    onLogWorkout: () -> Unit = {},
    onOpenRanking: () -> Unit = {}
) {
    val selectedGym by sessionViewModel.selectedGym.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    selectedGym?.let { gym -> viewModel.setGymData(gym) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeTopBar(
                userName = uiState.userName,
                onOpenRanking = onOpenRanking
            )
        }

        item {
            QuickActionsRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                onLogWorkout = onLogWorkout,
                onOpenRanking = onOpenRanking
            )
        }

        item {
            val (frontCounts, backCounts) = buildMuscleCounts(uiState)
            MusclesTrainedThisWeekCardPro(
                frontCounts = frontCounts,
                backCounts = backCounts,
                modifier = Modifier.padding(horizontal = 16.dp),
                onSettingsClick = { /* TODO */ }
            )
        }

        item {
            WorkoutCalendarCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                weeklyGoal = 8,
                completed = 0
            )
        }

        item {
            SetsByMuscleCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                items = listOf(
                    "Pecho" to 0,
                    "Espalda" to 0,
                    "Piernas" to 0,
                    "Hombros" to 0,
                    "Bíceps" to 0,
                    "Tríceps" to 0,
                    "Abdomen" to 0,
                    "Glúteos" to 0
                ),
                onSeeMore = { /* TODO */ }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

// ============================================
// TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    userName: String,
    onOpenRanking: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Hola 👋",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Text(
                    text = if (userName.isNotBlank()) userName else " ",
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = DesignTokens.Colors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DesignTokens.Colors.SurfaceElevated)
                    .border(1.dp, DesignTokens.Colors.SurfaceInputs, CircleShape)
                    .clickable { onOpenRanking() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "A",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymRankColors.PrimaryAccent
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DesignTokens.Colors.BackgroundBase
        )
    )
}

// ============================================
// QUICK ACTIONS
// ============================================

@Composable
private fun QuickActionsRow(
    modifier: Modifier = Modifier,
    onLogWorkout: () -> Unit,
    onOpenRanking: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Cargar entreno",
            subtitle = "Registrá tu sesión",
            icon = Icons.Default.FitnessCenter,
            onClick = onLogWorkout
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            title = "Ver ranking",
            subtitle = "Tu posición y top",
            icon = Icons.Default.Timeline,
            onClick = onOpenRanking
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = DesignTokens.Colors.SurfaceElevated,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GymRankColors.PrimaryAccent.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        GymRankColors.PrimaryAccent.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GymRankColors.PrimaryAccent
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }
        }
    }
}

// ============================================
// MUSCLES TRAINED CARD
// ============================================

@Composable
private fun MusclesTrainedThisWeekCardPro(
    frontCounts: Map<MuscleId, Int>,
    backCounts: Map<MuscleId, Int>,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurar",
                    tint = DesignTokens.Colors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        MuscleIntensityLegend(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BodyCanvas(
                title = "Frente",
                counts = frontCounts,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(14.dp))
            BodyCanvas(
                title = "Espalda",
                counts = backCounts,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "* Basado en entrenamientos completados",
            fontSize = 12.sp,
            color = DesignTokens.Colors.TextSecondary
        )
    }
}

// ============================================
// BODY (SVG) — override por IDs
// ============================================

private const val BODY_FRONT_IMAGE_URL = "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg"
private const val BODY_BACK_IMAGE_URL  = "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg"

@Composable
private fun BodyCanvas(
    title: String,
    counts: Map<MuscleId, Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DesignTokens.Colors.TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        val isFront = title.lowercase().contains("frente")
        val imageUrl = if (isFront) BODY_FRONT_IMAGE_URL else BODY_BACK_IMAGE_URL

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DesignTokens.Colors.SurfaceElevated)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Body $title",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}


private fun svgIdsForMuscle(id: MuscleId, isFront: Boolean): List<String> {
    return when (id) {
        MuscleId.Chest -> if (isFront) listOf("chest_l", "chest_r") else emptyList()
        MuscleId.Abs -> if (isFront) listOf("abs_upper", "abs_mid", "abs_lower") else emptyList()
        MuscleId.Shoulders ->
            if (isFront) listOf("deltoid_l", "deltoid_r")
            else listOf("rear_deltoid_l", "rear_deltoid_r")

        MuscleId.Biceps -> if (isFront) listOf("biceps_l", "biceps_r") else emptyList()
        MuscleId.Triceps -> if (isFront) emptyList() else listOf("triceps_l", "triceps_r")

        MuscleId.Back -> if (isFront) emptyList() else listOf("back_upper", "back_lower")

        MuscleId.Glutes -> if (isFront) emptyList() else listOf("glutes_l", "glutes_r")

        MuscleId.Legs ->
            if (isFront) listOf("quad_l", "quad_r")
            else listOf("hamstring_l", "hamstring_r")

        MuscleId.Calves ->
            if (isFront) listOf("calf_front_l", "calf_front_r")
            else listOf("calf_l", "calf_r")
    }
}

private fun buildSvgOverrides(
    counts: Map<MuscleId, Int>,
    isFront: Boolean
): Map<String, Color> {
    val c1 = GymRankColors.PrimaryAccent.copy(alpha = 0.28f)
    val c2 = GymRankColors.PrimaryAccent.copy(alpha = 0.58f)
    val c3 = GymRankColors.PrimaryAccent.copy(alpha = 0.92f)

    fun colorForCount(c: Int): Color? = when {
        c <= 0 -> null
        c == 1 -> c1
        c == 2 -> c2
        else -> c3
    }

    val out = mutableMapOf<String, Color>()

    counts.forEach { (muscleId, count) ->
        val col = colorForCount(count) ?: return@forEach
        svgIdsForMuscle(muscleId, isFront).forEach { svgId ->
            out[svgId] = col
        }
    }

    return out
}

// ============================================
// CALENDAR
// ============================================

@Composable
private fun WorkoutCalendarCard(
    modifier: Modifier = Modifier,
    weeklyGoal: Int,
    completed: Int
) {
    GlassCard(modifier = modifier) {
        Text(
            text = "Calendario de entrenamientos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DesignTokens.Colors.TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        val pct =
            if (weeklyGoal <= 0) 0f
            else (completed.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Objetivo semanal: $completed/$weeklyGoal",
                fontSize = 12.sp,
                color = DesignTokens.Colors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(pct * 100).toInt()}%",
                fontSize = 12.sp,
                color = DesignTokens.Colors.TextSecondary
            )
        }

        Spacer(Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = GymRankColors.PrimaryAccent,
            trackColor = DesignTokens.Colors.SurfaceInputs
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")) { day ->
                DayPill(day = day, done = false)
            }
        }
    }
}

@Composable
private fun DayPill(day: String, done: Boolean) {
    val bg =
        if (done) GymRankColors.PrimaryAccent.copy(alpha = 0.14f)
        else DesignTokens.Colors.SurfaceElevated

    val stroke =
        if (done) GymRankColors.PrimaryAccent.copy(alpha = 0.55f)
        else DesignTokens.Colors.SurfaceInputs

    val txt =
        if (done) GymRankColors.PrimaryAccent
        else DesignTokens.Colors.TextPrimary

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day, color = txt, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// SETS BY MUSCLE
// ============================================

@Composable
private fun SetsByMuscleCard(
    modifier: Modifier = Modifier,
    items: List<Pair<String, Int>>,
    onSeeMore: () -> Unit
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sets por músculo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Esta semana",
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Filtrar",
                    tint = DesignTokens.Colors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val max = (items.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { (name, value) ->
                SetMiniCard(name = name, value = value, max = max)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ver más",
            color = GymRankColors.PrimaryAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSeeMore() }
        )
    }
}

@Composable
private fun SetMiniCard(
    name: String,
    value: Int,
    max: Int
) {
    val pct = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.width(86.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(62.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DesignTokens.Colors.SurfaceElevated)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value.toString(),
                    color = DesignTokens.Colors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = GymRankColors.PrimaryAccent,
                    trackColor = DesignTokens.Colors.SurfaceInputs
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = name,
            fontSize = 12.sp,
            color = DesignTokens.Colors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun MuscleIntensityLegend(
    modifier: Modifier = Modifier
) {
    val c1 = GymRankColors.PrimaryAccent.copy(alpha = 0.35f)
    val c2 = GymRankColors.PrimaryAccent.copy(alpha = 0.65f)
    val c3 = GymRankColors.PrimaryAccent.copy(alpha = 0.95f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Intensidad",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DesignTokens.Colors.TextSecondary
        )

        LegendDot(label = "1x", color = c1)
        LegendDot(label = "2x", color = c2)
        LegendDot(label = "3x+", color = c3)
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DesignTokens.Colors.TextPrimary
        )
    }
}

// ============================================
// Helpers (counts)
// ============================================

private fun buildMuscleCounts(
    uiState: HomeUiState
): Pair<Map<MuscleId, Int>, Map<MuscleId, Int>> {
    val last = uiState.lastWorkout
    val muscles: List<String> = last?.muscles ?: emptyList()
    val counts = mutableMapOf<MuscleId, Int>().withDefault { 0 }

    fun inc(id: MuscleId) { counts[id] = counts.getOrDefault(id, 0) + 1 }

    muscles.forEach { m ->
        when (m.lowercase()) {
            "pecho" -> inc(MuscleId.Chest)
            "espalda" -> inc(MuscleId.Back)
            "piernas" -> inc(MuscleId.Legs)
            "hombros" -> inc(MuscleId.Shoulders)
            "biceps", "bíceps" -> inc(MuscleId.Biceps)
            "triceps", "tríceps" -> inc(MuscleId.Triceps)
            "abdomen", "core" -> inc(MuscleId.Abs)
            "gluteos", "glúteos" -> inc(MuscleId.Glutes)
            "pantorrillas", "gemelos" -> inc(MuscleId.Calves)
        }
    }

    // por ahora: mismo mapa para frente y espalda
    return counts.toMap() to counts.toMap()
}
