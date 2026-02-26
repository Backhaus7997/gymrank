package com.example.gymrank.ui.screens.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close

// ---------------- MODELS ----------------

data class ExerciseSummary(
    val name: String,
    val reps: Int,
    val weightKg: Float?,
    val isBodyWeight: Boolean = false
)

data class FeedPost(
    val id: String,
    val ownerUid: String,
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
fun FeedScreen(
    vm: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onOpenWorkout: (ownerUid: String, workoutId: String) -> Unit = { _, _ -> } // ✅ NUEVO
) {
    var selectedTab by remember { mutableStateOf(FeedTab.PUBLIC) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        FeedTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        Spacer(Modifier.height(12.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        state.error?.let {
            Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == FeedTab.FRIENDS) {
                item {
                    FriendsHeader(
                        hasFriends = state.friendsUids.isNotEmpty(),
                        onSearch = { vm.search(it) },
                        results = state.searchResults,
                        onAdd = { vm.addFriend(it) }
                    )
                }
            }

            val posts = if (selectedTab == FeedTab.PUBLIC) state.publicPosts else state.friendsPosts

            if (posts.isEmpty()) {
                item {
                    if (selectedTab == FeedTab.FRIENDS && state.friendsUids.isEmpty()) {
                        EmptyFriendsState()
                    } else {
                        EmptyFeedState()
                    }
                }
            } else {
                items(posts) { post ->
                    FeedPostCard(
                        post = post,
                        canUnfollow = (selectedTab == FeedTab.FRIENDS),
                        onUnfollow = { vm.removeFriend(post.ownerUid) },
                        onOpen = { onOpenWorkout(post.ownerUid, post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👥", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text("Todavía no agregaste amigos", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Agregá amigos para ver sus entrenamientos acá.",
            color = DesignTokens.Colors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FriendsHeader(
    hasFriends: Boolean,
    onSearch: (String) -> Unit,
    results: List<Pair<String, com.example.gymrank.data.repository.FeedRepositoryFirestoreImpl.UserDoc>>,
    onAdd: (String) -> Unit
) {
    var q by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DesignTokens.Colors.SurfaceElevated)
            .padding(14.dp)
    ) {
        Text(
            if (hasFriends) "Amigos" else "Todavía no agregaste amigos",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(10.dp))

        // ✅ Search bar pro
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141A16),
            border = BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = DesignTokens.Colors.TextSecondary
                )

                Spacer(Modifier.width(10.dp))

                TextField(
                    value = q,
                    onValueChange = {
                        q = it
                        if (it.trim().length >= 2) onSearch(it.trim())
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Buscar por username",
                            color = DesignTokens.Colors.TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GymRankColors.PrimaryAccent,
                        focusedTextColor = GymRankColors.TextPrimary,
                        unfocusedTextColor = GymRankColors.TextPrimary
                    )
                )

                if (q.isNotBlank()) {
                    IconButton(
                        onClick = { q = "" },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = DesignTokens.Colors.TextSecondary
                        )
                    }
                }
            }
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            results.take(5).forEach { (uid, user) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(user.username, fontWeight = FontWeight.Medium)
                        Text(
                            user.experience,
                            color = DesignTokens.Colors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { onAdd(uid) },
                        colors = ButtonDefaults.buttonColors(containerColor = GymRankColors.PrimaryAccent)
                    ) {
                        Text("Agregar", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
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
private fun FeedPostCard(
    post: FeedPost,
    canUnfollow: Boolean,
    onUnfollow: () -> Unit,
    onOpen: () -> Unit
) {
    GlassCard(glow = true) {
        Column(Modifier.fillMaxWidth()) {

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

                    var menuOpen by remember { mutableStateOf(false) }
                    var confirmOpen by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = DesignTokens.Colors.TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            if (canUnfollow) {
                                DropdownMenuItem(
                                    text = { Text("Dejar de seguir") },
                                    onClick = {
                                        menuOpen = false
                                        confirmOpen = true
                                    }
                                )
                            }
                        }
                    }

                    if (confirmOpen) {
                        AlertDialog(
                            onDismissRequest = { confirmOpen = false },
                            title = { Text("Dejar de seguir") },
                            text = { Text("¿Querés dejar de seguir a ${post.userName}?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmOpen = false
                                    onUnfollow()
                                }) { Text("Sí, dejar de seguir") }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmOpen = false }) { Text("Cancelar") }
                            }
                        )
                    }
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onOpen() } // ✅ CLICK
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