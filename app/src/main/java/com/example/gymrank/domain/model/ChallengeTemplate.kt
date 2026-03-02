package com.example.gymrank.domain.model

data class ChallengeTemplate(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val level: String = "",
    val durationDays: Int = 0,
    val points: Int = 0,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)