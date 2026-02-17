package com.example.gymrank.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutRepositoryImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutRepo = WorkoutRepositoryImpl(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // collect last workout continuously
        viewModelScope.launch {
            workoutRepo.getLastWorkout().collect { w ->
                _uiState.value = _uiState.value.copy(lastWorkout = w)
            }
        }
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
