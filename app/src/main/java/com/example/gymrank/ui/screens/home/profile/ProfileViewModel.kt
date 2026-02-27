package com.example.gymrank.ui.screens.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val uid: String = "",
    val username: String = "",
    val experience: String = ExperienceOptions.DEFAULT_VALUE,
    val gender: String = GenderOptions.DEFAULT_VALUE,
    val feedVisibility: String = VisibilityOptions.DEFAULT_VALUE, // PUBLIC|FRIENDS|PRIVATE
    val photoBase64: String? = null,

    val friendRequestsHint: String = "No tenés solicitudes pendientes.",
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val error: String? = null,
)

val ProfileUiState.experienceLabel: String get() = ExperienceOptions.toLabel(experience)
val ProfileUiState.genderLabel: String get() = GenderOptions.toLabel(gender)
val ProfileUiState.visibilityLabel: String get() = VisibilityOptions.toLabel(feedVisibility)

class ProfileViewModel(
    private val repo: UserRepositoryImpl = UserRepositoryImpl()
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    init {
        viewModelScope.launch {
            runCatching {
                val p = repo.getMyProfile()
                _ui.update {
                    it.copy(
                        uid = p.uid,
                        username = p.username,
                        experience = p.experience,
                        gender = p.gender,
                        feedVisibility = p.feedVisibility,
                        photoBase64 = p.photoBase64,
                        error = null,
                        savedOk = false
                    )
                }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message ?: "Error cargando perfil") }
            }
        }
    }

    fun onUsernameChanged(v: String) = _ui.update { it.copy(username = v, savedOk = false, error = null) }
    fun onExperienceChanged(v: String) = _ui.update { it.copy(experience = v, savedOk = false, error = null) }
    fun onGenderChanged(v: String) = _ui.update { it.copy(gender = v, savedOk = false, error = null) }
    fun onVisibilityChanged(v: String) = _ui.update { it.copy(feedVisibility = v, savedOk = false, error = null) }
    fun onPhotoBase64Picked(base64: String) = _ui.update { it.copy(photoBase64 = base64, savedOk = false, error = null) }

    fun setError(msg: String?) = _ui.update { it.copy(error = msg) }
    fun setSavingState(v: Boolean) = _ui.update { it.copy(isSaving = v) }

    fun save() {
        val s = _ui.value
        if (s.username.trim().isEmpty()) {
            _ui.update { it.copy(error = "El nombre no puede estar vacío.") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null, savedOk = false) }

            runCatching {
                repo.updateMyProfile(
                    username = s.username.trim(),
                    experience = s.experience,
                    gender = s.gender,
                    feedVisibility = s.feedVisibility,
                    photoBase64 = s.photoBase64
                )
            }.onSuccess {
                _ui.update { it.copy(isSaving = false, savedOk = true) }
            }.onFailure { e ->
                _ui.update { it.copy(isSaving = false, error = e.message ?: "Error guardando cambios") }
            }
        }
    }
}

/**
 * Opciones (como strings) para que coincida con tu Firestore actual.
 * experience en tu DB: "Avanzado" / "Intermedio" / "Principiante"
 * gender: "Masculino" / "Femenino" / "Otro"
 * feedVisibility: "PUBLIC" / "FRIENDS" / "PRIVATE"
 */
object ExperienceOptions {
    const val PRINCIPIANTE = "Principiante"
    const val INTERMEDIO = "Intermedio"
    const val AVANZADO = "Avanzado"

    const val DEFAULT_VALUE = INTERMEDIO

    val labels = listOf(PRINCIPIANTE, INTERMEDIO, AVANZADO)

    fun toValue(label: String): String = when (label.trim()) {
        PRINCIPIANTE -> PRINCIPIANTE
        AVANZADO -> AVANZADO
        else -> INTERMEDIO
    }

    fun toLabel(value: String): String = toValue(value)
}

object GenderOptions {
    const val MASCULINO = "Masculino"
    const val FEMENINO = "Femenino"
    const val OTRO = "Otro"

    const val DEFAULT_VALUE = OTRO

    val labels = listOf(MASCULINO, FEMENINO, OTRO)

    fun toValue(label: String): String = when (label.trim()) {
        MASCULINO -> MASCULINO
        FEMENINO -> FEMENINO
        else -> OTRO
    }

    fun toLabel(value: String): String = toValue(value)
}

object VisibilityOptions {
    const val PUBLIC = "PUBLIC"
    const val FRIENDS = "FRIENDS"
    const val PRIVATE = "PRIVATE"

    const val DEFAULT_VALUE = PUBLIC

    val labels = listOf("Público", "Solo amigos", "Privado")

    fun toValue(label: String): String = when (label.trim().lowercase()) {
        "solo amigos", "amigos" -> FRIENDS
        "privado" -> PRIVATE
        else -> PUBLIC
    }

    fun toLabel(value: String): String = when (value.trim().uppercase()) {
        FRIENDS -> "Solo amigos"
        PRIVATE -> "Privado"
        else -> "Público"
    }
}