package com.example.gymrank.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
fun WorkoutScreen(
    onExploreClick: () -> Unit = {},
    onCoachClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCreateRoutineClick: () -> Unit = {},
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF1C1C1E) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color(0xFFFFFFFF) }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }

    // Accent de esta pestaña
    val accent = Color(0xFF2EF2A0)

    Scaffold(containerColor = bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Entrenar", color = textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* TODO menu */ }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            FeatureGrid(
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surface = surface,
                onExploreClick = onExploreClick,
                onCoachClick = onCoachClick,
                onHistoryClick = onHistoryClick,
                onProgressClick = onProgressClick
            )

            Spacer(Modifier.height(20.dp))

            RecoverySection(
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent
            )

            Spacer(Modifier.height(20.dp))

            // ✅ ACÁ estaba el error: faltaba pasar onCreateRoutineClick
            RoutinesSection(
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                onCreateRoutineClick = onCreateRoutineClick
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
private fun FeatureGrid(
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    surface: Color,
    onExploreClick: () -> Unit,
    onCoachClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProgressClick: () -> Unit,
) {
    val items = listOf(
        FeatureItem("Explorar", "Rutinas y programas", Icons.Filled.Search),
        FeatureItem("Coach IA", "Plan inteligente", Icons.Outlined.AutoAwesome),
        FeatureItem("Historial", "Tus entrenamientos", Icons.Filled.History),
        FeatureItem("Progreso", "Evolución y stats", Icons.Filled.BarChart),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[0], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onExploreClick() }
            FeatureButton(items[1], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onCoachClick() }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureButton(items[2], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onHistoryClick() }
            FeatureButton(items[3], accent, textPrimary, textSecondary, surface, modifier = Modifier.weight(1f)) { onProgressClick() }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val gradient = Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.86f)))

    GlassCard(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 78.dp)
                    .background(gradient, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.subtitle,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
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
private fun RecoverySection(
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    GlassCard(glow = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recuperación muscular", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { /* TODO */ }) {
                Text("Detalles", color = accent, fontSize = 14.sp)
            }
        }

        Text("Estimado en base a tus entrenamientos", color = textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        val muscles = remember {
            listOf(
                "Abductores" to 100,
                "Abdominales" to 100,
                "Isquiotibiales" to 75,
                "Cuádriceps" to 80,
                "Hombros" to 90,
                "Glúteos" to 70,
                "Espalda" to 85,
                "Pecho" to 95
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            muscles.take(2).forEach { (name, percent) ->
                RecoveryMuscleCard(
                    name = name,
                    percent = percent,
                    accent = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class MuscleGroup { LEG, ARM, TORSO, OTHER }

private fun muscleGroupFor(name: String): MuscleGroup {
    val n = name.lowercase().trim()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u")
    return when (n) {
        "cuadriceps", "isquiotibiales", "pantorrillas", "gemelos",
        "gluteos", "aductores", "abductores", "piernas" -> MuscleGroup.LEG
        "biceps", "triceps", "antebrazos", "hombros", "deltoides" -> MuscleGroup.ARM
        "pecho", "espalda", "abdominales", "abs", "lumbares", "trapecios", "core" -> MuscleGroup.TORSO
        else -> MuscleGroup.OTHER
    }
}

@Composable
private fun MuscleIcon(group: MuscleGroup, tint: Color, modifier: Modifier = Modifier) {
    val icon: ImageVector = when (group) {
        MuscleGroup.LEG -> Icons.Filled.DirectionsRun
        MuscleGroup.ARM -> Icons.Filled.FitnessCenter
        MuscleGroup.TORSO -> Icons.Filled.Accessibility
        MuscleGroup.OTHER -> Icons.Filled.FitnessCenter
    }
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier)
}

@Composable
private fun RecoveryMuscleCard(
    name: String,
    percent: Int,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    MuscleIcon(
                        group = muscleGroupFor(name),
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$percent%",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val p = (percent.coerceIn(0, 100)) / 100f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(p)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.75f))
                )
            }
        }
    }
}

@Composable
private fun RoutinesSection(
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onCreateRoutineClick: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Mis rutinas (0/1)", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Creá una rutina y repetila", color = textSecondary, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                    .clickable { onCreateRoutineClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear rutina", tint = accent)
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCreateRoutineClick() }
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📝", fontSize = 52.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Tocá + para crear tu rutina",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "O encontrá programas en Explorar y Coach IA",
                color = textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
