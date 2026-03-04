package com.example.gymrank.domain.model

import com.google.firebase.Timestamp

data class MissionTemplate(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val durationDays: Int = 0,
    val level: String = "",
    val points: Int = 0,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)