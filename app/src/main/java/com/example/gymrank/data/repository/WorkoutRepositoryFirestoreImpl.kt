package com.example.gymrank.data.repository

import android.util.Log
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.model.WorkoutExercise
import com.example.gymrank.domain.repository.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore structure:
 * users/{uid}/workouts/{workoutId}
 *
 * ✅ Importante:
 * - NO guardamos "visibility" en cada workout.
 * - La privacidad vive en users/{uid}.feedVisibility
 */
class WorkoutRepositoryFirestoreImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WorkoutRepository {

    private fun workoutsCol(uid: String) =
        db.collection("users").document(uid).collection("workouts")

    override suspend fun saveWorkout(workout: Workout) {
        val uid = auth.currentUser?.uid ?: error("No hay usuario logueado (uid null)")
        val col = workoutsCol(uid)

        val isNew = workout.id.isBlank()
        val docRef = if (!isNew) col.document(workout.id) else col.document()
        val nowMillis = System.currentTimeMillis()

        Log.d("WorkoutRepo", "saveWorkout START uid=$uid isNew=$isNew docId=${docRef.id}")

        val exercisesPayload: List<Map<String, Any?>> =
            workout.exercises.map { ex ->
                mapOf(
                    "name" to ex.name,
                    "sets" to ex.sets,
                    "reps" to ex.reps,
                    "usesBodyweight" to ex.usesBodyweight,
                    "weightKg" to ex.weightKg,
                    "weekday" to ex.weekday
                )
            }

        // ✅ NO visibility acá
        val payload = hashMapOf<String, Any?>(
            "id" to docRef.id,

            // timestamps legacy (compat)
            "timestampMillis" to (workout.timestampMillis ?: nowMillis),

            // rutina
            "title" to workout.title,
            "description" to workout.description,
            "gymId" to workout.gymId,

            // ejercicios
            "exercises" to exercisesPayload,

            // compat / otros
            "durationMinutes" to workout.durationMinutes,
            "type" to workout.type,
            "muscles" to workout.muscles,
            "intensity" to workout.intensity,
            "notes" to workout.notes,

            // updated siempre
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // ✅ createdAt SOLO si es nuevo
        if (isNew) payload["createdAt"] = FieldValue.serverTimestamp()

        try {
            docRef.set(payload, SetOptions.merge()).await()
            Log.d("WorkoutRepo", "saveWorkout OK uid=$uid docId=${docRef.id}")
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "saveWorkout FAILED uid=$uid docId=${docRef.id}", e)
            throw e
        }
    }

    // ----------------- MAPPER -----------------

    private fun docToWorkout(d: com.google.firebase.firestore.DocumentSnapshot): Workout? {
        return runCatching {
            val createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time
            val updatedAtMillis = d.getTimestamp("updatedAt")?.toDate()?.time
            val tsMillis = d.getLong("timestampMillis") ?: createdAtMillis ?: 0L

            val exercises = (d.get("exercises") as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val name = m["name"] as? String ?: ""
                val sets = (m["sets"] as? Number)?.toInt() ?: 0
                val reps = (m["reps"] as? Number)?.toInt() ?: 0
                val usesBodyweight = m["usesBodyweight"] as? Boolean ?: false
                val weightKg = (m["weightKg"] as? Number)?.toInt()
                val weekday = (m["weekday"] as? Number)?.toInt()

                WorkoutExercise(
                    name = name,
                    sets = sets,
                    reps = reps,
                    usesBodyweight = usesBodyweight,
                    weightKg = weightKg,
                    weekday = weekday
                )
            } ?: emptyList()

            Workout(
                id = d.getString("id") ?: d.id,
                createdAt = createdAtMillis,
                updatedAt = updatedAtMillis,

                title = d.getString("title") ?: "",
                description = d.getString("description"),
                gymId = d.getString("gymId"),

                exercises = exercises,

                // ✅ NO visibility en domain
                timestampMillis = tsMillis,
                durationMinutes = (d.getLong("durationMinutes") ?: 0L).toInt(),
                type = d.getString("type"),
                muscles = (d.get("muscles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                intensity = d.getString("intensity"),
                notes = d.getString("notes")
            )
        }.getOrNull()
    }

    /**
     * ✅ Lista en vivo + tolerante a timing de auth
     */
    override fun getWorkouts(): Flow<List<Workout>> = callbackFlow {
        var reg: ListenerRegistration? = null

        fun attach(uid: String) {
            reg?.remove()

            reg = workoutsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e("WorkoutRepo", "getWorkouts snapshot error", err)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val list = snap?.documents.orEmpty().mapNotNull { d -> docToWorkout(d) }
                    trySend(list)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != null) {
                attach(uid)
            } else {
                reg?.remove()
                reg = null
                trySend(emptyList())
            }
        }

        auth.addAuthStateListener(authListener)
        auth.currentUser?.uid?.let { attach(it) } ?: trySend(emptyList())

        awaitClose {
            reg?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    override fun getLastWorkout(): Flow<Workout?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val reg = workoutsCol(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("WorkoutRepo", "getLastWorkout snapshot error", err)
                    close(err)
                    return@addSnapshotListener
                }

                val d = snap?.documents?.firstOrNull()
                trySend(d?.let { docToWorkout(it) })
            }

        awaitClose { reg.remove() }
    }
}