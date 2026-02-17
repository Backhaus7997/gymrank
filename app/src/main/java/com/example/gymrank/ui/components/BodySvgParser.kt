package com.example.gymrank.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

data class SvgPathNode(
    val id: String,
    val path: Path
)

object BodySvgParser {

    /**
     * Soporta:
     * - <path id="..." d="..."/>
     * - <rect id="..." x=".." y=".." width=".." height=".." rx=".." ry=".."/>
     *
     * (Para tu SVG actual, con rects, esto es suficiente)
     */
    fun parsePaths(svgXml: String): List<SvgPathNode> {
        val out = mutableListOf<SvgPathNode>()

        // -------- PATH --------
        val pathRegex = Regex(
            pattern = """<path\b[^>]*\bid="([^"]+)"[^>]*\bd="([^"]+)"[^>]*/?>""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        val parser = PathParser()

        for (m in pathRegex.findAll(svgXml)) {
            val id = m.groupValues[1]
            val d = m.groupValues[2]
            val p = try {
                parser.parsePathString(d).toPath()
            } catch (_: Throwable) {
                null
            }
            if (p != null) out += SvgPathNode(id, p)
        }

        // -------- RECT --------
        val rectRegex = Regex(
            pattern = """<rect\b([^>]*)/?>""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        for (m in rectRegex.findAll(svgXml)) {
            val attrs = m.groupValues[1]

            val id = attr(attrs, "id") ?: continue
            val x = attrFloat(attrs, "x") ?: 0f
            val y = attrFloat(attrs, "y") ?: 0f
            val w = attrFloat(attrs, "width") ?: continue
            val h = attrFloat(attrs, "height") ?: continue

            val rx = attrFloat(attrs, "rx") ?: 0f
            val ry = attrFloat(attrs, "ry") ?: 0f

            val p = Path()

            if (rx > 0f || ry > 0f) {
                val rrx = rx.coerceAtLeast(0f)
                val rry = ry.coerceAtLeast(0f)
                p.addRoundRect(
                    RoundRect(
                        rect = Rect(x, y, x + w, y + h),
                        cornerRadius = CornerRadius(rrx, rry)
                    )
                )
            } else {
                p.addRect(Rect(x, y, x + w, y + h))
            }

            out += SvgPathNode(id, p)
        }

        return out
    }

    private fun attr(attrs: String, name: String): String? {
        val r = Regex("""\b$name\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        return r.find(attrs)?.groupValues?.get(1)
    }

    private fun attrFloat(attrs: String, name: String): Float? {
        val v = attr(attrs, name) ?: return null
        // Por si viene "60px" o algo raro, nos quedamos con número simple
        val cleaned = v.trim().replace("px", "")
        return cleaned.toFloatOrNull()
    }
}
