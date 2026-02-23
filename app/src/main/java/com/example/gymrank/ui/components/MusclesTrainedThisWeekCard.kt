package com.example.gymrank.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.gymrank.R
import com.example.gymrank.ui.theme.GymRankColors

private data class MuscleMask(
    @DrawableRes val front: Int? = null,
    @DrawableRes val back: Int? = null
)

/**
 * ✅ IMPORTANTE
 * - Cada máscara debe ser PNG con fondo TRANSPARENTE
 * - La zona a pintar (músculo) en BLANCO puro (#FFFFFF)
 * - Mismo tamaño/encuadre que la imagen base (muscle_front / muscle_back)
 *
 * ✅ NOTA
 * Este archivo asume que tu enum MuscleId incluye:
 * Chest, Abs, Obliques, Shoulders, Biceps, Forearms, Legs, Calves, Traps,
 * Triceps, Lats, Back, LowerBack, Glutes
 *
 * Y que existen estos drawables en res/drawable:
 * mask_front_* y mask_back_*
 */
private val muscleMasks: Map<MuscleId, MuscleMask> = mapOf(
    // =====================
    // FRONT-only
    // =====================
    MuscleId.Chest to MuscleMask(front = R.drawable.mask_front_chest),
    MuscleId.Abs to MuscleMask(front = R.drawable.mask_front_abs),
    MuscleId.Obliques to MuscleMask(front = R.drawable.mask_front_obliques),
    MuscleId.Biceps to MuscleMask(front = R.drawable.mask_front_biceps),
    MuscleId.Quads to MuscleMask(front = R.drawable.mask_front_quads),

    // =====================
    // BACK-only
    // =====================
    MuscleId.Triceps to MuscleMask(back = R.drawable.mask_back_triceps),
    MuscleId.Lats to MuscleMask(back = R.drawable.mask_back_lats),
    MuscleId.Back to MuscleMask(back = R.drawable.mask_back_back),
    MuscleId.LowerBack to MuscleMask(back = R.drawable.mask_back_lowerback),
    MuscleId.Glutes to MuscleMask(back = R.drawable.mask_back_glutes),
    MuscleId.Hamstrings to MuscleMask(back = R.drawable.mask_back_hamstrings),

    // =====================
    // BOTH (FRONT + BACK)  ✅ ESTA ES LA CLAVE
    // =====================
    MuscleId.Shoulders to MuscleMask(
        front = R.drawable.mask_front_shoulders,
        back = R.drawable.mask_back_shoulders
    ),
    MuscleId.Traps to MuscleMask(
        front = R.drawable.mask_front_traps,
        back = R.drawable.mask_back_traps
    ),
    MuscleId.Forearms to MuscleMask(
        front = R.drawable.mask_front_forearms,
        back = R.drawable.mask_back_forearms
    ),
    MuscleId.Calves to MuscleMask(
        front = R.drawable.mask_front_calves,
        back = R.drawable.mask_back_calves
    ),
)

private fun colorForCount(count: Int, accent: Color): Color? {
    val alpha = when {
        count <= 0 -> return null
        count == 1 -> 0.28f
        count == 2 -> 0.58f
        else -> 0.92f
    }
    return accent.copy(alpha = alpha)
}

/**
 * Dibuja:
 * 1) Base (line art)
 * 2) Máscaras tintadas encima (según counts)
 *
 * counts: Map<MuscleId, Int> donde Int = cantidad de veces entrenado (o intensidad)
 */
@Composable
fun BodyWithMuscleMasks(
    isFront: Boolean,
    counts: Map<MuscleId, Int>,
    modifier: Modifier = Modifier
) {
    val accent = GymRankColors.PrimaryAccent
    val baseRes = if (isFront) R.drawable.muscle_front else R.drawable.muscle_back

    // Orden fijo para que el overlay quede consistente y “lindo”
    val drawOrder = if (isFront) {
        listOf(
            // piernas primero (debajo)
            MuscleId.Calves, MuscleId.Quads, MuscleId.Shoulders, MuscleId.Traps,
            // core + pecho
            MuscleId.Abs, MuscleId.Obliques, MuscleId.Chest,
            // brazos
            MuscleId.Biceps, MuscleId.Forearms
        )
    } else {
        listOf(
            // piernas primero
            MuscleId.Calves, MuscleId.Hamstrings,
            // glúteos
            MuscleId.Glutes,
            // espalda (de abajo hacia arriba aprox)
            MuscleId.LowerBack, MuscleId.Lats, MuscleId.Back, MuscleId.Traps,
            // brazos
            MuscleId.Triceps, MuscleId.Forearms,
            // hombros al final
            MuscleId.Shoulders
        )
    }

    Box(modifier = modifier) {
        // 1) Base line art
        Image(
            painter = painterResource(baseRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 2) Overlays (máscaras)
        drawOrder.forEach { muscleId ->
            val count = counts[muscleId] ?: 0
            val mask = muscleMasks[muscleId] ?: return@forEach
            val maskRes = if (isFront) mask.front else mask.back
            val tint = colorForCount(count, accent)

            if (maskRes != null && tint != null) {
                Image(
                    painter = painterResource(maskRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(tint)
                )
            }
        }
    }
}
