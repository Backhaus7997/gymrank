package com.example.gymrank.data.repository

import android.util.Log
import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge
import com.example.gymrank.domain.repository.ChallengeRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth

class ChallengeRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChallengeRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val templatesCol get() = db.collection("challenge_templates")
    private val userChallengesCol get() = db.collection("user_challenges")

    override suspend fun getChallengeTemplates(): List<ChallengeTemplate> {
        return try {
            val snap = templatesCol
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snap.documents.mapNotNull { d -> d.toChallengeTemplateOrNull() }
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "getChallengeTemplates FAILED", e)
            throw e
        }
    }

    override suspend fun getChallengeTemplateById(templateId: String): ChallengeTemplate? {
        return try {
            val d = templatesCol.document(templateId).get().await()
            if (!d.exists()) return null
            d.toChallengeTemplateOrNull()
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "getChallengeTemplateById FAILED", e)
            throw e
        }
    }

    override suspend fun acceptChallenge(uid: String, templateId: String): UserChallenge {
        return try {
            val existing = userChallengesCol
                .whereEqualTo("uid", uid)
                .whereEqualTo("templateId", templateId)
                .whereEqualTo("status", ChallengeStatus.ACTIVE.name)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                val doc = existing.documents.first()
                return doc.toUserChallengeOrNull() ?: UserChallenge(
                    id = doc.id,
                    uid = uid,
                    templateId = templateId,
                    status = ChallengeStatus.ACTIVE,
                    updatedAt = System.currentTimeMillis()
                )
            }

            val now = System.currentTimeMillis()
            val ref = userChallengesCol.document()
            val data = mapOf(
                "uid" to uid,
                "templateId" to templateId,
                "status" to ChallengeStatus.ACTIVE.name,
                "startedAt" to now,
                "createdAt" to now,
                "updatedAt" to now
            )

            ref.set(data, SetOptions.merge()).await()

            UserChallenge(
                id = ref.id,
                uid = uid,
                templateId = templateId,
                status = ChallengeStatus.ACTIVE,
                startedAt = now,
                createdAt = now,
                updatedAt = now
            )
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "acceptChallenge FAILED", e)
            throw e
        }
    }

    override suspend fun getUserChallenges(
        uid: String,
        statuses: List<ChallengeStatus>
    ): List<UserChallenge> {
        return try {
            if (statuses.isEmpty()) return emptyList()

            val statusNames = statuses.map { it.name }

            val snap = userChallengesCol
                .whereEqualTo("uid", uid)
                .whereIn("status", statusNames)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { it.toUserChallengeOrNull() }
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "getUserChallenges FAILED", e)
            throw e
        }
    }

    override suspend fun updateUserChallengeStatus(
        uid: String,
        userChallengeId: String,
        status: ChallengeStatus
    ): UserChallenge {
        return try {
            val now = System.currentTimeMillis()
            val ref = userChallengesCol.document(userChallengeId)

            val current = ref.get().await()
            if (!current.exists()) throw IllegalStateException("Desafío no encontrado")
            val docUid = current.getString("uid").orEmpty()
            if (docUid.isNotBlank() && docUid != uid) throw IllegalStateException("No autorizado")

            val patch = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to now
            )

            when (status) {
                ChallengeStatus.ACTIVE -> {
                    if (current.getLong("startedAt") == null) patch["startedAt"] = now
                }
                ChallengeStatus.COMPLETED -> patch["completedAt"] = now
                ChallengeStatus.CANCELED -> patch["canceledAt"] = now
            }

            ref.set(patch, SetOptions.merge()).await()

            val after = ref.get().await()
            after.toUserChallengeOrNull()
                ?: UserChallenge(
                    id = userChallengeId,
                    uid = uid,
                    templateId = current.getString("templateId").orEmpty(),
                    status = status,
                    updatedAt = now
                )
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "updateUserChallengeStatus FAILED", e)
            throw e
        }
    }

    // --- Mappers privados ---

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallengeTemplateOrNull(): ChallengeTemplate? {
        return runCatching {
            val createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            val updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time

            // ✅ points puede venir como Number o como String ("200")
            val pointsInt =
                (getLong("points")?.toInt())
                    ?: getString("points")?.toIntOrNull()
                    ?: 0

            ChallengeTemplate(
                id = id,
                title = getString("title").orEmpty(),
                subtitle = getString("subtitle").orEmpty(),
                level = getString("level").orEmpty(),
                durationDays = (getLong("durationDays") ?: 0L).toInt(),
                points = pointsInt,
                imageUrl = getString("imageUrl"),
                tags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isActive = getBoolean("isActive") ?: true,
                createdAt = createdAtMillis,
                updatedAt = updatedAtMillis
            )
        }.getOrNull()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserChallengeOrNull(): UserChallenge? {
        return runCatching {
            val statusStr = getString("status").orEmpty()
            val status = runCatching { ChallengeStatus.valueOf(statusStr) }.getOrElse { ChallengeStatus.ACTIVE }

            UserChallenge(
                id = id,
                uid = getString("uid").orEmpty(),
                templateId = getString("templateId").orEmpty(),
                status = status,
                startedAt = getLong("startedAt"),
                completedAt = getLong("completedAt"),
                canceledAt = getLong("canceledAt"),
                createdAt = getLong("createdAt"),
                updatedAt = getLong("updatedAt")
            )
        }.getOrNull()
    }

    suspend fun completeUserChallengeAndAwardPoints(
        userChallengeId: String,
        pointsRepo: PointsRepositoryFirestoreImpl
    ) {
        val ref = db.collection("user_challenges").document(userChallengeId)

        // 1) Marcar COMPLETED una vez
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) return@runTransaction null

            val status = (snap.getString("status") ?: ChallengeStatus.ACTIVE.name).uppercase()
            if (status == ChallengeStatus.COMPLETED.name) return@runTransaction null

            tx.update(
                ref,
                mapOf(
                    "status" to ChallengeStatus.COMPLETED.name,
                    "completedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()

        // 2) Leer puntos del template (source of truth)
        val after = ref.get().await()
        if (!after.exists()) return

        val templateId = after.getString("templateId").orEmpty()
        if (templateId.isBlank()) return

        val tSnap = templatesCol.document(templateId).get().await()
        val points = (tSnap.getLong("points") ?: 0L).toInt()
        if (points <= 0) return

        // 3) Award idempotente
        val eventId = "challenge_completed_${after.getString("uid").orEmpty()}_$userChallengeId"
        pointsRepo.awardPointsIdempotent(
            eventId = eventId,
            sourceType = "challenge",
            sourceId = userChallengeId,
            points = points
        )
    }

    /**
     * ✅ Recorre COMPLETED y si pointsAwarded==false, los premia usando el ledger (idempotente)
     * Mantiene compatibilidad con data actual.
     */
    suspend fun awardCompletedChallengesIfNeeded(pointsRepo: PointsRepositoryFirestoreImpl) {
        val uid = auth.currentUser?.uid ?: return

        val snap = userChallengesCol
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", ChallengeStatus.COMPLETED.name)
            .get()
            .await()

        if (snap.isEmpty) return

        for (d in snap.documents) {
            val alreadyAwarded = d.getBoolean("pointsAwarded") ?: false
            if (alreadyAwarded) continue

            val instanceId = d.id
            val templateId = d.getString("templateId").orEmpty()
            if (templateId.isBlank()) {
                d.reference.set(mapOf("pointsAwarded" to true), SetOptions.merge()).await()
                continue
            }

            val tSnap = templatesCol.document(templateId).get().await()
            val points = (tSnap.getLong("points") ?: 0L).toInt()

            if (points > 0) {
                val eventId = "challenge_completed_${uid}_$instanceId"
                pointsRepo.awardPointsIdempotent(
                    eventId = eventId,
                    sourceType = "challenge",
                    sourceId = instanceId,
                    points = points
                )
            }

            d.reference.set(mapOf("pointsAwarded" to true), SetOptions.merge()).await()
        }
    }
}