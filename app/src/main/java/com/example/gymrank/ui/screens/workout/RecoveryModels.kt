package com.example.gymrank.ui.screens.workout

import com.example.gymrank.domain.model.Workout
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

data class MuscleRecoveryItem(
    val name: String,
    val percent: Int,             // 0..100
    val progress: Float,          // 0..1
    val nextDueMillis: Long?,     // próximo entreno planificado (o estimado)
    val lastTrainedMillis: Long?, // último entreno real
    val freqDays: Int,            // frecuencia estimada por historial o plan (si existe)
    val daysLeft: Int             // días que faltan (0 = hoy/vencido)
)

/**
 * Plan semanal: key = Calendar.DAY_OF_WEEK (1=Dom ... 7=Sáb)
 * musclesByDayOfWeek[Calendar.TUESDAY] = listOf("Hombros","Bíceps","Tríceps")
 */
data class WeeklyPlan(
    val musclesByDayOfWeek: Map<Int, List<String>> = emptyMap()
) {
    fun musclesFor(dayOfWeek: Int): List<String> = musclesByDayOfWeek[dayOfWeek].orEmpty()
}

object RecoveryEstimator {

    private const val DEFAULT_FREQ_DAYS = 7
    private const val MAX_FREQ_DAYS = 14

    fun estimate(
        allWorkouts: List<Workout>,
        weeklyPlan: WeeklyPlan? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MuscleRecoveryItem> {

        val todayStart = startOfDayMillis(nowMillis)

        /**
         * Key = normalizado (sin acentos, lowercase, etc.)
         * Value = displayName (bonito) + días entrenados
         */
        data class MuscleAcc(
            val displayName: String,
            val days: MutableList<Long>
        )

        val trainedByMuscleKey = linkedMapOf<String, MuscleAcc>()

        fun addMuscleOccurrence(rawName: String, dayStart: Long) {
            val display = canonicalDisplayName(rawName) ?: return
            val key = normalizeKey(display)

            val acc = trainedByMuscleKey.getOrPut(key) {
                MuscleAcc(displayName = display, days = mutableListOf())
            }

            acc.days.add(dayStart)
        }

        // =========================
        // 1) Historial real
        // =========================
        allWorkouts.forEach { w ->
            val ts = (w.timestampMillis ?: w.createdAt ?: w.updatedAt ?: 0L)
            if (ts <= 0L) return@forEach

            val dayStart = startOfDayMillis(ts)
            val muscles = w.muscles.map { it.trim() }.filter { it.isNotBlank() }

            muscles.forEach { m ->
                addMuscleOccurrence(m, dayStart)
            }
        }

        // =========================
        // 2) Agregar músculos del plan aunque no tengan historial
        // =========================
        weeklyPlan?.musclesByDayOfWeek
            ?.values
            ?.flatten()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.forEach { m ->
                val display = canonicalDisplayName(m) ?: return@forEach
                val key = normalizeKey(display)
                trainedByMuscleKey.getOrPut(key) { MuscleAcc(displayName = display, days = mutableListOf()) }
            }

        val out = trainedByMuscleKey.entries.map { (_, acc) ->
            val muscleDisplay = acc.displayName
            val days = acc.days.distinct().sortedDescending()

            val lastTrained = days.firstOrNull()

            val gapsDays = days.zipWithNext { newer, older ->
                TimeUnit.MILLISECONDS.toDays(newer - older).toInt()
            }.filter { it > 0 }

            val avgGapDays = when {
                gapsDays.isEmpty() -> DEFAULT_FREQ_DAYS
                else -> gapsDays.take(4)
                    .let { (it.sum().toFloat() / it.size.toFloat()).roundToInt() }
                    .coerceIn(1, MAX_FREQ_DAYS)
            }

            // ✅ si hay plan, la frecuencia se calcula por "veces por semana" (más real)
            val planFreqDays = weeklyPlan?.let { freqDaysFromPlan(it, muscleDisplay) }
            val effectiveFreqDays = (planFreqDays ?: avgGapDays).coerceIn(1, MAX_FREQ_DAYS)

            // Próximo due: prioriza plan semanal (si existe)
            val nextDueFromPlan = weeklyPlan?.let { nextDueFromWeeklyPlan(it, muscleDisplay, todayStart) }
            val nextDueFallback = lastTrained?.let {
                it + TimeUnit.DAYS.toMillis(effectiveFreqDays.toLong())
            }
            val nextDue = nextDueFromPlan ?: nextDueFallback

            val daysLeft = when {
                nextDue == null -> 0
                nextDue <= todayStart -> 0
                else -> ceil(TimeUnit.MILLISECONDS.toHours(nextDue - todayStart) / 24.0)
                    .toInt()
                    .coerceAtLeast(1)
            }

            val percent = when {
                nextDue == null -> 100
                nextDue <= todayStart -> 100
                lastTrained == null -> curvePercent(daysLeft, effectiveFreqDays)
                else -> {
                    val totalDays = TimeUnit.MILLISECONDS
                        .toDays((nextDue - lastTrained).coerceAtLeast(TimeUnit.DAYS.toMillis(1)))
                        .toInt()
                        .coerceAtLeast(1)

                    val elapsedDays = TimeUnit.MILLISECONDS
                        .toDays((todayStart - lastTrained).coerceAtLeast(0L))
                        .toInt()
                        .coerceIn(0, totalDays)

                    ((elapsedDays.toFloat() / totalDays.toFloat()) * 100f)
                        .roundToInt()
                        .coerceIn(0, 100)
                }
            }

            MuscleRecoveryItem(
                name = muscleDisplay,
                percent = percent,
                progress = (percent / 100f).coerceIn(0f, 1f),
                nextDueMillis = nextDue,
                lastTrainedMillis = lastTrained,
                freqDays = effectiveFreqDays,
                daysLeft = daysLeft
            )
        }

        return out.sortedWith(
            compareBy<MuscleRecoveryItem> { it.daysLeft }
                .thenByDescending { it.percent }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
        )
    }

    /**
     * Curva para que:
     * - daysLeft=1 => casi 100 (ej 95+)
     * - daysLeft=0 => 100
     * - daysLeft grande => baja
     */
    private fun curvePercent(daysLeft: Int, freqDays: Int): Int {
        if (daysLeft <= 0) return 100
        val f = freqDays.coerceAtLeast(1)

        val ratio = (daysLeft.toFloat() / f.toFloat()).coerceIn(0f, 1f)
        val gamma = 0.55f
        val p = 100f * (1f - ratio.pow(gamma))
        return p.roundToInt().coerceIn(0, 100)
    }

    private fun startOfDayMillis(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Devuelve el próximo día (desde hoy inclusive) donde el plan contenga ese músculo.
     * OJO: comparamos por key normalizado (para que Cuadriceps/Cuádriceps sea lo mismo).
     */
    private fun nextDueFromWeeklyPlan(plan: WeeklyPlan, muscleName: String, todayStart: Long): Long? {
        val targetKey = normalizeKey(canonicalDisplayName(muscleName) ?: muscleName)

        val cal = Calendar.getInstance()
        cal.timeInMillis = todayStart

        repeat(8) {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val found = plan.musclesFor(dow).any { planMuscle ->
                val key = normalizeKey(canonicalDisplayName(planMuscle) ?: planMuscle)
                key == targetKey
            }
            if (found) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /**
     * Calcula frecuencia (días) a partir del plan:
     * - cuenta cuántas veces aparece el músculo en la semana (1..7)
     * - freqDays = ceil(7 / vecesPorSemana)
     */
    private fun freqDaysFromPlan(plan: WeeklyPlan, muscleName: String): Int? {
        val targetKey = normalizeKey(canonicalDisplayName(muscleName) ?: muscleName)

        val weeklyHits = plan.musclesByDayOfWeek.values.sumOf { list ->
            list.count { normalizeKey(canonicalDisplayName(it) ?: it) == targetKey }
        }

        if (weeklyHits <= 0) return null

        return ceil(7.0 / weeklyHits.toDouble())
            .toInt()
            .coerceIn(1, MAX_FREQ_DAYS)
    }

    /**
     * ✅ Normaliza para usar como key (sin acentos, lower, trim)
     */
    private fun normalizeKey(s: String): String =
        s.trim()
            .lowercase(Locale.getDefault())
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
            .replace("ü", "u")
            .replace(Regex("""\s+"""), " ")

    /**
     * ✅ Canoniza nombres para que NO se dupliquen y puedas elegir 1 display fijo.
     * IMPORTANTES:
     * - Cuadriceps/Cuádriceps -> "Cuádriceps"
     * - Pantorrillas/Gemelos -> "Gemelos"  (así Pantorrillas NO aparece)
     */
    private fun canonicalDisplayName(raw: String): String? {
        val k = normalizeKey(raw)

        return when (k) {
            // piernas
            "cuadriceps", "quad", "quads", "quadriceps" -> "Cuádriceps"
            "pantorrillas", "pantorrilla", "gemelos", "gemelo", "calves", "calf" -> "Gemelos"
            "isquios", "isquiotibiales", "hamstrings", "femorales" -> "Isquios"

            // torso/arms
            "pecho", "pectorales", "pectoral" -> "Pecho"
            "espalda", "back", "dorsales", "dorsal", "lats" -> "Espalda"
            "hombros", "deltoides", "deltoide", "shoulders" -> "Hombros"
            "trapecios", "trapecio", "traps", "trap" -> "Trapecios"
            "biceps", "bíceps", "bicep" -> "Bíceps"
            "triceps", "tríceps", "tricep" -> "Tríceps"
            "antebrazos", "antebrazo", "forearms", "forearm" -> "Antebrazos"
            "abdomen", "abs", "core" -> "Abdomen"
            "gluteos", "glúteos", "glutes", "glute" -> "Glúteos"

            else -> {
                // Si querés ser estricto y mostrar SOLO los de tu lista:
                // return null
                // Si preferís mostrar "otros" sin romper:
                raw.trim().takeIf { it.isNotBlank() }
            }
        }
    }
}