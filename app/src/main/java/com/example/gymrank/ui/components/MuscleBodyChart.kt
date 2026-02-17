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
 * Uses hand-crafted paths approximating a human body silhouette with per-muscle regions.
 */

enum class MuscleId {
    Chest, Back, Legs, Shoulders, Biceps, Triceps, Abs, Glutes, Calves
}

data class MuscleColors(
    val none: Color = Color(0xFF222222),
    val one: Color = GymRankColors.PrimaryAccent.copy(alpha = 0.35f),
    val two: Color = GymRankColors.PrimaryAccent.copy(alpha = 0.65f),
    val threePlus: Color = GymRankColors.PrimaryAccent
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
    // If no external paths provided, use our SVG registry
    val effectiveFrontPaths = frontPaths ?: MuscleSvgRegistry.frontMuscles
    val effectiveBackPaths = backPaths ?: MuscleSvgRegistry.backMuscles

    Column(modifier = modifier) {
        if (showHeader) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Músculos entrenados", style = MaterialTheme.typography.titleMedium, color = GymRankColors.TextPrimary)
                Legend(colors)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(modifier = Modifier.weight(1f).height(260.dp)) {
                // Draw base
                withTransform({
                    val sx = size.width / MuscleSvgRegistry.frontViewBox.width
                    val sy = size.height / MuscleSvgRegistry.frontViewBox.height
                    val k = minOf(sx, sy)
                    scale(k, k)
                }) {
                    drawPath(MuscleSvgRegistry.frontBase, Color(0xFF0B0B0B))
                }
                // Draw muscles
                drawBody(frontCounts, isFront = true, colors, svgPaths = effectiveFrontPaths, svgViewBox = MuscleSvgRegistry.frontViewBox)
            }
            Canvas(modifier = Modifier.weight(1f).height(260.dp)) {
                withTransform({
                    val sx = size.width / MuscleSvgRegistry.backViewBox.width
                    val sy = size.height / MuscleSvgRegistry.backViewBox.height
                    val k = minOf(sx, sy)
                    scale(k, k)
                }) {
                    drawPath(MuscleSvgRegistry.backBase, Color(0xFF0B0B0B))
                }
                drawBody(backCounts, isFront = false, colors, svgPaths = effectiveBackPaths, svgViewBox = MuscleSvgRegistry.backViewBox)
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
        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
            drawCircle(color)
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = GymRankColors.TextSecondary)
    }
}

