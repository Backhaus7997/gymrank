package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl

class DiscoverViewModelFactory(
    private val repo: ChallengeRepositoryFirestoreImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoverViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}