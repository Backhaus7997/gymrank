package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AcceptedChallengesSharedViewModel : ViewModel() {

    // Guarda el último templateId aceptado (evento simple)
    private val _acceptedTemplateId = MutableStateFlow<String?>(null)
    val acceptedTemplateId: StateFlow<String?> = _acceptedTemplateId.asStateFlow()

    fun markAccepted(templateId: String) {
        _acceptedTemplateId.value = templateId
    }

    fun clear() {
        _acceptedTemplateId.value = null
    }
}