package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.repository.GymRepository
import kotlinx.coroutines.delay

class GymRepositoryImpl : GymRepository {

    override suspend fun getGyms(): Result<List<Gym>> {
        // Simulate network delay
        delay(1000)

        // Mock gym data - Argentine gyms
        val gyms = listOf(
            Gym(id = "1", name = "Iron Temple", city = "Buenos Aires"),
            Gym(id = "2", name = "Titan Gym", city = "Córdoba"),
            Gym(id = "3", name = "Beast Factory", city = "Rosario"),
            Gym(id = "4", name = "Fuerza Sur", city = "La Plata"),
            Gym(id = "5", name = "Power House", city = "Mendoza"),
            Gym(id = "6", name = "Sparta Fitness", city = "Mar del Plata"),
            Gym(id = "7", name = "Alpha Training", city = "Salta"),
            Gym(id = "8", name = "Evolution Gym", city = "San Miguel de Tucumán"),
            Gym(id = "9", name = "Warrior Zone", city = "Santa Fe"),
            Gym(id = "10", name = "Gladiator Gym", city = "Neuquén"),
            Gym(id = "11", name = "Peak Performance", city = "Bahía Blanca"),
            Gym(id = "12", name = "Iron Paradise", city = "San Juan")
        )

        return Result.success(gyms)
    }
}
