package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onBack: () -> Unit) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = Color(0xFF121212)
    val glass = Color(0xFF1C1C1E)
    val stroke = Color(0xFF2C2C2E)

    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    // Acento “verde gym rank”
    val accent = Color(0xFF2EF2A0)

    var tab by remember { mutableStateOf(ExploreTab.Official) }
    var query by remember { mutableStateOf("") }

    // ✅ Cambiá estas URLs por las que quieras (pueden ser https://...)
    val allPrograms = remember {
        listOf(
            ProgramItem(
                title = "Candito - Fuerza 6 Semanas",
                subtitle = "Ideal para testear 1RM y competir",
                chips = listOf("PRO", "6 Semanas", "Intermedio", "Fuerza"),
                frequency = "4x/sem",
                difficulty = "Intermedio",
                imageUrl = "https://images.pexels.com/photos/703016/pexels-photo-703016.jpeg"
            ),
            ProgramItem(
                title = "PPL 2x (Push/Pull/Legs)",
                subtitle = "Volumen equilibrado + progresión simple",
                chips = listOf("Comunidad", "Intermedio"),
                frequency = "6x/sem",
                difficulty = "Intermedio",
                imageUrl = "https://images.pexels.com/photos/3764537/pexels-photo-3764537.jpeg"
            ),
            ProgramItem(
                title = "Juggernaut - Deadlift",
                subtitle = "Enfocado a levantar más en 16 semanas",
                chips = listOf("PRO", "16 Semanas", "Fuerza"),
                frequency = "3x/sem",
                difficulty = "Intermedio",
                imageUrl = "https://images.pexels.com/photos/2780762/pexels-photo-2780762.jpeg"
            ),
            ProgramItem(
                title = "Full Body - Base",
                subtitle = "Rutina para arrancar sin quemarte",
                chips = listOf("Comunidad", "Principiante"),
                frequency = "3x/sem",
                difficulty = "Principiante",
                imageUrl = "https://images.pexels.com/photos/2827400/pexels-photo-2827400.jpeg"
            )
        )
    }

    val filtered = allPrograms
        .filter {
            if (tab == ExploreTab.Official) it.chips.any { c -> c.equals("PRO", true) }
            else !it.chips.any { c -> c.equals("PRO", true) }
        }
        .filter { it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true) }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Explorar", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(bg)
        ) {
            // Header / Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(glass, surface)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Programas y rutinas",
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Elegí un programa y empezá hoy.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tabs pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillTab(
                    text = "Oficial",
                    selected = tab == ExploreTab.Official,
                    accent = accent,
                    stroke = stroke,
                    onClick = { tab = ExploreTab.Official }
                )
                PillTab(
                    text = "Comunidad",
                    selected = tab == ExploreTab.Community,
                    accent = accent,
                    stroke = stroke,
                    onClick = { tab = ExploreTab.Community }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar programas…", color = textSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = glass,
                    unfocusedContainerColor = glass,
                    focusedBorderColor = stroke,
                    unfocusedBorderColor = stroke,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    cursorColor = accent
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            // List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { item ->
                    ProgramCard(
                        item = item,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        stroke = stroke
                    )
                }

                item {
                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }
}

private enum class ExploreTab { Official, Community }

private data class ProgramItem(
    val title: String,
    val subtitle: String,
    val chips: List<String>,
    val frequency: String,
    val difficulty: String,
    val imageUrl: String? = null
)

@Composable
private fun PillTab(
    text: String,
    selected: Boolean,
    accent: Color,
    stroke: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent
    val border = if (selected) accent.copy(alpha = 0.55f) else stroke

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(border, border))
        ),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (selected) accent else Color(0xFF8E8E93),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ProgramCard(
    item: ProgramItem,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    stroke: Color
) {
    GlassCard {
        // ✅ Banner con imagen + overlay (queda pro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            val url = item.imageUrl?.trim().orEmpty()

            if (url.isNotEmpty() && !url.startsWith("https://images.pexels.com/photos/3601094/pexels-photo-3601094.jpeg")) {
                AsyncImage(
                    model = url,
                    contentDescription = "Banner ${item.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback lindo si todavía no pusiste URL
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1F2322),
                                    Color(0xFF0F0F0F)
                                )
                            )
                        )
                )
            }

            // Overlay para que el badge y el look se vean “premium”
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Badge PRO / Comunidad
            val badge = if (item.chips.any { it.equals("PRO", true) }) "PRO" else "COMUNIDAD"
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                color = if (badge == "PRO") accent.copy(alpha = 0.18f) else Color(0xFF2C2C2E),
                shape = RoundedCornerShape(999.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            if (badge == "PRO") accent else stroke,
                            if (badge == "PRO") accent else stroke
                        )
                    )
                )
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (badge == "PRO") accent else textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            item.title,
            color = textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            item.subtitle,
            color = textSecondary,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(10.dp))

        // Chips (tags)
        FlowRowCompat(
            horizontalGap = 8.dp,
            verticalGap = 8.dp
        ) {
            item.chips.take(4).forEach { chip ->
                TagChip(text = chip, accent = accent, stroke = stroke, textSecondary = textSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Footer: frecuencia + dificultad + CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatPill(
                modifier = Modifier.weight(1f),
                label = "Frecuencia",
                value = item.frequency,
                stroke = stroke,
                textSecondary = textSecondary
            )

            Spacer(Modifier.width(10.dp))

            StatPill(
                modifier = Modifier.weight(1f),
                label = "Nivel",
                value = item.difficulty,
                stroke = stroke,
                textSecondary = textSecondary
            )

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(64.dp),   // misma altura que pills
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Ver", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

    }
}

@Composable
private fun TagChip(
    text: String,
    accent: Color,
    stroke: Color,
    textSecondary: Color
) {
    val isPro = text.equals("PRO", true)

    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 88.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(999.dp),
        color = if (isPro) accent.copy(alpha = 0.16f) else Color(0xFF1C1C1E),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    if (isPro) accent.copy(alpha = 0.55f) else stroke,
                    if (isPro) accent.copy(alpha = 0.55f) else stroke
                )
            )
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isPro) accent else textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    stroke: Color,
    textSecondary: Color
) {
    Surface(
        modifier = modifier
            .height(64.dp),    // altura fija premium
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1C1E),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(stroke, stroke))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = textSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}



/**
 * FlowRow “compatible” sin traer librerías:
 * Renderiza chips en varias líneas con wrap simple.
 */
@Composable
private fun FlowRowCompat(
    horizontalGap: Dp,
    verticalGap: Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        val maxWidth = constraints.maxWidth
        var x = 0
        var y = 0
        var rowHeight = 0

        val positions = ArrayList<Triple<Int, Int, androidx.compose.ui.layout.Placeable>>()

        val hGapPx = horizontalGap.roundToPx()
        val vGapPx = verticalGap.roundToPx()

        placeables.forEach { p ->
            if (x + p.width > maxWidth) {
                x = 0
                y += rowHeight + vGapPx
                rowHeight = 0
            }
            positions.add(Triple(x, y, p))
            x += p.width + hGapPx
            rowHeight = maxOf(rowHeight, p.height)
        }

        val height = y + rowHeight

        layout(width = maxWidth, height = height) {
            positions.forEach { (px, py, p) ->
                p.placeRelative(px, py)
            }
        }
    }
}
