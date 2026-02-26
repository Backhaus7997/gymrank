package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.ChallengeTemplate

interface ChallengeRepository {
    suspend fun getChallengeTemplates(): List<ChallengeTemplate>
}