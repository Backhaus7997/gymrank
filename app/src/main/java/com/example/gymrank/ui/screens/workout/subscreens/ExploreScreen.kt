package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.domain.model.WorkoutTemplate
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onBack: () -> Unit,
    onViewProgram: (templateId: String) -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = Color(0xFF121212)
    val glass = Color(0xFF1C1C1E)
    val stroke = Color(0xFF2C2C2E)

    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    val accent = Color(0xFF2EF2A0)

    var tab by remember { mutableStateOf(ExploreTab.Official) }
    var query by remember { mutableStateOf("") }

    val vm: ExploreViewModel = viewModel()
    val ui by vm.state.collectAsState()

    val allPrograms = remember(ui.templates) {
        ui.templates.map { it.toProgramItem() }
    }

    val filtered = allPrograms
        .filter { if (tab == ExploreTab.Official) it.isPro else !it.isPro }
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
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(colors = listOf(glass, surface)))
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

            // Tabs
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

            when {
                ui.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                ui.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No se pudieron cargar programas", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(ui.error ?: "", color = textSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { vm.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Reintentar", fontWeight = FontWeight.Bold) }
                    }
                }

                else -> {
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
                                stroke = stroke,
                                onView = { onViewProgram(item.id) }
                            )
                        }

                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }
}

private enum class ExploreTab { Official, Community }

private data class ProgramItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val chips: List<String>,
    val frequency: String,
    val difficulty: String,
    val isPro: Boolean,
    val imageUrl: String? = null
)

private fun WorkoutTemplate.toProgramItem(): ProgramItem {
    val chips = buildList {
        if (isPro) add("PRO") else add("Comunidad")
        if (weeks > 0) add("${weeks} Semanas")
        if (level.isNotBlank()) add(level)
        goalTags.take(2).forEach { add(it) }
    }

    val freq = if (frequencyPerWeek > 0) "${frequencyPerWeek}x/sem" else "-"

    return ProgramItem(
        id = id,
        title = title.ifBlank { id },
        subtitle = description.ifBlank { " " },
        chips = chips,
        frequency = freq,
        difficulty = if (level.isNotBlank()) level else "—",
        isPro = isPro,
        imageUrl = coverUrl
    )
}

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
    val shape = RoundedCornerShape(999.dp)

    // ✅ evitamos Surface(onClick=) para no comernos errores raros de versiones
    Surface(
        shape = shape,
        color = bg,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(border, border))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier.clip(shape).clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (selected) accent else Color(0xFF8E8E93),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ProgramCard(
    item: ProgramItem,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    stroke: Color,
    onView: () -> Unit
) {
    GlassCard {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp) // un toque más alto para que respire y se vea como en tu foto
                .clip(RoundedCornerShape(14.dp))
        ) {
            val url = item.imageUrl?.trim().orEmpty()

            if (url.startsWith("http")) {
                AsyncImage(
                    model = url,
                    contentDescription = "Banner ${item.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1F2322), Color(0xFF0F0F0F))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.60f)
                            )
                        )
                    )
            )

            val badge = if (item.isPro) "PRO" else "COMUNIDAD"
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                color = if (item.isPro) accent.copy(alpha = 0.18f) else Color(0xFF2C2C2E),
                shape = RoundedCornerShape(999.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(if (item.isPro) accent else stroke, if (item.isPro) accent else stroke))
                )
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (item.isPro) accent else textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ✅ título/subtítulo con límites para que nunca “rompan”
        Text(
            item.title,
            color = textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            item.subtitle,
            color = textSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))

        FlowRowCompat(horizontalGap = 8.dp, verticalGap = 8.dp) {
            item.chips.take(4).forEach { chip ->
                TagChip(
                    text = chip,
                    accent = accent,
                    stroke = stroke,
                    textSecondary = textSecondary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

            // ✅ botón un poquito más compacto para pantallas chicas
            Button(
                onClick = onView,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(min = 64.dp), // mantiene el “cuadradito” tipo tu foto
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text("Ver", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun TagChip(text: String, accent: Color, stroke: Color, textSecondary: Color) {
    val isPro = text.equals("PRO", true)

    // ✅ sin minWidth fijo (88dp te rompe todo en pantallas chicas)
    Surface(
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isPro) accent else textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1C1E),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(listOf(stroke, stroke)))
    ) {
        // ✅ responsive: si hay poco ancho, baja un toque la fuente
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            val tight = this.maxWidth < 120.dp
            val valueSize = if (tight) 14.sp else 16.sp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    color = textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = valueSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FlowRowCompat(horizontalGap: Dp, verticalGap: Dp, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
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
            positions.forEach { (px, py, p) -> p.placeRelative(px, py) }
        }
    }
}