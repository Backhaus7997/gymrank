package com.example.gymrank.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "app_preferences"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class AppPreferencesDataStore(private val context: Context) {

    companion object {
        private const val ONBOARDING_PREFIX = "onboarding_completed_"

        fun onboardingKeyFor(userKey: String) = booleanPreferencesKey(ONBOARDING_PREFIX + sanitizeKey(userKey))

        private fun sanitizeKey(userKey: String): String {
            // Keep simple: lowercase, trim, replace spaces with underscore and @ with _at_
            return userKey.lowercase().trim().replace("@", "_at_").replace(" ", "_")
        }
    }

    suspend fun setOnboardingCompleted(userKey: String, completed: Boolean) {
        val key = onboardingKeyFor(userKey)
        context.dataStore.edit { prefs ->
            prefs[key] = completed
        }
    }

    fun onboardingCompletedFlow(userKey: String): Flow<Boolean> {
        val key = onboardingKeyFor(userKey)
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    suspend fun isOnboardingCompletedOnce(userKey: String): Boolean {
        val key = onboardingKeyFor(userKey)
        return context.dataStore.data.map { it[key] ?: false }.first()
    }
}
