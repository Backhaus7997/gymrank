@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.gymrank.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.data.repository.UserRepositoryImpl
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.ui.components.BodyWithMuscleMasks
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.session.SessionViewModel
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

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
    onLogout: () -> Unit = {},
    onOpenFriendRequests: () -> Unit = {},
    onOpenProfile: () -> Unit = {}, // lo dejamos por compatibilidad
) {
    val selectedGym by sessionViewModel.selectedGym.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // ============================
    // ✅ Feed visibility (perfil)
    // ============================
    val userRepo = remember { UserRepositoryImpl() }

    var feedVisibility by remember { mutableStateOf(FeedVisibility.PUBLIC) }
    var isFeedVisibilityLoading by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    val privacySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ============================
    // ✅ Profile BottomSheet (como la 2da imagen)
    // ============================
    var showProfileSheet by remember { mutableStateOf(false) }
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Campos de perfil (Firestore)
    var profileUsername by remember { mutableStateOf("") }
    var profileFullName by remember { mutableStateOf("") }
    var profileExperience by remember { mutableStateOf("Intermedio") }
    var profileGender by remember { mutableStateOf("Otro") }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }

    // estados para upload foto
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var uploadPhotoError by remember { mutableStateOf<String?>(null) }

    // ✅ Launcher picker de imagen (Photo Picker) - mejor que GetContent y sin permisos
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null || uid.isBlank()) return@rememberLauncherForActivityResult

        scope.launch {
            isUploadingPhoto = true
            uploadPhotoError = null

            runCatching {
                val storageRef = FirebaseStorage.getInstance()
                    .reference
                    .child("users/$uid/profile_${System.currentTimeMillis()}.jpg")

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val payload = hashMapOf<String, Any?>(
                    "photoUrl" to downloadUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update(payload)
                    .await()

                profilePhotoUrl = downloadUrl
            }.onFailure { e ->
                uploadPhotoError = e.message ?: "Error subiendo la foto"
            }

            isUploadingPhoto = false
        }
    }

    LaunchedEffect(selectedGym) {
        selectedGym?.let { viewModel.setGymData(it) }
    }

    // ✅ Workouts (para sets por músculo de la semana)
    val workoutRepo = remember { WorkoutRepositoryFirestoreImpl() }
    val allWorkouts by remember { workoutRepo.getWorkouts() }.collectAsState(initial = emptyList())
    val weekRange = remember { currentWeekRangeMillis() }

    val weekWorkouts = remember(allWorkouts, weekRange) {
        allWorkouts.filter { w ->
            val ts = w.timestampMillis ?: w.createdAt ?: 0L
            ts in weekRange.first until weekRange.second
        }
    }

    val setsByMuscleItems = remember(weekWorkouts) { buildSetsByMuscleItemsThisWeek(weekWorkouts) }

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

            profileFullName = snap.getString("name") ?: ""
            profileUsername = snap.getString("username") ?: ""
            profileName = profileFullName.ifBlank { profileUsername.ifBlank { uiState.userName } }

            profileExperience = snap.getString("experience") ?: profileExperience
            profileGender = snap.getString("gender") ?: profileGender
            profilePhotoUrl = snap.getString("photoUrl")

            feedVisibility = snap.getString("feedVisibility").toFeedVisibility()
        }
    }

    var weekPlan by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isPlanLoading by remember { mutableStateOf(false) }
    var planError by remember { mutableStateOf<String?>(null) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveStatus) {
        if (saveStatus != null && saveStatus!!.contains("✅")) {
            delay(2500)
            if (saveStatus != null && saveStatus!!.contains("✅")) saveStatus = null
        }
    }

    var showPlanDaysModal by remember { mutableStateOf(false) }
    var showMusclePickerModal by remember { mutableStateOf(false) }
    var editingDayIndex by remember { mutableIntStateOf(0) }

    val allMuscleOptions = remember {
        listOf(
            "Pecho", "Espalda", "Femorales", "Hombros", "Bíceps", "Tríceps",
            "Abdomen", "Glúteos", "Cuadriceps", "Pantorrillas", "Trapecios", "Antebrazos"
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

    LaunchedEffect(uid) { loadWeekPlan() }

    val selectedDayKey = dayKeys.getOrNull(selectedDayIndex) ?: "mon"
    val selectedDayMuscles = weekPlan[selectedDayKey].orEmpty()

    val hasAnyPlan = remember(weekPlan) { weekPlan.values.any { it.isNotEmpty() } }
    val planButtonLabel = if (hasAnyPlan) "Editar plan" else "Cargar plan"

    val (resolvedFront, resolvedBack) = remember(selectedDayMuscles) {
        buildMuscleCountsFromRoutinePlan(selectedDayMuscles)
    }

    val completedThisWeek = remember(todayIndex) { (todayIndex + 1).coerceIn(0, 7) }

    // ============================================
    // UI MODALS (plan)
    // ============================================
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
            confirmButton = { TextButton(onClick = { showPlanDaysModal = false }) { Text("Cerrar") } }
        )
    }

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
            dismissButton = { TextButton(onClick = { showMusclePickerModal = false }) { Text("Cancelar") } }
        )
    }

    // ============================
    // ✅ BottomSheet: Privacidad
    // ============================
    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            sheetState = privacySheetState
        ) {
            Surface(color = DesignTokens.Colors.SurfaceElevated) {
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
                            runCatching { userRepo.updateMyFeedVisibility(v.toFirestore()) }
                                .onSuccess {
                                    feedVisibility = v
                                    showPrivacySheet = false
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
    }

    // ============================
    // ✅ BottomSheet: Perfil
    // ============================
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = profileSheetState
        ) {
            Surface(color = DesignTokens.Colors.SurfaceElevated) {
                ProfileSheetContent(
                    username = profileUsername.ifBlank { profileName.ifBlank { uiState.userName } },
                    experience = profileExperience,
                    gender = profileGender,
                    fullName = profileFullName,
                    photoUrl = profilePhotoUrl,
                    isUploadingPhoto = isUploadingPhoto,
                    uploadPhotoError = uploadPhotoError,
                    onClose = {
                        scope.launch {
                            profileSheetState.hide()
                            showProfileSheet = false
                        }
                    },
                    onPickPhoto = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSave = { newName, newExperience, newGender ->
                        if (uid.isBlank()) return@ProfileSheetContent
                        scope.launch {
                            runCatching {
                                db.collection("users").document(uid).update(
                                    mapOf(
                                        "name" to newName,
                                        "experience" to newExperience,
                                        "gender" to newGender,
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                ).await()
                            }.onSuccess {
                                profileFullName = newName
                                profileExperience = newExperience
                                profileGender = newGender
                                profileName = newName.ifBlank { profileUsername.ifBlank { uiState.userName } }
                            }
                        }
                    },
                    onOpenFriendRequests = {
                        // ✅ FIX parpadeo: primero cerramos sheet y luego navegamos
                        scope.launch {
                            profileSheetState.hide()
                            showProfileSheet = false
                            delay(80)
                            onOpenFriendRequests()
                        }
                    },
                    onLogoutConfirmed = {
                        // ✅ confirmación ya se hace dentro del sheet
                        scope.launch {
                            profileSheetState.hide()
                            showProfileSheet = false
                            delay(80)
                            onLogout()
                        }
                    }
                )
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
                onOpenProfile = { showProfileSheet = true }
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
// TOP BAR (abre Perfil)
// ============================================
@Composable
private fun HomeTopBar(
    userName: String,
    onOpenRanking: () -> Unit,
    onOpenProfile: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
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
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = GymRankColors.PrimaryAccent
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DesignTokens.Colors.BackgroundBase)
    )
}

// ============================================
// PERFIL SHEET CONTENT
// ============================================

@Composable
private fun ProfileSheetContent(
    username: String,
    experience: String,
    gender: String,
    fullName: String,
    photoUrl: String?,
    isUploadingPhoto: Boolean,
    uploadPhotoError: String?,
    onClose: () -> Unit,
    onPickPhoto: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onOpenFriendRequests: () -> Unit,
    onLogoutConfirmed: () -> Unit
) {
    var nameState by remember { mutableStateOf(fullName) }
    var experienceState by remember { mutableStateOf(experience) }
    var genderState by remember { mutableStateOf(gender) }

    var expExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val expOptions = listOf("Principiante", "Intermedio", "Avanzado")
    val genderOptions = listOf("Masculino", "Femenino", "Otro")

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que querés cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogoutConfirmed()
                    }
                ) { Text("Sí, cerrar") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)   // ✅ ahora scrollea en celular
            .navigationBarsPadding()       // ✅ evita quedar tapado por barra de abajo
            .padding(16.dp)
    ) {
        // Header: Perfil + X
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DesignTokens.Colors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = DesignTokens.Colors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Usuario + cambiar foto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DesignTokens.Colors.SurfaceElevated)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DesignTokens.Colors.SurfaceInputs),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = DesignTokens.Colors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username.ifBlank { "—" },
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Text(
                    text = experienceState,
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSecondary
                )
            }

            OutlinedButton(
                onClick = { if (!isUploadingPhoto) onPickPhoto() },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.45f))
            ) {
                Text(
                    if (isUploadingPhoto) "Subiendo..." else "Cambiar foto",
                    color = GymRankColors.PrimaryAccent
                )
            }
        }

        if (!uploadPhotoError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(uploadPhotoError, color = GymRankColors.Error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))

        Text("Nombre", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = nameState,
            onValueChange = { nameState = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tu nombre") }
        )

        Spacer(Modifier.height(12.dp))

        Text("Experiencia", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = expExpanded,
            onExpandedChange = { expExpanded = !expExpanded }
        ) {
            OutlinedTextField(
                value = experienceState,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expExpanded) }
            )
            ExposedDropdownMenu(
                expanded = expExpanded,
                onDismissRequest = { expExpanded = false }
            ) {
                expOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            experienceState = opt
                            expExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Género", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            OutlinedTextField(
                value = genderState,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genderOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            genderState = opt
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onSave(nameState, experienceState, genderState) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GymRankColors.PrimaryAccent)
        ) {
            Text("GUARDAR CAMBIOS", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        // Solicitudes
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Solicitudes de amistad",
                fontWeight = FontWeight.Bold,
                color = DesignTokens.Colors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "No tenés solicitudes pendientes.",
                color = DesignTokens.Colors.TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenFriendRequests,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = GymRankColors.PrimaryAccent
                )
                Spacer(Modifier.width(10.dp))
                Text("Ver solicitudes", color = GymRankColors.PrimaryAccent, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Cerrar sesión (con confirmación)
        OutlinedButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = GymRankColors.PrimaryAccent)
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión", color = GymRankColors.PrimaryAccent, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
    }
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
                Icon(imageVector = icon, contentDescription = null, tint = GymRankColors.PrimaryAccent)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = DesignTokens.Colors.TextSecondary)
            }
        }
    }
}

