package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.MissionTemplate
import com.example.gymrank.domain.model.UserMission
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserMissionsRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

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

                            // ✅ CLAVE: leer templateId del doc (si existe)
                            templateId = d.getString("templateId"),

                            title = d.getString("title") ?: "",
                            subtitle = d.getString("subtitle") ?: "",
                            description = d.getString("description") ?: "",
                            tags = (d.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            level = d.getString("level") ?: "",
                            durationDays = (d.getLong("durationDays") ?: 0L).toInt(),
                            points = (d.getLong("points") ?: 0L).toInt(),
                            imageUrl = d.getString("imageUrl") ?: ""
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
        if (existing.exists()) {
            // ya existe => en progreso / aceptada
            throw IllegalStateException("already_started")
        }

        val now = Timestamp.now()
        val payload = hashMapOf(
            "id" to docId,
            "uid" to uid,

            // ✅ CLAVE (ya lo tenías): se guarda el templateId
            "templateId" to t.id,

            "title" to t.title,
            "subtitle" to t.subtitle,
            "description" to t.description,
            "tags" to t.tags,
            "level" to t.level,
            "durationDays" to t.durationDays,
            "points" to t.points,
            "imageUrl" to t.imageUrl,
            "status" to "active",
            "createdAt" to now,
            "updatedAt" to now
        )

        docRef.set(payload).await()
        return docId
    }

    /**
     * Misión custom creada por el usuario (NO usa Difficulty/Focus enums).
     */
    suspend fun createCustomMission(
        title: String,
        description: String,
        durationDays: Int,
        difficultyLabel: String, // "Fácil" / "Media" / "Difícil"
        focusKey: String,        // "upper" / "lower" / "cardio" / "mobility"
        focusLabel: String,      // "Superior" / "Inferior" / "Cardio" / "Movilidad"
        points: Int,
        objectiveWorkouts: Int
    ): String {
        val uid = auth.currentUser?.uid ?: error("not_authenticated")

        val docRef = db.collection("user_missions").document()
        val now = Timestamp.now()

        val payload = hashMapOf(
            "id" to docRef.id,
            "uid" to uid,
            "title" to title,
            "subtitle" to focusLabel,
            "description" to description,
            "tags" to listOf("custom", focusKey),
            "level" to difficultyLabel,
            "durationDays" to durationDays,
            "points" to points,
            "objectiveWorkouts" to objectiveWorkouts,
            "progressWorkouts" to 0,
            "status" to "active",
            "createdAt" to now,
            "updatedAt" to now
        )

        docRef.set(payload).await()
        return docRef.id
    }
}