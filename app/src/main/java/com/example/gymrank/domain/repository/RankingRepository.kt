package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.RankingPeriod
import com.example.gymrank.domain.model.RankingResult

interface RankingRepository {
    suspend fun fetchRanking(
        gymId: String,
        period: RankingPeriod,
        limit: Long = 50
    ): RankingResult
}