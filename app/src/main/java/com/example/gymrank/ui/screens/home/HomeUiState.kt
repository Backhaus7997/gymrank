package com.example.gymrank.ui.screens.home

data class HomeUiState(
    val userName: String = "Usuario",
    val hasGym: Boolean = false,
    val gymName: String? = null,
    val gymLocation: String? = null,
    val currentRanking: Int? = null,
    val currentPoints: Int? = null,
    val challenges: List<ChallengeCard> = emptyList(),
    val isLoading: Boolean = false
)

data class ChallengeCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val progress: Float = 0f,
    val isActive: Boolean = true
)

sealed class QuickAction(
    val id: String,
    val title: String,
    val icon: String
) {
    object LogWorkout : QuickAction("log_workout", "Registrar\nentrenamiento", "💪")
    object LogPR : QuickAction("log_pr", "Cargar\nmarca", "🏆")
    object ViewProgress : QuickAction("view_progress", "Ver\nprogreso", "📊")
    object InviteFriend : QuickAction("invite_friend", "Invitar a\nun amigo", "👥")
}
