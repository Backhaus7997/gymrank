package com.example.gymrank.domain.model

data class Workout(
    val id: String = "",
    val createdAt: Long? = null,           // lo seteamos desde Firestore
    val updatedAt: Long? = null,

    // Para tu UI de rutina
    val title: String = "",
    val description: String? = null,
    val gymId: String? = null,
    val exercises: List<WorkoutExercise> = emptyList(),

    // Campos que ya tenías (los dejamos por compatibilidad)
    val timestampMillis: Long? = null,
    val durationMinutes: Int? = null,
    val type: String? = null,
    val muscles: List<String> = emptyList(),
    val intensity: String? = null,
    val notes: String? = null
)

data class WorkoutExercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val usesBodyweight: Boolean = false,
    val weightKg: Int? = null
)
