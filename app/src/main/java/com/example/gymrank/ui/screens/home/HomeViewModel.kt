package com.example.gymrank.ui.screens.home

import androidx.lifecycle.ViewModel
import com.example.gymrank.domain.model.Gym
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        // Mock data para testing
        val mockChallenges = listOf(
            ChallengeCard(
                id = "season",
                title = "Temporada de Verano",
                subtitle = "Termina en 45 días",
                emoji = "🏖️",
                progress = 0.65f,
                isActive = true
            ),
            ChallengeCard(
                id = "weekly",
                title = "Desafío Semanal",
                subtitle = "3 de 5 entrenamientos",
                emoji = "🔥",
                progress = 0.6f,
                isActive = true
            ),
            ChallengeCard(
                id = "monthly",
                title = "Objetivo del Mes",
                subtitle = "Subir 10 posiciones",
                emoji = "🎯",
                progress = 0.4f,
                isActive = true
            )
        )

        _uiState.update {
            it.copy(
                userName = "Atleta",
                hasGym = false,
                challenges = mockChallenges
            )
        }
    }

    fun setGymData(gym: Gym) {
        _uiState.update {
            it.copy(
                hasGym = true,
                gymName = gym.name,
                gymLocation = gym.city,
                currentRanking = 14,
                currentPoints = 6240
            )
        }
    }
}
