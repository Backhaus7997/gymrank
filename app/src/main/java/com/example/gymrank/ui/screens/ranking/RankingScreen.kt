package com.example.gymrank.ui.screens.ranking

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// MODELS
// ============================================

enum class RankingPeriod(val label: String) {
    Weekly("Semanal"),
    Monthly("Mensual"),
    AllTime("Historial")
}

data class RankingEntry(
    val position: Int,
    val name: String,
    val points: Int,
    val isMe: Boolean = false
)

// ============================================
// MAIN SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    gymName: String = "Iron Temple",
    gymLocation: String = "Buenos Aires, AR",
    myPosition: Int = 14,
    myPoints: Int = 6240,
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onViewDetails: () -> Unit = {}
) {
    // Mock data
    val weekly = remember {
        listOf(
            RankingEntry(1, "Mateo Rossi", 2450),
            RankingEntry(2, "Sofía Hernández", 2100),
            RankingEntry(3, "Lucas González", 1850),
            RankingEntry(4, "Valentina Solís", 1640),
            RankingEntry(5, "Joaquín Paz", 1520),
            RankingEntry(6, "Micaela Suárez", 1450),
            RankingEntry(7, "Tomás Herrera", 1320),
            RankingEntry(8, "Camila Vargas", 1280),
            RankingEntry(9, "Franco Díaz", 1150),
            RankingEntry(10, "Martina López", 1090),
            RankingEntry(11, "Agustín Morales", 980),
            RankingEntry(12, "Luciana Castro", 920),
            RankingEntry(13, "Nicolás Méndez", 860),
            RankingEntry(14, "Vos", 810, isMe = true),
            RankingEntry(15, "Paula Ramírez", 750),
            RankingEntry(16, "Santiago Torres", 690)
        )
    }

    val monthly = remember {
        weekly.map { it.copy(points = it.points * 3) }
    }

    val allTime = remember {
        weekly.map { it.copy(points = it.points * 10) }
    }

    var selectedPeriod by remember { mutableStateOf(RankingPeriod.Weekly) }

    val entries = remember(selectedPeriod) {
        when (selectedPeriod) {
            RankingPeriod.Weekly -> weekly
            RankingPeriod.Monthly -> monthly
            RankingPeriod.AllTime -> allTime
        }
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            RankingTopBar(
                gymName = gymName,
                gymLocation = gymLocation,
                onBack = onBack,
                onSearch = onSearch,
                onNotifications = onNotifications
            )
        },
        bottomBar = {
            MyPositionBar(
                position = myPosition,
                points = myPoints,
                onViewDetails = onViewDetails
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(padding)
        ) {
            // Tabs
            PeriodTabs(
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it }
            )

            Spacer(Modifier.height(16.dp))

            // Top 3 podium
            val top3 = entries.filter { it.position in 1..3 }.sortedBy { it.position }
            if (top3.size == 3) {
                PodiumTop3(
                    first = top3[0],
                    second = top3[1],
                    third = top3[2]
                )
            }

            Spacer(Modifier.height(20.dp))

            // Resto del ranking
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries.filter { it.position >= 4 }) { entry ->
                    RankingRow(
                        entry = entry,
                        highlight = entry.isMe
                    )
                }

                // Espacio para el bottom bar
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ============================================
// TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingTopBar(
    gymName: String,
    gymLocation: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = gymName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Location chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2C2C2E),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = gymLocation,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFFFFFFFF)
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFFFFFFFF)
                )
            }
            IconButton(onClick = onNotifications) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color(0xFFFFFFFF)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF000000)
        )
    )
}

// ============================================
// TABS
// ============================================

@Composable
private fun PeriodTabs(
    selected: RankingPeriod,
    onSelected: (RankingPeriod) -> Unit
) {
    val items = RankingPeriod.entries

    TabRow(
        selectedTabIndex = items.indexOf(selected),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        containerColor = Color.Transparent,
        contentColor = Color(0xFFD0FD3E),
        indicator = {},
        divider = {
            HorizontalDivider(
                color = Color(0xFF38383A),
                thickness = 1.dp
            )
        }
    ) {
        items.forEach { period ->
            Tab(
                selected = period == selected,
                onClick = { onSelected(period) },
                text = {
                    Column {
                        Text(
                            text = period.label,
                            fontWeight = if (period == selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        if (period == selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color(0xFFD0FD3E))
                            )
                        }
                    }
                },
                selectedContentColor = Color(0xFFD0FD3E),
                unselectedContentColor = Color(0xFF8E8E93)
            )
        }
    }
}

// ============================================
// PODIUM TOP 3
// ============================================

@Composable
private fun PodiumTop3(
    first: RankingEntry,
    second: RankingEntry,
    third: RankingEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // #2
        PodiumCard(
            entry = second,
            size = 80.dp,
            label = "#2",
            emoji = "🥈"
        )

        Spacer(Modifier.width(8.dp))

        // #1 (winner - más grande)
        PodiumCard(
            entry = first,
            size = 110.dp,
            label = "#1",
            emoji = "🥇",
            isWinner = true
        )

        Spacer(Modifier.width(8.dp))

        // #3
        PodiumCard(
            entry = third,
            size = 80.dp,
            label = "#3",
            emoji = "🥉"
        )
    }
}

@Composable
private fun PodiumCard(
    entry: RankingEntry,
    size: Dp,
    label: String,
    emoji: String,
    isWinner: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar con emoji
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isWinner)
                        Color(0xFFD0FD3E).copy(alpha = 0.20f)
                    else
                        Color(0xFF2C2C2E)
                )
                .border(
                    width = if (isWinner) 2.dp else 1.dp,
                    color = if (isWinner) Color(0xFFD0FD3E) else Color(0xFF38383A),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = if (isWinner) 42.sp else 32.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        // Posición
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWinner) Color(0xFFD0FD3E) else Color(0xFF8E8E93)
        )

        Spacer(Modifier.height(4.dp))

        // Nombre
        Text(
            text = entry.name,
            fontSize = 14.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold,
            color = Color(0xFFFFFFFF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Puntos
        Text(
            text = "${entry.points} pts",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8E8E93)
        )
    }
}

// ============================================
// RANKING ROW (lista #4+)
// ============================================

@Composable
private fun RankingRow(
    entry: RankingEntry,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (highlight)
        Color(0xFFD0FD3E).copy(alpha = 0.15f)
    else
        Color(0xFF1C1C1E)

    val borderColor = if (highlight)
        Color(0xFFD0FD3E).copy(alpha = 0.5f)
    else
        Color(0xFF2C2C2E)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición
            Text(
                text = "#${entry.position}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFD0FD3E) else Color(0xFFFFFFFF),
                modifier = Modifier.width(42.dp)
            )

            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, Color(0xFF38383A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry.isMe) "🎯" else "🏋️",
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Nombre + subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.name,
                    fontSize = 15.sp,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Competidor/a",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93)
                )
            }

            // Puntos
            Text(
                text = "${entry.points} pts",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFD0FD3E) else Color(0xFFFFFFFF)
            )
        }
    }
}

// ============================================
// BOTTOM BAR (mi posición)
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
        color = Color(0xFF1C1C1E),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu posición",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "#$position · ${points.toString().reversed().chunked(3).joinToString(".").reversed()} pts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
            }

            Button(
                onClick = onViewDetails,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0FD3E),
                    contentColor = Color(0xFF000000)
                ),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Ver detalles",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
