package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val templates: List<ChallengeTemplate> = emptyList()
)

class DiscoverViewModel(
    private val repo: ChallengeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            runCatching { repo.getChallengeTemplates() }
                .onSuccess { list ->
                    _state.value = DiscoverUiState(
                        loading = false,
                        templates = list
                    )
                }
                .onFailure { e ->
                    _state.value = DiscoverUiState(
                        loading = false,
                        error = e.message ?: "Error cargando desafíos"
                    )
                }
        }
    }
}