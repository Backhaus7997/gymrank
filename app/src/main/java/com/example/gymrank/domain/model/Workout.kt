package com.example.gymrank.domain.model

data class Workout(
    val timestampMillis: Long,
    val durationMinutes: Int,
    val type: String,
    val muscles: List<String>,
    val intensity: String,
    val notes: String?
)
