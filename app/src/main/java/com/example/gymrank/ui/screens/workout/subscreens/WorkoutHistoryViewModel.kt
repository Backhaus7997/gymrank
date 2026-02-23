package com.example.gymrank.ui.screens.workout.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutHistoryUiState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class WorkoutHistoryViewModel(
    private val repo: WorkoutRepository = WorkoutRepositoryFirestoreImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repo.getWorkouts().collect { list ->
                    _uiState.update {
                        it.copy(
                            workouts = list,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error leyendo entrenamientos"
                    )
                }
            }
        }
    }
}