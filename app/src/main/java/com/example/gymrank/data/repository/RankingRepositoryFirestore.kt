package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.RankingEntryUi
import com.example.gymrank.domain.model.RankingPeriod
import com.example.gymrank.domain.model.RankingResult
import com.example.gymrank.domain.repository.RankingRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RankingRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val myUserId: String
) : RankingRepository {

    override suspend fun fetchRanking(
        gymId: String,
        period: RankingPeriod,
        limit: Long
    ): RankingResult {

        val pointsField = when (period) {
            RankingPeriod.Weekly -> "weeklyPoints"
            RankingPeriod.Monthly -> "monthlyPoints"
            RankingPeriod.AllTime -> "allTimePoints"
        }

        val snap = db.collection("users")
            .whereEqualTo("gymId", gymId)
            .orderBy(pointsField, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        val top = snap.documents.mapIndexed { index, doc ->
            val uid = doc.id
            val name = doc.getString("displayName") ?: "Sin nombre"
            val points = (doc.getLong(pointsField) ?: 0L).toInt()

            RankingEntryUi(
                position = index + 1,
                userId = uid,
                name = name,
                points = points,
                isMe = uid == myUserId
            )
        }

        val meInTop = top.firstOrNull { it.isMe }
        if (meInTop != null) {
            return RankingResult(
                top = top,
                mePosition = meInTop.position,
                mePoints = meInTop.points
            )
        }

        // Si no estoy en el top, leo mi doc para saber mis puntos
        val meDoc = db.collection("users").document(myUserId).get().await()
        val mePoints = (meDoc.getLong(pointsField) ?: 0L).toInt()

        // Calcular posición (MVP): cuantos tienen más puntos que yo + 1
        val higherCount = db.collection("users")
            .whereEqualTo("gymId", gymId)
            .whereGreaterThan(pointsField, mePoints)
            .get()
            .await()
            .size()

        return RankingResult(
            top = top,
            mePosition = higherCount + 1,
            mePoints = mePoints
        )
    }
}