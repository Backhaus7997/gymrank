package com.example.gymrank.ui.screens.friendrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.FeedRepositoryFirestoreImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FriendRequestsBadgeViewModel(
    repo: FeedRepositoryFirestoreImpl = FeedRepositoryFirestoreImpl()
) : ViewModel() {
    val pendingCount: StateFlow<Int> =
        repo.observeIncomingPendingRequestsCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}