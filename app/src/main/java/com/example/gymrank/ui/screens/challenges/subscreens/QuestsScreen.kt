@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymrank.domain.model.MissionTemplate
import com.example.gymrank.domain.model.UserMission
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

private enum class MissionsTab { DISCOVER, LIBRARY }

private enum class QuestStep {
    NONE,
    DURATION,
    FOCUS,
    DETAILS,
    SUMMARY,
    OVERVIEW_TEMPLATE
}

/**
 * ✅ IMPORTANTE:
 * NO declaramos Difficulty/Focus acá porque YA existen en ChallengeEnums.kt
 * (mismo package). Las usamos directo.
 */

@Composable
fun QuestsScreen(
    onBack: () -> Unit,
    vm: QuestsViewModel = viewModel()
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    var tab by remember { mutableStateOf(MissionsTab.DISCOVER) }
    var query by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(QuestStep.NONE) }
    var selectedTemplate by remember { mutableStateOf<MissionTemplate?>(null) }

    // ---------- CUSTOM FLOW STATE ----------
    var durationDays by remember { mutableIntStateOf(14) } // 14 / 21 / 28
    var difficulty by remember { mutableStateOf(difficultyFromDuration(14)) } // ✅ auto
    var focus by remember { mutableStateOf(Focus.LOWER) }
    var customTitle by remember { mutableStateOf("") }
    var customDesc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.observe() }

    val ui by vm.uiState.collectAsState()
    val startState by vm.startState.collectAsState()
    val createState by vm.createState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * ✅ MISMO ENFOQUE QUE CHALLENGES:
     * ids de templates ya aceptados / en progreso vienen de Firestore (ui.library)
     */
    val startedTemplateIds: Set<String> = remember(ui.library) {
        ui.library.mapNotNull { it.getTemplateIdSafe() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    LaunchedEffect(startState.startError) {
        val err = startState.startError ?: return@LaunchedEffect

        if (err == "already_started") {
            tab = MissionsTab.LIBRARY
            step = QuestStep.NONE
        }

        val msg = if (err == "already_started") "Esta misión ya está en progreso" else err
        snackbarHostState.showSnackbar(message = msg)
        vm.clearStartState()
    }

    // ✅ Cuando se inicia una misión template, la mandamos a "Mi biblioteca"
    LaunchedEffect(startState.startedInstanceId) {
        startState.startedInstanceId ?: return@LaunchedEffect
        tab = MissionsTab.LIBRARY
        step = QuestStep.NONE
        vm.clearStartState()
    }

    // ✅ Cuando se crea una misión custom, la mandamos a "Mi biblioteca"
    LaunchedEffect(createState.createdId) {
        createState.createdId ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = "Misión creada")
        tab = MissionsTab.LIBRARY
        step = QuestStep.NONE
        vm.clearCreateState()
    }

    LaunchedEffect(createState.error) {
        val err = createState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = err)
        vm.clearCreateState()
    }

    /**
     * ✅ DESCUBRIR:
     * Ocultamos templates que ya están en library (persistente)
     */
    val filteredDiscover: List<MissionTemplate> = remember(ui.discover, startedTemplateIds, query) {
        val q = query.trim().lowercase()

        val base = ui.discover.filter { it.id !in startedTemplateIds }

        if (q.isBlank()) base
        else base.filter { m ->
            m.title.lowercase().contains(q) ||
                    m.subtitle.lowercase().contains(q) ||
                    m.description.lowercase().contains(q) ||
                    m.tags.any { it.lowercase().contains(q) }
        }
    }

    val filteredLibrary: List<UserMission> = remember(ui.library, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) ui.library
        else ui.library.filter { m ->
            m.title.lowercase().contains(q) ||
                    m.subtitle.lowercase().contains(q) ||
                    m.description.lowercase().contains(q) ||
                    m.tags.any { it.lowercase().contains(q) }
        }
    }

    val total = if (tab == MissionsTab.DISCOVER) filteredDiscover.size else filteredLibrary.size

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Misiones", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* menu */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = textPrimary)
                    }
                }
            )
        },
        // ✅ Ocultar el botón "Crear misión" cuando hay modal/flow abierto
        bottomBar = {
            if (step == QuestStep.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg.copy(alpha = 0.92f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            selectedTemplate = null
                            durationDays = 14
                            difficulty = difficultyFromDuration(14)
                            focus = Focus.LOWER
                            customTitle = ""
                            customDesc = ""
                            step = QuestStep.DURATION
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Crear misión", color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.ExtraBold)
                    }
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
            item {
                MissionsTabs(
                    selected = tab,
                    onSelected = { tab = it },
                    accent = accent,
                    surface = surface,
                    textSecondary = textSecondary
                )
            }

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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tab == MissionsTab.DISCOVER) "Descubrir" else "Mi biblioteca",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Total: $total",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (tab == MissionsTab.DISCOVER) {
                if (ui.loadingDiscover) {
                    item { InlineInfoCard(text = "Cargando misiones…", surface = surface, accent = accent, textSecondary = textSecondary) }
                }
                ui.discoverError?.let { msg ->
                    item { InlineErrorCard(text = msg, surface = surface) }
                }

                items(filteredDiscover, key = { it.id }) { mission ->
                    MissionTemplateCardFixed(
                        mission = mission,
                        accent = accent,
                        surface = surface,
                        input = input,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            selectedTemplate = mission
                            step = QuestStep.OVERVIEW_TEMPLATE
                        }
                    )
                }

                if (!ui.loadingDiscover && ui.discoverError == null && filteredDiscover.isEmpty()) {
                    item { EmptyState(accent = accent, surface = surface, textPrimary = textPrimary, textSecondary = textSecondary) }
                }
            } else {
                if (ui.loadingLibrary) {
                    item { InlineInfoCard(text = "Cargando Mi biblioteca…", surface = surface, accent = accent, textSecondary = textSecondary) }
                }
                ui.libraryError?.let { msg ->
                    item { InlineErrorCard(text = msg, surface = surface) }
                }

                items(filteredLibrary, key = { it.id }) { mission ->
                    UserMissionCard(
                        mission = mission,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                if (!ui.loadingLibrary && ui.libraryError == null && filteredLibrary.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Todavía no aceptaste misiones", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Elegí una en Descubrir o creá una misión.",
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---------- MODALS ----------
        if (step != QuestStep.NONE) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            when (step) {

                // ✅ Duración -> Siguiente -> FOCUS (salta dificultad)
                QuestStep.DURATION -> QuestModalShellV2(
                    title = "Crear misión",
                    subtitle = "Elegí la duración (14 a 28 días)",
                    icon = Icons.Filled.EmojiEvents,
                    accent = accent,
                    surface = surface,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    imageUrl = null,
                    onClose = { step = QuestStep.NONE }
                ) {
                    TwoUpRow(gap = 12.dp) { cardW ->
                        SelectCardV2(
                            modifier = Modifier.width(cardW).height(118.dp),
                            title = "14 DÍAS",
                            subtitle = "Misión de 2 semanas",
                            selected = durationDays == 14,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Star,
                            onClick = {
                                durationDays = 14
                                difficulty = difficultyFromDuration(14)
                            }
                        )
                        SelectCardV2(
                            modifier = Modifier.width(cardW).height(118.dp),
                            title = "21 DÍAS",
                            subtitle = "Misión de 3 semanas",
                            selected = durationDays == 21,
                            accent = accent,
                            surface = input,
                            icon = Icons.Filled.Star,
                            onClick = {
                                durationDays = 21
                                difficulty = difficultyFromDuration(21)
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // ✅ 28 centrado
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val cardW = (maxWidth - 12.dp) / 2
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            SelectCardV2(
                                modifier = Modifier.width(cardW).height(118.dp),
                                title = "28 DÍAS",
                                subtitle = "Misión de 4 semanas",
                                selected = durationDays == 28,
                                accent = accent,
                                surface = input,
                                icon = Icons.Filled.Star,
                                onClick = {
                                    durationDays = 28
                                    difficulty = difficultyFromDuration(28)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    PrimaryCTAGlow(
                        accent = accent,
                        text = "Siguiente",
                        onClick = {
                            difficulty = difficultyFromDuration(durationDays)
                            step = QuestStep.FOCUS
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryCTA(accent = accent, textPrimary = textPrimary, text = "Cancelar", onClick = { step = QuestStep.NONE })
                }

                QuestStep.FOCUS -> {
                    QuestModalShellV2(
                        title = "Elegir enfoque",
                        subtitle = "¿Qué querés priorizar?",
                        icon = Icons.Filled.EmojiEvents,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        imageUrl = null,
                        onClose = { step = QuestStep.NONE }
                    ) {
                        TwoUpRow(gap = 12.dp) { cardW ->
                            SelectCardV2(
                                modifier = Modifier.width(cardW).height(118.dp),
                                title = "TREN SUPERIOR",
                                subtitle = "Brazos, pecho, espalda",
                                selected = focus == Focus.UPPER,
                                accent = accent,
                                surface = input,
                                icon = Icons.Filled.EmojiEvents,
                                onClick = { focus = Focus.UPPER }
                            )
                            SelectCardV2(
                                modifier = Modifier.width(cardW).height(118.dp),
                                title = "TREN INFERIOR",
                                subtitle = "Piernas y glúteos",
                                selected = focus == Focus.LOWER,
                                accent = accent,
                                surface = input,
                                icon = Icons.Filled.EmojiEvents,
                                onClick = { focus = Focus.LOWER }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        TwoUpRow(gap = 12.dp) { cardW ->
                            SelectCardV2(
                                modifier = Modifier.width(cardW).height(118.dp),
                                title = "CARDIO",
                                subtitle = "Corazón y resistencia",
                                selected = focus == Focus.CARDIO,
                                accent = accent,
                                surface = input,
                                icon = Icons.Filled.EmojiEvents,
                                onClick = { focus = Focus.CARDIO }
                            )
                            SelectCardV2(
                                modifier = Modifier.width(cardW).height(118.dp),
                                title = "MOVILIDAD",
                                subtitle = "Flexibilidad y movilidad",
                                selected = focus == Focus.ABS,
                                accent = accent,
                                surface = input,
                                icon = Icons.Filled.EmojiEvents,
                                onClick = { focus = Focus.ABS }
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        PrimaryCTAGlow(accent = accent, text = "Siguiente", onClick = { step = QuestStep.DETAILS })
                        Spacer(Modifier.height(10.dp))
                        SecondaryCTA(accent = accent, textPrimary = textPrimary, text = "Volver", onClick = { step = QuestStep.DURATION })
                    }
                }

                QuestStep.DETAILS -> {
                    val enabled = customTitle.trim().isNotBlank() && customDesc.trim().isNotBlank()
                    val keyboard = LocalSoftwareKeyboardController.current
                    val focusManager = LocalFocusManager.current

                    QuestModalShellV2(
                        title = "Detalles",
                        subtitle = "Poné un nombre y descripción",
                        icon = Icons.Filled.EmojiEvents,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        imageUrl = null,
                        onClose = { step = QuestStep.NONE }
                    ) {
                        Text("Nombre de la misión", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        TextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            placeholder = { Text("Ej: Cinta una hora", color = textSecondary.copy(alpha = 0.65f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = input,
                                unfocusedContainerColor = input,
                                disabledContainerColor = input,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Descripción", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        TextField(
                            value = customDesc,
                            onValueChange = { customDesc = it },
                            placeholder = { Text("Ej: Correr en la cinta una hora", color = textSecondary.copy(alpha = 0.65f)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = input,
                                unfocusedContainerColor = input,
                                disabledContainerColor = input,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("Completá nombre y descripción para continuar.", color = textSecondary, fontSize = 12.sp)

                        Spacer(Modifier.height(16.dp))
                        PrimaryCTAGlow(
                            accent = if (enabled) accent else accent.copy(alpha = 0.35f),
                            text = "Siguiente",
                            onClick = { if (enabled) step = QuestStep.SUMMARY }
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryCTA(accent = accent, textPrimary = textPrimary, text = "Volver", onClick = { step = QuestStep.FOCUS })
                    }
                }

                QuestStep.SUMMARY -> {
                    val autoDiff = difficultyFromDuration(durationDays)
                    val rewardPts = rewardPointsFromDuration(durationDays)

                    val diffLabel = when (autoDiff) {
                        Difficulty.EASY -> "Fácil"
                        Difficulty.MEDIUM -> "Media"
                        Difficulty.HARD -> "Difícil"
                        Difficulty.INSANE -> "Extrema"
                    }
                    val focusLabel = when (focus) {
                        Focus.UPPER -> "Superior"
                        Focus.LOWER -> "Inferior"
                        Focus.CARDIO -> "Cardio"
                        Focus.ABS -> "Movilidad"
                    }

                    val objectiveWorkouts = objectiveFrom(durationDays, autoDiff)

                    QuestModalShellV2(
                        title = "Resumen",
                        subtitle = "Confirmá tu misión",
                        icon = Icons.Filled.EmojiEvents,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        imageUrl = null,
                        onClose = { step = QuestStep.NONE }
                    ) {
                        Text(
                            text = customTitle.trim().ifBlank { "Misión" },
                            color = textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (customDesc.trim().isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(customDesc.trim(), color = textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.height(14.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            PillV2("${durationDays} DÍAS", accent)
                            PillV2(diffLabel, accent)
                            PillV2(focusLabel, accent)
                        }

                        Spacer(Modifier.height(12.dp))

                        TwoUpRow(gap = 12.dp) { cardW ->
                            MiniStat(
                                modifier = Modifier.width(cardW),
                                label = "Objetivo",
                                value = "$objectiveWorkouts entrenos",
                                surface = input,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent
                            )
                            MiniStat(
                                modifier = Modifier.width(cardW),
                                label = "Recompensa",
                                value = "$rewardPts pts",
                                surface = input,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        PrimaryCTAGlow(
                            accent = accent,
                            text = if (createState.creating) "CREANDO..." else "CREAR MISIÓN",
                            onClick = {
                                if (createState.creating) return@PrimaryCTAGlow

                                val finalDifficulty = difficultyFromDuration(durationDays)
                                val finalPoints = rewardPointsFromDuration(durationDays)

                                vm.createCustomMission(
                                    title = customTitle.trim(),
                                    description = customDesc.trim(),
                                    durationDays = durationDays,
                                    difficulty = finalDifficulty,
                                    focus = focus,
                                    points = finalPoints,
                                    objectiveWorkouts = objectiveWorkouts
                                )
                                step = QuestStep.NONE
                            }
                        )

                        Spacer(Modifier.height(10.dp))
                        SecondaryCTA(accent = accent, textPrimary = textPrimary, text = "Volver", onClick = { step = QuestStep.DETAILS })
                    }
                }

                QuestStep.OVERVIEW_TEMPLATE -> {
                    val t = selectedTemplate
                    val alreadyStarted = t?.id != null && t.id in startedTemplateIds

                    QuestModalShellV2(
                        title = t?.title ?: "Misión",
                        subtitle = t?.subtitle ?: "Detalle",
                        icon = Icons.Filled.EmojiEvents,
                        accent = accent,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        imageUrl = t?.imageUrl,
                        onClose = { step = QuestStep.NONE }
                    ) {
                        if (t == null) {
                            Text("No se pudo cargar la misión.", color = textSecondary)
                            return@QuestModalShellV2
                        }

                        val duration = formatDays(t.durationDays)
                        val level = t.level.ifBlank { "—" }
                        val points = if (t.points > 0) "${t.points} pts" else "—"
                        val mainTag = t.tags.firstOrNull()?.let { prettifyTag(it) } ?: ""

                        Text(
                            t.title,
                            color = textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (t.subtitle.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(t.subtitle, color = textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.height(14.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            PillV2(duration, accent)
                            PillV2(level, accent)
                            if (mainTag.isNotBlank()) PillV2(mainTag, accent)
                            if (t.points > 0) PillV2(points, accent)
                        }

                        Spacer(Modifier.height(16.dp))

                        // ✅ MISMO COMPORTAMIENTO QUE CHALLENGES:
                        // si ya está en library -> botón desactivado
                        PrimaryCTAGlow(
                            accent = if (alreadyStarted) accent.copy(alpha = 0.35f) else accent,
                            text = if (alreadyStarted) "YA EN TU BIBLIOTECA" else "EMPEZAR",
                            onClick = {
                                if (alreadyStarted) return@PrimaryCTAGlow
                                vm.startTemplateMission(t)
                                step = QuestStep.NONE
                            }
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

/* ---------------- UI bits ---------------- */

@Composable
private fun MissionsTabs(
    selected: MissionsTab,
    onSelected: (MissionsTab) -> Unit,
    accent: Color,
    surface: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surface.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegTab(
                text = "Descubrir",
                selected = selected == MissionsTab.DISCOVER,
                onClick = { onSelected(MissionsTab.DISCOVER) },
                accent = accent,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
            SegTab(
                text = "Mi biblioteca",
                selected = selected == MissionsTab.LIBRARY,
                onClick = { onSelected(MissionsTab.LIBRARY) },
                accent = accent,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent
    val border = if (selected) accent.copy(alpha = 0.45f) else Color.Transparent
    val tint = if (selected) Color.White else textSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = text, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
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
                placeholder = { Text("Buscar misiones", color = textSecondary) },
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

@Composable
private fun InlineInfoCard(text: String, surface: Color, accent: Color, textSecondary: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = textSecondary, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
    }
}

@Composable
private fun InlineErrorCard(text: String, surface: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surface,
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = Color.Red.copy(alpha = 0.85f), modifier = Modifier.padding(14.dp), fontSize = 13.sp)
    }
}

@Composable
private fun EmptyState(accent: Color, surface: Color, textPrimary: Color, textSecondary: Color) {
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
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.12f), surface)))
                .padding(1.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = accent, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Sin resultados", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, textAlign = TextAlign.Center)
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

/** Card template */
@Composable
private fun MissionTemplateCardFixed(
    mission: MissionTemplate,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val durationChip = formatDays(mission.durationDays)
    val levelChip = mission.level.ifBlank { "Nivel" }
    val tagChip = mission.tags.firstOrNull()?.let { prettifyTag(it) }.orEmpty()

    val subtitle = when {
        mission.subtitle.isNotBlank() -> mission.subtitle
        mission.description.isNotBlank() -> mission.description
        else -> ""
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mission.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.10f)))
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = mission.title,
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        TinyPill(levelChip, accent)
                        TinyPill(durationChip, accent)
                        if (mission.points > 0) TinyPill("${mission.points} pts", accent)
                        if (tagChip.isNotBlank()) TinyPill(tagChip, accent)
                    }
                }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Ver", color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Puntos",
                    value = if (mission.points > 0) "+${mission.points} pts" else "—",
                    surface = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Duración",
                    value = durationChip,
                    surface = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
            }
        }
    }
}

/** Card user mission */
@Composable
private fun UserMissionCard(
    mission: UserMission,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val durationChip = formatDays(mission.durationDays)
    val levelChip = mission.level.ifBlank { "Nivel" }
    val pointsChip = if (mission.points > 0) "${mission.points} pts" else ""
    val tagChip = mission.tags.firstOrNull()?.let { prettifyTag(it) }.orEmpty()

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = accent)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    mission.title,
                    color = textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (mission.subtitle.isNotBlank()) {
                    Text(
                        mission.subtitle,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    TinyPill(levelChip, accent)
                    TinyPill(durationChip, accent)
                    if (pointsChip.isNotBlank()) TinyPill(pointsChip, accent)
                    if (tagChip.isNotBlank()) TinyPill(tagChip, accent)
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/* ---------- Modal shell + cards ---------- */

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

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
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
                                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))
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
                            Text(title, color = textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(subtitle, color = textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun TwoUpRow(
    gap: Dp,
    content: @Composable RowScope.(cardWidth: Dp) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardW: Dp = (maxWidth - gap) / 2
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) { content(cardW) }
    }
}

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

                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryCTAGlow(accent: Color, text: String, onClick: () -> Unit) {
    val glow = Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent))

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
            Text(text, color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SecondaryCTA(accent: Color, textPrimary: Color, text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
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
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/* -------- helpers -------- */

private fun UserMission.getTemplateIdSafe(): String? {
    // Busca en propiedades/campos comunes sin depender del modelo en compile-time
    return readStringFieldOrGetter("templateId")
        ?: readStringFieldOrGetter("missionTemplateId")
        ?: readStringFieldOrGetter("baseTemplateId")
        ?: readStringFieldOrGetter("template_id")
        ?: readStringFieldOrGetter("template")
}

private fun Any.readStringFieldOrGetter(name: String): String? {
    // 1) Intentar getter: getTemplateId(), getMissionTemplateId(), etc.
    val getterName = "get" + name.replaceFirstChar { it.uppercase() }
    runCatching {
        val m = this.javaClass.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
        val v = m?.invoke(this)
        if (v is String && v.isNotBlank()) return v
    }

    // 2) Intentar field directo: templateId, missionTemplateId, etc.
    runCatching {
        val f = this.javaClass.declaredFields.firstOrNull { it.name == name }
        f?.isAccessible = true
        val v = f?.get(this)
        if (v is String && v.isNotBlank()) return v
    }

    return null
}

private fun formatDays(days: Int): String = if (days > 0) "$days días" else "—"

private fun prettifyTag(raw: String): String {
    val clean = raw.trim().lowercase()
    val mapped = when (clean) {
        "consistency" -> "Constancia"
        "workout" -> "Entrenos"
        "recovery" -> "Recuperación"
        "mobility" -> "Movilidad"
        "strength" -> "Fuerza"
        else -> clean.replaceFirstChar { it.uppercase() }
    }
    return if (mapped.length <= 14) mapped else mapped.take(12) + "…"
}

private fun difficultyFromDuration(days: Int): Difficulty = when (days) {
    14 -> Difficulty.EASY
    21 -> Difficulty.MEDIUM
    else -> Difficulty.HARD // 28
}

private fun rewardPointsFromDuration(days: Int): Int = when (days) {
    14 -> 250
    21 -> 350
    else -> 500 // 28
}

private fun objectiveFrom(durationDays: Int, difficulty: Difficulty): Int {
    return when (durationDays) {
        14 -> when (difficulty) {
            Difficulty.EASY -> 6
            Difficulty.MEDIUM -> 8
            Difficulty.HARD -> 10
            Difficulty.INSANE -> 12
        }
        21 -> when (difficulty) {
            Difficulty.EASY -> 9
            Difficulty.MEDIUM -> 12
            Difficulty.HARD -> 15
            Difficulty.INSANE -> 18
        }
        else -> when (difficulty) { // 28
            Difficulty.EASY -> 12
            Difficulty.MEDIUM -> 16
            Difficulty.HARD -> 20
            Difficulty.INSANE -> 24
        }
    }
}