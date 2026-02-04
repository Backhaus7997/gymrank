package com.example.gymrank.domain.model

/**
 * UI model for Gym cards with image support
 */
data class GymUi(
    val id: String,
    val name: String,
    val location: String,
    val imageUrl: String,
    val isHighCompetition: Boolean = false
)
