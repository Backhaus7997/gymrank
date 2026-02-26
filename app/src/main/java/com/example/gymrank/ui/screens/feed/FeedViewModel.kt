package com.example.gymrank.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.FeedRepositoryFirestoreImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = false,
    val publicPosts: List<FeedPost> = emptyList(),
    val friendsPosts: List<FeedPost> = emptyList(),
    val friendsUids: List<String> = emptyList(),
    val searchResults: List<Pair<String, FeedRepositoryFirestoreImpl.UserDoc>> = emptyList(),
    val error: String? = null
)

class FeedViewModel(
    private val repo: FeedRepositoryFirestoreImpl = FeedRepositoryFirestoreImpl()
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState(loading = true))
    val state: StateFlow<FeedUiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            runCatching {
                val friends = repo.getMyFriendsUids()
                val public = repo.getPublicFeed()
                val friendsFeed = repo.getFriendsFeed(friends)

                _state.value = _state.value.copy(
                    loading = false,
                    friendsUids = friends,
                    publicPosts = public,
                    friendsPosts = friendsFeed
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.message ?: "Error cargando feed"
                )
            }
        }
    }

    fun search(username: String) {
        viewModelScope.launch {
            runCatching {
                val results = repo.searchUsersByUsernameExact(username)
                _state.value = _state.value.copy(searchResults = results, error = null)
            }.onFailure {
                _state.value = _state.value.copy(error = it.message ?: "Error buscando usuario")
            }
        }
    }

    fun addFriend(uid: String) {
        viewModelScope.launch {
            runCatching {
                repo.addFriend(uid)
                load()
                _state.value = _state.value.copy(searchResults = emptyList())
            }.onFailure {
                _state.value = _state.value.copy(error = it.message ?: "Error agregando amigo")
            }
        }
    }
}