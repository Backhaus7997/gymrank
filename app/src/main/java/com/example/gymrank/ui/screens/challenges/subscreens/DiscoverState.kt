package com.example.gymrank.ui.screens.challenges.subscreens

import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge

data class DiscoverState(
    val loading: Boolean = false,
    val templates: List<ChallengeTemplate> = emptyList(),
    val userChallenges: List<UserChallenge> = emptyList(),
    val error: String? = null
)