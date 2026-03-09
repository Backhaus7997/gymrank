package com.example.gymrank.ui.screens.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.RankingRepositoryFirestoreImpl
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RankingUiState(
    val selectedPeriod: RankingPeriod = RankingPeriod.WEEKLY,
    val gymId: String? = null,
    val gymName: String? = null,

    val loading: Boolean = false,
    val error: String? = null,

    val ranking: List<RankingUserRow> = emptyList(),

    val myUid: String? = null,
    val myPosition: Int? = null, // 1-based
    val myPoints: Long = 0L
)

class RankingViewModel(
    private val repo: RankingRepositoryFirestoreImpl = RankingRepositoryFirestoreImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState(myUid = repo.currentUid()))
    val uiState: StateFlow<RankingUiState> = _uiState

    private var currentJobKey: String? = null

    init {
        viewModelScope.launch {
            loadUserGymContext()
            startObserve()
        }
    }

    fun onSelectPeriod(period: RankingPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, error = null) }
        startObserve()
    }

    fun refresh() {
        _uiState.update { it.copy(error = null) }
        startObserve()
    }

    private fun startObserve() {
        val period = _uiState.value.selectedPeriod
        val gymId = _uiState.value.gymId

        val key = "${period.name}::${gymId.orEmpty()}"
        if (currentJobKey == key) return
        currentJobKey = key

        _uiState.update { it.copy(loading = true, error = null, ranking = emptyList()) }

        viewModelScope.launch {
            repo.observeRanking(period = period, gymId = gymId).collect { res ->
                res.fold(
                    onSuccess = { list ->
                        val myUid = _uiState.value.myUid
                        val myIndex = if (myUid != null) list.indexOfFirst { it.uid == myUid } else -1
                        val myPos = if (myIndex >= 0) myIndex + 1 else null
                        val myPts = if (myIndex >= 0) list[myIndex].points else 0L

                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = null,
                                ranking = list,
                                myPosition = myPos,
                                myPoints = myPts
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(loading = false, error = e.message ?: "Error", ranking = emptyList()) }
                    }
                )
            }
        }
    }

    /**
     * Lee del user actual su gymId, y opcionalmente el nombre del gym (si lo tenés en gyms/{id}).
     */
    private suspend fun loadUserGymContext() {
        val uid = repo.currentUid() ?: return

        try {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            val gymId = userDoc.getString("gymId")

            var gymName: String? = null
            if (!gymId.isNullOrBlank()) {
                // opcional: si tenés colección gyms
                val gymDoc = FirebaseFirestore.getInstance().collection("gyms").document(gymId).get().await()
                gymName = gymDoc.getString("name")
            }

            _uiState.update { it.copy(gymId = gymId, gymName = gymName) }
        } catch (_: Exception) {
            // Si falla, igual dejamos ranking global
        }
    }
}