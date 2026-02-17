package com.example.gymrank.ui.session

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.UserPreferencesRepositoryImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppStartDestination { Welcome, Login, Onboarding, Home }

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val _selectedGym = MutableStateFlow<Gym?>(null)
    val selectedGym: StateFlow<Gym?> = _selectedGym.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val userPrefs by lazy { UserPreferencesRepositoryImpl(getApplication()) }

    // Onboarding completed per current user as a Flow<Boolean>
    private val onboardingCompleted: Flow<Boolean> = currentUser.flatMapLatest { user ->
        val key = user?.let { userKey(it) } ?: ""
        Log.d("SessionVM", "Observing onboarding flag for userKey='$key'")
        if (key.isBlank()) flowOf(false) else userPrefs.onboardingCompletedFlow(key)
    }

    // Derive app start destination from login and onboarding flag
    val appStartDestination: StateFlow<AppStartDestination> = combine(currentUser, onboardingCompleted) { user, completed ->
        val dest = when {
            user == null -> AppStartDestination.Welcome
            !completed -> AppStartDestination.Onboarding
            else -> AppStartDestination.Home
        }
        Log.d("SessionVM", "Computed startDestination=$dest (user=${user?.email}, completed=$completed)")
        dest
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppStartDestination.Welcome)

    fun selectGym(gym: Gym) { _selectedGym.value = gym }
    fun setUser(user: User) { _currentUser.value = user }
    fun clearSession() { _selectedGym.value = null; _currentUser.value = null }

    fun decidePostAuthNavigation(onNavigateToOnboarding: () -> Unit, onNavigateToHome: () -> Unit) {
        val user = currentUser.value
        if (user == null) { onNavigateToHome(); return }
        val key = userKey(user)
        viewModelScope.launch {
            val completed = userPrefs.isOnboardingCompleted(key)
            Log.d("SessionVM", "decidePostAuthNavigation completed=$completed for key=$key")
            if (completed) onNavigateToHome() else onNavigateToOnboarding()
        }
    }

    private fun userKey(user: User): String = if (user.id.isNotBlank()) user.id else user.email.lowercase().trim()
}
