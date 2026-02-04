package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.User
import com.example.gymrank.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class AuthRepositoryImpl : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        // Simulate network delay
        delay(1500)

        // Mock authentication logic
        return if (email.isNotEmpty() && password.isNotEmpty()) {
            Result.success(
                User(
                    id = "mock_user_123",
                    email = email,
                    name = "Usuario Demo"
                )
            )
        } else {
            Result.failure(Exception("Credenciales inválidas"))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        // Simulate network delay
        delay(1500)

        // Mock sign up logic
        return if (email.isNotEmpty() && password.length >= 6) {
            Result.success(
                User(
                    id = "new_user_${System.currentTimeMillis()}",
                    email = email,
                    name = "Nuevo Usuario"
                )
            )
        } else {
            Result.failure(Exception("Error al crear cuenta"))
        }
    }
}
