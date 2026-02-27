package com.example.gymrank.ui.screens.feed.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.gymrank.ui.screens.feed.ExerciseSummary
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

// ---------------- UI MODEL (lista) ----------------

private data class WorkoutListItemUi(
    val id: String,
    val title: String,
    val timestampMillis: Long?,
    val exercises: List<ExerciseSummary>
)

// ---------------- HELPERS ----------------

private fun relativeTime(ts: Long?): String {
    if (ts == null) return "—"
    val now = System.currentTimeMillis()
    val diff = (now - ts).coerceAtLeast(0L)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Recién"
        minutes < 60 -> "Hace ${minutes} min"
        hours < 24 -> "Hace ${hours} h"
        else -> "Hace ${days} d"
    }
}

private fun parseExercises(exRaw: Any?): List<ExerciseSummary> {
    val list = exRaw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        ExerciseSummary(
            name = (m["name"] as? String).orEmpty(),
            reps = (m["reps"] as? Number)?.toInt() ?: 0,
            weightKg = (m["weightKg"] as? Number)?.toFloat(),
            isBodyWeight = (m["usesBodyweight"] as? Boolean) == true
        )
    }
}

// ---------------- SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    ownerUid: String,
    workoutId: String, // se mantiene por compatibilidad con tu nav (no se usa)
    onBack: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<WorkoutListItemUi>>(emptyList()) }

    LaunchedEffect(ownerUid) {
        loading = true
        error = null
        items = emptyList()

        runCatching {
            val db = FirebaseFirestore.getInstance()

            // ✅ últimos 5 entrenamientos del usuario
            val snaps = db.collection("users")
                .document(ownerUid)
                .collection("workouts")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            val mapped = snaps.documents.map { doc ->
                WorkoutListItemUi(
                    id = doc.id,
                    title = doc.getString("title").orEmpty().ifBlank { "Entrenamiento" },
                    timestampMillis = when (val v = doc.get("createdAt")) {
                        is com.google.firebase.Timestamp -> v.toDate().time
                        is Number -> v.toLong()
                        else -> null
                    },
                    exercises = parseExercises(doc.get("exercises"))
                )
            }

            items = mapped
        }.onFailure {
            error = it.message ?: "Error cargando entrenamientos"
        }

        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamientos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // ✅ mejor en celu (evita quedar pegado a notch / nav bar)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                error != null -> Text(
                    text = error!!,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(16.dp)
                )

                items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No hay entrenamientos", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.padding(6.dp))
                        Text(
                            "Todavía no se registraron entrenos para este usuario.",
                            color = DesignTokens.Colors.TextSecondary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { it.id }) { w ->
                            WorkoutCompactCard(item = w)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CARD ----------------

@Composable
private fun WorkoutCompactCard(item: WorkoutListItemUi) {
    val shape = RoundedCornerShape(18.dp)

    // ✅ más sólido (menos “vidrio”) para que en celular se lea bien
    val cardBg = Color(0xFF101614) // oscuro, tipo la referencia
    val rowBg = Color(0xFF141A16)  // un toque más claro

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(
                BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.18f)),
                shape = shape
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val show = item.exercises.take(4) // ✅ compacto (en la card)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                show.forEach { ex ->
                    ExerciseRowCompact(ex = ex, rowBg = rowBg)
                }
            }

            Text(
                text = relativeTime(item.timestampMillis),
                color = DesignTokens.Colors.TextSecondary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ExerciseRowCompact(
    ex: ExerciseSummary,
    rowBg: Color
) {
    val shape = RoundedCornerShape(14.dp)

    val weightText = when {
        ex.isBodyWeight -> "BW"
        ex.weightKg == null -> "0 kg"
        else -> "${ex.weightKg.toInt()} kg"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = rowBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = GymRankColors.PrimaryAccent.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.size(10.dp))

            Text(
                text = ex.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.size(10.dp))

            // ✅ formato como la referencia: "reps • peso"
            Text(
                text = "${ex.reps} • $weightText",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}