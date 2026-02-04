package com.example.gymrank.domain.repository

import com.example.gymrank.domain.model.Gym

interface GymRepository {
    suspend fun getGyms(): Result<List<Gym>>
}
