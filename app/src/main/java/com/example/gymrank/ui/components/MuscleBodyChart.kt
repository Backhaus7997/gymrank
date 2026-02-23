package com.example.gymrank.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.gymrank.ui.theme.GymRankColors

/**
 * Muscle body chart (front + back) with per-muscle weekly counts coloring.
 * Uses SVG registry paths when provided; otherwise uses placeholder paths.
 */

enum class MuscleId {
    // FRONT
    Chest,
    Abs,
    Obliques,
    Shoulders,
    Traps,
    Biceps,
    Forearms,
    Calves,
    Quads,

    // BACK
    Back,
    Lats,
    LowerBack,
    Triceps,
    Glutes,
    Hamstrings
}

data class MuscleColors(
    // OJO: none ahora es transparente (para no “tapar” el dibujo base)
    val none: Color = Color.Transparent,
    val one: Color = GymRankColors.PrimaryAccent.copy(alpha = 0.35f),
    val two: Color = GymRankColors.PrimaryAccent.copy(alpha = 0.65f),
    val threePlus: Color = GymRankColors.PrimaryAccent.copy(alpha = 0.95f)
)

@Composable
fun MuscleBodyChart(
    frontCounts: Map<MuscleId, Int>,
    backCounts: Map<MuscleId, Int>,
    modifier: Modifier = Modifier,
    colors: MuscleColors = MuscleColors(),
    showHeader: Boolean = false,
    frontPaths: Map<MuscleId, Path>? = null,
    backPaths: Map<MuscleId, Path>? = null,
) {
    val effectiveFrontPaths = frontPaths ?: MuscleSvgRegistry.frontMuscles
    val effectiveBackPaths = backPaths ?: MuscleSvgRegistry.backMuscles

    Column(modifier = modifier) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Músculos entrenados",
                    style = MaterialTheme.typography.titleMedium,
                    color = GymRankColors.TextPrimary
                )
                Legend(colors)
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FRONT
            Canvas(modifier = Modifier.weight(1f).height(260.dp)) {
                withTransform({
                    val sx = size.width / MuscleSvgRegistry.frontViewBox.width
                    val sy = size.height / MuscleSvgRegistry.frontViewBox.height
                    val k = minOf(sx, sy)
                    scale(k, k)
                }) {
                    drawPath(MuscleSvgRegistry.frontBase, Color(0xFF0B0B0B))
                }

                drawBody(
                    counts = frontCounts,
                    isFront = true,
                    colors = colors,
                    svgPaths = effectiveFrontPaths,
                    svgViewBox = MuscleSvgRegistry.frontViewBox
                )
            }

            // BACK
            Canvas(modifier = Modifier.weight(1f).height(260.dp)) {
                withTransform({
                    val sx = size.width / MuscleSvgRegistry.backViewBox.width
                    val sy = size.height / MuscleSvgRegistry.backViewBox.height
                    val k = minOf(sx, sy)
                    scale(k, k)
                }) {
                    drawPath(MuscleSvgRegistry.backBase, Color(0xFF0B0B0B))
                }

                drawBody(
                    counts = backCounts,
                    isFront = false,
                    colors = colors,
                    svgPaths = effectiveBackPaths,
                    svgViewBox = MuscleSvgRegistry.backViewBox
                )
            }
        }
    }
}

@Composable
private fun Legend(colors: MuscleColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendChip("1x", colors.one)
        LegendChip("2x", colors.two)
        LegendChip("3x+", colors.threePlus)
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(14.dp)) { drawCircle(color) }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GymRankColors.TextSecondary
        )
    }
}

