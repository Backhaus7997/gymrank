package com.example.gymrank.ui.screens.onboarding

data class OnboardingUiState(
    val edad: String = "",
    val peso: String = "",
    val altura: String = "",
    val genero: String = "",
    val step: OnboardingStep = OnboardingStep.Edad,
    val isLoading: Boolean = false,
    val error: String? = null
)
