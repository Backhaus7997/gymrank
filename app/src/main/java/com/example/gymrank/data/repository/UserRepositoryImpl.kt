package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.Gym
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error("No hay usuario logueado (uid null)")

    /**
     * Guarda el gym seleccionado en el doc del usuario:
     * users/{uid}
     *
     * Campos (según tu código actual):
     * - gymId
     * - gymNameCache
     * - gymSelectedAt
     * - updatedAt
     *
     * ✅ Usamos set(merge) para que no falle si el doc aún no existe.
     */
    suspend fun saveSelectedGym(gym: Gym) {
        val uid = requireUid()

        val updates: Map<String, Any?> = mapOf(
            "gymId" to gym.id,
            "gymNameCache" to gym.name,
            // si en algún momento querés guardar city, lo dejamos opcional:
            "gymCityCache" to gym.city, // si no lo querés, podés borrar esta línea
            "gymSelectedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    /**
     * Lee el gym guardado desde users/{uid}
     *
     * Devuelve null si no hay gym guardado.
     */
    suspend fun getSelectedGym(): Gym? {
        val uid = auth.currentUser?.uid ?: return null

        val doc = db.collection("users")
            .document(uid)
            .get()
            .await()

        if (!doc.exists()) return null

        val gymId = doc.getString("gymId")?.trim().orEmpty()
        if (gymId.isEmpty()) return null

        val gymName = doc.getString("gymNameCache")?.trim().orEmpty()
        val gymCity = doc.getString("gymCityCache")?.trim().orEmpty()

        return Gym(
            id = gymId,
            name = gymName,
            city = gymCity
        )
    }

    suspend fun saveOnboarding(
        username: String,
        dob: String,
        weightKg: Int,
        heightCm: Int,
        gender: String,
        experience: String
    ) {
        val uid = requireUid()

        val updates: Map<String, Any?> = mapOf(
            "username" to username,
            "dob" to dob,
            "weightKg" to weightKg,
            "heightCm" to heightCm,
            "gender" to gender,
            "experience" to experience,
            "profileCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // ✅ también merge para no explotar si el doc no existe
        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }
}
