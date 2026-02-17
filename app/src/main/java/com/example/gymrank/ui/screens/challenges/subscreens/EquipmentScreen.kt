package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreen(onBack: () -> Unit) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    val equipmentItems = remember {
        listOf(
            "Barra de dominadas",
            "Barras paralelas (dips)",
            "Soga para saltar",
            "Bandas de resistencia",
            "Kettlebell",
            "Mancuernas"
        )
    }

    val selected = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) {
        equipmentItems.forEach { if (!selected.containsKey(it)) selected[it] = false }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Desafíos", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = textPrimary)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = surface,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 420.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Equipamiento", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text("Seleccioná lo que tenés disponible", color = textSecondary, fontSize = 12.sp)
                            }
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = textSecondary)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { equipmentItems.forEach { selected[it] = true } },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                            ) {
                                Text("Seleccionar todo", color = textPrimary)
                            }

                            OutlinedButton(
                                onClick = { equipmentItems.forEach { selected[it] = false } },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                            ) {
                                Text("Limpiar", color = textPrimary)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("Equipamiento", color = textPrimary, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = input,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.10f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(equipmentItems) { item ->
                                    EquipmentRow(
                                        name = item,
                                        checked = selected[item] == true,
                                        accent = accent,
                                        textPrimary = textPrimary,
                                        onToggle = { selected[item] = !(selected[item] == true) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = { onBack() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = GymRankColors.PrimaryAccentText)
                            Spacer(Modifier.width(10.dp))
                            Text("Guardar", color = GymRankColors.PrimaryAccentText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentRow(
    name: String,
    checked: Boolean,
    accent: Color,
    textPrimary: Color,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = textPrimary, fontSize = 15.sp)
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}
