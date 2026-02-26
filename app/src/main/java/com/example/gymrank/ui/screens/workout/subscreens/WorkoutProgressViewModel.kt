package com.example.gymrank.ui.screens.workout.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutLog
import com.example.gymrank.data.repository.WorkoutProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class ExerciseProgressPoint(
    val tMillis: Long,
    val maxWeight: Double,
    val maxReps: Int,
    val volume: Double
)

data class ExerciseProgress(
    val exerciseName: String,
    val points: List<ExerciseProgressPoint>
) {
    val lastWeight: Double get() = points.lastOrNull()?.maxWeight ?: 0.0
    val lastReps: Int get() = points.lastOrNull()?.maxReps ?: 0
    val bestWeight: Double get() = points.maxOfOrNull { it.maxWeight } ?: 0.0
    val bestReps: Int get() = points.maxOfOrNull { it.maxReps } ?: 0
    val bestVolume: Double get() = points.maxOfOrNull { it.volume } ?: 0.0
}

data class WorkoutProgressUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workouts: List<WorkoutLog> = emptyList(),
    val selectedMuscle: String? = null,
    val availableMuscles: List<String> = emptyList(),
    val exerciseProgress: List<ExerciseProgress> = emptyList()
)

class WorkoutProgressViewModel(
    private val repo: WorkoutProgressRepository = WorkoutProgressRepository()
) : ViewModel() {

    private val _ui: MutableStateFlow<WorkoutProgressUiState> =
        MutableStateFlow(WorkoutProgressUiState())

    val ui: StateFlow<WorkoutProgressUiState> = _ui

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            runCatching { repo.getWorkoutsForCurrentUser() }
                .onSuccess { workouts ->
                    val musclesFromDb = workouts
                        .flatMap { it.muscles }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    _ui.update {
                        it.copy(
                            isLoading = false,
                            workouts = workouts,
                            availableMuscles = musclesFromDb
                        )
                    }

                    _ui.value.selectedMuscle?.let { selectMuscle(it) }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Error cargando progreso"
                        )
                    }
                }
        }
    }

    fun clearSelection() {
        _ui.update { it.copy(selectedMuscle = null, exerciseProgress = emptyList()) }
    }
    /**
     * Muestra TODOS los ejercicios que alguna vez aparecieron en workouts etiquetados con ese músculo,
     * y arma su progreso a lo largo del tiempo (aunque no aparezcan en todas las sesiones).
     */
    fun selectMuscle(muscle: String) {
        val workouts = _ui.value.workouts

        val taggedWorkouts = workouts.filter { w ->
            w.muscles.any { m -> m.equals(muscle, ignoreCase = true) }
        }

        // Catálogo histórico de ejercicios para ese músculo
        val exerciseNamesForMuscle = taggedWorkouts
            .flatMap { it.exercises }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val progress = exerciseNamesForMuscle.mapNotNull { exName ->
            val points = taggedWorkouts.mapNotNull { w ->
                val matches = w.exercises.filter { it.name.trim().equals(exName, ignoreCase = true) }
                if (matches.isEmpty()) return@mapNotNull null

                var maxWeight = 0.0
                var maxReps = 0
                var maxVolume = 0.0

                for (it in matches) {
                    maxWeight = max(maxWeight, it.weightKg)
                    maxReps = max(maxReps, it.reps)
                    val vol = it.weightKg * it.reps * it.sets
                    if (vol > maxVolume) maxVolume = vol
                }

                ExerciseProgressPoint(
                    tMillis = w.createdAtMillis,
                    maxWeight = maxWeight,
                    maxReps = maxReps,
                    volume = maxVolume
                )
            }.sortedBy { it.tMillis }

            if (points.isEmpty()) null else ExerciseProgress(exerciseName = exName, points = points)
        }.sortedByDescending { it.points.lastOrNull()?.tMillis ?: 0L }

        _ui.update {
            it.copy(
                selectedMuscle = muscle,
                exerciseProgress = progress
            )
        }
    }
}

