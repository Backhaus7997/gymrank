package com.example.gymrank.ui.screens.ranking.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ============================================
// THEME (local, porque las del RankingScreen son private)
// ============================================

private val ScreenBg = Color(0xFF070B0A)
private val CardBg = Color(0xFF0F1412)
private val CardStroke = Color(0xFF1E2622)
private val Muted = Color(0xFF8E8E93)
private val White = Color(0xFFFFFFFF)
private val AccentGreen = Color(0xFF32E37A)
private val IconButtonBg = Color(0xFF151A18)
private val DividerC = Color(0xFF1C2420)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetailsScreen(
    gymName: String,
    gymLocation: String,
    periodLabel: String,
    myPosition: Int,
    myPoints: Int,
    onBack: () -> Unit
) {
    // Imagen “hero” (podés cambiarla por la que quieras)
    val heroUrl =
        "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=1200"

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalles",
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        color = IconButtonBg,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBg
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // ============================================
            // HERO (imagen + overlay) + mini info
            // ============================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                AsyncImage(
                    model = heroUrl,
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // “badge” periodo
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = AccentGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                AccentGreen.copy(alpha = 0.55f),
                                AccentGreen.copy(alpha = 0.55f)
                            )
                        )
                    )
                ) {
                    Text(
                        text = periodLabel,
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = gymName,
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = gymLocation,
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ============================================
            // CARD PRINCIPAL: Tu rendimiento
            // ============================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.14f))
                                .border(1.dp, AccentGreen.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = null,
                                tint = AccentGreen
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tu rendimiento",
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Resumen de puntos y posición (placeholder por ahora).",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = DividerC, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBig(
                            title = "Posición",
                            value = "#$myPosition",
                            icon = Icons.Filled.FitnessCenter,
                            modifier = Modifier.weight(1f)
                        )
                        StatBig(
                            title = "Puntos",
                            value = formatPts(myPoints),
                            icon = Icons.Filled.Bolt,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Desglose",
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            // ============================================
            // LISTA DE ITEMS (con icons + value pill)
            // ============================================
            DetailRow(
                icon = Icons.Filled.CheckCircle,
                title = "Entrenamientos completados",
                value = "8"
            )
            Spacer(Modifier.height(10.dp))

            DetailRow(
                icon = Icons.Filled.LocalFireDepartment,
                title = "Rachas",
                value = "3 días"
            )
            Spacer(Modifier.height(10.dp))

            DetailRow(
                icon = Icons.Filled.FitnessCenter,
                title = "PRs registrados",
                value = "2"
            )
            Spacer(Modifier.height(10.dp))

            DetailRow(
                icon = Icons.Filled.Bolt,
                title = "Asistencia",
                value = "Alta"
            )

            Spacer(Modifier.height(14.dp))

            // ============================================
            // “Próximamente”
            // ============================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(18.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            AccentGreen.copy(alpha = 0.55f),
                            CardStroke
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Próximamente",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Gráficos por semana/mes + historial completo.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        color = AccentGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    AccentGreen.copy(alpha = 0.55f),
                                    AccentGreen.copy(alpha = 0.55f)
                                )
                            )
                        )
                    ) {
                        Text(
                            text = "OK",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatBig(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B0F0D),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(CardStroke, CardStroke))
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = 0.12f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AccentGreen)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = value,
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(IconButtonBg)
                    .border(1.dp, CardStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentGreen)
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = title,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                color = AccentGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            AccentGreen.copy(alpha = 0.55f),
                            AccentGreen.copy(alpha = 0.55f)
                        )
                    )
                )
            ) {
                Text(
                    text = value,
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

private fun formatPts(value: Int): String {
    val s = value.toString()
    val rev = s.reversed().chunked(3).joinToString(".")
    return rev.reversed()
}
