package com.example.gymrank.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.AuthRepositoryImpl
import com.example.gymrank.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _emailError.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _passwordError.value = null
    }

    fun onLoginClick() {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = authRepository.login(_email.value, _password.value)

            _uiState.value = result.fold(
                onSuccess = { user -> LoginUiState.Success(user) },
                onFailure = { error -> LoginUiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (_email.value.isEmpty()) {
            _emailError.value = "El email es requerido"
            isValid = false
        } else if (!isValidEmail(_email.value)) {
            _emailError.value = "Email inválido"
            isValid = false
        }

        if (_password.value.isEmpty()) {
            _passwordError.value = "La contraseña es requerida"
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun resetUiState() {
        _uiState.value = LoginUiState.Idle
    }
}
