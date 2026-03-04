package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.domain.model.ChallengeStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repo: ChallengeRepositoryFirestoreImpl
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true, error = null)

                val uid = auth.currentUser?.uid
                    ?: run {
                        _state.value = _state.value.copy(
                            loading = false,
                            error = "Usuario no logueado"
                        )
                        return@launch
                    }

                val templates = repo.getChallengeTemplates()

                // ✅ Tu biblioteca sale de user_challenges (ACTIVE + COMPLETED)
                val userChallenges = repo.getUserChallenges(
                    uid = uid,
                    statuses = listOf(ChallengeStatus.ACTIVE, ChallengeStatus.COMPLETED)
                )

                _state.value = _state.value.copy(
                    loading = false,
                    templates = templates,
                    userChallenges = userChallenges,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando desafíos"
                )
            }
        }
    }

    fun acceptTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch

                // ✅ Persistimos en Firestore
                val created = repo.acceptChallenge(uid = uid, templateId = templateId)

                // ✅ Actualizamos state local para UI inmediata
                val current = _state.value.userChallenges.toMutableList()

                val idx = current.indexOfFirst { it.templateId == templateId && it.status == ChallengeStatus.ACTIVE }
                if (idx >= 0) {
                    current[idx] = created
                } else {
                    current.add(0, created)
                }

                _state.value = _state.value.copy(userChallenges = current)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "No se pudo aceptar el desafío"
                )
            }
        }
    }
}