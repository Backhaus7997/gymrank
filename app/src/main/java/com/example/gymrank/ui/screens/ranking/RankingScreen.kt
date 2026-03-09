package com.example.gymrank.ui.screens.ranking

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
            var q = if (!ignoreGym && !gId.isNullOrBlank()) {
                db.collection("users").whereEqualTo("gymId", gId)
            } else {
                db.collection("users")
            }

            q = when (period) {
                RankingPeriod.WEEKLY -> q.whereEqualTo("weeklyKey", weekKeyNow)
                RankingPeriod.MONTHLY -> q.whereEqualTo("monthlyKey", monthKeyNow)
                RankingPeriod.ALL_TIME -> q
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
            var higherQ = if (!ignoreGym && !gId.isNullOrBlank()) {
                db.collection("users").whereEqualTo("gymId", gId)
            } else {
                db.collection("users")
            }

            higherQ = when (period) {
                RankingPeriod.WEEKLY -> higherQ.whereEqualTo("weeklyKey", weekKeyNow)
                RankingPeriod.MONTHLY -> higherQ.whereEqualTo("monthlyKey", monthKeyNow)
                RankingPeriod.ALL_TIME -> higherQ
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
                onNotifications = onNotifications
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
                onSelected = { selectedPeriod = it }
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

                else -> {
                    val sorted = entries.sortedBy { it.position }
                    val top = sorted.take(3)

                    if (top.isNotEmpty()) {
                        PodiumTop3Adaptive(top = top)
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
    onNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = White
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gymName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Surface(color = IconButtonBg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = gymLocation,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            CircleIconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = White)
            }

            Spacer(Modifier.width(10.dp))

            CircleIconButton(onClick = onNotifications) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = White)
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
    onSelected: (RankingPeriod) -> Unit
) {
    val items = remember { RankingPeriod.values().toList() }
    val selectedIndex = items.indexOf(selected)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { idx, period ->
                val isSelected = idx == selectedIndex

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = { onSelected(period) }, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            text = period.label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) AccentGreen else Muted
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) AccentGreen else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}

// ============================================
// PODIUM (ADAPTIVE)
// ============================================

@Composable
private fun PodiumTop3Adaptive(top: List<RankingEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        when (top.size) {
            1 -> {
                Spacer(Modifier.width(110.dp))
                PodiumCard(entry = top[0], size = 108.dp, label = "#1", medal = "🥇", isWinner = true)
                Spacer(Modifier.width(110.dp))
            }

            2 -> {
                PodiumCard(entry = top[1], size = 76.dp, label = "#2", medal = "🥈", isWinner = false)
                PodiumCard(entry = top[0], size = 108.dp, label = "#1", medal = "🥇", isWinner = true)
                Spacer(Modifier.width(110.dp))
            }

            else -> {
                PodiumCard(entry = top[1], size = 76.dp, label = "#2", medal = "🥈", isWinner = false)
                PodiumCard(entry = top[0], size = 108.dp, label = "#1", medal = "🥇", isWinner = true)
                PodiumCard(entry = top[2], size = 76.dp, label = "#3", medal = "🥉", isWinner = false)
            }
        }
    }
}

@Composable
private fun PodiumCard(entry: RankingEntry, size: Dp, label: String, medal: String, isWinner: Boolean) {
    // ✅ Resaltar solo si es MI entrada, no por ser ganador
    val shouldHighlight = entry.isMe

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    when {
                        shouldHighlight -> AccentGreen.copy(alpha = 0.15f)
                        isWinner -> Color.Transparent
                        else -> IconButtonBg
                    }
                )
                .border(
                    width = when {
                        shouldHighlight -> 3.dp
                        isWinner -> 2.dp
                        else -> 1.dp
                    },
                    color = when {
                        shouldHighlight -> AccentGreen
                        isWinner -> AccentGreen.copy(alpha = 0.4f) // Más sutil para ganador
                        else -> CardStroke
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) { Text(text = medal, fontSize = if (isWinner) 44.sp else 32.sp) }

        Spacer(Modifier.height(10.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                shouldHighlight -> AccentGreen
                isWinner -> AccentGreen.copy(alpha = 0.8f)
                else -> Muted
            }
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (shouldHighlight) "Tú" else entry.name,
            fontSize = 14.sp,
            fontWeight = if (shouldHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (shouldHighlight) AccentGreen else White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "${formatPts(entry.points)} pts",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (shouldHighlight) AccentGreen.copy(alpha = 0.9f) else Muted
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

// ============================================
// ROW
// ============================================

@Composable
private fun RankingRow(entry: RankingEntry, modifier: Modifier = Modifier) {
    val backgroundColor = if (entry.isMe) AccentGreen.copy(alpha = 0.12f) else CardBg
    val borderColor = if (entry.isMe) AccentGreen.copy(alpha = 0.6f) else CardStroke
    val borderWidth = if (entry.isMe) 2.dp else 1.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = if (entry.isMe) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.position}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isMe) AccentGreen else Muted,
                modifier = Modifier.width(38.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (entry.isMe) AccentGreen.copy(alpha = 0.2f) else IconButtonBg)
                    .border(1.dp, if (entry.isMe) AccentGreen else CardStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry.isMe) "👤" else "🏋️",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontSize = 15.sp,
                    fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (entry.isMe) AccentGreen else White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (entry.isMe) "Tú" else "Competidor/a",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (entry.isMe) AccentGreen.copy(alpha = 0.8f) else Muted
                )
            }

            Text(
                text = "${formatPts(entry.points)} pts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isMe) AccentGreen else White
            )
        }
    }
}

// ============================================
// BOTTOM BAR
// ============================================

@Composable
private fun MyPositionBar(position: Int, points: Int, onViewDetails: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF000000),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Tu posición", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (position > 0) "#$position · ${formatPts(points)} pts" else "— · ${formatPts(points)} pts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Button(
                onClick = onViewDetails,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF000000)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(text = "Ver detalles", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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