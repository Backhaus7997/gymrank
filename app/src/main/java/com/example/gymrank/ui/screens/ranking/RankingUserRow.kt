package com.example.gymrank.ui.screens.ranking

data class RankingUserRow(
    val uid: String,
    val displayName: String,
    val photoUrl: String? = null,
    val points: Long = 0L,
    val gymId: String? = null
)