@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.gymrank.ui.screens.challenges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.UserChallenge
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.data.repository.UserMissionsRepositoryFirestoreImpl
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.floor
import kotlin.math.max

private const val DAY_MILLIS = 86_400_000L

@Composable
fun ChallengesScreen(
    onOpenDiscover: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenGamble: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenUserChallengeDetail: (userChallengeId: String, templateId: String) -> Unit = { _, _ -> } // queda por compat
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF1C1C1E) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    // ✅ siempre arrancar en PENDIENTES
    var selectedView by remember { mutableStateOf(ChallengesView.PENDING) }

    val vm: ChallengesViewModel = viewModel()
    val state by vm.state.collectAsState()
    val detail by vm.detail.collectAsState()

    // ticker para que cards avancen solas
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    // ✅ al volver a la pantalla refresca sí o sí
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ✅ Autocomplete: si alguno venció, lo marcamos COMPLETED
    LaunchedEffect(state.active, now) {
        vm.autoCompleteExpired(state.active, now)
    }

    // ✅ NO cambiar a "Completados" durante el primer load
    var countsInitialized by remember { mutableStateOf(false) }
    var prevCompletedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.loading) {
        if (!state.loading && !countsInitialized) {
            countsInitialized = true
            prevCompletedCount = state.completed.size
            selectedView = ChallengesView.PENDING
        }
    }

    // ✅ después del primer load: si aumenta completed => ir a Completados
    LaunchedEffect(state.completed.size) {
        if (!countsInitialized) return@LaunchedEffect
        val newCount = state.completed.size
        if (newCount > prevCompletedCount) selectedView = ChallengesView.HISTORY
        prevCompletedCount = newCount
    }

    val list = remember(selectedView, state) {
        when (selectedView) {
            ChallengesView.PENDING -> state.active
            ChallengesView.HISTORY -> state.completed
        }
    }

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Desafíos", color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* TODO menu */ }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = textPrimary)
                }
            }

            Spacer(Modifier.height(14.dp))

            ChallengesFeatureGrid(
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface,
                onOpenDiscover = onOpenDiscover,
                onOpenQuests = onOpenQuests,
                onOpenGamble = onOpenGamble,
                onOpenEquipment = onOpenEquipment
            )

            Spacer(Modifier.height(16.dp))

            Text("Vista:", color = textPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            SegmentedPills(
                selected = selectedView,
                onSelectedChange = { selectedView = it },
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.loading -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                state.error != null -> {
                    GlassCard(glow = true) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                "No se pudieron cargar tus desafíos/misiones",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(state.error ?: "", color = textSecondary, fontSize = 13.sp)
                        }
                    }
                }

                list.isEmpty() -> {
                    ChallengesEmptyStateCard(
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        title = if (selectedView == ChallengesView.PENDING) "No hay activos"
                        else "No hay terminados",
                        subtitle =
                            if (selectedView == ChallengesView.PENDING) "Aceptá uno en Descubrir o empezá una misión"
                            else "Completá desafíos/misiones para ver tu historial"
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        items(list, key = { it.key }) { item ->
                            UnifiedCard(
                                item = item,
                                now = now,
                                surface = surface,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent,
                                onClick = { vm.openDetail(item) } // ✅ ahora abre resumen/modal
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // ✅ modal/resumen detalle (misión/desafío)
        if (detail != null) {
            ChallengeDetailModal(
                item = detail!!,
                now = now,
                accent = accent,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClose = { vm.closeDetail() },
                onAbandon = {
                    vm.abandon(detail!!)
                    vm.closeDetail()
                },
                onComplete = { vm.manualComplete(detail!!, now) }
            )
        }
    }
}

enum class ChallengesView { PENDING, HISTORY }
enum class UnifiedKind { MISSION, CHALLENGE }

/** ✅ Unificado (misión + desafío) */
data class UnifiedItemUi(
    val key: String,
    val kind: UnifiedKind,
    val instanceId: String,
    val templateId: String, // siempre string (vacío si no hay)
    val isCompleted: Boolean,
    val title: String,
    val subtitle: String,
    val kindLabel: String,  // "MISIÓN" / "DESAFÍO"
    val levelLabel: String, // "Fácil" / "Medio" / "Difícil"
    val durationDays: Int,
    val points: Int,
    val startedAt: Long?
)

data class ChallengesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val active: List<UnifiedItemUi> = emptyList(),
    val completed: List<UnifiedItemUi> = emptyList()
)

/** ---------------- VIEWMODEL ---------------- */
class ChallengesViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChallengesUiState())
    val state: StateFlow<ChallengesUiState> = _state.asStateFlow()

    // ✅ detalle seleccionado (modal)
    private val _detail = MutableStateFlow<UnifiedItemUi?>(null)
    val detail: StateFlow<UnifiedItemUi?> = _detail.asStateFlow()

    fun openDetail(item: UnifiedItemUi) { _detail.value = item }
    fun closeDetail() { _detail.value = null }

    private val db = FirebaseFirestore.getInstance()

    // Ajustá si difiere en tu Firestore
    private val userChallengesCol = db.collection("user_challenges")
    private val challengeTemplatesCol = db.collection("challenge_templates")

    private val userMissionsCol = db.collection("user_missions")
    private val missionTemplatesCol = db.collection("missionTemplate")

    private var regChallenges: ListenerRegistration? = null
    private var regMissions: ListenerRegistration? = null

    private val challengeTemplateCache = mutableMapOf<String, ChallengeTemplateMini>()
    private val missionTemplateCache = mutableMapOf<String, MissionTemplateMini>()

    // para no spamear escrituras
    private val autoCompletedKeys = mutableSetOf<String>()

    private var lastChallenges: List<UserChallenge> = emptyList()
    private var lastMissions: List<UserMissionDoc> = emptyList()

    private val challengeRepo = ChallengeRepositoryFirestoreImpl()
    private val userMissionsRepo = UserMissionsRepositoryFirestoreImpl()
    private val pointsRepo = com.example.gymrank.data.repository.PointsRepositoryFirestoreImpl()

    init {
        listenChallenges()
        listenMissions()

        // ✅ Catch-up automático al inicializar
        viewModelScope.launch {
            runCatching {
                challengeRepo.awardCompletedChallengesIfNeeded(pointsRepo)
            }
        }
    }

    fun refreshNow() {
        fetchOnceChallenges()
        fetchOnceMissions()
    }

    fun manualComplete(item: UnifiedItemUi, nowMillis: Long) {
        val start = item.startedAt ?: return
        val duration = item.durationDays.coerceAtLeast(1)
        val dayNumber = computeDayNumber(nowMillis, start, duration)

        // ✅ solo cuando terminó
        if (dayNumber < duration || item.isCompleted) return

        viewModelScope.launch {
            runCatching {
                when (item.kind) {
                    UnifiedKind.CHALLENGE -> {
                        challengeRepo.completeUserChallengeAndAwardPoints(
                            userChallengeId = item.instanceId,
                            pointsRepo = pointsRepo
                        )
                    }
                    UnifiedKind.MISSION -> {
                        userMissionsRepo.completeUserMissionAndAwardPoints(
                            userMissionId = item.instanceId,
                            pointsRepo = pointsRepo
                        )
                    }
                }
                closeDetail()
            }
        }
    }

    fun autoCompleteExpired(active: List<UnifiedItemUi>, nowMillis: Long) {
        FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            active.forEach { item ->
                if (item.isCompleted) return@forEach
                val start = item.startedAt ?: return@forEach
                val duration = item.durationDays.coerceAtLeast(1)

                val dayNumber = computeDayNumber(nowMillis, start, duration)
                val expired = dayNumber >= duration
                if (!expired) return@forEach

                if (!autoCompletedKeys.add(item.key)) return@forEach

                runCatching {
                    when (item.kind) {
                        UnifiedKind.CHALLENGE -> {
                            challengeRepo.completeUserChallengeAndAwardPoints(
                                userChallengeId = item.instanceId,
                                pointsRepo = pointsRepo
                            )
                        }
                        UnifiedKind.MISSION -> {
                            userMissionsRepo.completeUserMissionAndAwardPoints(
                                userMissionId = item.instanceId,
                                pointsRepo = pointsRepo
                            )
                        }
                    }
                }.onFailure {
                    autoCompletedKeys.remove(item.key)
                }
            }
        }
    }


    private fun listenChallenges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.value = ChallengesUiState(loading = false, error = "Usuario no autenticado")
            return
        }

        regChallenges?.remove()

        val q = userChallengesCol
            .whereEqualTo("uid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        _state.value = _state.value.copy(loading = true, error = null)

        regChallenges = q.addSnapshotListener { snap, e ->
            if (e != null) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
                return@addSnapshotListener
            }

            val raw = snap?.documents.orEmpty().mapNotNull { it.toUserChallengeOrNull() }
            lastChallenges = raw

            viewModelScope.launch {
                hydrateChallengeTemplates(raw)
                recomputeUnified()
            }
        }
    }

    private fun listenMissions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.value = ChallengesUiState(loading = false, error = "Usuario no autenticado")
            return
        }

        regMissions?.remove()

        val q = userMissionsCol
            .whereEqualTo("uid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        _state.value = _state.value.copy(loading = true, error = null)

        regMissions = q.addSnapshotListener { snap, e ->
            if (e != null) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
                return@addSnapshotListener
            }

            val raw = snap?.documents.orEmpty().mapNotNull { it.toUserMissionDocOrNull() }
            lastMissions = raw

            viewModelScope.launch {
                hydrateMissionTemplates(raw)
                recomputeUnified()
            }
        }
    }

    private fun fetchOnceChallenges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val snap = userChallengesCol
                    .whereEqualTo("uid", uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().await()

                val raw = snap.documents.mapNotNull { it.toUserChallengeOrNull() }
                lastChallenges = raw
                hydrateChallengeTemplates(raw)
                recomputeUnified()
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    private fun fetchOnceMissions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val snap = userMissionsCol
                    .whereEqualTo("uid", uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().await()

                val raw = snap.documents.mapNotNull { it.toUserMissionDocOrNull() }
                lastMissions = raw
                hydrateMissionTemplates(raw)
                recomputeUnified()
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    /** ✅ FIX: async dentro de coroutineScope */
    private suspend fun hydrateChallengeTemplates(raw: List<UserChallenge>) = coroutineScope {
        val needed = raw.map { it.templateId }
            .distinct()
            .filter { it.isNotBlank() && !challengeTemplateCache.containsKey(it) }

        if (needed.isEmpty()) return@coroutineScope

        runCatching {
            needed.map { tid ->
                async {
                    val d = challengeTemplatesCol.document(tid).get().await()
                    d.toChallengeTemplateMiniOrNull()?.let { challengeTemplateCache[tid] = it }
                }
            }.awaitAll()
        }
    }

    /** ✅ FIX: async dentro de coroutineScope */
    private suspend fun hydrateMissionTemplates(raw: List<UserMissionDoc>) = coroutineScope {
        val needed = raw.mapNotNull { it.templateId }
            .distinct()
            .filter { it.isNotBlank() && !missionTemplateCache.containsKey(it) }

        if (needed.isEmpty()) return@coroutineScope

        runCatching {
            needed.map { tid ->
                async {
                    val d = missionTemplatesCol.document(tid).get().await()
                    d.toMissionTemplateMiniOrNull()?.let { missionTemplateCache[tid] = it }
                }
            }.awaitAll()
        }
    }

    private fun recomputeUnified() {
        val challengeUi = lastChallenges.map { uc ->
            val t = challengeTemplateCache[uc.templateId]
            val title = t?.title ?: "Desafío"
            val subtitle = when {
                t?.subtitle?.isNotBlank() == true -> t.subtitle
                else -> "Cargando detalles..."
            }

            val level = normalizeLevel(t?.level ?: "Fácil")
            val duration = (t?.durationDays ?: 14).coerceAtLeast(1)
            val points = t?.points ?: 0

            UnifiedItemUi(
                key = "C_${uc.id}",
                kind = UnifiedKind.CHALLENGE,
                instanceId = uc.id,
                templateId = uc.templateId,
                isCompleted = (uc.status == ChallengeStatus.COMPLETED),
                title = title,
                subtitle = subtitle,
                kindLabel = "DESAFÍO",
                levelLabel = level,
                durationDays = duration,
                points = points,
                startedAt = uc.startedAt
            )
        }

        val missionUi = lastMissions.map { um ->
            val t = um.templateId?.let { missionTemplateCache[it] }

            val title = um.title.ifBlank { t?.title ?: "Misión" }
            val subtitle = when {
                um.subtitle.isNotBlank() -> um.subtitle
                t?.subtitle?.isNotBlank() == true -> t.subtitle
                um.description.isNotBlank() -> um.description
                else -> "Misión activa"
            }

            val level = normalizeLevel(um.level.ifBlank { t?.level ?: "Fácil" })
            val duration = (um.durationDays.takeIf { it > 0 } ?: (t?.durationDays ?: 14)).coerceAtLeast(1)
            val points = (um.points.takeIf { it > 0 } ?: (t?.points ?: 0))

            UnifiedItemUi(
                key = "M_${um.id}",
                kind = UnifiedKind.MISSION,
                instanceId = um.id,
                templateId = um.templateId ?: "",
                isCompleted = um.isCompleted,
                title = title,
                subtitle = subtitle,
                kindLabel = "MISIÓN",
                levelLabel = level,
                durationDays = duration,
                points = points,
                startedAt = um.startedAt
            )
        }

        val unified = (challengeUi + missionUi)
            .sortedByDescending { it.startedAt ?: 0L }

        _state.value = ChallengesUiState(
            loading = false,
            error = null,
            active = unified.filter { !it.isCompleted },
            completed = unified.filter { it.isCompleted }
        )

        // ✅ Catch-up cada vez que se actualizan los datos
        if (unified.any { it.isCompleted }) {
            viewModelScope.launch {
                runCatching {
                    challengeRepo.awardCompletedChallengesIfNeeded(pointsRepo)
                }
            }
        }
    }

    fun abandon(item: UnifiedItemUi) {
        viewModelScope.launch {
            runCatching {
                when (item.kind) {
                    UnifiedKind.CHALLENGE -> {
                        userChallengesCol.document(item.instanceId).delete().await()
                    }
                    UnifiedKind.MISSION -> {
                        userMissionsCol.document(item.instanceId).delete().await()
                    }
                }
            }.onFailure { e ->
                // Si querés, podés loguear o mostrar error (por ahora lo dejamos silencioso)
                // _state.value = _state.value.copy(error = e.message)
            }
            // No hace falta recompute manual:
            // los snapshot listeners van a refrescar solos al borrarse el doc.
        }
    }

    // --------- FIRESTORE PARSERS (MINI) ---------

    /** Lee un campo que puede venir como Timestamp o como Long (millis) */
    private fun com.google.firebase.firestore.DocumentSnapshot.readMillis(field: String): Long? {
        val v = get(field) ?: return null
        return when (v) {
            is com.google.firebase.Timestamp -> v.toDate().time
            is Long -> v
            is Int -> v.toLong()
            is Double -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserChallengeOrNull(): UserChallenge? {
        return runCatching {
            val statusStr = getString("status").orEmpty()
            val status = runCatching { ChallengeStatus.valueOf(statusStr) }
                .getOrElse { ChallengeStatus.ACTIVE }

            // ✅ START REAL: startedAt si existe, sino createdAt
            val startReal = readMillis("startedAt") ?: readMillis("createdAt")

            UserChallenge(
                id = id,
                uid = getString("uid").orEmpty(),
                templateId = getString("templateId").orEmpty(),
                status = status,
                startedAt = startReal,
                completedAt = readMillis("completedAt"),
                canceledAt = readMillis("canceledAt"),
                createdAt = readMillis("createdAt"),
                updatedAt = readMillis("updatedAt")
            )
        }.getOrNull()
    }

    private data class UserMissionDoc(
        val id: String,
        val uid: String,
        val templateId: String?,
        val title: String,
        val subtitle: String,
        val description: String,
        val level: String,
        val durationDays: Int,
        val points: Int,
        val tags: List<String>,
        val startedAt: Long?,
        val isCompleted: Boolean
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserMissionDocOrNull(): UserMissionDoc? {
        return runCatching {
            val statusStr = getString("status")?.uppercase() ?: ""
            val completedAt = readMillis("completedAt")
            val isCompleted = statusStr == "COMPLETED" || (completedAt != null && completedAt > 0L)

            // ✅ START REAL: startedAt si existe, sino createdAt
            val startReal = readMillis("startedAt") ?: readMillis("createdAt")

            UserMissionDoc(
                id = id,
                uid = getString("uid").orEmpty(),
                templateId = getString("templateId"),
                title = getString("title").orEmpty(),
                subtitle = getString("subtitle").orEmpty(),
                description = getString("description").orEmpty(),
                level = getString("level").orEmpty(),
                durationDays = (readMillis("durationDays") ?: 0L).toInt(),
                points = ((readMillis("points") ?: 0L).toInt()),
                tags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                startedAt = startReal,
                isCompleted = isCompleted
            )
        }.getOrNull()
    }

    private data class ChallengeTemplateMini(
        val id: String,
        val title: String,
        val subtitle: String,
        val level: String,
        val durationDays: Int,
        val points: Int
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallengeTemplateMiniOrNull(): ChallengeTemplateMini? {
        return runCatching {
            val pointsInt =
                (getLong("points")?.toInt())
                    ?: getString("points")?.toIntOrNull()
                    ?: 0

            ChallengeTemplateMini(
                id = id,
                title = getString("title").orEmpty(),
                subtitle = getString("subtitle").orEmpty(),
                level = getString("level").orEmpty(),
                durationDays = (getLong("durationDays") ?: 0L).toInt(),
                points = pointsInt
            )
        }.getOrNull()
    }

    private data class MissionTemplateMini(
        val id: String,
        val title: String,
        val subtitle: String,
        val description: String,
        val level: String,
        val durationDays: Int,
        val points: Int
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toMissionTemplateMiniOrNull(): MissionTemplateMini? {
        return runCatching {
            val pointsInt =
                (getLong("points")?.toInt())
                    ?: getString("points")?.toIntOrNull()
                    ?: 0

            MissionTemplateMini(
                id = id,
                title = getString("title").orEmpty(),
                subtitle = getString("subtitle").orEmpty(),
                description = getString("description").orEmpty(),
                level = getString("level").orEmpty(),
                durationDays = (getLong("durationDays") ?: 0L).toInt(),
                points = pointsInt
            )
        }.getOrNull()
    }

    override fun onCleared() {
        regChallenges?.remove()
        regMissions?.remove()
        super.onCleared()
    }
}

/** ---------------- UI ---------------- */

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ChallengesFeatureGrid(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    onOpenDiscover: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenGamble: () -> Unit,
    onOpenEquipment: () -> Unit
) {
    val items = remember(onOpenDiscover, onOpenQuests, onOpenGamble, onOpenEquipment) {
        listOf(
            FeatureItem("Descubrir", "Nuevos desafíos", Icons.Filled.Search, onOpenDiscover),
            FeatureItem("Misiones", "Crear y gestionar", Icons.Filled.Task, onOpenQuests),
            FeatureItem("Apuestas", "Rueda y dados", Icons.Filled.Casino, onOpenGamble),
            FeatureItem("Equipamiento", "Tus ítems y stats", Icons.Filled.Build, onOpenEquipment),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureButton(items[0], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
            FeatureButton(items[1], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureButton(items[2], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
            FeatureButton(items[3], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureButton(
    item: FeatureItem,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.86f)))
    val shape = RoundedCornerShape(16.dp)

    GlassCard(modifier = modifier, contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { item.onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp) // ✅ más compacto
                    .background(gradient, shape)
                    .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp) // ✅ más chico
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = item.title, tint = accent, modifier = Modifier.size(18.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = textPrimary,
                        fontSize = 12.sp, // ✅ más chico
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = textSecondary,
                        fontSize = 12.sp, // ✅ más chico
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun SegmentedPills(
    selected: ChallengesView,
    onSelectedChange: (ChallengesView) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color
) {
    val border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    val bgUnselected = surface.copy(alpha = 0.70f)
    val bgSelected = accent.copy(alpha = 0.14f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedPill(
            modifier = Modifier.weight(1f),
            label = "Pendientes",
            icon = Icons.Filled.Checklist,
            selected = selected == ChallengesView.PENDING,
            bgSelected = bgSelected,
            bgUnselected = bgUnselected,
            border = border,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        ) { onSelectedChange(ChallengesView.PENDING) }

        SegmentedPill(
            modifier = Modifier.weight(1f),
            label = "Completados",
            icon = Icons.Filled.History,
            selected = selected == ChallengesView.HISTORY,
            bgSelected = bgSelected,
            bgUnselected = bgUnselected,
            border = border,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        ) { onSelectedChange(ChallengesView.HISTORY) }
    }
}

@Composable
private fun SegmentedPill(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    bgSelected: Color,
    bgUnselected: Color,
    border: BorderStroke,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)

    Surface(
        shape = shape,
        color = if (selected) bgSelected else bgUnselected,
        border = border,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(40.dp) // ✅ más compacto
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp)) // ✅ más chico
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (selected) textPrimary else textSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp, // ✅ más chico
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun UnifiedCard(
    item: UnifiedItemUi,
    now: Long,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        color = surface,
        shape = shape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, // ✅ más chico
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(item.kindLabel, accent)
                    LevelPill(item.levelLabel.ifBlank { "Fácil" }, accent)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.subtitle,
                color = textSecondary,
                fontSize = 13.sp, // ✅ más chico
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            if (!item.isCompleted) {
                val duration = item.durationDays.coerceAtLeast(1)
                val start = item.startedAt ?: now

                val dayNumber = computeDayNumber(now, start, duration)
                val remaining = max(duration - dayNumber, 0)
                val progress = (dayNumber.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent.copy(alpha = 0.55f),
                    trackColor = Color.White.copy(alpha = 0.10f)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Día $dayNumber de $duration • Restan $remaining",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniPill("Terminado", accent)
                    if (item.points > 0) MiniPill("${item.points} pts", accent)
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun LevelPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun MiniPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun ChallengesEmptyStateCard(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    title: String,
    subtitle: String
) {
    GlassCard(glow = true) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.12f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧗", fontSize = 72.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                title,
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                subtitle,
                color = textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(54.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.35f))
            )
        }
    }
}

/** ---------------- MODAL DETALLE ---------------- */

@Composable
private fun ChallengeDetailModal(
    item: UnifiedItemUi,
    now: Long,
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClose: () -> Unit,
    onAbandon: () -> Unit,
    onComplete: () -> Unit
) {
    val duration = item.durationDays.coerceAtLeast(1)
    val start = item.startedAt ?: now
    val dayNumber = computeDayNumber(now, start, duration)
    val remaining = max(duration - dayNumber, 0)
    val progress = (dayNumber.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val canComplete = (dayNumber >= duration) && !item.isCompleted

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClose)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {}
        ) {
            Column(Modifier.padding(18.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAbandon,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                    ) {
                        Text("Abandonar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onComplete,
                        enabled = canComplete,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent.copy(alpha = 0.30f)
                        )
                    ) {
                        Text("Completado", color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    MiniPill(item.kindLabel, accent)
                    MiniPill(item.levelLabel, accent)
                    MiniPill("${item.durationDays} días", accent)
                    if (item.points > 0) MiniPill("${item.points} pts", accent)
                }

                Spacer(Modifier.height(16.dp))

                if (!item.isCompleted) {
                    Text("Progreso", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent.copy(alpha = 0.60f),
                        trackColor = Color.White.copy(alpha = 0.10f)
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Día $dayNumber de $duration • Restan $remaining",
                        color = textSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Podés marcarla como completada cuando termines los $duration días.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    MiniPill("Terminado", accent)
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAbandon,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                    ) {
                        Text("Abandonar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onComplete,
                        enabled = canComplete,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent.copy(alpha = 0.30f)
                        )
                    ) {
                        Text("Completado", color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/** ---------------- HELPERS ---------------- */

private fun computeDayNumber(nowMillis: Long, startMillis: Long, durationDays: Int): Int {
    val safeDuration = durationDays.coerceAtLeast(1)
    val elapsedDays = floor(((nowMillis - startMillis).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()).toInt()
    return (elapsedDays + 1).coerceIn(1, safeDuration)
}

private fun normalizeLevel(raw: String): String {
    val s = raw.trim().lowercase()
    return when {
        s.contains("fac") || s == "easy" -> "Fácil"
        s.contains("med") || s == "medium" -> "Medio"
        s.contains("dif") || s == "hard" -> "Difícil"
        else -> raw.replaceFirstChar { it.uppercase() }.ifBlank { "Fácil" }
    }
}