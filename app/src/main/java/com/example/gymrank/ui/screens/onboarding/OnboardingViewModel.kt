package com.example.gymrank.ui.screens.onboarding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.UserPreferencesRepositoryImpl
import com.example.gymrank.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val appContext: Context, private val currentUser: User?) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNext() {
        val next = when (_uiState.value.step) {
            OnboardingStep.Edad -> OnboardingStep.Peso
            OnboardingStep.Peso -> OnboardingStep.Altura
            OnboardingStep.Altura -> OnboardingStep.Genero
            OnboardingStep.Genero -> OnboardingStep.Resumen
            OnboardingStep.Resumen -> OnboardingStep.Resumen
        }
        _uiState.value = _uiState.value.copy(step = next)
    }

    fun onBack() {
        val prev = when (_uiState.value.step) {
            OnboardingStep.Resumen -> OnboardingStep.Genero
            OnboardingStep.Genero -> OnboardingStep.Altura
            OnboardingStep.Altura -> OnboardingStep.Peso
            OnboardingStep.Peso -> OnboardingStep.Edad
            OnboardingStep.Edad -> OnboardingStep.Edad
        }
        _uiState.value = _uiState.value.copy(step = prev)
    }

    fun updateEdad(value: String) { _uiState.value = _uiState.value.copy(edad = value) }
    fun updatePeso(value: String) { _uiState.value = _uiState.value.copy(peso = value) }
    fun updateAltura(value: String) { _uiState.value = _uiState.value.copy(altura = value) }
    fun updateGenero(value: String) { _uiState.value = _uiState.value.copy(genero = value) }

    fun finishOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Simular guardar perfil (mock)
            delay(800)
            // Guardar flag en DataStore
            val repo = UserPreferencesRepositoryImpl(appContext)
            val userKey = normalizeUserKey(currentUser?.id, currentUser?.email)
            Log.d("OnboardingVM", "Setting onboardingCompleted=true for key=$userKey")
            repo.setOnboardingCompleted(userKey, true)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onFinished()
        }
    }

    private fun normalizeUserKey(id: String?, email: String?): String {
        return id?.takeIf { it.isNotBlank() } ?: (email?.lowercase()?.trim() ?: "unknown_user")
    }
}
