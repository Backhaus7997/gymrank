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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
// ROUTES
// ============================================

object RankingRoutes {
    // ranking_details/{gymName}/{gymLocation}/{periodLabel}/{myPosition}/{myPoints}
    const val Details = "ranking_details/{gymName}/{gymLocation}/{periodLabel}/{myPosition}/{myPoints}"

    fun detailsRoute(
        gymName: String,
        gymLocation: String,
        periodLabel: String,
        myPosition: Int,
        myPoints: Int
    ): String {
        fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.toString())
        return "ranking_details/${enc(gymName)}/${enc(gymLocation)}/${enc(periodLabel)}/$myPosition/$myPoints"
    }

    // Si lo querés copiar al NavHost:
    val detailsArgs = listOf(
        navArgument("gymName") { type = NavType.StringType },
        navArgument("gymLocation") { type = NavType.StringType },
        navArgument("periodLabel") { type = NavType.StringType },
        navArgument("myPosition") { type = NavType.IntType },
        navArgument("myPoints") { type = NavType.IntType }
    )
}

// ============================================
// THEME COLORS (match screenshot vibe)
// ============================================

private val ScreenBg = Color(0xFF070B0A)          // very dark green/black
private val CardBg = Color(0xFF0F1412)            // list rows
private val CardStroke = Color(0xFF1E2622)        // subtle border
private val Muted = Color(0xFF8E8E93)
private val White = Color(0xFFFFFFFF)
private val AccentGreen = Color(0xFF32E37A)       // neon green
private val IconButtonBg = Color(0xFF151A18)      // circular icon bg
private val Divider = Color(0xFF1C2420)

// ============================================
// MAIN SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    navController: NavController,
    gymName: String = "Iron Temple",
    gymLocation: String = "Buenos Aires, AR",
    myPosition: Int = 14,
    myPoints: Int = 6240,
    onBack: () -> Unit = { navController.popBackStack() },
    onSearch: () -> Unit = {},
    onNotifications: () -> Unit = {}
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
            RankingEntry(8, "Agustín Rivas", 1280),
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

    val monthly = remember { weekly.map { it.copy(points = it.points * 3) } }
    val allTime = remember { weekly.map { it.copy(points = it.points * 10) } }

    var selectedPeriod by remember { mutableStateOf(RankingPeriod.Weekly) }

    val entries = remember(selectedPeriod) {
        when (selectedPeriod) {
            RankingPeriod.Weekly -> weekly
            RankingPeriod.Monthly -> monthly
            RankingPeriod.AllTime -> allTime
        }
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
                position = myPosition,
                points = myPoints,
                onViewDetails = {
                    navController.navigate(
                        RankingRoutes.detailsRoute(
                            gymName = gymName,
                            gymLocation = gymLocation,
                            periodLabel = selectedPeriod.label,
                            myPosition = myPosition,
                            myPoints = myPoints
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

            val top3 = entries.filter { it.position in 1..3 }.sortedBy { it.position }
            if (top3.size == 3) {
                PodiumTop3(
                    first = top3[0],
                    second = top3[1],
                    third = top3[2]
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries.filter { it.position >= 4 && !it.isMe }) { entry ->
                    RankingRow(entry = entry)
                }
                item { Spacer(Modifier.height(90.dp)) }
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
                Surface(
                    color = IconButtonBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
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
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Buscar",
                    tint = White
                )
            }

            Spacer(Modifier.width(10.dp))

            CircleIconButton(onClick = onNotifications) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notificaciones",
                    tint = White
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
    Surface(
        onClick = onClick,
        color = IconButtonBg,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
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
    val items = RankingPeriod.entries
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
                    TextButton(
                        onClick = { onSelected(period) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
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
        verticalAlignment = Alignment.Top
    ) {
        PodiumCard(
            entry = second,
            size = 76.dp,
            label = "#2",
            medal = "🥈",
            isWinner = false
        )

        PodiumCard(
            entry = first,
            size = 108.dp,
            label = "#1",
            medal = "🥇",
            isWinner = true
        )

        PodiumCard(
            entry = third,
            size = 76.dp,
            label = "#3",
            medal = "🥉",
            isWinner = false
        )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(110.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isWinner) Color.Transparent else IconButtonBg)
                .border(
                    width = if (isWinner) 3.dp else 1.dp,
                    color = if (isWinner) AccentGreen else CardStroke,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = medal,
                fontSize = if (isWinner) 44.sp else 32.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWinner) AccentGreen else Muted
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = entry.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "${formatPts(entry.points)} pts",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Muted
        )
    }
}

// ============================================
// RANKING ROW
// ============================================

@Composable
private fun RankingRow(
    entry: RankingEntry,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.position}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Muted,
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
                Text(text = "🏋️", fontSize = 16.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Competidor/a",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted
                )
            }

            Text(
                text = "${formatPts(entry.points)} pts",
                fontSize = 14.sp,
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
                Text(
                    text = "Tu posición",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "#$position · ${formatPts(points)} pts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Button(
                onClick = onViewDetails,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color(0xFF000000)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
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

// ============================================
// UTILS
// ============================================

private fun formatPts(value: Int): String {
    val s = value.toString()
    val rev = s.reversed().chunked(3).joinToString(".")
    return rev.reversed()
}
