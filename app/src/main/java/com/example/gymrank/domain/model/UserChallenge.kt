package com.example.gymrank.domain.model

data class UserChallenge(
    val id: String = "",                 // doc id
    val uid: String = "",                // user id
    val templateId: String = "",         // challenge_templates/{id}
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,

    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val canceledAt: Long? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null
)