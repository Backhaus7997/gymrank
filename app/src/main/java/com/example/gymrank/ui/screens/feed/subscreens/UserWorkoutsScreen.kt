package com.example.gymrank.ui.screens.feed.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

private const val DEFAULT_WORKOUT_COVER =
    "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=60"

data class UserWorkoutItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val whenLabel: String,
    val exercisesCount: Int,
    val createdAtMillis: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWorkoutsScreen(
    ownerUid: String,
    onBack: () -> Unit,
    onOpenWorkoutDetail: (workoutId: String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<UserWorkoutItem>>(emptyList()) }

    LaunchedEffect(ownerUid) {
        loading = true
        error = null
        items = emptyList()

        runCatching {
            val db = FirebaseFirestore.getInstance()

            // Intentamos query con orderBy si existe (createdAt / updatedAt).
            // Si falla por índice/campo inexistente, hacemos fallback sin order y ordenamos client-side.
            val snaps = try {
                db.collection("users")
                    .document(ownerUid)
                    .collection("workouts")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (_: Throwable) {
                db.collection("users")
                    .document(ownerUid)
                    .collection("workouts")
                    .get()
                    .await()
            }

            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val list = snaps.documents.map { doc ->
                val title = doc.getString("title").orEmpty().ifBlank { "Entrenamiento" }
                val imageUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() } ?: DEFAULT_WORKOUT_COVER

                val createdAtMillis = when (val v = doc.get("createdAt")) {
                    is Timestamp -> v.toDate().time
                    is Long -> v
                    is Double -> v.toLong()
                    is Int -> v.toLong()
                    else -> null
                } ?: when (val v = doc.get("updatedAt")) {
                    is Timestamp -> v.toDate().time
                    is Long -> v
                    is Double -> v.toLong()
                    is Int -> v.toLong()
                    else -> null
                } ?: doc.getLong("timestampMillis")

                val whenLabel = createdAtMillis?.let { df.format(Date(it)) } ?: "Sin fecha"

                val exRaw = doc.get("exercises") as? List<*>
                val exCount = exRaw?.size ?: 0

                UserWorkoutItem(
                    id = doc.id,
                    title = title,
                    imageUrl = imageUrl,
                    whenLabel = whenLabel,
                    exercisesCount = exCount,
                    createdAtMillis = createdAtMillis ?: 0L
                )
            }.sortedByDescending { it.whenLabel } // si no hay millis, al menos no rompe

            items = list
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
                items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏋️", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(10.dp))
                        Text("Este usuario todavía no cargó entrenamientos", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cuando cargue alguno, van a aparecer todos acá juntos.",
                            color = DesignTokens.Colors.TextSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items) { w ->
                            WorkoutListCard(
                                item = w,
                                onOpen = { onOpenWorkoutDetail(w.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutListCard(
    item: UserWorkoutItem,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DesignTokens.Colors.SurfaceElevated)
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Column(Modifier.padding(14.dp)) {
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "${item.exercisesCount} ejercicios • ${item.whenLabel}",
                color = DesignTokens.Colors.TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141A16))
                    .clickable { onOpen() }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = GymRankColors.PrimaryAccent.copy(alpha = 0.9f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Abrir detalle",
                    fontWeight = FontWeight.SemiBold,
                    color = GymRankColors.TextPrimary
                )
            }
        }
    }
}