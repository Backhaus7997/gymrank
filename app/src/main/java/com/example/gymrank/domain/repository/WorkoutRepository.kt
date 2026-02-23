package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun saveWorkout(workout: Workout)

    // ✅ historial
    fun getWorkouts(): Flow<List<Workout>>

    // (lo dejás si lo usás en otro lado)
    fun getLastWorkout(): Flow<Workout?>
}
