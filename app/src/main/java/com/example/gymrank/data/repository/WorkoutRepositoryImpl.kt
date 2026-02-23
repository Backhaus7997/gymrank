package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.repository.WorkoutRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WorkoutRepository {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("No hay usuario logueado")
    }

    private fun workoutsCol(uid: String) =
        db.collection("users").document(uid).collection("workouts")

    override suspend fun saveWorkout(workout: Workout) {
        val uid = requireUid()

        val docRef = workoutsCol(uid).document()

        val nowMillis = System.currentTimeMillis()

        val payload = hashMapOf<String, Any?>(
            "id" to docRef.id,

            // ✅ aseguramos timestampMillis siempre
            "timestampMillis" to (workout.timestampMillis ?: nowMillis),

            "durationMinutes" to (workout.durationMinutes ?: 0),
            "type" to (workout.type ?: ""),
            "intensity" to (workout.intensity ?: ""),
            "notes" to workout.notes,
            "muscles" to workout.muscles,

            // ✅ timestamps Firestore
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(payload).await()
    }

    override fun getWorkouts(): Flow<List<Workout>> = callbackFlow {
        val uid = runCatching { requireUid() }.getOrNull()
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = workoutsCol(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val reg: ListenerRegistration = query.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val docs = snap?.documents.orEmpty()
            val list = docs.mapNotNull { d ->
                runCatching {
                    val createdAtMillis =
                        d.getTimestamp("createdAt")?.toDate()?.time
                    val updatedAtMillis =
                        d.getTimestamp("updatedAt")?.toDate()?.time

                    val tsMillis =
                        d.getLong("timestampMillis") ?: createdAtMillis ?: 0L

                    Workout(
                        id = d.getString("id") ?: d.id,
                        createdAt = createdAtMillis,
                        updatedAt = updatedAtMillis,

                        timestampMillis = tsMillis,
                        durationMinutes = (d.getLong("durationMinutes") ?: 0L).toInt(),
                        type = d.getString("type") ?: "",
                        muscles = (d.get("muscles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        intensity = d.getString("intensity") ?: "",
                        notes = d.getString("notes")?.takeIf { it.isNotBlank() }
                    )
                }.getOrNull()
            }

            trySend(list)
        }

        awaitClose { reg.remove() }
    }

    override fun getLastWorkout(): Flow<Workout?> = callbackFlow {
        val uid = runCatching { requireUid() }.getOrNull()
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val query = workoutsCol(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)

        val reg: ListenerRegistration = query.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null)
                return@addSnapshotListener
            }

            val d = snap?.documents?.firstOrNull()
            if (d == null) {
                trySend(null)
                return@addSnapshotListener
            }

            val workout = runCatching {
                val createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time
                val updatedAtMillis = d.getTimestamp("updatedAt")?.toDate()?.time
                val tsMillis = d.getLong("timestampMillis") ?: createdAtMillis ?: 0L

                Workout(
                    id = d.getString("id") ?: d.id,
                    createdAt = createdAtMillis,
                    updatedAt = updatedAtMillis,

                    timestampMillis = tsMillis,
                    durationMinutes = (d.getLong("durationMinutes") ?: 0L).toInt(),
                    type = d.getString("type") ?: "",
                    muscles = (d.get("muscles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    intensity = d.getString("intensity") ?: "",
                    notes = d.getString("notes")?.takeIf { it.isNotBlank() }
                )
            }.getOrNull()

            trySend(workout)
        }

        awaitClose { reg.remove() }
    }
}
