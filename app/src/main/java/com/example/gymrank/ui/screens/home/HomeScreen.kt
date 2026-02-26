@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.gymrank.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.R
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.ui.components.BodyWithMuscleMasks
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.components.MuscleId
import com.example.gymrank.ui.session.SessionViewModel
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset



// ============================================
// Screen
// ============================================

private enum class FeedVisibility { PUBLIC, FRIENDS, PRIVATE }

private fun FeedVisibility.toFirestore(): String = name

private fun String?.toFeedVisibility(): FeedVisibility = when (this?.trim()?.uppercase()) {
    "FRIENDS" -> FeedVisibility.FRIENDS
    "PRIVATE" -> FeedVisibility.PRIVATE
    else -> FeedVisibility.PUBLIC
}

private fun FeedVisibility.labelEs(): String = when (this) {
    FeedVisibility.PUBLIC -> "Público"
    FeedVisibility.FRIENDS -> "Solo amigos"
    FeedVisibility.PRIVATE -> "Privado"
}

private fun FeedVisibility.subtitleEs(): String = when (this) {
    FeedVisibility.PUBLIC -> "Cualquiera puede ver tus entrenamientos en el feed público."
    FeedVisibility.FRIENDS -> "Solo tus amigos pueden ver tus entrenamientos."
    FeedVisibility.PRIVATE -> "Nadie más puede ver tus entrenamientos."
}

