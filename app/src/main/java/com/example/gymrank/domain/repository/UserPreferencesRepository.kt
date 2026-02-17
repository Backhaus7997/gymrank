package com.example.gymrank.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    suspend fun isOnboardingCompleted(userKey: String): Boolean
    suspend fun setOnboardingCompleted(userKey: String, completed: Boolean)
    fun onboardingCompletedFlow(userKey: String): Flow<Boolean>
}
