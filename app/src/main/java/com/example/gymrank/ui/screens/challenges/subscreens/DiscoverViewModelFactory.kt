package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymrank.domain.repository.ChallengeRepository

class DiscoverViewModelFactory(
    private val repo: ChallengeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiscoverViewModel(repo) as T
    }
}