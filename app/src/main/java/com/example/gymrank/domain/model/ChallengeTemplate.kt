package com.example.gymrank.domain.model

data class ChallengeTemplate(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val level: String = "",        // Principiante/Intermedio/Avanzado/Experto
    val durationDays: Int = 0,     // 10, 14, 21, 30, etc.
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)