// ============================================
// MUSCLES TRAINED CARD
// ============================================

@Composable
private fun MusclesTrainedThisWeekCardPro(
    frontCounts: Map<com.example.gymrank.ui.components.MuscleId, Int>,
    backCounts: Map<com.example.gymrank.ui.components.MuscleId, Int>,
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
            BodyCanvas(title = "Frente", counts = frontCounts, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(14.dp))
            BodyCanvas(title = "Espalda", counts = backCounts, modifier = Modifier.weight(1f))
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
    counts: Map<com.example.gymrank.ui.components.MuscleId, Int>,
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
// CALENDAR
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
                DayPillSelectable(
                    day = dayLabelsShort[idx],
                    isToday = idx == todayIndex,
                    isSelected = idx == selectedDayIndex,
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
// ✅ Mi Rutina card (chips 2 columnas)
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
                        .border(
                            1.dp,
                            GymRankColors.PrimaryAccent.copy(alpha = 0.55f),
                            RoundedCornerShape(999.dp)
                        )
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
                    text = "No hay músculos cargados para este día. Tocá “$planButtonLabel” para configurarlo.",
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RoutineChipsTwoColumns(items: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2
    ) {
        items.forEach { label ->
            RoutineChip(label = label, modifier = Modifier.fillMaxWidth(0.48f))
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
// SETS BY MUSCLE - VER MÁS / VER MENOS
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
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
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
): Pair<Map<com.example.gymrank.ui.components.MuscleId, Int>, Map<com.example.gymrank.ui.components.MuscleId, Int>> {

    fun canonToMuscleId(name: String): com.example.gymrank.ui.components.MuscleId? =
        when (name.trim().lowercase(Locale.getDefault())) {
            "pecho", "pectorales", "pectoral" -> com.example.gymrank.ui.components.MuscleId.Chest
            "abdomen", "abs", "core" -> com.example.gymrank.ui.components.MuscleId.Abs
            "oblicuos", "oblicuo", "obliques" -> com.example.gymrank.ui.components.MuscleId.Obliques
            "biceps", "bíceps", "bicep" -> com.example.gymrank.ui.components.MuscleId.Biceps
            "antebrazos", "antebrazo", "forearms", "forearm" -> com.example.gymrank.ui.components.MuscleId.Forearms
            "cuadriceps", "cuádriceps", "quads", "quadriceps", "piernas" -> com.example.gymrank.ui.components.MuscleId.Quads
            "pantorrillas", "pantorrilla", "gemelos", "calves", "calf" -> com.example.gymrank.ui.components.MuscleId.Calves

            "hombros", "deltoides", "deltoide", "shoulders" -> com.example.gymrank.ui.components.MuscleId.Shoulders
            "trapecios", "trapecio", "traps", "trap" -> com.example.gymrank.ui.components.MuscleId.Traps

            "triceps", "tríceps", "tricep" -> com.example.gymrank.ui.components.MuscleId.Triceps
            "espalda", "back" -> com.example.gymrank.ui.components.MuscleId.Back
            "dorsales", "dorsal", "lats", "dorsal ancho" -> com.example.gymrank.ui.components.MuscleId.Lats
            "lumbar", "lumbares", "lower back", "espalda baja" -> com.example.gymrank.ui.components.MuscleId.LowerBack
            "gluteos", "glúteos", "glutes", "glute" -> com.example.gymrank.ui.components.MuscleId.Glutes
            "isquios", "isquiotibiales", "hamstrings", "femorales" -> com.example.gymrank.ui.components.MuscleId.Hamstrings
            else -> null
        }

    val front = mutableMapOf<com.example.gymrank.ui.components.MuscleId, Int>()
    val back = mutableMapOf<com.example.gymrank.ui.components.MuscleId, Int>()

    fun incFront(id: com.example.gymrank.ui.components.MuscleId) { front[id] = (front[id] ?: 0) + 1 }
    fun incBack(id: com.example.gymrank.ui.components.MuscleId) { back[id] = (back[id] ?: 0) + 1 }

    muscles.mapNotNull { canonToMuscleId(it) }
        .distinct()
        .forEach { id ->
            when (id) {
                com.example.gymrank.ui.components.MuscleId.Shoulders,
                com.example.gymrank.ui.components.MuscleId.Traps,
                com.example.gymrank.ui.components.MuscleId.Forearms,
                com.example.gymrank.ui.components.MuscleId.Calves -> { incFront(id); incBack(id) }

                com.example.gymrank.ui.components.MuscleId.Chest,
                com.example.gymrank.ui.components.MuscleId.Abs,
                com.example.gymrank.ui.components.MuscleId.Obliques,
                com.example.gymrank.ui.components.MuscleId.Biceps,
                com.example.gymrank.ui.components.MuscleId.Quads -> incFront(id)

                com.example.gymrank.ui.components.MuscleId.Triceps,
                com.example.gymrank.ui.components.MuscleId.Lats,
                com.example.gymrank.ui.components.MuscleId.Back,
                com.example.gymrank.ui.components.MuscleId.LowerBack,
                com.example.gymrank.ui.components.MuscleId.Glutes,
                com.example.gymrank.ui.components.MuscleId.Hamstrings -> incBack(id)

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
        "Pecho", "Espalda", "Femorales", "Hombros", "Bíceps", "Tríceps",
        "Abdomen", "Glúteos", "Cuadriceps", "Pantorrillas", "Trapecios", "Antebrazos"
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
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
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