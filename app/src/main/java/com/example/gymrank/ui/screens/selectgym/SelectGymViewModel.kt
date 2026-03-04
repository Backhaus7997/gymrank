package com.example.gymrank.ui.screens.selectgym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.GymRepositoryImpl
import com.example.gymrank.data.repository.UserRepositoryImpl
import com.example.gymrank.domain.model.Gym
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SelectGymViewModel(
    private val repo: GymRepositoryImpl = GymRepositoryImpl(),
    private val userRepo: UserRepositoryImpl = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectGymUiState(isLoading = true))
    val uiState: StateFlow<SelectGymUiState> = _uiState

    init {
        loadGyms()
    }

    fun loadGyms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repo.getGyms()
                .onSuccess { gyms ->
                    _uiState.value = SelectGymUiState(
                        isLoading = false,
                        gyms = gyms,
                        error = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = SelectGymUiState(
                        isLoading = false,
                        gyms = emptyList(),
                        error = e.message ?: "Error cargando gimnasios"
                    )
                }
        }
    }

    fun selectGym(gym: Gym, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepo.saveSelectedGym(gym)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "No se pudo guardar el gimnasio"
                )
            }
        }
    }

    fun continueWithoutGym(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepo.clearSelectedGym()
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "No se pudo continuar sin gimnasio"
                )
            }
        }
    }
}