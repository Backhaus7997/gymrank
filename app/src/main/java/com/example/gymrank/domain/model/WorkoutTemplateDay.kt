package com.example.gymrank.domain.model

data class WorkoutTemplateDay(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val weekday: Int = 1,
    val order: Int = 0,
    val exercises: List<WorkoutTemplateExercise> = emptyList()
)

data class WorkoutTemplateExercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val usesBodyweight: Boolean = false,
    val weightKg: Int? = null,
    val weekday: Int = 1
)