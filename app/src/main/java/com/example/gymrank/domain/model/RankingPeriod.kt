package com.example.gymrank.domain.model

enum class RankingPeriod(
    val label: String,
    val field: String
) {
    WEEKLY("Semanal", "weeklyPoints"),
    MONTHLY("Mensual", "monthlyPoints"),
    ALL_TIME("Historial", "totalPoints"),
    GLOBAL("Global", "globalPoints")
}