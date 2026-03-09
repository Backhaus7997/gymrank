package com.example.gymrank.data.repository

import com.example.gymrank.ui.screens.ranking.RankingPeriod
import com.example.gymrank.ui.screens.ranking.RankingUserRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RankingRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val usersCol get() = db.collection("users")

    fun observeRanking(
        period: RankingPeriod,
        gymId: String?,
        limit: Long = 100
    ): Flow<Result<List<RankingUserRow>>> = callbackFlow {
        var reg: ListenerRegistration? = null

        try {
            val qBase = if (!gymId.isNullOrBlank()) {
                usersCol
                    .whereEqualTo("gymId", gymId)
                    .orderBy(period.field, Query.Direction.DESCENDING)
            } else {
                usersCol
                    .orderBy(period.field, Query.Direction.DESCENDING)
            }

            val q = qBase.limit(limit)

            reg = q.addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    val uid = d.id
                    val name = d.getString("displayName")
                        ?: d.getString("username")
                        ?: "Usuario"
                    val photo = d.getString("photoUrl")
                    val gId = d.getString("gymId")
                    val points = (d.getLong(period.field) ?: 0L)

                    RankingUserRow(
                        uid = uid,
                        displayName = name,
                        photoUrl = photo,
                        points = points,
                        gymId = gId
                    )
                }

                trySend(Result.success(list))
            }
        } catch (e: Exception) {
            trySend(Result.failure(e))
        }

        awaitClose { reg?.remove() }
    }

    fun currentUid(): String? = auth.currentUser?.uid
}