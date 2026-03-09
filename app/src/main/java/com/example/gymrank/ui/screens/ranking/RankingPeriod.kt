package com.example.gymrank.ui.screens.ranking

enum class RankingPeriod(
    val label: String,
    val field: String
) {
    WEEKLY("Semanal", "weeklyPoints"),
    MONTHLY("Mensual", "monthlyPoints"),
    ALL_TIME("Historial", "totalPoints")
}