package com.example.gymrank.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.FeedRepositoryFirestoreImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FeedWorkoutItem(
    val id: String,
    val title: String,
    val durationMinutes: Int? = null,
    val intensity: String? = null,
    val type: String? = null,
    val muscles: List<String> = emptyList(),
    val description: String? = null,
    val notes: String? = null,
    val timestampLabel: String = "",
    val exercises: List<ExerciseSummary> = emptyList()
)

data class FeedUiState(
    val loading: Boolean = false,
    val publicPosts: List<FeedPost> = emptyList(),
    val friendsPosts: List<FeedPost> = emptyList(),
    val friendsUids: List<String> = emptyList(),
    val searchResults: List<Pair<String, FeedRepositoryFirestoreImpl.UserDoc>> = emptyList(),
    val error: String? = null,

    // ✅ NUEVO: cache para “Ver más” expandido
    val expandedWorkoutsByUser: Map<String, List<FeedWorkoutItem>> = emptyMap(),
    val expandedLoadingByUser: Map<String, Boolean> = emptyMap()
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
                // no hace falta recargar todo el feed necesariamente, pero lo dejamos como lo tenías
                load()
            }.onFailure {
                _state.value = _state.value.copy(error = it.message ?: "Error enviando solicitud")
            }
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            runCatching {
                repo.removeFriend(uid)
                load()
            }.onFailure {
                _state.value = _state.value.copy(error = it.message ?: "Error dejando de seguir")
            }
        }
    }

    // ✅ NUEVO: traer últimos 5 entrenos del usuario (para expand)
    fun loadRecentWorkoutsForUser(ownerUid: String) {
        if (ownerUid.isBlank()) return

        // si ya está cargado, no pegamos otra vez
        if (_state.value.expandedWorkoutsByUser.containsKey(ownerUid)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                expandedLoadingByUser = _state.value.expandedLoadingByUser + (ownerUid to true)
            )

            runCatching {
                val list = repo.getRecentWorkoutsForUser(ownerUid, limit = 5)

                _state.value = _state.value.copy(
                    expandedWorkoutsByUser = _state.value.expandedWorkoutsByUser + (ownerUid to list),
                    expandedLoadingByUser = _state.value.expandedLoadingByUser + (ownerUid to false)
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    expandedLoadingByUser = _state.value.expandedLoadingByUser + (ownerUid to false),
                    error = it.message ?: "Error cargando entrenamientos"
                )
            }
        }
    }
}