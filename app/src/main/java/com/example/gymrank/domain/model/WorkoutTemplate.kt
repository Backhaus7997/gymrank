package com.example.gymrank.domain.model

data class WorkoutTemplate(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val visibility: String = "official", // official | community
    val isPro: Boolean = false,
    val weeks: Int = 0,
    val level: String = "",
    val frequencyPerWeek: Int = 0,
    val goalTags: List<String> = emptyList(),
    val coverUrl: String? = null
)