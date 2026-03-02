package com.example.gymrank.ui.screens.challenges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.floor

private const val DAY_MILLIS = 86_400_000L

@Composable
fun ChallengesScreen(
    onOpenDiscover: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenGamble: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenUserChallengeDetail: (userChallengeId: String, templateId: String) -> Unit = { _, _ -> }
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

    // ✅ Autocomplete global: si alguno venció, lo marcamos COMPLETED
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
        if (newCount > prevCompletedCount) {
            selectedView = ChallengesView.HISTORY
        }
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

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(18.dp))

            Text("Vista:", color = textPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            SegmentedPills(
                selected = selectedView,
                onSelectedChange = { selectedView = it },
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface
            )

            Spacer(Modifier.height(14.dp))

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
                                "No se pudieron cargar tus desafíos",
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
                        title = if (selectedView == ChallengesView.PENDING) "No hay desafíos activos"
                        else "No hay desafíos terminados",
                        subtitle =
                            if (selectedView == ChallengesView.PENDING) "Aceptá uno en Descubrir para empezar"
                            else "Completá desafíos para ver tu historial"
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        items(list) { uc ->
                            UserChallengeCard(
                                item = uc,
                                now = now,
                                surface = surface,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent,
                                onClick = { onOpenUserChallengeDetail(uc.userChallengeId, uc.templateId) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

enum class ChallengesView { PENDING, HISTORY }

data class UserChallengeUi(
    val userChallengeId: String,
    val templateId: String,
    val status: ChallengeStatus,
    val title: String,
    val subtitle: String,
    val level: String,
    val durationDays: Int,
    val points: Int,
    val startedAt: Long?
)

data class ChallengesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val active: List<UserChallengeUi> = emptyList(),
    val completed: List<UserChallengeUi> = emptyList()
)

class ChallengesViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChallengesUiState())
    val state: StateFlow<ChallengesUiState> = _state.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val userChallengesCol = db.collection("user_challenges")
    private val templatesCol = db.collection("challenge_templates")

    private var reg: ListenerRegistration? = null
    private val templateCache = mutableMapOf<String, ChallengeTemplate>()

    // para no spamear escrituras
    private val autoCompletedIds = mutableSetOf<String>()

    init {
        listenMyChallenges()
    }

    fun autoCompleteExpired(active: List<UserChallengeUi>, nowMillis: Long) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            active.forEach { item ->
                if (item.status != ChallengeStatus.ACTIVE) return@forEach
                val start = item.startedAt ?: return@forEach
                val duration = item.durationDays.coerceAtLeast(1)

                val elapsedDays = floor(((nowMillis - start).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()).toInt()
                if (elapsedDays < duration) return@forEach

                // ya intentamos auto completar
                if (!autoCompletedIds.add(item.userChallengeId)) return@forEach

                runCatching {
                    val patch = mapOf(
                        "status" to ChallengeStatus.COMPLETED.name,
                        "completedAt" to nowMillis,
                        "updatedAt" to nowMillis
                    )
                    userChallengesCol.document(item.userChallengeId).set(patch, com.google.firebase.firestore.SetOptions.merge()).await()
                }.onFailure {
                    // si falló, permitimos reintentar más tarde
                    autoCompletedIds.remove(item.userChallengeId)
                }
            }
        }
    }

    fun refreshNow() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            runCatching {
                val snap = userChallengesCol
                    .whereEqualTo("uid", uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val raw = snap.documents.mapNotNull { it.toUserChallengeOrNull() }

                val neededTemplateIds = raw.map { it.templateId }
                    .distinct()
                    .filter { it.isNotBlank() && !templateCache.containsKey(it) }

                if (neededTemplateIds.isNotEmpty()) {
                    runCatching {
                        neededTemplateIds.map { tid ->
                            async {
                                val d = templatesCol.document(tid).get().await()
                                d.toChallengeTemplateOrNull()?.let { templateCache[tid] = it }
                            }
                        }.awaitAll()
                    }
                }

                val ui = raw.map { uc ->
                    val t = templateCache[uc.templateId]
                    UserChallengeUi(
                        userChallengeId = uc.id,
                        templateId = uc.templateId,
                        status = uc.status,
                        title = t?.title ?: "Desafío",
                        subtitle = t?.subtitle ?: "Template: ${uc.templateId}",
                        level = t?.level?.ifBlank { "Facil" } ?: "Facil",
                        durationDays = t?.durationDays ?: 0,
                        points = t?.points ?: 0,
                        startedAt = uc.startedAt
                    )
                }

                ChallengesUiState(
                    loading = false,
                    error = null,
                    active = ui.filter { it.status == ChallengeStatus.ACTIVE },
                    completed = ui.filter { it.status == ChallengeStatus.COMPLETED }
                )
            }.onSuccess { newState ->
                _state.value = newState
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    private fun listenMyChallenges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.value = ChallengesUiState(loading = false, error = "Usuario no autenticado")
            return
        }

        reg?.remove()

        val q = userChallengesCol
            .whereEqualTo("uid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        _state.value = _state.value.copy(loading = true, error = null)

        reg = q.addSnapshotListener { snap, e ->
            if (e != null) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
                return@addSnapshotListener
            }

            val docs = snap?.documents.orEmpty()
            val raw = docs.mapNotNull { it.toUserChallengeOrNull() }

            val uiFast = raw.map { uc ->
                val t = templateCache[uc.templateId]
                UserChallengeUi(
                    userChallengeId = uc.id,
                    templateId = uc.templateId,
                    status = uc.status,
                    title = t?.title ?: "Desafío",
                    subtitle = t?.subtitle ?: "Cargando detalles...",
                    level = t?.level?.ifBlank { "Facil" } ?: "Facil",
                    durationDays = t?.durationDays ?: 0,
                    points = t?.points ?: 0,
                    startedAt = uc.startedAt
                )
            }

            _state.value = ChallengesUiState(
                loading = false,
                error = null,
                active = uiFast.filter { it.status == ChallengeStatus.ACTIVE },
                completed = uiFast.filter { it.status == ChallengeStatus.COMPLETED }
            )

            viewModelScope.launch {
                val neededTemplateIds = raw.map { it.templateId }
                    .distinct()
                    .filter { it.isNotBlank() && !templateCache.containsKey(it) }

                if (neededTemplateIds.isNotEmpty()) {
                    runCatching {
                        neededTemplateIds.map { tid ->
                            async {
                                val d = templatesCol.document(tid).get().await()
                                d.toChallengeTemplateOrNull()?.let { templateCache[tid] = it }
                            }
                        }.awaitAll()
                    }
                }

                val uiFinal = raw.map { uc ->
                    val t = templateCache[uc.templateId]
                    UserChallengeUi(
                        userChallengeId = uc.id,
                        templateId = uc.templateId,
                        status = uc.status,
                        title = t?.title ?: "Desafío",
                        subtitle = t?.subtitle ?: "Template: ${uc.templateId}",
                        level = t?.level?.ifBlank { "Facil" } ?: "Facil",
                        durationDays = t?.durationDays ?: 0,
                        points = t?.points ?: 0,
                        startedAt = uc.startedAt
                    )
                }

                _state.value = ChallengesUiState(
                    loading = false,
                    error = null,
                    active = uiFinal.filter { it.status == ChallengeStatus.ACTIVE },
                    completed = uiFinal.filter { it.status == ChallengeStatus.COMPLETED }
                )
            }
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserChallengeOrNull(): UserChallenge? {
        return runCatching {
            val statusStr = getString("status").orEmpty()
            val status = runCatching { ChallengeStatus.valueOf(statusStr) }.getOrElse { ChallengeStatus.ACTIVE }

            UserChallenge(
                id = id,
                uid = getString("uid").orEmpty(),
                templateId = getString("templateId").orEmpty(),
                status = status,
                startedAt = getLong("startedAt"),
                completedAt = getLong("completedAt"),
                canceledAt = getLong("canceledAt"),
                createdAt = getLong("createdAt"),
                updatedAt = getLong("updatedAt")
            )
        }.getOrNull()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallengeTemplateOrNull(): ChallengeTemplate? {
        return runCatching {
            val createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            val updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time

            val pointsInt =
                (getLong("points")?.toInt())
                    ?: getString("points")?.toIntOrNull()
                    ?: 0

            ChallengeTemplate(
                id = id,
                title = getString("title").orEmpty(),
                subtitle = getString("subtitle").orEmpty(),
                level = getString("level").orEmpty(),
                durationDays = (getLong("durationDays") ?: 0L).toInt(),
                points = pointsInt,
                imageUrl = getString("imageUrl"),
                tags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isActive = getBoolean("isActive") ?: true,
                createdAt = createdAtMillis,
                updatedAt = updatedAtMillis
            )
        }.getOrNull()
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}

// ---------------- UI COMPONENTS ----------------

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[0], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
            FeatureButton(items[1], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .heightIn(min = 78.dp)
                    .background(gradient, shape)
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
                    Icon(item.icon, contentDescription = item.title, tint = accent, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = textSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
            .height(44.dp)
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (selected) textPrimary else textSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun UserChallengeCard(
    item: UserChallengeUi,
    now: Long,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        color = surface,
        shape = shape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(10.dp))

                LevelPill(text = item.level.ifBlank { "Facil" }, accent = accent)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = item.subtitle,
                color = textSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            if (item.status == ChallengeStatus.ACTIVE) {
                val duration = item.durationDays.coerceAtLeast(1)
                val start = item.startedAt ?: now

                val elapsedDays =
                    floor(((now - start).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()).toInt()

                val dayNumber = (elapsedDays + 1).coerceIn(1, duration)
                val remaining = (duration - elapsedDays).coerceAtLeast(0)
                val progress = (elapsedDays.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent.copy(alpha = 0.55f),
                    trackColor = Color.White.copy(alpha = 0.10f)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Día $dayNumber de $duration • Restan $remaining",
                    color = textSecondary,
                    fontSize = 14.sp
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
private fun LevelPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
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