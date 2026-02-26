package com.example.gymrank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gymrank.navigation.Screen

private data class BottomItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

// Extra lift so the bar doesn’t touch the home indicator
private val BOTTOM_BAR_EXTRA_PADDING = 12.dp

@Composable
fun GymRankBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val items: List<BottomItem> = listOf(
        BottomItem("feed", Icons.Filled.RssFeed, "Feed"),
        BottomItem("challenges", Icons.Filled.List, "Desafíos"),
        BottomItem(Screen.Home.route, Icons.Filled.Home, "Home"),
        BottomItem("workout", Icons.Filled.FitnessCenter, "Workout"),
        BottomItem("rank", Icons.Filled.Leaderboard, "Ranking")
    )

    Surface(color = Color(0xFF000000)) {
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = BOTTOM_BAR_EXTRA_PADDING)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item: BottomItem ->
                    val selected = item.route == currentRoute
                    val tint = if (selected) Color(0xFF2EF2A0) else Color(0xFF8E8E93)
                    val labelColor = if (selected) Color(0xFFFFFFFF) else Color(0xFF8E8E93)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Color(0xFF2C2C2E) else Color.Transparent)
                            .clickable {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            // leve margen lateral para que el texto no quede pegado
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint
                        )

                        // ✅ Siempre visible, pero nunca wrappea
                        Text(
                            text = item.label,
                            color = labelColor,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}