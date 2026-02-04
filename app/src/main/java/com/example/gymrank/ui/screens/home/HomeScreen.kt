package com.example.gymrank.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.ui.components.GradientBackground
import com.example.gymrank.ui.components.PrimaryButton
import com.example.gymrank.ui.session.SessionViewModel

@Composable
fun HomeScreen(
    sessionViewModel: SessionViewModel,
    viewModel: HomeViewModel = viewModel(),
    onOpenRanking: () -> Unit = {},
    onSelectGym: () -> Unit = {},
    onLogWorkout: () -> Unit = {},
    onLogPR: () -> Unit = {},
    onViewProgress: () -> Unit = {},
    onInviteFriend: () -> Unit = {}
) {
    val selectedGym by sessionViewModel.selectedGym.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Actualizar ViewModel con gym si existe
    selectedGym?.let { gym ->
        viewModel.setGymData(gym)
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            HomeTopBar(
                userName = uiState.userName
            )
        }
    ) { paddingValues ->
        GradientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Hero Card
                item {
                    if (uiState.hasGym && uiState.gymName != null) {
                        HeroCardWithGym(
                            gymName = uiState.gymName!!,
                            location = uiState.gymLocation ?: "",
                            ranking = uiState.currentRanking ?: 0,
                            points = uiState.currentPoints ?: 0,
                            onOpenRanking = onOpenRanking,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        HeroCardNoGym(
                            onSelectGym = onSelectGym,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // Desafíos Section
                item {
                    SectionTitle(
                        title = "Desafíos",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.challenges) { challenge ->
                            ChallengeCard(challenge = challenge)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // Acciones rápidas Section
                item {
                    SectionTitle(
                        title = "Acciones rápidas",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    QuickActionsGrid(
                        onLogWorkout = onLogWorkout,
                        onLogPR = onLogPR,
                        onViewProgress = onViewProgress,
                        onInviteFriend = onInviteFriend,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// COMPOSABLES
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    userName: String
) {
    TopAppBar(
        title = {
            Text(
                text = "Hola 👋",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )
        },
        actions = {
            // Notification icon
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color(0xFFFFFFFF)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color(0xFF2C2C2E), CircleShape)
                    .clickable { /* TODO: Profile */ },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "A",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0FD3E)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF000000)
        )
    )
}

@Composable
private fun HeroCardWithGym(
    gymName: String,
    location: String,
    ranking: Int,
    points: Int,
    onOpenRanking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
        ) {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1C1C1E),
                                Color(0xFF0B0F0E)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Gym info
                Text(
                    text = gymName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Ranking
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Posición",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#$ranking",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD0FD3E)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${points.toString().reversed().chunked(3).joinToString(".").reversed()} pts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Ver ranking",
                    onClick = onOpenRanking
                )
            }
        }
    }
}

@Composable
private fun HeroCardNoGym(
    onSelectGym: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏋️",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Todavía no elegiste tu gimnasio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Elegí tu gym para empezar a competir",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Elegir gimnasio",
                    onClick = onSelectGym
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFFFFF),
        modifier = modifier
    )
}

@Composable
private fun ChallengeCard(
    challenge: ChallengeCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = challenge.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = challenge.subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    Text(
                        text = challenge.emoji,
                        fontSize = 32.sp
                    )
                }

                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                        Text(
                            text = "${(challenge.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0FD3E)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2C2C2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(challenge.progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFD0FD3E))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onLogWorkout: () -> Unit,
    onLogPR: () -> Unit,
    onViewProgress: () -> Unit,
    onInviteFriend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple(QuickAction.LogWorkout, onLogWorkout, Color(0xFFD0FD3E)),
        Triple(QuickAction.LogPR, onLogPR, Color(0xFFFFD700)),
        Triple(QuickAction.ViewProgress, onViewProgress, Color(0xFF00D4FF)),
        Triple(QuickAction.InviteFriend, onInviteFriend, Color(0xFFFF6B9D))
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.take(2).forEach { (action, onClick, accentColor) ->
                QuickActionCard(
                    action = action,
                    onClick = onClick,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.drop(2).forEach { (action, onClick, accentColor) ->
                QuickActionCard(
                    action = action,
                    onClick = onClick,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = action.icon,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = action.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
