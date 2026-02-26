package com.example.gymrank.ui.screens.feed.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymrank.ui.screens.feed.ExerciseSummary
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val DEFAULT_WORKOUT_COVER =
    "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=60"

data class WorkoutDetailUi(
    val title: String = "",
    val imageUrl: String? = null,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val intensity: String? = null,
    val muscles: List<String> = emptyList(),
    val notes: String? = null,
    val type: String? = null,
    val exercises: List<ExerciseSummary> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    ownerUid: String,
    workoutId: String,
    onBack: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<WorkoutDetailUi?>(null) }

    LaunchedEffect(ownerUid, workoutId) {
        loading = true
        error = null
        data = null

        runCatching {
            val db = FirebaseFirestore.getInstance()
            val snap = db.collection("users")
                .document(ownerUid)
                .collection("workouts")
                .document(workoutId)
                .get()
                .await()

            if (!snap.exists()) error("Entrenamiento no encontrado")

            val title = snap.getString("title").orEmpty()
            val imageUrl = snap.getString("imageUrl") ?: DEFAULT_WORKOUT_COVER

            val description = snap.getString("description")
            val durationMinutes = snap.getLong("durationMinutes")?.toInt()
            val intensity = snap.getString("intensity")
            val muscles =
                (snap.get("muscles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val notes = snap.getString("notes")
            val type = snap.getString("type")

            val exRaw = snap.get("exercises") as? List<*>
            val exercises: List<ExerciseSummary> = exRaw?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                ExerciseSummary(
                    name = (m["name"] as? String).orEmpty(),
                    reps = (m["reps"] as? Number)?.toInt() ?: 0,
                    weightKg = (m["weightKg"] as? Number)?.toFloat(),
                    isBodyWeight = (m["usesBodyweight"] as? Boolean) == true
                )
            } ?: emptyList()

            data = WorkoutDetailUi(
                title = title,
                imageUrl = imageUrl,
                description = description,
                durationMinutes = durationMinutes,
                intensity = intensity,
                muscles = muscles,
                notes = notes,
                type = type,
                exercises = exercises
            )
        }.onFailure {
            error = it.message ?: "Error cargando entrenamiento"
        }

        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null -> Text(
                    error!!,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(16.dp)
                )

                data != null -> {
                    val w = data!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        HeroHeader(w)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            w.durationMinutes?.let { InfoChip(text = "$it min") }
                            w.intensity?.takeIf { it.isNotBlank() }?.let { InfoChip(text = it) }
                            w.type?.takeIf { it.isNotBlank() }?.let { InfoChip(text = it) }
                        }

                        if (w.muscles.isNotEmpty() || !w.description.isNullOrBlank()) {
                            SectionCard(title = "Detalles") {
                                if (w.muscles.isNotEmpty()) {
                                    Text(
                                        "Músculos: ${w.muscles.joinToString(", ")}",
                                        color = DesignTokens.Colors.TextSecondary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (!w.description.isNullOrBlank()) {
                                    Text(
                                        w.description!!,
                                        color = DesignTokens.Colors.TextSecondary
                                    )
                                }
                            }
                        }

                        SectionCard(
                            title = "Ejercicios",
                            subtitle = if (w.exercises.isNotEmpty()) "${w.exercises.size} en total" else null
                        ) {
                            if (w.exercises.isEmpty()) {
                                Text(
                                    "No hay ejercicios cargados.",
                                    color = DesignTokens.Colors.TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    w.exercises.forEach { ex ->
                                        ExerciseItem(ex) // ✅ acá se ve mejor reps/peso
                                    }
                                }
                            }
                        }

                        if (!w.notes.isNullOrBlank()) {
                            SectionCard(title = "Notas") {
                                Text(w.notes!!, color = DesignTokens.Colors.TextSecondary)
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(w: WorkoutDetailUi) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(shape)
            .background(DesignTokens.Colors.SurfaceElevated)
    ) {
        val coverUrl = w.imageUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_WORKOUT_COVER

        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC000000)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                w.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = GymRankColors.TextPrimary
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF141A16),
        border = BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.35f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = GymRankColors.PrimaryAccent,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DesignTokens.Colors.SurfaceElevated)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        color = DesignTokens.Colors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        content()
    }
}

/**
 * ✅ MEJORA: a la derecha muestra “Reps” y “Peso” en dos líneas, bien alineado.
 */
@Composable
private fun ExerciseItem(ex: ExerciseSummary) {
    val repsText = "${ex.reps}"
    val weightText = when {
        ex.isBodyWeight -> "BW"
        ex.weightKg == null -> "—"
        else -> "${ex.weightKg.toInt()} kg"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141A16))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Izquierda: ícono + nombre
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = GymRankColors.PrimaryAccent.copy(alpha = 0.9f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                ex.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Derecha: tabla mini “Reps / Peso”
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reps",
                    color = DesignTokens.Colors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    repsText,
                    fontWeight = FontWeight.SemiBold,
                    color = GymRankColors.TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Peso",
                    color = DesignTokens.Colors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    weightText,
                    fontWeight = FontWeight.SemiBold,
                    color = GymRankColors.TextPrimary
                )
            }
        }
    }
}