@Composable
fun HomeScreen(
    sessionViewModel: SessionViewModel,
    viewModel: HomeViewModel = viewModel(),
    onLogWorkout: () -> Unit = {},
    onOpenRanking: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val selectedGym by sessionViewModel.selectedGym.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val db = remember { FirebaseFirestore.getInstance() }

    // ============================
    // ✅ Feed visibility (perfil)
    // ============================
    val userRepo = remember { com.example.gymrank.data.repository.UserRepositoryImpl() }
    val scope = rememberCoroutineScope()

    var feedVisibility by remember { mutableStateOf(FeedVisibility.PUBLIC) }
    var isFeedVisibilityLoading by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    val privacySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(selectedGym) {
        selectedGym?.let { viewModel.setGymData(it) }
    }

    // ✅ SETS POR MUSCULO (semana) -> entrenamientos reales
    val workoutRepo = remember { WorkoutRepositoryFirestoreImpl() }
    val allWorkouts by remember { workoutRepo.getWorkouts() }.collectAsState(initial = emptyList())
    val weekRange = remember { currentWeekRangeMillis() }

    val weekWorkouts = remember(allWorkouts, weekRange) {
        allWorkouts.filter { w ->
            val ts = w.timestampMillis ?: w.createdAt ?: 0L
            ts in weekRange.first until weekRange.second
        }
    }

    val setsByMuscleItems = remember(weekWorkouts) {
        buildSetsByMuscleItemsThisWeek(weekWorkouts)
    }

    // ============================================
    // ✅ Calendar selection (default = today)
    // ============================================

    val dayLabelsShort = remember { listOf("L", "M", "M", "J", "V", "S", "D") }
    val dayLabelsLong = remember {
        listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    }
    val dayKeys = remember { listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun") }

    val todayIndex = remember { todayIndexMondayFirst() } // 0..6
    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }

    // ============================================
    // ✅ Routine Plan - Firestore (stored per day)
    // ============================================


    var profileName by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect

        runCatching {
            val snap = db.collection("users").document(uid).get().await()
            profileName = snap.getString("name")
                ?: snap.getString("username")
                        ?: ""

            // ✅ leer visibilidad del perfil
            feedVisibility = snap.getString("feedVisibility").toFeedVisibility()
        }
    }

    // Cache local del plan semanal: dayKey -> muscles
    var weekPlan by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isPlanLoading by remember { mutableStateOf(false) }
    var planError by remember { mutableStateOf<String?>(null) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    // ✅ Auto-ocultar mensaje de éxito luego de un tiempo
    LaunchedEffect(saveStatus) {
        if (saveStatus != null && saveStatus!!.contains("✅")) {
            delay(2500)
            if (saveStatus != null && saveStatus!!.contains("✅")) {
                saveStatus = null
            }
        }
    }

    // UI modals
    var showPlanDaysModal by remember { mutableStateOf(false) }
    var showMusclePickerModal by remember { mutableStateOf(false) }
    var editingDayIndex by remember { mutableIntStateOf(0) }

    val allMuscleOptions = remember {
        listOf(
            "Pecho",
            "Espalda",
            "Femorales",
            "Hombros",
            "Bíceps",
            "Tríceps",
            "Abdomen",
            "Glúteos",
            "Cuadriceps",
            "Pantorrillas",
            "Trapecios",
            "Antebrazos"
        )
    }

    fun normalizeMuscles(list: List<String>): List<String> =
        list.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }

    fun loadWeekPlan() {
        planError = null
        saveStatus = null

        if (uid.isBlank()) {
            planError = "No hay usuario logueado."
            weekPlan = emptyMap()
            return
        }

        isPlanLoading = true
        db.collection("users")
            .document(uid)
            .collection("routinePlan")
            .get()
            .addOnSuccessListener { qs ->
                val map = mutableMapOf<String, List<String>>()
                qs.documents.forEach { doc ->
                    val key = doc.id
                    val muscles =
                        (doc.get("muscles") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    map[key] = normalizeMuscles(muscles)
                }
                weekPlan = map
                isPlanLoading = false
            }
            .addOnFailureListener { e ->
                isPlanLoading = false
                weekPlan = emptyMap()
                planError = e.message ?: "Error cargando plan"
            }
    }

    fun saveDayPlan(dayKey: String, muscles: List<String>) {
        planError = null
        saveStatus = null

        if (uid.isBlank()) {
            saveStatus = "No hay usuario logueado."
            return
        }

        val normalized = normalizeMuscles(muscles)
        val payload = hashMapOf(
            "muscles" to normalized,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("routinePlan")
            .document(dayKey)
            .set(payload)
            .addOnSuccessListener {
                weekPlan = weekPlan.toMutableMap().apply { put(dayKey, normalized) }
                saveStatus = "✅ Plan guardado!"
            }
            .addOnFailureListener { e ->
                saveStatus = e.message ?: "Error guardando plan"
            }
    }

    // Cargar plan al entrar / cuando cambia el usuario
    LaunchedEffect(uid) {
        loadWeekPlan()
    }

    val selectedDayKey = dayKeys.getOrNull(selectedDayIndex) ?: "mon"
    val selectedDayMuscles = weekPlan[selectedDayKey].orEmpty()

    // ✅ label dinámico del botón del plan (si hay algo cargado en cualquier día -> Editar)
    val hasAnyPlan = remember(weekPlan) { weekPlan.values.any { it.isNotEmpty() } }
    val planButtonLabel = if (hasAnyPlan) "Editar plan" else "Cargar plan"

    // ============================================
    // ✅ BODY COUNTS: ahora vienen del plan del día seleccionado
    // ============================================

    val (resolvedFront, resolvedBack) = remember(selectedDayMuscles) {
        buildMuscleCountsFromRoutinePlan(selectedDayMuscles)
    }

    // ============================================
    // ✅ Calendar progress: workouts reales de la semana
    // ============================================

    val completedThisWeek = remember(todayIndex) {
        (todayIndex + 1).coerceIn(0, 7)
    }

    // ============================================
    // UI MODALS
    // ============================================

    // Modal 1: Lista de días
    if (showPlanDaysModal) {
        AlertDialog(
            onDismissRequest = { showPlanDaysModal = false },
            title = { Text(planButtonLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dayLabelsLong.forEachIndexed { idx, label ->
                        val key = dayKeys[idx]
                        val count = weekPlan[key]?.size ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesignTokens.Colors.SurfaceElevated)
                                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(12.dp))
                                .clickable {
                                    editingDayIndex = idx
                                    showPlanDaysModal = false
                                    showMusclePickerModal = true
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = DesignTokens.Colors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (count == 0) "—" else count.toString(),
                                color = DesignTokens.Colors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlanDaysModal = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal 2: Selector de músculos para un día
    if (showMusclePickerModal) {
        val editKey = dayKeys.getOrNull(editingDayIndex) ?: "mon"
        val editDayLabel = dayLabelsLong.getOrNull(editingDayIndex) ?: "Día"

        var tempSelected by remember(editKey, weekPlan) {
            mutableStateOf(weekPlan[editKey].orEmpty().toSet())
        }

        AlertDialog(
            onDismissRequest = { showMusclePickerModal = false },
            title = { Text(editDayLabel) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Elegí los músculos para este día",
                        fontSize = 12.sp,
                        color = DesignTokens.Colors.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))

                    allMuscleOptions.forEach { m ->
                        val checked = tempSelected.contains(m)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DesignTokens.Colors.SurfaceElevated)
                                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(12.dp))
                                .clickable {
                                    tempSelected = if (checked) tempSelected - m else tempSelected + m
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = m,
                                modifier = Modifier.weight(1f),
                                color = DesignTokens.Colors.TextPrimary
                            )
                            if (checked) {
                                Text("✓", color = GymRankColors.PrimaryAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveDayPlan(editKey, tempSelected.toList())
                        showMusclePickerModal = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showMusclePickerModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ============================
    // ✅ BottomSheet: Privacidad del perfil
    // ============================
    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            sheetState = privacySheetState,
            containerColor = DesignTokens.Colors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Privacidad del perfil", fontWeight = FontWeight.Bold)

                Text(
                    "Esto define quién puede ver tus entrenamientos en el feed.",
                    color = DesignTokens.Colors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(6.dp))

                fun pick(v: FeedVisibility) {
                    if (uid.isBlank()) return
                    isFeedVisibilityLoading = true

                    scope.launch {
                        runCatching {
                            userRepo.updateMyFeedVisibility(v.toFirestore())
                        }.onSuccess {
                            feedVisibility = v
                            showPrivacySheet = false
                        }.onFailure {
                            // Si querés, podés mostrar esto en UI. Por ahora lo dejamos simple.
                        }
                        isFeedVisibilityLoading = false
                    }
                }

                PrivacyOptionRow(
                    selected = feedVisibility == FeedVisibility.PUBLIC,
                    title = "Público",
                    subtitle = FeedVisibility.PUBLIC.subtitleEs(),
                    onClick = { pick(FeedVisibility.PUBLIC) }
                )

                PrivacyOptionRow(
                    selected = feedVisibility == FeedVisibility.FRIENDS,
                    title = "Solo amigos",
                    subtitle = FeedVisibility.FRIENDS.subtitleEs(),
                    onClick = { pick(FeedVisibility.FRIENDS) }
                )

                PrivacyOptionRow(
                    selected = feedVisibility == FeedVisibility.PRIVATE,
                    title = "Privado",
                    subtitle = FeedVisibility.PRIVATE.subtitleEs(),
                    onClick = { pick(FeedVisibility.PRIVATE) }
                )

                if (isFeedVisibilityLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = GymRankColors.PrimaryAccent,
                        trackColor = DesignTokens.Colors.SurfaceInputs
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // ============================================
    // MAIN LIST
    // ============================================

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeTopBar(
                userName = if (profileName.isNotBlank()) profileName else uiState.userName,
                onOpenRanking = onOpenRanking,
                onLogout = onLogout,
                feedVisibilityLabel = feedVisibility.labelEs(),
                onOpenPrivacy = { showPrivacySheet = true }
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
            MusclesTrainedThisWeekCardPro(
                frontCounts = resolvedFront,
                backCounts = resolvedBack,
                modifier = Modifier.padding(horizontal = 16.dp),
                onSettingsClick = { /* TODO */ }
            )
        }

        // ✅ Calendar
        item {
            WorkoutCalendarCard(
                modifier = Modifier.padding(horizontal = 14.dp),
                weeklyGoal = 7,
                completed = completedThisWeek,
                dayLabelsShort = dayLabelsShort,
                todayIndex = todayIndex,
                selectedDayIndex = selectedDayIndex,
                onSelectDay = { selectedDayIndex = it }
            )
        }

        // ✅ Mi rutina (en feed)
        item {
            RoutineSummaryCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedDayIndex = selectedDayIndex,
                dayLabelsLong = dayLabelsLong,
                musclesForSelectedDay = selectedDayMuscles,
                isLoading = isPlanLoading,
                error = planError,
                saveStatus = saveStatus,
                planButtonLabel = planButtonLabel,
                onOpenPlan = { showPlanDaysModal = true }
            )
        }

        item {
            SetsByMuscleCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                items = setsByMuscleItems
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PrivacyOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141A16),
        border = BorderStroke(
            1.dp,
            if (selected) GymRankColors.PrimaryAccent.copy(alpha = 0.6f)
            else GymRankColors.PrimaryAccent.copy(alpha = 0.15f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    color = DesignTokens.Colors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selected) {
                Text("✓", color = GymRankColors.PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================
// TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    userName: String,
    onOpenRanking: () -> Unit,
    onLogout: () -> Unit,
    feedVisibilityLabel: String,
    onOpenPrivacy: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Querés cerrar sesión ahora?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) { Text("Sí, cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    TopAppBar(
        title = {
            // ✅ Esto evita que se corten los textos de la izquierda
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f) // ✅ el título se queda con el ancho posible
                ) {
                    Text(
                        text = "Hola 👋",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (userName.isNotBlank()) userName else " ",
                        fontSize = 12.sp,
                        color = DesignTokens.Colors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

            Spacer(Modifier.width(8.dp))

            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.SurfaceElevated)
                        .border(1.dp, DesignTokens.Colors.SurfaceInputs, CircleShape)
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        tint = GymRankColors.PrimaryAccent
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    offset = DpOffset(x = (-8).dp, y = 10.dp),
                    modifier = Modifier
                        .widthIn(min = 190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DesignTokens.Colors.SurfaceElevated)
                        .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Perfil") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = GymRankColors.PrimaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenPrivacy()
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = DesignTokens.Colors.TextPrimary,
                            leadingIconColor = GymRankColors.PrimaryAccent
                        )
                    )

                    HorizontalDivider(color = DesignTokens.Colors.SurfaceInputs)

                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = GymRankColors.PrimaryAccent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showLogoutDialog = true
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = DesignTokens.Colors.TextPrimary,
                            leadingIconColor = GymRankColors.PrimaryAccent
                        )
                    )
                }
            }

            Spacer(Modifier.width(10.dp))
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
                    text = "Basado en tu rutina configurada",
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
            text = "* Basado en el día seleccionado (plan semanal)",
            fontSize = 12.sp,
            color = DesignTokens.Colors.TextSecondary
        )
    }
}

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

        val isFront = title.lowercase(Locale.getDefault()).contains("frente")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DesignTokens.Colors.SurfaceElevated)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            BodyWithMuscleMasks(
                isFront = isFront,
                counts = counts,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )
        }
    }
}

