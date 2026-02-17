package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutProgressScreen(onBack: () -> Unit) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso", color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        containerColor = bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Todavía no hay datos de progreso",
                    color = textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Empezá a registrar entrenamientos para ver tu evolución.",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
