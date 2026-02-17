package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

private data class ChallengeCard(
    val title: String,
    val subtitle: String,
    val level: String,
    val days: String,
    val imageUrl: String? = null // ✅ URL remota
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onBack: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    var tab by remember { mutableIntStateOf(0) }   // ✅ recomendado por el warning
    var query by remember { mutableStateOf("") }

    val items = remember {
        listOf(
            ChallengeCard(
                title = "Los 50 diarios 🔥",
                subtitle = "Completá el desafío de peso corporal de 50 días",
                level = "Intermedio",
                days = "50 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=800"
            ),
            ChallengeCard(
                title = "Desafío 75 Hard",
                subtitle = "Un desafío para fortalecer la mentalidad",
                level = "Experto",
                days = "75 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1517963879433-6ad2b056d712?w=800"
            ),
            ChallengeCard(
                title = "Quemá entre 500–750 kcal por día…",
                subtitle = "Quemá 500–750 kcal diarias con pasos y cardio",
                level = "Avanzado",
                days = "30 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800"
            ),
            ChallengeCard(
                title = "10.000 pasos diarios ️",
                subtitle = "Caminá al menos 10k pasos todos los días",
                level = "Principiante",
                days = "21 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800"
            ),
            ChallengeCard(
                title = "Cardio sin excusas",
                subtitle = "30 minutos de cardio continuo por día",
                level = "Intermedio",
                days = "14 DÍAS",
                imageUrl = "https://images.pexels.com/photos/4944973/pexels-photo-4944973.jpeg"
            ),
            ChallengeCard(
                title = "Core de acero",
                subtitle = "Entrená abdomen y core todos los días",
                level = "Intermedio",
                days = "30 DÍAS",
                imageUrl = "https://images.pexels.com/photos/700392/pexels-photo-700392.jpeg"
            ),
            ChallengeCard(
                title = "Fuerza total",
                subtitle = "Levantá pesado 4 veces por semana",
                level = "Avanzado",
                days = "6 SEMANAS",
                imageUrl = "https://images.pexels.com/photos/4853660/pexels-photo-4853660.jpeg"
            ),
            ChallengeCard(
                title = "HIIT extremo",
                subtitle = "Sesiones HIIT cortas pero intensas",
                level = "Experto",
                days = "14 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800"
            ),
            ChallengeCard(
                title = "Sin azúcar",
                subtitle = "Eliminá azúcar agregada de tu dieta",
                level = "Intermedio",
                days = "21 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1506089676908-3592f7389d4d?w=800"
            ),
            ChallengeCard(
                title = "Push-ups challenge",
                subtitle = "Aumentá tus flexiones cada día",
                level = "Principiante",
                days = "30 DÍAS",
                imageUrl = "https://images.pexels.com/photos/1199607/pexels-photo-1199607.jpeg"
            ),
            ChallengeCard(
                title = "Early workout",
                subtitle = "Entrená antes de las 9 AM",
                level = "Intermedio",
                days = "14 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800"
            ),
            ChallengeCard(
                title = "Running streak ️",
                subtitle = "Corré al menos 3 km diarios",
                level = "Avanzado",
                days = "10 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
            ),
            ChallengeCard(
                title = "Movilidad y estiramiento",
                subtitle = "15 min diarios de movilidad",
                level = "Principiante",
                days = "21 DÍAS",
                imageUrl = "https://images.unsplash.com/photo-1549576490-b0b4831ef60a?w=800"
            )

        )
    }

    // filtro por búsqueda
    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else items.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Desafíos", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {

            // Segmented: Descubrir / Mi biblioteca
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SegBtn(
                    label = "Descubrir",
                    selected = tab == 0,
                    accent = accent,
                    onClick = { tab = 0 }
                )
                SegBtn(
                    label = "Mi biblioteca",
                    selected = tab == 1,
                    accent = accent,
                    onClick = { tab = 1 }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary) },
                placeholder = { Text("Buscar", color = textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = input,
                    unfocusedContainerColor = input,
                    focusedBorderColor = accent.copy(alpha = 0.35f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    cursorColor = accent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Text("Total: ${filtered.size}", color = textSecondary, fontSize = 13.sp)

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(filtered) { c ->
                    ChallengeListCard(
                        card = c,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SegBtn(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent
    val border = if (selected) accent.copy(alpha = 0.55f) else Color.Transparent

    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = if (selected) BorderStroke(1.dp, border) else null,
        onClick = onClick
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChallengeListCard(
    card: ChallengeCard,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Surface(
        color = surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ✅ Miniatura: URL -> AsyncImage, si no hay -> placeholder
            Box(
                Modifier
                    .size(width = 128.dp, height = 82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val url = card.imageUrl
                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ImageIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    card.title,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    card.subtitle,
                    color = textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChipPill(text = card.level, accent = accent)
                    ChipPill(text = card.days, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun ChipPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}
