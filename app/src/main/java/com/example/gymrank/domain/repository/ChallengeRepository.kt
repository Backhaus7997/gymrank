package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge

interface ChallengeRepository {

    // templates
    suspend fun getChallengeTemplates(): List<ChallengeTemplate>
    suspend fun getChallengeTemplateById(templateId: String): ChallengeTemplate?

    // user challenges
    suspend fun acceptChallenge(uid: String, templateId: String): UserChallenge
    suspend fun getUserChallenges(uid: String, statuses: List<ChallengeStatus>): List<UserChallenge>
    suspend fun updateUserChallengeStatus(uid: String, userChallengeId: String, status: ChallengeStatus): UserChallenge
}