package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun saveWorkout(workout: Workout)
    fun getLastWorkout(): Flow<Workout?>
}
