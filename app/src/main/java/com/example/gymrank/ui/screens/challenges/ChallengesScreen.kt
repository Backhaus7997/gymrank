package com.example.gymrank.ui.screens.challenges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable


@Composable
fun ChallengesScreen(
    onOpenDiscover: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenGamble: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenUserChallengeDetail: (userChallengeId: String, templateId: String) -> Unit = { _, _ -> }
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF1C1C1E) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    // Verde como en Challenges
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    var selectedView by remember { mutableStateOf(ChallengesView.TODO) }

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Desafíos", color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* TODO menu */ }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            ChallengesFeatureGrid(
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface,
                onOpenDiscover = onOpenDiscover,
                onOpenQuests = onOpenQuests,
                onOpenGamble = onOpenGamble,
                onOpenEquipment = onOpenEquipment
            )

            Spacer(Modifier.height(18.dp))

            Text(
                "Vista:",
                color = textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))

            SegmentedPills(
                selected = selectedView,
                onSelectedChange = { selectedView = it },
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface
            )

            Spacer(Modifier.height(24.dp))

            ChallengesEmptyStateCard(
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface
            )
        }
    }
}

enum class ChallengesView { TODO, PROGRESS, ACTIVITY }

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun ChallengesFeatureGrid(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    onOpenDiscover: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenGamble: () -> Unit,
    onOpenEquipment: () -> Unit
) {
    val items = remember(onOpenDiscover, onOpenQuests, onOpenGamble, onOpenEquipment) {
        listOf(
            FeatureItem("Descubrir", "Nuevos desafíos", Icons.Filled.Search, onOpenDiscover),
            FeatureItem("Misiones", "Crear y gestionar", Icons.Filled.Task, onOpenQuests),
            FeatureItem("Apuestas", "Rueda y dados", Icons.Filled.Casino, onOpenGamble),
            FeatureItem("Equipamiento", "Tus ítems y stats", Icons.Filled.Build, onOpenEquipment),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[0], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
            FeatureButton(items[1], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[2], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
            FeatureButton(items[3], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureButton(
    item: FeatureItem,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.86f)))
    val shape = RoundedCornerShape(16.dp)

    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { item.onClick() } // ✅ en vez de Surface(onClick=)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 78.dp)
                    .background(gradient, shape)
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isNarrow = this.maxWidth < 140.dp
                    val titleSize = if (isNarrow) 12.sp else 13.sp
                    val subtitleSize = if (isNarrow) 12.sp else 13.sp

                    Column {
                        Text(
                            text = item.title,
                            color = textPrimary,
                            fontSize = titleSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = (titleSize.value + 1).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            color = textSecondary,
                            fontSize = subtitleSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = (subtitleSize.value + 1).sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun SegmentedPills(
    selected: ChallengesView,
    onSelectedChange: (ChallengesView) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color
) {
    val border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    val bgUnselected = surface.copy(alpha = 0.70f)
    val bgSelected = accent.copy(alpha = 0.14f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedPill(
            modifier = Modifier.weight(1f),
            label = "Pendientes",
            icon = Icons.Filled.Checklist,
            selected = selected == ChallengesView.TODO,
            bgSelected = bgSelected,
            bgUnselected = bgUnselected,
            border = border,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        ) { onSelectedChange(ChallengesView.TODO) }

        SegmentedPill(
            modifier = Modifier.weight(1f),
            label = "Progreso",
            icon = Icons.Filled.BarChart,
            selected = selected == ChallengesView.PROGRESS,
            bgSelected = bgSelected,
            bgUnselected = bgUnselected,
            border = border,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        ) { onSelectedChange(ChallengesView.PROGRESS) }

        SegmentedPill(
            modifier = Modifier.weight(1f),
            label = "Actividad",
            icon = Icons.Filled.Timeline,
            selected = selected == ChallengesView.ACTIVITY,
            bgSelected = bgSelected,
            bgUnselected = bgUnselected,
            border = border,
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        ) { onSelectedChange(ChallengesView.ACTIVITY) }
    }
}

@Composable
private fun SegmentedPill(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    bgSelected: Color,
    bgUnselected: Color,
    border: BorderStroke,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)

    Surface(
        shape = shape,
        color = if (selected) bgSelected else bgUnselected,
        border = border,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .clickable { onClick() } // ✅ en vez de Surface(onClick=)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isTight = this.maxWidth < 115.dp
            val isVeryTight = this.maxWidth < 95.dp

            val horizontalPadding = if (isTight) 8.dp else 12.dp
            val iconSize = if (isTight) 16.dp else 18.dp
            val spacer = if (isTight) 6.dp else 8.dp
            val textSize = if (isTight) 12.sp else 13.sp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!isVeryTight) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(Modifier.width(spacer))
                }

                Text(
                    text = label,
                    color = if (selected) textPrimary else textSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = textSize,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun ChallengesEmptyStateCard(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color
) {
    GlassCard(glow = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧗", fontSize = 72.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "No hay desafíos activos",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Creá un desafío para mejorar tu nivel",
                color = textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            // micro detalle para “igualar” visualmente el estilo
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(54.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.35f))
            )
        }
    }
}