private fun DrawScope.drawBody(
    counts: Map<MuscleId, Int>,
    isFront: Boolean,
    colors: MuscleColors,
    svgPaths: Map<MuscleId, Path>? = null,
    svgViewBox: Rect? = null
) {
    // If SVG paths provided, draw them scaled into our canvas
    if (svgPaths != null && svgViewBox != null) {
        fun colorFor(count: Int): Color = when {
            count <= 0 -> colors.none
            count == 1 -> colors.one
            count == 2 -> colors.two
            else -> colors.threePlus
        }
        val sx = size.width / svgViewBox.width
        val sy = size.height / svgViewBox.height
        svgPaths.forEach { (id, originalPath) ->
            val p = Path().also { it.addPath(originalPath) }
            withTransform({
                // Scale to our canvas; keep aspect by using min scale
                val k = minOf(sx, sy)
                scale(scaleX = k, scaleY = k)
            }) {
                drawPath(p, colorFor(counts[id] ?: 0))
                // subtle outline for better legibility over dark background
                drawPath(
                    p,
                    color = Color.Black.copy(alpha = 0.35f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        return
    }

    // If SVG paths provided, draw them instead of placeholders
    if (svgPaths != null) {
        fun colorFor(count: Int): Color = when {
            count <= 0 -> colors.none
            count == 1 -> colors.one
            count == 2 -> colors.two
            else -> colors.threePlus
        }
        svgPaths.forEach { (id, p) ->
            drawPath(p, colorFor(counts[id] ?: 0))
        }
        return
    }

    // scale helpers
    fun w(p: Float) = size.width * p
    fun h(p: Float) = size.height * p

    translate(left = 0f, top = 0f) {
        // Base silhouette: head, neck, torso, pelvis, arms, legs (neutral dark)
        val head = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(w(0.45f), h(0.02f), w(0.55f), h(0.10f)))
        }
        val neck = Path().apply {
            moveTo(w(0.48f), h(0.10f)); lineTo(w(0.52f), h(0.10f)); lineTo(w(0.52f), h(0.14f)); lineTo(w(0.48f), h(0.14f)); close()
        }
        val torsoOutline = Path().apply {
            // smoother chest/torso contour
            moveTo(w(0.30f), h(0.14f))
            cubicTo(w(0.28f), h(0.17f), w(0.26f), h(0.22f), w(0.28f), h(0.28f))
            cubicTo(w(0.32f), h(0.36f), w(0.38f), h(0.48f), w(0.50f), h(0.52f))
            cubicTo(w(0.62f), h(0.48f), w(0.68f), h(0.36f), w(0.72f), h(0.28f))
            cubicTo(w(0.74f), h(0.22f), w(0.72f), h(0.17f), w(0.70f), h(0.14f))
            close()
        }
        val pelvis = Path().apply {
            moveTo(w(0.42f), h(0.52f)); lineTo(w(0.58f), h(0.52f)); lineTo(w(0.60f), h(0.58f)); lineTo(w(0.40f), h(0.58f)); close()
        }
        val leftArm = Path().apply {
            moveTo(w(0.28f), h(0.28f)); cubicTo(w(0.18f), h(0.32f), w(0.16f), h(0.40f), w(0.20f), h(0.48f));
            lineTo(w(0.22f), h(0.50f)); lineTo(w(0.26f), h(0.40f)); close()
        }
        val rightArm = Path().apply {
            moveTo(w(0.72f), h(0.28f)); cubicTo(w(0.82f), h(0.32f), w(0.84f), h(0.40f), w(0.80f), h(0.48f));
            lineTo(w(0.78f), h(0.50f)); lineTo(w(0.74f), h(0.40f)); close()
        }
        val leftLeg = Path().apply {
            moveTo(w(0.45f), h(0.58f)); lineTo(w(0.46f), h(0.92f)); lineTo(w(0.48f), h(0.98f)); lineTo(w(0.49f), h(0.58f)); close()
        }
        val rightLeg = Path().apply {
            moveTo(w(0.51f), h(0.58f)); lineTo(w(0.52f), h(0.98f)); lineTo(w(0.54f), h(0.92f)); lineTo(w(0.55f), h(0.58f)); close()
        }

        // Silhouette base draw
        val base = Color(0xFF0B0B0B)
        val outlineStroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round,
            pathEffect = PathEffect.cornerPathEffect(12f))
        drawPath(head, base); drawPath(neck, base); drawPath(torsoOutline, base)
        drawPath(pelvis, base); drawPath(leftArm, base); drawPath(rightArm, base)
        drawPath(leftLeg, base); drawPath(rightLeg, base)
        // subtle outline to avoid "muddy" shapes on dark cards
        listOf(head, neck, torsoOutline, pelvis, leftArm, rightArm, leftLeg, rightLeg).forEach { p ->
            drawPath(p, Color.Black.copy(alpha = 0.35f), style = outlineStroke)
        }

        // Per-muscle regions (more organic shapes)
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
            lineTo(w(0.54f), h(0.42f)); lineTo(w(0.46f), h(0.42f)); close()
        }
        val shoulders = Path().apply {
            moveTo(w(0.30f), h(0.20f)); cubicTo(w(0.32f), h(0.16f), w(0.38f), h(0.14f), w(0.42f), h(0.16f));
            lineTo(w(0.40f), h(0.22f)); cubicTo(w(0.36f), h(0.22f), w(0.32f), h(0.22f), w(0.30f), h(0.20f)); close()
        }
        val biceps = Path().apply {
            moveTo(w(0.26f), h(0.28f)); cubicTo(w(0.24f), h(0.32f), w(0.24f), h(0.36f), w(0.26f), h(0.40f));
            lineTo(w(0.28f), h(0.38f)); lineTo(w(0.29f), h(0.32f)); close()
        }
        val triceps = Path().apply {
            moveTo(w(0.74f), h(0.28f)); cubicTo(w(0.76f), h(0.32f), w(0.76f), h(0.36f), w(0.74f), h(0.40f));
            lineTo(w(0.72f), h(0.38f)); lineTo(w(0.71f), h(0.32f)); close()
        }
        val legs = Path().apply {
            moveTo(w(0.44f), h(0.58f)); cubicTo(w(0.46f), h(0.70f), w(0.54f), h(0.70f), w(0.56f), h(0.58f));
            lineTo(w(0.56f), h(0.90f)); lineTo(w(0.44f), h(0.90f)); close()
        }
        val glutes = Path().apply {
            moveTo(w(0.42f), h(0.50f)); cubicTo(w(0.50f), h(0.52f), w(0.58f), h(0.50f), w(0.60f), h(0.56f));
            lineTo(w(0.40f), h(0.56f)); close()
        }
        val calves = Path().apply {
            moveTo(w(0.45f), h(0.90f)); cubicTo(w(0.47f), h(0.94f), w(0.53f), h(0.94f), w(0.55f), h(0.90f));
            lineTo(w(0.55f), h(0.98f)); lineTo(w(0.45f), h(0.98f)); close()
        }
        val back = Path().apply {
            moveTo(w(0.36f), h(0.22f))
            cubicTo(w(0.40f), h(0.24f), w(0.60f), h(0.24f), w(0.64f), h(0.22f))
            cubicTo(w(0.62f), h(0.38f), w(0.58f), h(0.48f), w(0.50f), h(0.50f))
            cubicTo(w(0.42f), h(0.48f), w(0.38f), h(0.38f), w(0.36f), h(0.22f))
            close()
        }

        fun colorFor(count: Int): Color = when {
            count <= 0 -> colors.none
            count == 1 -> colors.one
            count == 2 -> colors.two
            else -> colors.threePlus
        }

        // Draw filled regions with slight stroke for separation
        fun drawRegion(path: Path, id: MuscleId) {
            val c = colorFor(counts[id] ?: 0)
            drawPath(path, c)
            drawPath(path, Color.Black.copy(alpha = 0.35f), style = outlineStroke)
        }

        if (isFront) {
            drawRegion(chest, MuscleId.Chest)
            drawRegion(abs, MuscleId.Abs)
            drawRegion(shoulders, MuscleId.Shoulders)
            drawRegion(biceps, MuscleId.Biceps)
            drawRegion(triceps, MuscleId.Triceps)
            drawRegion(legs, MuscleId.Legs)
            drawRegion(glutes, MuscleId.Glutes)
            drawRegion(calves, MuscleId.Calves)
        } else {
            drawRegion(back, MuscleId.Back)
            drawRegion(shoulders, MuscleId.Shoulders)
            drawRegion(biceps, MuscleId.Biceps)
            drawRegion(triceps, MuscleId.Triceps)
            drawRegion(legs, MuscleId.Legs)
            drawRegion(glutes, MuscleId.Glutes)
            drawRegion(calves, MuscleId.Calves)
        }
    }
}
