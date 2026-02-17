package com.example.gymrank.data.repository

import android.content.Context
import com.example.gymrank.data.local.AppPreferencesDataStore
import com.example.gymrank.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {
    private val ds = AppPreferencesDataStore(context)

    override suspend fun isOnboardingCompleted(userKey: String): Boolean =
        ds.isOnboardingCompletedOnce(userKey)

    override suspend fun setOnboardingCompleted(userKey: String, completed: Boolean) {
        ds.setOnboardingCompleted(userKey, completed)
    }

    override fun onboardingCompletedFlow(userKey: String): Flow<Boolean> =
        ds.onboardingCompletedFlow(userKey)
}
