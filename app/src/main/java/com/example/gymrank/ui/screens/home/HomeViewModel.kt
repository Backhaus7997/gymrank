package com.example.gymrank.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutRepositoryImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.ui.components.MuscleId
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl


class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val workoutRepo = WorkoutRepositoryFirestoreImpl()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val user = auth.currentUser
        _uiState.update { it.copy(userName = user?.email?.substringBefore("@") ?: "Atleta") }

        // ✅ last workout (sigue existiendo)
        viewModelScope.launch {
            workoutRepo.getLastWorkout().collectLatest { w ->
                _uiState.update { it.copy(lastWorkout = w) }
            }
        }

        // ✅ semana completa (para pintar)
        viewModelScope.launch {
            workoutRepo.getWorkouts().collectLatest { list ->
                val (startWeek, endWeek) = weekBoundsMillis()

                val week = list.filter { w ->
                    val t = w.timestampMillis ?: w.createdAt ?: 0L
                    t in startWeek..endWeek
                }

                val (front, back) = buildCountsFromWorkouts(week)

                _uiState.update {
                    it.copy(
                        weekFrontCounts = front,
                        weekBackCounts = back,
                        weekWorkoutsCount = week.size
                    )
                }
            }
        }

        loadHomeData()
    }

    private fun loadHomeData() {
        val mockChallenges = listOf(
            ChallengeCard("season", "Temporada de Verano", "Termina en 45 días", "🏖️", 0.65f, true),
            ChallengeCard("weekly", "Desafío Semanal", "3 de 5 entrenamientos", "🔥", 0.6f, true),
            ChallengeCard("monthly", "Objetivo del Mes", "Subir 10 posiciones", "🎯", 0.4f, true)
        )

        _uiState.update { it.copy(hasGym = false, challenges = mockChallenges) }
    }

    fun setGymData(gym: Gym) {
        _uiState.update {
            it.copy(
                hasGym = true,
                gymName = gym.name,
                gymLocation = gym.city,
                currentRanking = 14,
                currentPoints = 6240
            )
        }
    }

    private fun weekBoundsMillis(): Pair<Long, Long> {
        // semana: Lunes 00:00 → Domingo 23:59:59
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz)

        cal.firstDayOfWeek = Calendar.MONDAY

        // end = ahora (pero lo dejamos como domingo fin de día para estabilidad)
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val startCal = cal.clone() as Calendar
        startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        return startCal.timeInMillis to endCal.timeInMillis
    }

    private fun buildCountsFromWorkouts(
        workouts: List<Workout>
    ): Pair<Map<MuscleId, Int>, Map<MuscleId, Int>> {
        val front = mutableMapOf<MuscleId, Int>()
        val back = mutableMapOf<MuscleId, Int>()

        fun inc(map: MutableMap<MuscleId, Int>, id: MuscleId) {
            map[id] = (map[id] ?: 0) + 1
        }

        workouts.forEach { w ->
            w.muscles.forEach { raw ->
                when (raw.trim().lowercase()) {
                    "pecho", "pectorales", "pectoral" -> inc(front, MuscleId.Chest)

                    "abdomen", "abs", "core" -> inc(front, MuscleId.Abs)
                    "oblicuos", "oblicuo", "obliques" -> inc(front, MuscleId.Obliques)

                    "hombros", "deltoides", "deltoide", "shoulders" -> {
                        inc(front, MuscleId.Shoulders); inc(back, MuscleId.Shoulders)
                    }

                    "trapecios", "trapecio", "traps", "trap" -> {
                        inc(front, MuscleId.Traps); inc(back, MuscleId.Traps)
                    }

                    "biceps", "bíceps", "bicep" -> inc(front, MuscleId.Biceps)
                    "antebrazos", "antebrazo", "forearms", "forearm" -> {
                        inc(front, MuscleId.Forearms); inc(back, MuscleId.Forearms)
                    }

                    "piernas", "cuadriceps", "cuádriceps", "quads", "quadriceps" -> inc(front, MuscleId.Quads)
                    "pantorrillas", "pantorrilla", "gemelos", "calves", "calf" -> {
                        inc(front, MuscleId.Calves); inc(back, MuscleId.Calves)
                    }

                    "gluteos", "glúteos", "glutes", "glute" -> inc(back, MuscleId.Glutes)

                    "espalda", "back" -> inc(back, MuscleId.Back)
                    "dorsales", "dorsal", "lats", "dorsal ancho", "dorsales anchos" -> inc(back, MuscleId.Lats)
                    "lumbar", "lumbares", "lower back", "espalda baja" -> inc(back, MuscleId.LowerBack)

                    "triceps", "tríceps", "tricep" -> inc(back, MuscleId.Triceps)
                    "isquios", "isquiotibiales", "hamstrings", "femorales" -> inc(back, MuscleId.Hamstrings)
                }
            }
        }

        return front.toMap() to back.toMap()
    }
}
