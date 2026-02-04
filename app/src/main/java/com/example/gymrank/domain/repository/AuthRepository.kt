package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
}