// ============================================
// CALENDAR (SELECTABLE + TODAY)
// ============================================

@Composable
private fun WorkoutCalendarCard(
    modifier: Modifier = Modifier,
    weeklyGoal: Int,
    completed: Int,
    dayLabelsShort: List<String>,
    todayIndex: Int,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
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
            items(dayLabelsShort.size) { idx ->
                val isToday = idx == todayIndex
                val isSelected = idx == selectedDayIndex

                DayPillSelectable(
                    day = dayLabelsShort[idx],
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = { onSelectDay(idx) }
                )
            }
        }
    }
}

@Composable
private fun DayPillSelectable(
    day: String,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> GymRankColors.PrimaryAccent.copy(alpha = 0.18f)
        else -> DesignTokens.Colors.SurfaceElevated
    }

    val stroke = when {
        isSelected -> GymRankColors.PrimaryAccent.copy(alpha = 0.85f)
        isToday -> GymRankColors.PrimaryAccent.copy(alpha = 0.45f)
        else -> DesignTokens.Colors.SurfaceInputs
    }

    val txt = when {
        isSelected -> GymRankColors.PrimaryAccent
        else -> DesignTokens.Colors.TextPrimary
    }

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day, color = txt, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// ✅ Mi Rutina card (feed) - CHIPS COMO 2DA IMAGEN
// ============================================

