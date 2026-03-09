package com.example.gymrank.ui.screens.ranking

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.gymrank.domain.model.RankingPeriod

// ============================================
// UI MODEL
// ============================================

data class RankingEntry(
    val position: Int,
    val name: String,
    val points: Int,
    val isMe: Boolean = false
)

// ============================================
// THEME COLORS
// ============================================

private val ScreenBg = Color(0xFF070B0A)
private val CardBg = Color(0xFF0F1412)
private val CardStroke = Color(0xFF1E2622)
private val Muted = Color(0xFF8E8E93)
private val White = Color(0xFFFFFFFF)
private val AccentGreen = Color(0xFF32E37A)
private val IconButtonBg = Color(0xFF151A18)
private val Divider = Color(0xFF1C2420)
private val SoftGlow = Color(0xFF103526)

private const val TAG = "RankingScreen"

// ============================================
// Period Keys (AR) (must match PointsRepositoryFirestoreImpl/UserRepositoryImpl)
// ============================================

private val arTz: TimeZone = TimeZone.getTimeZone("America/Argentina/Buenos_Aires")

private fun currentWeekKeyAr(): String {
    val cal = Calendar.getInstance(arTz, Locale.getDefault()).apply {
        firstDayOfWeek = Calendar.MONDAY
        // mantenemos el mismo criterio que otros repos del proyecto
        time = Date()
    }
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')
    return "$year-W$week"
}

