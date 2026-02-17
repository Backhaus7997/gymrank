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
import java.util.Locale

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
                    // ✅ Orden estable para evitar que cambien posiciones/imagenes “solas”
                    val stable = gyms.sortedWith(
                        compareBy<Gym> { it.city.lowercase(Locale.getDefault()) }
                            .thenBy { it.name.lowercase(Locale.getDefault()) }
                            .thenBy { it.id }
                    )

                    _uiState.update { state ->
                        // ✅ Mantener el filtro actual si ya había búsqueda
                        val filtered = filterGyms(stable, state.searchQuery)

                        state.copy(
                            isLoading = false,
                            gyms = stable,
                            filteredGyms = filtered
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
            val filtered = filterGyms(state.gyms, query)
            state.copy(
                searchQuery = query,
                filteredGyms = filtered
            )
        }
    }

    fun retryLoadGyms() {
        loadGyms()
    }

    private fun filterGyms(gyms: List<Gym>, query: String): List<Gym> {
        val q = query.trim()
        if (q.isBlank()) return gyms

        return gyms.filter { gym ->
            gym.name.contains(q, ignoreCase = true) ||
                    gym.city.contains(q, ignoreCase = true)
        }
    }
}
