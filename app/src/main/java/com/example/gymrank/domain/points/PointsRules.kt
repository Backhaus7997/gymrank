package com.example.gymrank.domain.points

object PointsRules {

    // Lo que vos definiste antes:
    // facil 250, medio 350, dificil 500
    fun fromDifficulty(difficultyOrLevel: String?): Int? {
        val v = difficultyOrLevel?.trim()?.lowercase() ?: return null
        return when (v) {
            "facil", "fácil", "easy" -> 250
            "medio", "medium" -> 350
            "dificil", "difícil", "hard" -> 500
            else -> null
        }
    }

    /**
     * Si no hay dificultad clara, calculamos por duración.
     * Ajustá los cortes si los tuyos ya están definidos distinto.
     */
    fun fromDurationDays(durationDays: Int): Int {
        return when {
            durationDays <= 7 -> 250
            durationDays in 8..14 -> 350
            else -> 500
        }
    }

    /**
     * Regla final:
     * - Si existe dificultad/level => usa eso
     * - Si no => usa duración
     */
    fun resolve(difficultyOrLevel: String?, durationDays: Int?): Int {
        val byDiff = fromDifficulty(difficultyOrLevel)
        if (byDiff != null) return byDiff
        return fromDurationDays(durationDays ?: 0)
    }
}