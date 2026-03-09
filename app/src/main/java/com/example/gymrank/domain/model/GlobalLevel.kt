package com.example.gymrank.domain.model

enum class GlobalLevel(val displayName: String, val minLevel: Int, val maxLevel: Int?) {
    BRONZE("Bronze", 1, 5),
    IRON("Iron", 6, 10),
    STEEL("Steel", 11, 15),
    ATHLETE("Athlete", 16, 20),
    BEAST("Beast", 21, 25),
    TITAN("Titan", 26, 30),
    LEGEND("Legend", 31, 40),
    IMMORTAL("Immortal", 41, 50),
    OLYMPIAN("Olympian", 51, null);

    companion object {
        fun fromLevel(level: Int): GlobalLevel {
            return values().find {
                level >= it.minLevel && (it.maxLevel == null || level <= it.maxLevel)
            } ?: BRONZE
        }

        fun calculateLevel(globalPoints: Int): Int {
            // Cada nivel requiere más puntos que el anterior
            // Fórmula simple: level = sqrt(globalPoints / 10) + 1
            return kotlin.math.max(1, kotlin.math.sqrt(globalPoints.toDouble() / 10.0).toInt() + 1)
        }
    }
}