private fun currentMonthKeyAr(): String {
    val cal = Calendar.getInstance(arTz, Locale.getDefault()).apply { time = Date() }
    val y = cal.get(Calendar.YEAR)
    val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "$y-$m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() },
    onSearch: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }

    var myUid by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { a ->
            myUid = a.currentUser?.uid
            Log.d(TAG, "AuthState changed -> uid=$myUid")
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var selectedPeriod by remember { mutableStateOf(RankingPeriod.WEEKLY) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var myPositionState by remember { mutableStateOf(0) }
    var myPointsState by remember { mutableStateOf(0) }

    var gymId by remember { mutableStateOf<String?>(null) }
    var gymName by remember { mutableStateOf("Ranking") }
    var gymLocation by remember { mutableStateOf("Global") }

    suspend fun loadGymContext(uid: String) {
        loading = true
        error = null

        runCatching {
            val meDoc = db.collection("users").document(uid).get().await()

            val gId = meDoc.getString("gymId")
            gymId = gId

            if (!gId.isNullOrBlank()) {
                val gymDoc = db.collection("gyms").document(gId).get().await()
                gymName = gymDoc.getString("name") ?: "Gym"
                gymLocation = gymDoc.getString("location") ?: "Buenos Aires, AR"
            } else {
                gymName = "Ranking"
                gymLocation = "Global"
            }

            Log.d(TAG, "GymContext loaded -> gymId=$gymId, gymName=$gymName")
        }.onFailure { e ->
            Log.e(TAG, "Error loading gym context", e)
            gymId = null
            gymName = "Ranking"
            gymLocation = "Global"
            error = e.message ?: "Error leyendo contexto de gym"
        }

        loading = false
    }

    suspend fun loadRanking(uid: String, gId: String?, period: RankingPeriod) {
        loading = true
        error = null

        val pf = period.field
        val weekKeyNow = currentWeekKeyAr()
        val monthKeyNow = currentMonthKeyAr()

        suspend fun buildBaseQuery(ignoreGym: Boolean): com.google.firebase.firestore.Query {
            // Para ranking global, no filtrar por gym
            if (period == RankingPeriod.GLOBAL) {
                return db.collection("users")
                    .orderBy(pf, Query.Direction.DESCENDING)
                    .limit(50)
            }

            // Para rankings semanales/mensuales sin gym, mostrar mensaje
            if (gId.isNullOrBlank() && (period == RankingPeriod.WEEKLY || period == RankingPeriod.MONTHLY)) {
                return db.collection("users")
                    .whereEqualTo("__name__", "nonexistent_doc") // Query que no retorna nada
                    .limit(1)
            }

            var q = if (!ignoreGym && !gId.isNullOrBlank()) {
                db.collection("users").whereEqualTo("gymId", gId)
            } else {
                db.collection("users")
            }

            q = when (period) {
                RankingPeriod.WEEKLY -> q.whereEqualTo("weeklyKey", weekKeyNow)
                RankingPeriod.MONTHLY -> q.whereEqualTo("monthlyKey", monthKeyNow)
                RankingPeriod.ALL_TIME -> q
                RankingPeriod.GLOBAL -> q // No debería llegar aquí por el if anterior
            }

            return q.orderBy(pf, Query.Direction.DESCENDING).limit(50)
        }

        suspend fun runMainQuery(ignoreGym: Boolean): List<RankingEntry> {
            val snap = buildBaseQuery(ignoreGym).get().await()
            return snap.documents.mapIndexed { index, doc ->
                val id = doc.id
                val name = doc.getString("displayName")
                    ?: doc.getString("name")
                    ?: doc.getString("username")
                    ?: "Usuario"

                RankingEntry(
                    position = index + 1,
                    name = name,
                    points = (doc.getLong(pf) ?: 0L).toInt(),
                    isMe = id == uid
                )
            }
        }

        suspend fun runHigherThanQuery(ignoreGym: Boolean, mePts: Int): Int {
            // Para ranking global, no filtrar por gym
            if (period == RankingPeriod.GLOBAL) {
                val higherSnap = db.collection("users")
                    .whereGreaterThan(pf, mePts)
                    .get()
                    .await()
                return higherSnap.size()
            }

            var higherQ = if (!ignoreGym && !gId.isNullOrBlank()) {
                db.collection("users").whereEqualTo("gymId", gId)
            } else {
                db.collection("users")
            }

            higherQ = when (period) {
                RankingPeriod.WEEKLY -> higherQ.whereEqualTo("weeklyKey", weekKeyNow)
                RankingPeriod.MONTHLY -> higherQ.whereEqualTo("monthlyKey", monthKeyNow)
                RankingPeriod.ALL_TIME -> higherQ
                RankingPeriod.GLOBAL -> higherQ // No debería llegar aquí
            }

            val higherSnap = higherQ
                .whereGreaterThan(pf, mePts)
                .get()
                .await()

            return higherSnap.size()
        }

        runCatching {
            // 1) main list
            val list = runMainQuery(ignoreGym = false)
            entries = list

            // 2) my position
            val me = list.firstOrNull { it.isMe }
            if (me != null) {
                myPositionState = me.position
                myPointsState = me.points
            } else {
                val meDoc = db.collection("users").document(uid).get().await()
                val mePts = (meDoc.getLong(pf) ?: 0L).toInt()
                myPointsState = mePts

                val higherCount = runHigherThanQuery(ignoreGym = false, mePts = mePts)
                myPositionState = if (mePts <= 0) 0 else higherCount + 1
            }

            Log.d(TAG, "Ranking loaded -> entries=${entries.size}, mePos=$myPositionState, mePts=$myPointsState")
        }.recoverCatching { e ->
            // Fallback específico para falta de índices
            val msg = e.message.orEmpty()
            if (msg.contains("FAILED_PRECONDITION", ignoreCase = true) && msg.contains("index", ignoreCase = true)) {
                Log.e(TAG, "Ranking missing index. Falling back to global query.", e)

                // Fallback: sin gymId (menos restrictivo => distinto índice/puede ya existir)
                val list = runMainQuery(ignoreGym = true)
                entries = list

                val meDoc = db.collection("users").document(uid).get().await()
                val mePts = (meDoc.getLong(pf) ?: 0L).toInt()
                myPointsState = mePts

                // Evitamos whereGreaterThan en fallback (también suele requerir índice)
                myPositionState = 0

                error = "Falta un índice en Firestore para este ranking. Ya podés crear el índice desde Firebase Console. (Mostrando fallback global)"
                return@recoverCatching
            }

            throw e
        }.onFailure { e ->
            Log.e(TAG, "Error loading ranking", e)
            error = e.message ?: "Error cargando ranking"
            entries = emptyList()
            myPositionState = 0
            myPointsState = 0
        }

        loading = false
    }

    LaunchedEffect(myUid) {
        val uid = myUid
        if (uid.isNullOrBlank()) {
            error = "No hay usuario logueado"
            entries = emptyList()
            gymId = null
            gymName = "Ranking"
            gymLocation = "Global"
            loading = false
        } else {
            loadGymContext(uid)
        }
    }

    LaunchedEffect(gymId, selectedPeriod, myUid) {
        val uid = myUid ?: return@LaunchedEffect
        loadRanking(uid, gymId, selectedPeriod)
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            RankingHeader(
                gymName = gymName,
                gymLocation = gymLocation,
                onBack = onBack,
                onSearch = onSearch,
                onNotifications = onNotifications,
                onGlobalRanking = { selectedPeriod = RankingPeriod.GLOBAL }
            )
        },
        bottomBar = {
            MyPositionBar(
                position = myPositionState,
                points = myPointsState,
                onViewDetails = {
                    navController.navigate(
                        RankingRoutes.detailsRoute(
                            gymName = gymName,
                            gymLocation = gymLocation,
                            periodLabel = selectedPeriod.label,
                            myPosition = myPositionState,
                            myPoints = myPointsState
                        )
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(padding)
        ) {
            PeriodTabs(
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it },
                hasGym = !gymId.isNullOrBlank()
            )

            Spacer(Modifier.height(14.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }

                error != null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = error!!, color = White, fontSize = 14.sp)
                    }
                }

                // Mostrar mensaje cuando no tiene gimnasio y está viendo ranking semanal/mensual
                gymId.isNullOrBlank() && (selectedPeriod == RankingPeriod.WEEKLY || selectedPeriod == RankingPeriod.MONTHLY) -> {
                    NoGymRankingMessage()
                }

                else -> {
                    val sorted = entries.sortedBy { it.position }
                    val top = sorted.take(3)

                    if (top.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        PodiumTop3Adaptive(top = top)
                        Spacer(Modifier.height(18.dp))
                    } else {
                        Spacer(Modifier.height(10.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sorted.drop(3)) { entry ->
                            RankingRow(entry = entry)
                        }

                        if (sorted.isEmpty()) {
                            item { EmptyRankingState() }
                        }

                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }
}

// ============================================
// HEADER
// ============================================

@Composable
private fun RankingHeader(
    gymName: String,
    gymLocation: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onGlobalRanking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = White
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = gymName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Surface(
                    color = IconButtonBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = gymLocation,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            CircleIconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Buscar",
                    tint = White
                )
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onGlobalRanking,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(
                    text = "Top Global",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(onClick = onClick, color = IconButtonBg, shape = CircleShape) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center, content = content)
    }
}

// ============================================
// TABS
// ============================================

@Composable
private fun PeriodTabs(
    selected: RankingPeriod,
    onSelected: (RankingPeriod) -> Unit,
    hasGym: Boolean = true
) {
    val availablePeriods = listOf(RankingPeriod.WEEKLY, RankingPeriod.MONTHLY)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        availablePeriods.forEach { period ->
            val isSelected = selected == period

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                TextButton(
                    onClick = { onSelected(period) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = period.label,
                        color = if (isSelected) AccentGreen else White.copy(alpha = 0.65f),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) AccentGreen else Color.Transparent)
                )
            }
        }
    }

    HorizontalDivider(
        color = Divider,
        thickness = 1.dp
    )
}

