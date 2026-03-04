package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.MissionTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MissionTemplateRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Colección donde guardás los templates precargados.
     * En tu Firestore veo "missionTemplate" en el panel izquierdo,
     * pero si tu colección real se llama distinto (ej: "mission_templates"),
     * cambiá este string.
     */
    private val collectionName = "missionTemplate"

    fun observeActiveMissions(): Flow<Result<List<MissionTemplate>>> = callbackFlow {
        val sub = db.collection(collectionName)
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().map { d ->
                    MissionTemplate(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        subtitle = d.getString("subtitle") ?: "",
                        description = d.getString("description") ?: "",
                        imageUrl = d.getString("imageUrl") ?: "",
                        durationDays = (d.getLong("durationDays") ?: 0L).toInt(),
                        level = d.getString("level") ?: "",
                        points = (d.getLong("points") ?: 0L).toInt(),
                        tags = (d.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isActive = d.getBoolean("isActive") ?: true,
                        createdAt = d.getTimestamp("createdAt"),
                        updatedAt = d.getTimestamp("updatedAt")
                    )
                }

                trySend(Result.success(list))
            }

        awaitClose { sub.remove() }
    }
}