package com.example.gymrank.ui.screens.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import androidx.compose.foundation.clickable


// ---------------- MODELS ----------------

data class ExerciseSummary(
    val name: String,
    val reps: Int,
    val weightKg: Float?,
    val isBodyWeight: Boolean = false
)

data class FeedPost(
    val id: String,
    val userName: String,
    val avatarUrl: String,
    val level: Int,
    val workoutTitle: String,
    val workoutImageUrl: String,
    val visibility: String,
    val timestampLabel: String,
    val exercises: List<ExerciseSummary>
)

enum class FeedTab { FRIENDS, PUBLIC }

// ---------------- SCREEN ----------------

@Composable
fun FeedScreen() {
    var selectedTab by remember { mutableStateOf(FeedTab.PUBLIC) }
    val posts = remember { samplePosts() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        FeedTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val filtered = posts.filter {
                it.visibility == if (selectedTab == FeedTab.PUBLIC) "Público" else "Amigos"
            }

            if (filtered.isEmpty()) {
                item { EmptyFeedState() }
            } else {
                items(filtered) { post ->
                    FeedPostCard(post)
                }
            }
        }
    }
}

// ---------------- TABS ----------------

@Composable
private fun FeedTabs(selectedTab: FeedTab, onTabSelected: (FeedTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabButton("Amigos", selectedTab == FeedTab.FRIENDS) {
            onTabSelected(FeedTab.FRIENDS)
        }
        TabButton("Público", selectedTab == FeedTab.PUBLIC) {
            onTabSelected(FeedTab.PUBLIC)
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = if (selected) GymRankColors.TextPrimary else DesignTokens.Colors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .clickable { onClick() }
        )
        Box(
            Modifier
                .height(3.dp)
                .width(64.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) GymRankColors.PrimaryAccent else Color.Transparent)
        )
    }
}

// ---------------- CARD ----------------

