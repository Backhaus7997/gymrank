package com.example.gymrank.data.repository

import android.util.Log
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.repository.ChallengeRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChallengeRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChallengeRepository {

    private val templatesCol get() = db.collection("challenge_templates")

    override suspend fun getChallengeTemplates(): List<ChallengeTemplate> {
        return try {
            val snap = templatesCol
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snap.documents.mapNotNull { d ->
                runCatching {
                    val createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time
                    val updatedAtMillis = d.getTimestamp("updatedAt")?.toDate()?.time

                    ChallengeTemplate(
                        id = d.id,
                        title = d.getString("title").orEmpty(),
                        subtitle = d.getString("subtitle").orEmpty(),
                        level = d.getString("level").orEmpty(),
                        durationDays = (d.getLong("durationDays") ?: 0L).toInt(),
                        imageUrl = d.getString("imageUrl"),
                        tags = (d.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        isActive = d.getBoolean("isActive") ?: true,
                        createdAt = createdAtMillis,
                        updatedAt = updatedAtMillis
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "getChallengeTemplates FAILED", e)
            throw e
        }
    }
}