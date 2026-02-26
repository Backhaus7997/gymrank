package com.example.gymrank.ui.screens.workout.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutTemplateRepositoryFirestoreImpl
import com.example.gymrank.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val templates: List<WorkoutTemplate> = emptyList()
)

class ExploreViewModel(
    private val repo: WorkoutTemplateRepositoryFirestoreImpl = WorkoutTemplateRepositoryFirestoreImpl()
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState(loading = true))
    val state: StateFlow<ExploreUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.getTemplates() }
                .onSuccess { list ->
                    _state.value = ExploreUiState(loading = false, templates = list, error = null)
                }
                .onFailure { e ->
                    _state.value = ExploreUiState(loading = false, templates = emptyList(), error = e.message ?: "Error")
                }
        }
    }
}