private fun DrawScope.drawBody(
    counts: Map<MuscleId, Int>,
    isFront: Boolean,
    colors: MuscleColors,
    svgPaths: Map<MuscleId, Path>? = null,
    svgViewBox: Rect? = null
) {
    fun fillColorFor(count: Int): Color = when {
        count <= 0 -> Color.Transparent
        count == 1 -> colors.one
        count == 2 -> colors.two
        else -> colors.threePlus
    }

    val outlineStroke = Stroke(
        width = 2f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.cornerPathEffect(12f)
    )

    // ---------- SVG MODE ----------
    if (svgPaths != null && svgViewBox != null) {
        val sx = size.width / svgViewBox.width
        val sy = size.height / svgViewBox.height
        val k = minOf(sx, sy)

        fun trapsFallbackPath(): Path = Path().apply {
            moveTo(38f, 30f)
            cubicTo(44f, 24f, 56f, 24f, 62f, 30f)
            lineTo(58f, 38f)
            cubicTo(54f, 35f, 46f, 35f, 42f, 38f)
            close()
        }

        val idsToDraw: List<MuscleId> =
            if (isFront) listOf(
                MuscleId.Chest,
                MuscleId.Abs,
                MuscleId.Obliques,
                MuscleId.Shoulders,
                MuscleId.Traps,
                MuscleId.Biceps,
                MuscleId.Forearms,
                MuscleId.Quads,
                MuscleId.Calves,
                MuscleId.Glutes
            ) else listOf(
                MuscleId.Back,
                MuscleId.Lats,
                MuscleId.LowerBack,
                MuscleId.Shoulders,
                MuscleId.Traps,
                MuscleId.Triceps,
                MuscleId.Biceps,
                MuscleId.Forearms,
                MuscleId.Hamstrings,
                MuscleId.Calves,
                MuscleId.Glutes
            )

        withTransform({ scale(k, k) }) {
            idsToDraw.forEach { id ->
                val original = when (id) {
                    MuscleId.Traps -> svgPaths[id] ?: trapsFallbackPath()
                    else -> svgPaths[id]
                } ?: return@forEach

                val count = counts[id] ?: 0
                val fill = fillColorFor(count)

                // fill solo si count > 0
                if (fill.alpha > 0f) {
                    drawPath(original, fill)
                }

                // outline siempre (ayuda a ver regiones y debuggear)
                drawPath(
                    original,
                    color = Color.Black.copy(alpha = 0.35f),
                    style = outlineStroke
                )
            }
        }
        return
    }

    // ---------- NON-SVG MODE (placeholders) ----------
    if (svgPaths != null) {
        svgPaths.forEach { (id, p) ->
            val count = counts[id] ?: 0
            val fill = fillColorFor(count)
            if (fill.alpha > 0f) drawPath(p, fill)
            drawPath(p, Color.Black.copy(alpha = 0.35f), style = outlineStroke)
        }
        return
    }

    // Placeholder drawing (si algún día no pasás svgPaths)
    fun w(p: Float) = size.width * p
    fun h(p: Float) = size.height * p

    translate(left = 0f, top = 0f) {
        val chest = Path().apply {
            moveTo(w(0.34f), h(0.18f))
            cubicTo(w(0.40f), h(0.16f), w(0.60f), h(0.16f), w(0.66f), h(0.18f))
            cubicTo(w(0.64f), h(0.22f), w(0.56f), h(0.25f), w(0.50f), h(0.25f))
            cubicTo(w(0.44f), h(0.25f), w(0.36f), h(0.22f), w(0.34f), h(0.18f))
            close()
        }

        val abs = Path().apply {
            moveTo(w(0.46f), h(0.26f))
            cubicTo(w(0.48f), h(0.34f), w(0.52f), h(0.34f), w(0.54f), h(0.26f))
            lineTo(w(0.54f), h(0.42f))
            lineTo(w(0.46f), h(0.42f))
            close()
        }

        val shoulders = Path().apply {
            moveTo(w(0.30f), h(0.20f))
            cubicTo(w(0.32f), h(0.16f), w(0.38f), h(0.14f), w(0.42f), h(0.16f))
            lineTo(w(0.40f), h(0.22f))
            cubicTo(w(0.36f), h(0.22f), w(0.32f), h(0.22f), w(0.30f), h(0.20f))
            close()
        }

        val traps = Path().apply {
            moveTo(w(0.38f), h(0.14f))
            cubicTo(w(0.44f), h(0.12f), w(0.56f), h(0.12f), w(0.62f), h(0.14f))
            lineTo(w(0.58f), h(0.18f))
            cubicTo(w(0.54f), h(0.17f), w(0.46f), h(0.17f), w(0.42f), h(0.18f))
            close()
        }

        val biceps = Path().apply {
            moveTo(w(0.26f), h(0.28f))
            cubicTo(w(0.24f), h(0.32f), w(0.24f), h(0.36f), w(0.26f), h(0.40f))
            lineTo(w(0.28f), h(0.38f))
            lineTo(w(0.29f), h(0.32f))
            close()
        }

        val legs = Path().apply {
            moveTo(w(0.44f), h(0.58f))
            cubicTo(w(0.46f), h(0.70f), w(0.54f), h(0.70f), w(0.56f), h(0.58f))
            lineTo(w(0.56f), h(0.90f))
            lineTo(w(0.44f), h(0.90f))
            close()
        }

        val calves = Path().apply {
            moveTo(w(0.45f), h(0.90f))
            cubicTo(w(0.47f), h(0.94f), w(0.53f), h(0.94f), w(0.55f), h(0.90f))
            lineTo(w(0.55f), h(0.98f))
            lineTo(w(0.45f), h(0.98f))
            close()
        }

        fun drawRegion(path: Path, id: MuscleId) {
            val count = counts[id] ?: 0
            val fill = fillColorFor(count)
            if (fill.alpha > 0f) drawPath(path, fill)
            drawPath(path, Color.Black.copy(alpha = 0.35f), style = outlineStroke)
        }

        if (isFront) {
            drawRegion(chest, MuscleId.Chest)
            drawRegion(abs, MuscleId.Abs)
            drawRegion(shoulders, MuscleId.Shoulders)
            drawRegion(traps, MuscleId.Traps)
            drawRegion(biceps, MuscleId.Biceps)
            drawRegion(legs, MuscleId.Quads)
            drawRegion(calves, MuscleId.Calves)
        } else {
            drawRegion(shoulders, MuscleId.Shoulders)
            drawRegion(traps, MuscleId.Traps)
            drawRegion(biceps, MuscleId.Biceps)
            drawRegion(legs, MuscleId.Hamstrings)
            drawRegion(calves, MuscleId.Calves)
        }
    }
}
