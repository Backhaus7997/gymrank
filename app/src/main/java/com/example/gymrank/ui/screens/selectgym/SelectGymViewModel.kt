package com.example.gymrank.ui.screens.selectgym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.GymRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SelectGymViewModel(
    private val repo: GymRepositoryImpl = GymRepositoryImpl()
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
}
