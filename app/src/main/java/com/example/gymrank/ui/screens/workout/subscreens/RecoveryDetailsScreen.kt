@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.ui.screens.workout.MuscleRecoveryItem
import com.example.gymrank.ui.screens.workout.RecoveryEstimator
import com.example.gymrank.ui.screens.workout.WeeklyPlan
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun RecoveryDetailsScreen(
    onBack: () -> Unit
) {
    // ✅ colores consistentes con tu app
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    // ✅ workouts reales
    val workoutRepo = remember { WorkoutRepositoryFirestoreImpl() }
    val allWorkouts by remember { workoutRepo.getWorkouts() }
        .collectAsState(initial = emptyList())

    // ✅ plan real: users/{uid}/routinePlan/{mon..sun}
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val db = remember { FirebaseFirestore.getInstance() }

    val dayKeys = remember { listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun") }

    var weekPlanRaw by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isPlanLoading by remember { mutableStateOf(false) }
    var planError by remember { mutableStateOf<String?>(null) }

    fun normalizeMuscles(list: List<String>): List<String> =
        list.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }

    fun loadWeekPlan() {
        planError = null

        if (uid.isBlank()) {
            weekPlanRaw = emptyMap()
            planError = "No hay usuario logueado."
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
                    val muscles = (doc.get("muscles") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    map[key] = normalizeMuscles(muscles)
                }
                weekPlanRaw = map
                isPlanLoading = false
            }
            .addOnFailureListener { e ->
                isPlanLoading = false
                weekPlanRaw = emptyMap()
                planError = e.message ?: "Error cargando plan"
            }
    }

    // cargar plan al entrar / cuando cambia uid
    LaunchedEffect(uid) { loadWeekPlan() }

    // ✅ Adaptador: dayKey -> Calendar.DAY_OF_WEEK
    val weeklyPlan: WeeklyPlan? = remember(weekPlanRaw) {
        if (weekPlanRaw.isEmpty()) return@remember null

        val map = mutableMapOf<Int, List<String>>()
        dayKeys.forEach { key ->
            val muscles = weekPlanRaw[key].orEmpty()
            val dow = when (key) {
                "mon" -> Calendar.MONDAY
                "tue" -> Calendar.TUESDAY
                "wed" -> Calendar.WEDNESDAY
                "thu" -> Calendar.THURSDAY
                "fri" -> Calendar.FRIDAY
                "sat" -> Calendar.SATURDAY
                "sun" -> Calendar.SUNDAY
                else -> null
            }
            if (dow != null) map[dow] = muscles
        }

        WeeklyPlan(musclesByDayOfWeek = map)
    }

    val items: List<MuscleRecoveryItem> = remember(allWorkouts, weeklyPlan) {
        RecoveryEstimator.estimate(allWorkouts, weeklyPlan)
    }

    val fmt = remember { SimpleDateFormat("dd MMM", Locale("es", "AR")) }
    fun millisToLabel(millis: Long?): String =
        if (millis == null || millis <= 0L) "—" else fmt.format(Date(millis))

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recuperación",
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Basado en tu plan semanal (Home) + historial real",
                color = textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))

            if (isPlanLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = input
                )
                Spacer(Modifier.height(10.dp))
            }

            if (planError != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = input,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = planError ?: "Error",
                        color = GymRankColors.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (items.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = input,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sin datos todavía.",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.name }) { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = surface,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.10f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Próx: ${millisToLabel(item.nextDueMillis)} • Falta: ${item.daysLeft}d • Frec: ${item.freqDays}d",
                                        color = textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    color = input,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
                                ) {
                                    Text(
                                        text = "${item.percent}%",
                                        color = accent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(item.progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(accent.copy(alpha = 0.75f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}