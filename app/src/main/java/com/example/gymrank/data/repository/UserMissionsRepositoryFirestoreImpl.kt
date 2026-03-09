package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.MissionTemplate
import com.example.gymrank.domain.model.UserMission
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserMissionsRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_COMPLETED = "COMPLETED"
    }

    fun observeMyMissions(): Flow<Result<List<UserMission>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val sub = db.collection("user_missions")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    runCatching {
                        UserMission(
                            id = d.getString("id") ?: d.id,
                            uid = d.getString("uid") ?: uid,
                            templateId = d.getString("templateId"),

                            title = d.getString("title") ?: "",
                            subtitle = d.getString("subtitle") ?: "",
                            description = d.getString("description") ?: "",
                            tags = (d.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            level = d.getString("level") ?: "",
                            durationDays = (d.getLong("durationDays") ?: 0L).toInt(),
                            points = (d.getLong("points") ?: 0L).toInt(),
                            imageUrl = d.getString("imageUrl") ?: "",

                            status = (d.getString("status") ?: STATUS_ACTIVE).trim().uppercase()
                        )
                    }.getOrNull()
                }

                trySend(Result.success(list))
            }

        awaitClose { sub.remove() }
    }

    /**
     * Crea una instancia de misión desde template.
     * Regla: si ya existe user_missions/{uid}_{templateId} -> "already_started"
     */
    suspend fun startTemplateMission(t: MissionTemplate): String {
        val uid = auth.currentUser?.uid ?: error("not_authenticated")
        val docId = "${uid}_${t.id}"
        val docRef = db.collection("user_missions").document(docId)

        val existing = docRef.get().await()
        if (existing.exists()) throw IllegalStateException("already_started")

        val now = Timestamp.now()
        val payload = hashMapOf(
            "id" to docId,
            "uid" to uid,
            "templateId" to t.id,

            "title" to t.title,
            "subtitle" to t.subtitle,
            "description" to t.description,
            "tags" to t.tags,
            "level" to t.level,
            "durationDays" to t.durationDays,
            "points" to t.points,
            "imageUrl" to t.imageUrl,

            "status" to STATUS_ACTIVE,

            "createdAt" to now,
            "updatedAt" to now,

            "pointsAwarded" to false
        )

        docRef.set(payload).await()
        return docId
    }

    suspend fun createCustomMission(
        title: String,
        description: String,
        durationDays: Int,
        difficultyLabel: String,
        focusKey: String,
        focusLabel: String,
        points: Int,
        objectiveWorkouts: Int
    ): String {
        val uid = auth.currentUser?.uid ?: error("not_authenticated")

        val docRef = db.collection("user_missions").document()
        val now = Timestamp.now()

        val payload = hashMapOf(
            "id" to docRef.id,
            "uid" to uid,
            "templateId" to null,

            "title" to title,
            "subtitle" to focusLabel,
            "description" to description,
            "tags" to listOf("custom", focusKey),
            "level" to difficultyLabel,
            "durationDays" to durationDays,
            "points" to points,

            "objectiveWorkouts" to objectiveWorkouts,
            "progressWorkouts" to 0,

            "status" to STATUS_ACTIVE,

            "createdAt" to now,
            "updatedAt" to now,

            "pointsAwarded" to false
        )

        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun completeUserMissionAndAwardPoints(
        userMissionId: String,
        pointsRepo: PointsRepositoryFirestoreImpl
    ) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("user_missions").document(userMissionId)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) return@runTransaction null

            val status = (snap.getString("status") ?: STATUS_ACTIVE).trim().uppercase()
            if (status == STATUS_COMPLETED) return@runTransaction null

            tx.update(
                ref,
                mapOf(
                    "status" to STATUS_COMPLETED,
                    "completedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()

        val after = ref.get().await()
        if (!after.exists()) return

        val pointsFromMission = after.getLong("points")?.toInt()
        val pointsToAward = pointsFromMission ?: 0
        if (pointsToAward <= 0) return

        val eventId = "mission_completed_${uid}_$userMissionId"
        pointsRepo.awardPointsIdempotent(
            eventId = eventId,
            sourceType = "mission",
            sourceId = userMissionId,
            points = pointsToAward
        )
    }

    /**
     * ✅ Recorre COMPLETED y si pointsAwarded==false, los premia usando el ledger (idempotente)
     */
    suspend fun awardCompletedMissionsIfNeeded(pointsRepo: PointsRepositoryFirestoreImpl) {
        val uid = auth.currentUser?.uid ?: return

        val snap = db.collection("user_missions")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", STATUS_COMPLETED)
            .get()
            .await()

        if (snap.isEmpty) return

        for (d in snap.documents) {
            val alreadyAwarded = d.getBoolean("pointsAwarded") ?: false
            if (alreadyAwarded) continue

            val missionId = d.id
            val pts = (d.getLong("points") ?: 0L).toInt()
            val eventId = "mission_completed_${uid}_$missionId"

            if (pts > 0) {
                pointsRepo.awardPointsIdempotent(
                    eventId = eventId,
                    sourceType = "mission",
                    sourceId = missionId,
                    points = pts
                )
            }

            d.reference.set(mapOf("pointsAwarded" to true), SetOptions.merge()).await()
        }
    }
}