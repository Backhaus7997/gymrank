package com.example.gymrank.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ExerciseLog(
    val name: String = "",
    val reps: Int = 0,
    val sets: Int = 0,
    val weightKg: Double = 0.0,
    val usesBodyweight: Boolean = false
)

data class WorkoutLog(
    val id: String = "",
    val createdAtMillis: Long = 0L,
    val muscles: List<String> = emptyList(),
    val exercises: List<ExerciseLog> = emptyList()
)

class WorkoutProgressRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun getWorkoutsForCurrentUser(limit: Long = 250): List<WorkoutLog> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val snap = db.collection("users")
            .document(uid)
            .collection("workouts")
            .orderBy("createdAt")
            .limit(limit)
            .get()
            .await()

        return snap.documents.map { doc ->
            val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            val muscles = (doc.get("muscles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            val exercisesRaw = doc.get("exercises") as? List<Map<String, Any?>> ?: emptyList()
            val exercises = exercisesRaw.map { m ->
                ExerciseLog(
                    name = (m["name"] as? String) ?: "",
                    reps = (m["reps"] as? Number)?.toInt() ?: 0,
                    sets = (m["sets"] as? Number)?.toInt() ?: 0,
                    weightKg = (m["weightKg"] as? Number)?.toDouble() ?: 0.0,
                    usesBodyweight = (m["usesBodyweight"] as? Boolean) ?: false
                )
            }

            WorkoutLog(
                id = doc.id,
                createdAtMillis = createdAt.toDate().time,
                muscles = muscles,
                exercises = exercises
            )
        }
    }
}