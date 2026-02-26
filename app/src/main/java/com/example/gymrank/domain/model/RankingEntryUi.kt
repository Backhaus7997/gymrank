package com.example.gymrank.domain.model

data class RankingEntryUi(
    val position: Int,
    val userId: String,
    val name: String,
    val points: Int,
    val isMe: Boolean
)