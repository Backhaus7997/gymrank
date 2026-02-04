package com.example.gymrank.ui.screens.selectgym

import com.example.gymrank.domain.model.Gym

data class SelectGymUiState(
    val isLoading: Boolean = false,
    val gyms: List<Gym> = emptyList(),
    val filteredGyms: List<Gym> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)
