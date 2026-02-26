package com.example.gymrank.ui.screens.workout.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutTemplateRepositoryFirestoreImpl
import com.example.gymrank.domain.model.WorkoutTemplate
import com.example.gymrank.domain.model.WorkoutTemplateDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProgramDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val template: WorkoutTemplate? = null,
    val days: List<WorkoutTemplateDay> = emptyList()
)

class ProgramDetailViewModel(
    private val repo: WorkoutTemplateRepositoryFirestoreImpl = WorkoutTemplateRepositoryFirestoreImpl()
) : ViewModel() {

    private val _state = MutableStateFlow(ProgramDetailUiState())
    val state: StateFlow<ProgramDetailUiState> = _state

    fun load(templateId: String) {
        viewModelScope.launch {
            _state.value = ProgramDetailUiState(loading = true)
            runCatching {
                val template = repo.getTemplateById(templateId)
                val days = repo.getDays(templateId)
                template to days
            }
                .onSuccess { (template, days) ->
                    _state.value = ProgramDetailUiState(
                        loading = false,
                        template = template,
                        days = days,
                        error = null
                    )
                }
                .onFailure { e ->
                    _state.value = ProgramDetailUiState(
                        loading = false,
                        error = e.message ?: "Error",
                        template = null,
                        days = emptyList()
                    )
                }
        }
    }
}