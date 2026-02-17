package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

private enum class QuestStep { NONE, CREATE_TYPE, DIFFICULTY, FOCUS, OVERVIEW }
private enum class QuestType { DAILY, SIDE }
private enum class ExploreTab { OFFICIAL, COMMUNITY }

private data class QuestPlan(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val type: QuestType,
    val difficulty: Difficulty,
    val focus: Focus,
    val rewardText: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(
    onBack: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    var step by remember { mutableStateOf(QuestStep.NONE) }
    var type by remember { mutableStateOf(QuestType.DAILY) }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var focus by remember { mutableStateOf(Focus.LOWER) }

    var selectedPlan by remember { mutableStateOf<QuestPlan?>(null) }

    // UI state (nuevo)
    var tab by remember { mutableStateOf(ExploreTab.OFFICIAL) }
    var query by remember { mutableStateOf("") }

    val plans = remember {
        listOf(
            QuestPlan(
                title = "Candito - Fuerza 6 Semanas",
                subtitle = "Ideal para testear 1RM y competir",
                imageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=70",
                type = QuestType.DAILY,
                difficulty = Difficulty.MEDIUM,
                focus = Focus.LOWER,
                rewardText = "PRO"
            ),
            QuestPlan(
                title = "Juggernaut - Deadlift",
                subtitle = "Enfocado a levantar más en 16 semanas",
                imageUrl = "https://images.unsplash.com/photo-1517964603305-11c0f6f66012?auto=format&fit=crop&w=1200&q=70",
                type = QuestType.DAILY,
                difficulty = Difficulty.HARD,
                focus = Focus.LOWER,
                rewardText = "PRO"
            ),
            QuestPlan(
                title = "Upper Hypertrophy",
                subtitle = "Volumen inteligente para pecho/espalda/brazos",
                imageUrl = "https://images.unsplash.com/photo-1517832207067-4db24a2ae47c?auto=format&fit=crop&w=1200&q=70",
                type = QuestType.SIDE,
                difficulty = Difficulty.MEDIUM,
                focus = Focus.UPPER,
                rewardText = "PRO"
            ),
            QuestPlan(
                title = "Corta Hard • Cardio",
                subtitle = "Sprint intenso. Perfecta si tenés poco tiempo.",
                imageUrl = "https://images.pexels.com/photos/4944973/pexels-photo-4944973.jpeg",
                type = QuestType.SIDE,
                difficulty = Difficulty.HARD,
                focus = Focus.CARDIO,
                rewardText = "PRO"
            ),
            QuestPlan(
                title = "Core Builder",
                subtitle = "Abdomen + estabilidad y postura",
                imageUrl = "https://images.pexels.com/photos/8519698/pexels-photo-8519698.jpeg",
                type = QuestType.DAILY,
                difficulty = Difficulty.EASY,
                focus = Focus.ABS,
                rewardText = "PRO"
            ),
        )
    }

    val filteredPlans = remember(plans, query, tab) {
        val q = query.trim().lowercase()
        plans
            .asSequence()
            .filter {
                // (si querés, acá podés separar Official/Community con algún flag real)
                when (tab) {
                    ExploreTab.OFFICIAL -> true
                    ExploreTab.COMMUNITY -> true
                }
            }
            .filter { p ->
                if (q.isBlank()) true
                else (p.title.lowercase().contains(q) || p.subtitle.lowercase().contains(q))
            }
            .toList()
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Explorar", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = textPrimary)
                    }
                }
            )
        },
        bottomBar = {
            // ✅ CTA FIJO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedPlan = null; step = QuestStep.CREATE_TYPE },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Crear misión", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // ✅ HERO / Header Card (como la screenshot)
            item {
                ExploreHeroCard(
                    title = "Programas y rutinas",
                    subtitle = "Elegí un programa y empezá hoy.",
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // ✅ Tabs “Oficial / Comunidad”
            item {
                ExploreTabs(
                    selected = tab,
                    onSelected = { tab = it },
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // ✅ Search
            item {
                ExploreSearchField(
                    value = query,
                    onValueChange = { query = it },
                    accent = accent,
                    input = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // ✅ Listado (armonizado + thumbnail)
            items(filteredPlans) { plan ->
                QuestPlanCardV2(
                    plan = plan,
                    accent = accent,
                    surface = surface,
                    input = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = {
                        selectedPlan = plan
                        type = plan.type
                        difficulty = plan.difficulty
                        focus = plan.focus
                        step = QuestStep.OVERVIEW
                    }
                )
            }

            // (Tu empty state puede quedar abajo)
            if (filteredPlans.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            accent.copy(alpha = 0.12f),
                                            surface
                                        )
                                    )
                                )
                                .padding(1.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sin resultados",
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Probá con otro nombre o palabra clave.",
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
            }
        }

        // ---------- FLUJO MODAL ----------
        if (step != QuestStep.NONE) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            when (step) {
                QuestStep.CREATE_TYPE -> QuestModalShellV2(
                    title = "Crear misión",
                    subtitle = "Elegí el tipo de misión",
                    icon = Icons.Filled.EmojiEvents,
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    imageUrl = null,
                    onClose = { step = QuestStep.NONE }
                ) {
                    TwoUpRow(gap = 12.dp) { cardWidth ->
                        MissionTypeCard(
                            modifier = Modifier.width(cardWidth).height(132.dp),
                            title = "DIARIA",
                            subtitleTop = "24 horas",
                            subtitleBottom = "Constancia",
                            selected = type == QuestType.DAILY,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Schedule,
                            onClick = { type = QuestType.DAILY }
                        )

                        MissionTypeCard(
                            modifier = Modifier.width(cardWidth).height(132.dp),
                            title = "CORTA",
                            subtitleTop = "Sprint 3 horas",
                            subtitleBottom = "Intensidad rápida",
                            selected = type == QuestType.SIDE,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Bolt,
                            onClick = { type = QuestType.SIDE }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    PrimaryCTAGlow(accent = accent, text = "Siguiente", onClick = { step = QuestStep.DIFFICULTY })

                    Spacer(Modifier.height(10.dp))

                    SecondaryCTA(
                        accent = accent,
                        textPrimary = textPrimary,
                        text = "Cancelar",
                        onClick = { step = QuestStep.NONE }
                    )
                }

                QuestStep.DIFFICULTY -> QuestModalShellV2(
                    title = "Seleccionar dificultad",
                    subtitle = "¿Qué tan desafiante querés que sea?",
                    icon = Icons.Filled.Tune,
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    imageUrl = null,
                    onClose = { step = QuestStep.NONE }
                ) {
                    TwoUpRow(gap = 12.dp) { cardWidth ->
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "FÁCIL",
                            subtitle = "1.0x ELO",
                            selected = difficulty == Difficulty.EASY,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.FitnessCenter,
                            onClick = { difficulty = Difficulty.EASY }
                        )
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "MEDIA",
                            subtitle = "1.5x ELO",
                            selected = difficulty == Difficulty.MEDIUM,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Speed,
                            onClick = { difficulty = Difficulty.MEDIUM }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    TwoUpRow(gap = 12.dp) { cardWidth ->
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "DIFÍCIL",
                            subtitle = "2.0x ELO",
                            selected = difficulty == Difficulty.HARD,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.LocalFireDepartment,
                            onClick = { difficulty = Difficulty.HARD }
                        )
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "EXTREMA",
                            subtitle = "3.0x ELO",
                            selected = difficulty == Difficulty.INSANE,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.LocalFireDepartment,
                            onClick = { difficulty = Difficulty.INSANE }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    PrimaryCTAGlow(accent = accent, text = "Siguiente", onClick = { step = QuestStep.FOCUS })

                    Spacer(Modifier.height(10.dp))

                    SecondaryCTA(
                        accent = accent,
                        textPrimary = textPrimary,
                        text = "Volver",
                        onClick = { step = QuestStep.CREATE_TYPE }
                    )
                }

                QuestStep.FOCUS -> QuestModalShellV2(
                    title = "Elegir enfoque",
                    subtitle = "¿Qué grupo muscular querés trabajar?",
                    icon = Icons.Filled.Tune,
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    imageUrl = null,
                    onClose = { step = QuestStep.NONE }
                ) {
                    TwoUpRow(gap = 12.dp) { cardWidth ->
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "TREN SUPERIOR",
                            subtitle = "Brazos, pecho, espalda",
                            selected = focus == Focus.UPPER,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.FitnessCenter,
                            onClick = { focus = Focus.UPPER }
                        )
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "TREN INFERIOR",
                            subtitle = "Piernas y glúteos",
                            selected = focus == Focus.LOWER,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.FitnessCenter,
                            onClick = { focus = Focus.LOWER }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    TwoUpRow(gap = 12.dp) { cardWidth ->
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "ABDOMEN",
                            subtitle = "Core y abdominales",
                            selected = focus == Focus.ABS,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.LocalFireDepartment,
                            onClick = { focus = Focus.ABS }
                        )
                        SelectCardV2(
                            modifier = Modifier.width(cardWidth).height(118.dp),
                            title = "CARDIO",
                            subtitle = "Corazón y pulmones",
                            selected = focus == Focus.CARDIO,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Speed,
                            onClick = { focus = Focus.CARDIO }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    PrimaryCTAGlow(accent = accent, text = "Siguiente", onClick = { step = QuestStep.OVERVIEW })

                    Spacer(Modifier.height(10.dp))

                    SecondaryCTA(
                        accent = accent,
                        textPrimary = textPrimary,
                        text = "Volver",
                        onClick = { step = QuestStep.DIFFICULTY }
                    )
                }

                QuestStep.OVERVIEW -> {
                    val focusLabel = when (focus) {
                        Focus.UPPER -> "Tren superior"
                        Focus.LOWER -> "Tren inferior"
                        Focus.ABS -> "Abdomen"
                        Focus.CARDIO -> "Cardio"
                    }
                    val typeLabel = if (type == QuestType.DAILY) "Diaria" else "Corta"
                    val diffLabel = difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
                    val timeLabel = if (type == QuestType.DAILY) "24 horas" else "3 horas"
                    val reward = "Hasta +4 ELO"
                    val hero = selectedPlan?.imageUrl

                    QuestModalShellV2(
                        title = "Resumen de misión",
                        subtitle = "Tu misión te espera",
                        icon = Icons.Filled.EmojiEvents,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        imageUrl = hero,
                        onClose = { step = QuestStep.NONE }
                    ) {
                        Text(
                            "$typeLabel $diffLabel • $focusLabel",
                            color = textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillV2(diffLabel, accent)
                                PillV2(typeLabel, accent)
                                PillV2(
                                    when (focus) {
                                        Focus.UPPER -> "Superior"
                                        Focus.LOWER -> "Inferior"
                                        Focus.ABS -> "Abdomen"
                                        Focus.CARDIO -> "Cardio"
                                    },
                                    accent
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Límite de tiempo",
                                value = timeLabel,
                                icon = Icons.Filled.AccessTime,
                                accent = accent,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                surface = input
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "Recompensa ELO",
                                value = reward,
                                icon = Icons.Filled.Star,
                                accent = accent,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                surface = input,
                                valueTint = accent
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        PrimaryCTAGlow(
                            accent = accent,
                            text = "ACEPTAR MISIÓN",
                            onClick = { step = QuestStep.NONE }
                        )

                        Spacer(Modifier.height(10.dp))

                        SecondaryCTA(
                            accent = accent,
                            textPrimary = textPrimary,
                            text = "Volver",
                            onClick = { step = QuestStep.NONE }
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun ExploreHeroCard(
    title: String,
    subtitle: String,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val shape = RoundedCornerShape(18.dp)
    val glow = Brush.verticalGradient(
        listOf(
            accent.copy(alpha = 0.18f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glow)
            .padding(1.dp)
            .clip(shape)
            .background(surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(subtitle, color = textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExploreTabs(
    selected: ExploreTab,
    onSelected: (ExploreTab) -> Unit,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TabPill(
            text = "Oficial",
            selected = selected == ExploreTab.OFFICIAL,
            onClick = { onSelected(ExploreTab.OFFICIAL) },
            accent = accent,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
        TabPill(
            text = "Comunidad",
            selected = selected == ExploreTab.COMMUNITY,
            onClick = { onSelected(ExploreTab.COMMUNITY) },
            accent = accent,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val bg = if (selected) accent.copy(alpha = 0.14f) else surface.copy(alpha = 0.65f)
    val border = if (selected) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.12f)
    val tint = if (selected) accent else textSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ExploreSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(input)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary)
            Spacer(Modifier.width(10.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar programas…", color = textSecondary) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestPlanCardV2(
    plan: QuestPlan,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val duration = when (plan.type) {
        QuestType.DAILY -> "6 Semanas"
        QuestType.SIDE -> "16 Semanas"
    }
    val level = when (plan.difficulty) {
        Difficulty.EASY -> "Principiante"
        Difficulty.MEDIUM -> "Intermedio"
        Difficulty.HARD -> "Avanzado"
        Difficulty.INSANE -> "Elite"
    }
    val focusChip = when (plan.focus) {
        Focus.UPPER -> "Fuerza"
        Focus.LOWER -> "Fuerza"
        Focus.ABS -> "Core"
        Focus.CARDIO -> "Cardio"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(plan.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.10f))
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProChip(accent = accent, text = "PRO")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = plan.title,
                            color = textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = plan.subtitle,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TinyPill(text = duration, accent = accent)
                        TinyPill(text = level, accent = accent)
                        TinyPill(text = focusChip, accent = accent)
                    }
                }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Ver",
                        color = GymRankColors.PrimaryAccentText,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // “Stats” abajo, estilo tarjeta chica
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Frecuencia",
                    value = if (plan.type == QuestType.DAILY) "4x/sem" else "3x/sem",
                    surface = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Nivel",
                    value = level,
                    surface = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun ProChip(accent: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.80f))
    ) {
        Text(
            text = text,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TinyPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MiniStat(
    modifier: Modifier,
    label: String,
    value: String,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestModalShellV2(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    imageUrl: String?,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!imageUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.20f))
                                )
                            }
                        } else {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = accent)
                            }
                        }

                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(title, color = textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(subtitle, color = textSecondary, fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

/** Row de 2 columnas sin weight. */
@Composable
private fun TwoUpRow(
    gap: Dp,
    content: @Composable RowScope.(cardWidth: Dp) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardW: Dp = (maxWidth - gap) / 2
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            content(cardW)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionTypeCard(
    modifier: Modifier,
    title: String,
    subtitleTop: String,
    subtitleBottom: String,
    selected: Boolean,
    accent: Color,
    surface: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val border = if (selected) accent.copy(alpha = 0.65f) else accent.copy(alpha = 0.18f)
    val bg = if (selected) accent.copy(alpha = 0.14f) else surface.copy(alpha = 0.90f)
    val iconBg = if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
    val iconTint = if (selected) accent else Color.White.copy(alpha = 0.75f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Column {
                Text(subtitleTop, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitleBottom, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCardV2(
    modifier: Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    surface: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val border = if (selected) accent.copy(alpha = 0.65f) else accent.copy(alpha = 0.18f)
    val bg = if (selected) accent.copy(alpha = 0.14f) else surface.copy(alpha = 0.90f)
    val iconBg = if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
    val iconTint = if (selected) accent else Color.White.copy(alpha = 0.75f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.4.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                }
            }
        }
    }
}

/** ✅ CTA estilo “Gamble-ish”: borde glow + fill sólido */
@Composable
private fun PrimaryCTAGlow(
    accent: Color,
    text: String,
    onClick: () -> Unit
) {
    val glow = Brush.verticalGradient(
        listOf(
            accent.copy(alpha = 0.22f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glow)
            .padding(1.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text,
                color = GymRankColors.PrimaryAccentText,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SecondaryCTA(
    accent: Color,
    textPrimary: Color,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PillV2(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    valueTint: Color = textPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }

                Text(
                    label,
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                value,
                color = valueTint,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
