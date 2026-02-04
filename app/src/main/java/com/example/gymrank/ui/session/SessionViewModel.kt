package com.example.gymrank.ui.session

import androidx.lifecycle.ViewModel
import com.example.gymrank.domain.model.Gym
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionViewModel : ViewModel() {

    private val _selectedGym = MutableStateFlow<Gym?>(null)
    val selectedGym: StateFlow<Gym?> = _selectedGym.asStateFlow()

    fun selectGym(gym: Gym) {
        _selectedGym.value = gym
    }

    fun clearSession() {
        _selectedGym.value = null
    }
}