@Composable
private fun RoutineSummaryCard(
    modifier: Modifier = Modifier,
    selectedDayIndex: Int,
    dayLabelsLong: List<String>,
    musclesForSelectedDay: List<String>,
    isLoading: Boolean,
    error: String?,
    saveStatus: String?,
    planButtonLabel: String,
    onOpenPlan: () -> Unit
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mi rutina",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Plan semanal de entrenamiento",
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }

            TextButton(onClick = onOpenPlan) {
                Text(
                    text = planButtonLabel,
                    color = GymRankColors.PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = GymRankColors.PrimaryAccent,
                trackColor = DesignTokens.Colors.SurfaceInputs
            )
            Spacer(Modifier.height(10.dp))
        }

        if (error != null) {
            Text(text = error, color = GymRankColors.Error, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        if (saveStatus != null) {
            Text(
                text = saveStatus,
                color = if (saveStatus.contains("✅")) GymRankColors.PrimaryAccent else GymRankColors.Error,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        val todayIdx = remember { todayIndexMondayFirst() }
        val selectedDayLabel = dayLabelsLong.getOrNull(selectedDayIndex).orEmpty()
        val isSelectedToday = selectedDayIndex == todayIdx

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSelectedToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(GymRankColors.PrimaryAccent.copy(alpha = 0.18f))
                        .border(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "HOY",
                        color = GymRankColors.PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = selectedDayLabel,
                color = DesignTokens.Colors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        if (musclesForSelectedDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DesignTokens.Colors.SurfaceElevated)
                    .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "No hay músculos cargados para este día. Tocá “${planButtonLabel}” para configurarlo.",
                    color = DesignTokens.Colors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        } else {
            RoutineChipsTwoColumns(items = musclesForSelectedDay)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineChipsTwoColumns(items: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2
    ) {
        items.forEach { label ->
            RoutineChip(
                label = label,
                modifier = Modifier
                    // "mitad" del ancho, con margen para el spacing
                    .fillMaxWidth(0.48f)
            )
        }
    }
}

@Composable
private fun RoutineChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DesignTokens.Colors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = GymRankColors.PrimaryAccent.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ícono como en la 2da imagen
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GymRankColors.PrimaryAccent.copy(alpha = 0.18f))
                .border(
                    1.dp,
                    GymRankColors.PrimaryAccent.copy(alpha = 0.35f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                tint = GymRankColors.PrimaryAccent,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = label,
            color = DesignTokens.Colors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ============================================
// SETS BY MUSCLE - VER MÁS / VER MENOS (EXPAND)
// ============================================

@Composable
private fun SetsByMuscleCard(
    modifier: Modifier = Modifier,
    items: List<Pair<String, Int>>
) {
    var expanded by remember { mutableStateOf(false) }

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

        val maxVal = (items.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

        if (!expanded) {
            // ✅ vista compacta (como tu 1ra imagen: 4 cards)
            val preview = remember(items) { items.take(4) }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(preview) { (name, value) ->
                    SetMiniCard(name = name, value = value, max = maxVal)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ver más",
                color = GymRankColors.PrimaryAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { expanded = true }
            )
        } else {
            // ✅ vista expandida (como 2da imagen: grilla 3 columnas)
            SetsGrid3Columns(items = items, maxVal = maxVal)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ver menos",
                color = GymRankColors.PrimaryAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { expanded = false }
            )
        }
    }
}

@Composable
private fun SetsGrid3Columns(
    items: List<Pair<String, Int>>,
    maxVal: Int
) {
    val rows = remember(items) { chunkToRows(items, maxPerRow = 3) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (name, value) ->
                    Box(modifier = Modifier.weight(1f)) {
                        SetGridCard(name = name, value = value, max = maxVal)
                    }
                }

                // si la última fila tiene menos de 3, completamos espacio para que quede alineado
                val missing = 3 - row.size
                repeat(missing) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SetGridCard(
    name: String,
    value: Int,
    max: Int
) {
    val pct = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(74.dp)
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
                        .width(52.dp)
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

// ============================================
// LEGEND
// ============================================

@Composable
private fun MuscleIntensityLegend(modifier: Modifier = Modifier) {
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
private fun LegendDot(label: String, color: Color) {
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
// HELPERS
// ============================================

private fun buildMuscleCountsFromRoutinePlan(
    muscles: List<String>
): Pair<Map<MuscleId, Int>, Map<MuscleId, Int>> {

    fun canonToMuscleId(name: String): MuscleId? = when (name.trim().lowercase(Locale.getDefault())) {
        // FRONT
        "pecho", "pectorales", "pectoral" -> MuscleId.Chest
        "abdomen", "abs", "core" -> MuscleId.Abs
        "oblicuos", "oblicuo", "obliques" -> MuscleId.Obliques
        "biceps", "bíceps", "bicep" -> MuscleId.Biceps
        "antebrazos", "antebrazo", "forearms", "forearm" -> MuscleId.Forearms
        "cuadriceps", "cuádriceps", "quads", "quadriceps", "piernas" -> MuscleId.Quads
        "pantorrillas", "pantorrilla", "gemelos", "calves", "calf" -> MuscleId.Calves

        // BOTH
        "hombros", "deltoides", "deltoide", "shoulders" -> MuscleId.Shoulders
        "trapecios", "trapecio", "traps", "trap" -> MuscleId.Traps

        // BACK
        "triceps", "tríceps", "tricep" -> MuscleId.Triceps
        "espalda", "back" -> MuscleId.Back
        "dorsales", "dorsal", "lats", "dorsal ancho" -> MuscleId.Lats
        "lumbar", "lumbares", "lower back", "espalda baja" -> MuscleId.LowerBack
        "gluteos", "glúteos", "glutes", "glute" -> MuscleId.Glutes
        "isquios", "isquiotibiales", "hamstrings", "femorales" -> MuscleId.Hamstrings

        else -> null
    }

    val front = mutableMapOf<MuscleId, Int>()
    val back = mutableMapOf<MuscleId, Int>()

    fun incFront(id: MuscleId) { front[id] = (front[id] ?: 0) + 1 }
    fun incBack(id: MuscleId) { back[id] = (back[id] ?: 0) + 1 }

    muscles.mapNotNull { canonToMuscleId(it) }
        .distinct()
        .forEach { id ->
            when (id) {
                MuscleId.Shoulders, MuscleId.Traps, MuscleId.Forearms, MuscleId.Calves -> {
                    incFront(id); incBack(id)
                }

                MuscleId.Chest, MuscleId.Abs, MuscleId.Obliques, MuscleId.Biceps, MuscleId.Quads -> incFront(id)

                MuscleId.Triceps, MuscleId.Lats, MuscleId.Back, MuscleId.LowerBack, MuscleId.Glutes, MuscleId.Hamstrings -> incBack(id)

                else -> incFront(id)
            }
        }

    return front to back
}

private fun currentWeekRangeMillis(): Pair<Long, Long> {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.firstDayOfWeek = Calendar.MONDAY

    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val diff = (dayOfWeek + 5) % 7
    cal.add(Calendar.DAY_OF_YEAR, -diff)

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val start = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 7)
    val end = cal.timeInMillis
    return start to end
}

private fun buildSetsByMuscleItemsThisWeek(workouts: List<Workout>): List<Pair<String, Int>> {
    val ordered = listOf(
        "Pecho",
        "Espalda",
        "Femorales",
        "Hombros",
        "Bíceps",
        "Tríceps",
        "Abdomen",
        "Glúteos",
        "Cuadriceps",
        "Pantorrillas",
        "Trapecios",
        "Antebrazos"
    )

    fun canon(name: String): String? = when (name.trim().lowercase(Locale.getDefault())) {
        "pecho", "pectorales", "pectoral" -> "Pecho"
        "espalda", "back", "dorsales", "dorsal", "lats" -> "Espalda"
        "hombros", "deltoides", "deltoide" -> "Hombros"
        "trapecios", "trapecio" -> "Trapecios"
        "bíceps", "biceps", "bicep" -> "Bíceps"
        "tríceps", "triceps", "tricep" -> "Tríceps"
        "antebrazos", "antebrazo" -> "Antebrazos"
        "abdomen", "abs", "core" -> "Abdomen"
        "glúteos", "gluteos", "glutes", "glute" -> "Glúteos"
        "cuádriceps", "cuadriceps", "quads", "quadriceps" -> "Cuadriceps"
        "pantorrillas", "pantorrilla", "gemelos", "calves", "calf" -> "Pantorrillas"
        "isquios", "isquiotibiales", "hamstrings", "femorales" -> "Femorales"
        else -> null
    }

    val acc = linkedMapOf<String, Int>().apply { ordered.forEach { put(it, 0) } }

    workouts.forEach { w ->
        val muscles = w.muscles.mapNotNull { canon(it) }.distinct()
        if (muscles.isEmpty()) return@forEach

        val totalSetsRaw = w.exercises.sumOf { max(0, it.sets) }
        val totalSets = if (totalSetsRaw > 0) totalSetsRaw else 1

        val base = totalSets / muscles.size
        var rem = totalSets % muscles.size

        muscles.forEach { m ->
            val add = base + if (rem > 0) { rem -= 1; 1 } else 0
            acc[m] = (acc[m] ?: 0) + add
        }
    }

    return ordered.map { it to (acc[it] ?: 0) }
}

private fun todayIndexMondayFirst(): Int {
    val cal = Calendar.getInstance(Locale.getDefault())
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    return when (dow) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }
}

// Opcional
@Composable
fun MuscleFront(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.muscle_front),
        contentDescription = "Músculos (frente)",
        modifier = modifier
    )
}

@Composable
fun MuscleBack(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.muscle_back),
        contentDescription = "Músculos (espalda)",
        modifier = modifier
    )
}

private fun countUniqueWorkoutDaysThisWeek(workouts: List<Workout>): Int {
    if (workouts.isEmpty()) return 0

    val uniqueDays = workouts.mapNotNull { w ->
        val ts = w.timestampMillis ?: w.createdAt
        ts?.let { startOfDayMillis(it) }
    }.toSet()

    return uniqueDays.size
}

private fun startOfDayMillis(epochMillis: Long): Long {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.timeInMillis = epochMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun <T> chunkToRows(items: List<T>, maxPerRow: Int): List<List<T>> {
    if (items.isEmpty()) return emptyList()
    val out = mutableListOf<List<T>>()
    var i = 0
    while (i < items.size) {
        out.add(items.subList(i, minOf(i + maxPerRow, items.size)))
        i += maxPerRow
    }
    return out
}