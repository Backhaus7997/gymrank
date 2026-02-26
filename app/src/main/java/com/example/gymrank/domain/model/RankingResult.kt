package com.example.gymrank.domain.model

data class RankingResult(
    val top: List<RankingEntryUi>,
    val mePosition: Int,
    val mePoints: Int
)