// ============================================
// PODIUM (ADAPTIVE)
// ============================================

@Composable
private fun PodiumTop3Adaptive(top: List<RankingEntry>) {
    val first = top.getOrNull(0)
    val second = top.getOrNull(1)
    val third = top.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        if (second != null) {
            PodiumCard(
                entry = second,
                size = 92.dp,
                label = "#2",
                medal = "🥈",
                isWinner = false
            )
        } else {
            Spacer(modifier = Modifier.width(96.dp))
        }

        if (first != null) {
            PodiumCard(
                entry = first,
                size = 128.dp,
                label = "#1",
                medal = "🥇",
                isWinner = true
            )
        }

        if (third != null) {
            PodiumCard(
                entry = third,
                size = 92.dp,
                label = "#3",
                medal = "🥉",
                isWinner = false
            )
        } else {
            Spacer(modifier = Modifier.width(96.dp))
        }
    }
}

@Composable
private fun PodiumCard(
    entry: RankingEntry,
    size: Dp,
    label: String,
    medal: String,
    isWinner: Boolean
) {
    val isMe = entry.isMe

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(if (isWinner) 132.dp else 102.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    when {
                        isMe -> SoftGlow
                        else -> IconButtonBg
                    }
                )
                .border(
                    width = if (isWinner || isMe) 3.dp else 1.dp,
                    color = when {
                        isMe -> AccentGreen
                        isWinner -> AccentGreen.copy(alpha = 0.85f)
                        else -> CardStroke
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = medal,
                fontSize = if (isWinner) 48.sp else 34.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = label,
            color = if (isWinner || isMe) AccentGreen else Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (isMe) "Tú" else entry.name,
            color = if (isMe) AccentGreen else White,
            fontSize = if (isWinner) 15.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${formatPts(entry.points)} pts",
            color = if (isMe || isWinner) AccentGreen.copy(alpha = 0.95f) else White.copy(alpha = 0.82f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyRankingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Todavía no hay puntos en este período.",
            color = Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NoGymRankingMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícono del gimnasio
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏢",
                    fontSize = 32.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "No participás del ranking\nsemanal/mensual",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Tu gimnasio no está vinculado a la app.\nSolicitá a tu gym vincularse para acceder\nal ranking.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ============================================
// ROW
// ============================================

@Composable
private fun RankingRow(entry: RankingEntry, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardBg.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (entry.isMe) AccentGreen.copy(alpha = 0.45f) else CardStroke,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.position}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = White.copy(alpha = 0.8f),
                modifier = Modifier.width(38.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(IconButtonBg)
                    .border(1.dp, CardStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏋️",
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (entry.isMe) "Tú" else entry.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isMe) AccentGreen else White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Competidor/a",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted
                )
            }

            Text(
                text = "${formatPts(entry.points)} pts",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
    }
}

// ============================================
// BOTTOM BAR
// ============================================

@Composable
private fun MyPositionBar(
    position: Int,
    points: Int,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF050706),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu posición",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = White.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (position > 0) "#$position · ${formatPts(points)} pts" else "— · ${formatPts(points)} pts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Button(
                onClick = onViewDetails,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Text(
                    text = "Ver detalles",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================
// UTILS
// ============================================

private fun formatPts(value: Int): String {
    val s = value.toString()
    val rev = s.reversed().chunked(3).joinToString(".")
    return rev.reversed()
}