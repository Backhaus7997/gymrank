package com.example.gymrank.ui.screens.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.AuthRepositoryImpl
import com.example.gymrank.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, error = null) }
    }

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value

        // Clear previous errors
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                error = null
            )
        }

        // Validations
        var hasError = false

        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Email inválido") }
            hasError = true
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "La contraseña debe tener al menos 6 caracteres") }
            hasError = true
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Las contraseñas no coinciden") }
            hasError = true
        }

        if (hasError) return

        // Start sign up
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            authRepository.signUp(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al crear cuenta"
                        )
                    }
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
