package com.example.gymrank.ui.screens.selectgym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.GymRepositoryImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.repository.GymRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectGymViewModel(
    private val gymRepository: GymRepository = GymRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectGymUiState())
    val uiState: StateFlow<SelectGymUiState> = _uiState.asStateFlow()

    init {
        loadGyms()
    }

    private fun loadGyms() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            gymRepository.getGyms()
                .onSuccess { gyms ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            gyms = gyms,
                            filteredGyms = gyms
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar gimnasios"
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.gyms
            } else {
                state.gyms.filter { gym ->
                    gym.name.contains(query, ignoreCase = true) ||
                    gym.city.contains(query, ignoreCase = true)
                }
            }
            state.copy(
                searchQuery = query,
                filteredGyms = filtered
            )
        }
    }

    fun retryLoadGyms() {
        loadGyms()
    }
}
