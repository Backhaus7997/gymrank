package com.example.gymrank.domain.model

data class UserMission(
    val id: String = "",
    val uid: String = "",
    val templateId: String? = null,

    val title: String = "",
    val subtitle: String = "",
    val description: String = "",

    val durationDays: Int = 0,
    val level: String = "",
    val points: Int = 0,
    val progress: Int = 0,

    val status: String = "active", // active, completed, expired, cancelled, etc
    val startedAt: Long? = null,
    val endAt: Long? = null,

    val imageUrl: String = "",
    val tags: List<String> = emptyList()
)