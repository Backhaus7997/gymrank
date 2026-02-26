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

    companion object {
        // PUBLIC | FRIENDS | PRIVATE
        const val DEFAULT_FEED_VISIBILITY = "PUBLIC"
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error("No hay usuario logueado (uid null)")

    suspend fun saveSelectedGym(gym: Gym) {
        val uid = requireUid()

        val updates: Map<String, Any?> = mapOf(
            "gymId" to gym.id,
            "gymNameCache" to gym.name,
            "gymCityCache" to gym.city,
            "gymSelectedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

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

    /**
     * ✅ Guardamos onboarding + seteamos default de privacidad de feed si es primera vez.
     * (Como es onboarding, normalmente se llama una sola vez).
     */
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
            "feedVisibility" to DEFAULT_FEED_VISIBILITY,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    /**
     * ✅ Lee privacidad del perfil (PUBLIC | FRIENDS | PRIVATE)
     */
    suspend fun getMyFeedVisibility(): String {
        val uid = auth.currentUser?.uid ?: return DEFAULT_FEED_VISIBILITY
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("feedVisibility")?.trim()?.uppercase() ?: DEFAULT_FEED_VISIBILITY
    }

    /**
     * ✅ Actualiza privacidad del perfil (PUBLIC | FRIENDS | PRIVATE)
     */
    suspend fun updateMyFeedVisibility(value: String) {
        val uid = requireUid()
        val v = value.trim().uppercase()
        if (v !in listOf("PUBLIC", "FRIENDS", "PRIVATE")) return

        db.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "feedVisibility" to v,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }
}