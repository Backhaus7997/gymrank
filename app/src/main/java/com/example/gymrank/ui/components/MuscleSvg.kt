package com.example.gymrank.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser // Compose Vector PathParser

private fun pathFromSvg(data: String): Path =
    PathParser().parsePathString(data).toPath()

/**
 * Simple SVG-like path registry for a human body (front/back) grouped by major muscles.
 * The pathData strings are simplified to keep size reasonable. Replace with real SVG later.
 */
object MuscleSvgRegistry {

    /** ViewBox used to scale to canvas. */
    val frontViewBox = Rect(0f, 0f, 100f, 200f)
    val backViewBox = Rect(0f, 0f, 100f, 200f)

    /** Front: base silhouette (neutral). */
    val frontBase: Path = pathFromSvg(
        // head + torso + legs simplified
        "M50,6 a10,10 0 1,0 0,20 a10,10 0 1,0 0,-20 " +
        "M30,30 C25,40 25,60 32,80 C38,100 45,120 50,125 C55,120 62,100 68,80 C75,60 75,40 70,30 Z " +
        "M40,125 L46,190 L54,190 L60,125 Z"
    )

    /** Back: base silhouette (neutral). */
    val backBase: Path = pathFromSvg(
        "M50,6 a10,10 0 1,0 0,20 a10,10 0 1,0 0,-20 " +
        "M30,30 C26,44 26,68 33,86 C40,110 46,124 50,128 C54,124 60,110 67,86 C74,68 74,44 70,30 Z " +
        "M40,128 L46,190 L54,190 L60,128 Z"
    )

    /** Front muscle groups */
    val frontMuscles: Map<MuscleId, Path> = mapOf(
        MuscleId.Chest to pathFromSvg(
            "M34,40 C40,36 60,36 66,40 C64,46 56,50 50,50 C44,50 36,46 34,40 Z"
        ),
        MuscleId.Abs to pathFromSvg(
            "M44,52 C46,64 54,64 56,52 L56,82 L44,82 Z"
        ),
        MuscleId.Shoulders to pathFromSvg(
            "M28,36 C32,32 38,30 42,32 L40,40 C36,40 32,40 28,36 Z"
        ),
        MuscleId.Biceps to pathFromSvg(
            "M24,48 C22,54 22,60 24,64 L28,62 L30,56 Z"
        ),
        MuscleId.Triceps to pathFromSvg(
            "M76,48 C78,54 78,60 76,64 L72,62 L70,56 Z"
        ),
        MuscleId.Quads to pathFromSvg(
            "M44,90 C46,112 54,112 56,90 L56,170 L44,170 Z"
        ),
        MuscleId.Glutes to pathFromSvg(
            "M40,84 C50,88 60,84 62,92 L38,92 Z"
        ),
        MuscleId.Calves to pathFromSvg(
            "M46,170 C48,178 52,178 54,170 L54,190 L46,190 Z"
        ),
        MuscleId.Traps to pathFromSvg(
            "M46,170 C48,178 52,178 54,170 L54,190 L46,190 Z"
        )
    )

    /** Back muscle groups */
    val backMuscles: Map<MuscleId, Path> = mapOf(
        MuscleId.Back to pathFromSvg(
            "M36,44 C40,48 60,48 64,44 C62,70 58,86 50,90 C42,86 38,70 36,44 Z"
        ),
        MuscleId.Shoulders to pathFromSvg(
            "M28,38 C32,34 38,32 42,34 L40,42 C36,42 32,42 28,38 Z"
        ),
        MuscleId.Biceps to pathFromSvg(
            "M24,50 C22,56 22,62 24,66 L28,64 L30,58 Z"
        ),
        MuscleId.Triceps to pathFromSvg(
            "M76,50 C78,56 78,62 76,66 L72,64 L70,58 Z"
        ),
        MuscleId.Hamstrings to pathFromSvg(
            "M44,92 C46,114 54,114 56,92 L56,172 L44,172 Z"
        ),
        MuscleId.Glutes to pathFromSvg(
            "M40,86 C50,90 60,86 62,94 L38,94 Z"
        ),
        MuscleId.Calves to pathFromSvg(
            "M46,172 C48,180 52,180 54,172 L54,190 L46,190 Z"
        )
    )
}
