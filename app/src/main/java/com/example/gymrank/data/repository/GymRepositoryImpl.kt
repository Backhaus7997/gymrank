package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.Gym
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GymRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getGyms(): Result<List<Gym>> {
        return try {
            val snapshot = db.collection("gyms")
                .whereEqualTo("isActive", true) // opcional
                .get()
                .await()

            val gyms = snapshot.documents.map { doc ->
                Gym(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    city = doc.getString("city") ?: "",
                    address = doc.getString("address") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true
                )
            }

            Result.success(gyms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