@Composable
private fun FeedPostCard(post: FeedPost) {
    GlassCard(glow = true) {
        Column(Modifier.fillMaxWidth()) {

            // 🔥 Cover image
            AsyncImage(
                model = post.workoutImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            Column(Modifier.padding(16.dp)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = post.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(post.userName, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            LevelBadge(post.level)
                        }
                        Text(
                            "Primeros pasos",
                            color = DesignTokens.Colors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = DesignTokens.Colors.TextSecondary
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    post.workoutTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(10.dp))

                post.exercises.take(3).forEach {
                    ExerciseRow(it)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    "Ver entrenamiento completo →",
                    color = GymRankColors.PrimaryAccent,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "${post.visibility} • ${post.timestampLabel}",
                    color = DesignTokens.Colors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ---------------- SUB COMPONENTS ----------------

@Composable
private fun LevelBadge(level: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF1C2A1C),
        border = BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.4f))
    ) {
        Text(
            "Nivel $level",
            color = GymRankColors.PrimaryAccent,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ExerciseRow(ex: ExerciseSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DesignTokens.Colors.SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(ex.name, Modifier.weight(1f))
        val right = when {
            ex.isBodyWeight -> "Peso corporal"
            ex.weightKg == null -> "${ex.reps}"
            else -> "${ex.reps} · ${ex.weightKg.toInt()} kg"
        }
        Text(right, color = DesignTokens.Colors.TextSecondary)
    }
}

@Composable
private fun EmptyFeedState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏋️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("No hay entrenamientos todavía", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Seguís a personas que aún no entrenaron",
            color = DesignTokens.Colors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- MOCK DATA ----------------

// Mock data (en español)
private fun samplePosts(): List<FeedPost> = listOf(
    FeedPost(
        id = "1",
        userName = "brandedlifte16",
        level = 40,
        workoutTitle = "Pecho y abdominales",
        visibility = "Amigos",
        timestampLabel = "Recién",
        exercises = listOf(
            ExerciseSummary("Aperturas en máquina para pecho", 12, 40f),
            ExerciseSummary("Aperturas inclinadas con mancuernas", 10, 16f),
            ExerciseSummary("Flexiones diamante", 15, null, isBodyWeight = true)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=12",
        workoutImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=60"
    ),
    FeedPost(
        id = "2",
        userName = "Sonjinwoo17",
        level = 21,
        workoutTitle = "Hombros y brazos",
        visibility = "Amigos",
        timestampLabel = "Recién",
        exercises = listOf(
            ExerciseSummary("Press militar con barra", 8, 50f),
            ExerciseSummary("Elevaciones laterales", 12, 10f),
            ExerciseSummary("Curl en polea", 12, 25f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=33",
        workoutImageUrl = "https://images.pexels.com/photos/2247179/pexels-photo-2247179.jpeg"
    ),
    FeedPost(
        id = "3",
        userName = "Marcos",
        level = 12,
        workoutTitle = "Piernas",
        visibility = "Amigos",
        timestampLabel = "Ayer",
        exercises = listOf(
            ExerciseSummary("Sentadilla", 5, 120f),
            ExerciseSummary("Prensa", 12, 200f),
            ExerciseSummary("Curl femoral", 12, 35f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=7",
        workoutImageUrl = "https://images.unsplash.com/photo-1517963879433-6ad2b056d712?auto=format&fit=crop&w=1200&q=60"
    ),

    FeedPost(
        id = "4",
        userName = "luisa.fit",
        level = 18,
        workoutTitle = "Espalda y bíceps",
        visibility = "Amigos",
        timestampLabel = "Hace 10 min",
        exercises = listOf(
            ExerciseSummary("Jalón al pecho", 12, 55f),
            ExerciseSummary("Remo con mancuerna", 10, 32f),
            ExerciseSummary("Curl martillo", 12, 14f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=47",
        workoutImageUrl = "https://images.pexels.com/photos/2261485/pexels-photo-2261485.jpeg"
    ),
    FeedPost(
        id = "5",
        userName = "lucas.fit",
        level = 21,
        workoutTitle = "Push day",
        visibility = "Amigos",
        timestampLabel = "Hace 1 h",
        exercises = listOf(
            ExerciseSummary("Press inclinado", 10, 70f),
            ExerciseSummary("Fondos en paralelas", 12, null, isBodyWeight = true),
            ExerciseSummary("Extensión de tríceps", 12, 35f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=12",
        workoutImageUrl = "https://images.pexels.com/photos/4164761/pexels-photo-4164761.jpeg"
    ),
    FeedPost(
        id = "6",
        userName = "agus.strength",
        level = 41,
        workoutTitle = "Piernas pesado",
        visibility = "Público",
        timestampLabel = "Hace 3 h",
        exercises = listOf(
            ExerciseSummary("Sentadilla trasera", 5, 160f),
            ExerciseSummary("Prensa", 10, 280f),
            ExerciseSummary("Curl femoral", 12, 55f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=47",
        workoutImageUrl = "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg"
    ),
    FeedPost(
        id = "7",
        userName = "vale.runner",
        level = 18,
        workoutTitle = "Core + cardio",
        visibility = "Público",
        timestampLabel = "Hace 6 h",
        exercises = listOf(
            ExerciseSummary("Plancha", 45, null, isBodyWeight = true),
            ExerciseSummary("Crunch abdominal", 20, null, isBodyWeight = true),
            ExerciseSummary("Mountain climbers", 30, null, isBodyWeight = true)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=32",
        workoutImageUrl = "https://images.pexels.com/photos/3757376/pexels-photo-3757376.jpeg"
    ),
    FeedPost(
        id = "8",
        userName = "nico.powerlift",
        level = 55,
        workoutTitle = "Upper strength",
        visibility = "Solo amigos",
        timestampLabel = "Ayer",
        exercises = listOf(
            ExerciseSummary("Press banca", 3, 145f),
            ExerciseSummary("Remo con barra", 6, 120f),
            ExerciseSummary("Press militar", 5, 85f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=8",
        workoutImageUrl = "https://images.pexels.com/photos/2261485/pexels-photo-2261485.jpeg"
    ),
    FeedPost(
        id = "9",
        userName = "sofi.cross",
        level = 27,
        workoutTitle = "Metcon rápido",
        visibility = "Público",
        timestampLabel = "Hace 2 días",
        exercises = listOf(
            ExerciseSummary("Thrusters", 15, 40f),
            ExerciseSummary("Burpees", 20, null, isBodyWeight = true),
            ExerciseSummary("Wall balls", 25, 9f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=19",
        workoutImageUrl = "https://images.pexels.com/photos/841130/pexels-photo-841130.jpeg"
    ),
    FeedPost(
        id = "11",
        userName = "martin.gym",
        level = 38,
        workoutTitle = "Espalda + bíceps",
        visibility = "Público",
        timestampLabel = "Hace 4 días",
        exercises = listOf(
            ExerciseSummary("Dominadas", 8, null, isBodyWeight = true),
            ExerciseSummary("Jalón al pecho", 10, 75f),
            ExerciseSummary("Curl con barra", 10, 45f)
        ),
        avatarUrl = "https://i.pravatar.cc/128?img=5",
        workoutImageUrl = "https://images.pexels.com/photos/1552106/pexels-photo-1552106.jpeg"
    )
)
