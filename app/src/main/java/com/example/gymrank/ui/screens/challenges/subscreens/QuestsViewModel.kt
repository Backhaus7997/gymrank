package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.MissionTemplateRepositoryFirestoreImpl
import com.example.gymrank.data.repository.UserMissionsRepositoryFirestoreImpl
import com.example.gymrank.domain.model.MissionTemplate
import com.example.gymrank.domain.model.UserMission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestsUiState(
    val discover: List<MissionTemplate> = emptyList(),
    val library: List<UserMission> = emptyList(),
    val loadingDiscover: Boolean = false,
    val loadingLibrary: Boolean = false,
    val discoverError: String? = null,
    val libraryError: String? = null
)

data class StartState(
    val startedInstanceId: String? = null,
    val startError: String? = null
)

data class CreateState(
    val creating: Boolean = false,
    val createdId: String? = null,
    val error: String? = null
)

class QuestsViewModel(
    private val templatesRepo: MissionTemplateRepositoryFirestoreImpl = MissionTemplateRepositoryFirestoreImpl(),
    private val userMissionsRepo: UserMissionsRepositoryFirestoreImpl = UserMissionsRepositoryFirestoreImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState

    private val _startState = MutableStateFlow(StartState())
    val startState: StateFlow<StartState> = _startState

    private val _createState = MutableStateFlow(CreateState())
    val createState: StateFlow<CreateState> = _createState

    fun clearStartState() {
        _startState.value = StartState()
    }

    fun clearCreateState() {
        _createState.value = CreateState()
    }

    fun observe() {
        observeDiscover()
        observeLibrary()
    }

    private fun observeDiscover() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingDiscover = true, discoverError = null) }
            templatesRepo.observeActiveMissions().collect { res ->
                res.onSuccess { list ->
                    _uiState.update { it.copy(discover = list, loadingDiscover = false, discoverError = null) }
                }.onFailure { e ->
                    _uiState.update { it.copy(loadingDiscover = false, discoverError = e.message ?: "Error cargando misiones") }
                }
            }
        }
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingLibrary = true, libraryError = null) }
            userMissionsRepo.observeMyMissions().collect { res ->
                res.onSuccess { list ->
                    _uiState.update { it.copy(library = list, loadingLibrary = false, libraryError = null) }
                }.onFailure { e ->
                    _uiState.update { it.copy(loadingLibrary = false, libraryError = e.message ?: "Error cargando biblioteca") }
                }
            }
        }
    }

    fun startTemplateMission(t: MissionTemplate) {
        viewModelScope.launch {
            runCatching { userMissionsRepo.startTemplateMission(t) }
                .onSuccess { docId ->
                    _startState.value = StartState(startedInstanceId = docId)
                }
                .onFailure { e ->
                    _startState.value = StartState(startError = e.message ?: "No se pudo iniciar")
                }
        }
    }

    fun createCustomMission(
        title: String,
        description: String,
        durationDays: Int,
        difficulty: Difficulty,
        focus: Focus,
        points: Int,
        objectiveWorkouts: Int
    ) {
        val difficultyLabel = when (difficulty) {
            Difficulty.EASY -> "Fácil"
            Difficulty.MEDIUM -> "Media"
            Difficulty.HARD -> "Difícil"
            Difficulty.INSANE -> "Extrema"
        }

        val (focusKey, focusLabel) = when (focus) {
            Focus.UPPER -> "upper" to "Superior"
            Focus.LOWER -> "lower" to "Inferior"
            Focus.CARDIO -> "cardio" to "Cardio"
            Focus.ABS -> "mobility" to "Movilidad"
        }

        viewModelScope.launch {
            _createState.value = CreateState(creating = true)

            runCatching {
                userMissionsRepo.createCustomMission(
                    title = title,
                    description = description,
                    durationDays = durationDays,
                    difficultyLabel = difficultyLabel,
                    focusKey = focusKey,
                    focusLabel = focusLabel,
                    points = points,
                    objectiveWorkouts = objectiveWorkouts
                )
            }.onSuccess { newId ->
                _createState.value = CreateState(creating = false, createdId = newId)
            }.onFailure { e ->
                _createState.value = CreateState(creating = false, error = e.message ?: "No se pudo crear la misión")
            }
        }
    }
}