package com.example.gymrank.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun BodySvg(
    @RawRes svgRes: Int,
    overrides: Map<String, Color>,            // id -> color
    modifier: Modifier = Modifier,
    baseFill: Color = Color(0xFF2D3A35),      // cuerpo apagado
    baseStroke: Color? = null,                // borde opcional
    // padding: si no lo pasás, se calcula relativo al tamaño del canvas (mejor que px fijo)
    paddingPx: Float? = null,
    strokeWidthPx: Float = 1.2f
) {
    val ctx = LocalContext.current

    val svgText = remember(svgRes) {
        ctx.resources.openRawResource(svgRes).bufferedReader().use { it.readText() }
    }

    val nodes = remember(svgText) {
        BodySvgParser.parsePaths(svgText)
    }

    val bounds = remember(nodes) {
        computeBounds(nodes.map { it.path })
    }

    Canvas(modifier = modifier) {
        // Si no parseó nada, no hay nada que dibujar
        if (nodes.isEmpty()) return@Canvas
        if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) return@Canvas

        val canvasW = size.width
        val canvasH = size.height

        // Padding dinámico por default (7% del tamaño mínimo)
        val pad = (paddingPx ?: (size.minDimension * 0.07f)).coerceAtLeast(0f)

        val contentW = max(1f, canvasW - pad * 2f)
        val contentH = max(1f, canvasH - pad * 2f)

        val sx = contentW / bounds.width
        val sy = contentH / bounds.height
        val s = min(sx, sy).coerceAtLeast(0.0001f)

        val scaledW = bounds.width * s
        val scaledH = bounds.height * s

        val tx = (canvasW - scaledW) / 2f
        val ty = (canvasH - scaledH) / 2f

        withTransform({
            // 1) mover el contenido para que arranque en (0,0)
            translate(left = -bounds.left, top = -bounds.top)
            // 2) escalar a "fit"
            scale(scaleX = s, scaleY = s)
            // 3) centrar en canvas (tx/ty están en canvas px, por eso /s)
            translate(left = tx / s, top = ty / s)
        }) {
            val strokeW = (strokeWidthPx / s).coerceAtLeast(0.4f)

            // Base "apagada" para todos los paths
            nodes.forEach { node ->
                drawPath(
                    path = node.path,
                    color = baseFill,
                    style = Fill
                )
                if (baseStroke != null) {
                    drawPath(
                        path = node.path,
                        color = baseStroke,
                        style = Stroke(width = strokeW)
                    )
                }
            }

            // Overrides arriba
            nodes.forEach { node ->
                val c = overrides[node.id] ?: return@forEach
                drawPath(
                    path = node.path,
                    color = c,
                    style = Fill
                )
                if (baseStroke != null) {
                    drawPath(
                        path = node.path,
                        color = baseStroke,
                        style = Stroke(width = strokeW)
                    )
                }
            }
        }
    }
}

private fun computeBounds(paths: List<Path>): Rect? {
    var acc: Rect? = null
    for (p in paths) {
        val b = p.getBounds()
        if (b.width <= 0f || b.height <= 0f) continue
        acc = if (acc == null) b else union(acc!!, b)
    }
    return acc
}

private fun union(a: Rect, b: Rect): Rect {
    return Rect(
        left = min(a.left, b.left),
        top = min(a.top, b.top),
        right = max(a.right, b.right),
        bottom = max(a.bottom, b.bottom)
    )
}
