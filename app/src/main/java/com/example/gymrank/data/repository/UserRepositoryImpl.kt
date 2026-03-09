package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.Gym
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UserRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        const val DEFAULT_FEED_VISIBILITY = "PUBLIC"

        // legacy
        const val FIELD_POINTS = "points"

        // ranking
        const val FIELD_WEEKLY_POINTS = "weeklyPoints"
        const val FIELD_MONTHLY_POINTS = "monthlyPoints"
        const val FIELD_TOTAL_POINTS = "totalPoints"
        const val FIELD_WEEKLY_KEY = "weeklyKey"
        const val FIELD_MONTHLY_KEY = "monthlyKey"

        // workout prompt
        const val FIELD_WORKOUT_PROMPT_ANSWERED_ON = "workoutPromptAnsweredOn"
        const val FIELD_WORKOUT_PROMPT_ANSWER = "workoutPromptAnswer"
    }

    private val arTz: TimeZone = TimeZone.getTimeZone("America/Argentina/Buenos_Aires")

    private fun currentWeeklyKeyAR(): String {
        val cal = Calendar.getInstance(arTz, Locale.getDefault())
        cal.firstDayOfWeek = Calendar.MONDAY
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')
        return "$year-W$week"
    }

    private fun currentMonthlyKeyAR(): String {
        val cal = Calendar.getInstance(arTz, Locale.getDefault())
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return "$year-$month"
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

    suspend fun clearSelectedGym() {
        val uid = requireUid()

        val updates: Map<String, Any?> = mapOf(
            "gymId" to FieldValue.delete(),
            "gymNameCache" to FieldValue.delete(),
            "gymCityCache" to FieldValue.delete(),
            "gymSelectedAt" to FieldValue.delete(),
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
     * ✅ Onboarding
     * - Inicializa ranking fields + keys
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

        val wk = currentWeeklyKeyAR()
        val mk = currentMonthlyKeyAR()

        val updates: Map<String, Any?> = mapOf(
            "username" to username,
            "dob" to dob,
            "weightKg" to weightKg,
            "heightCm" to heightCm,
            "gender" to gender,
            "experience" to experience,
            "profileCompleted" to true,
            "feedVisibility" to DEFAULT_FEED_VISIBILITY,

            // legacy
            FIELD_POINTS to 0L,

            // ✅ ranking
            FIELD_WEEKLY_POINTS to 0L,
            FIELD_MONTHLY_POINTS to 0L,
            FIELD_TOTAL_POINTS to 0L,
            FIELD_WEEKLY_KEY to wk,
            FIELD_MONTHLY_KEY to mk,

            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    suspend fun getMyFeedVisibility(): String {
        val uid = auth.currentUser?.uid ?: return DEFAULT_FEED_VISIBILITY
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("feedVisibility")?.trim()?.uppercase() ?: DEFAULT_FEED_VISIBILITY
    }

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

    data class MyProfileData(
        val uid: String,
        val username: String,
        val experience: String,
        val gender: String,
        val feedVisibility: String,
        val photoBase64: String?
    )

    suspend fun getMyProfile(): MyProfileData {
        val uid = auth.currentUser?.uid ?: error("No hay usuario logueado (uid null)")
        val doc = db.collection("users").document(uid).get().await()

        val username = doc.getString("username")?.trim().orEmpty()
        val experience = doc.getString("experience")?.trim().orEmpty().ifBlank { "Intermedio" }
        val gender = doc.getString("gender")?.trim().orEmpty().ifBlank { "Otro" }

        val feedVisibility = doc.getString("feedVisibility")?.trim()?.uppercase()
            ?: DEFAULT_FEED_VISIBILITY

        val photoBase64 = doc.getString("photoBase64")

        return MyProfileData(
            uid = uid,
            username = username,
            experience = experience,
            gender = gender,
            feedVisibility = feedVisibility,
            photoBase64 = photoBase64
        )
    }

    suspend fun updateMyProfile(
        username: String,
        experience: String,
        gender: String,
        feedVisibility: String,
        photoBase64: String?
    ) {
        val uid = requireUid()

        val safeVisibility = feedVisibility.trim().uppercase().let {
            if (it in listOf("PUBLIC", "FRIENDS", "PRIVATE")) it else DEFAULT_FEED_VISIBILITY
        }

        val updates = mutableMapOf<String, Any?>(
            "username" to username.trim(),
            "experience" to experience.trim(),
            "gender" to gender.trim(),
            "feedVisibility" to safeVisibility,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (photoBase64 != null) {
            updates["photoBase64"] = photoBase64
        }

        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    // =========================================================
    // ✅ POINTS FIELDS (compat + ranking)
    // =========================================================

    /**
     * ✅ Si el usuario es viejo y no tiene fields, los crea.
     */
    suspend fun ensureMyPointsField() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        val updates = mutableMapOf<String, Any>()

        if (snap.getLong(FIELD_POINTS) == null) updates[FIELD_POINTS] = 0L
        if (snap.getLong(FIELD_WEEKLY_POINTS) == null) updates[FIELD_WEEKLY_POINTS] = 0L
        if (snap.getLong(FIELD_MONTHLY_POINTS) == null) updates[FIELD_MONTHLY_POINTS] = 0L
        if (snap.getLong(FIELD_TOTAL_POINTS) == null) updates[FIELD_TOTAL_POINTS] = 0L

        if (snap.getString(FIELD_WEEKLY_KEY).isNullOrBlank()) updates[FIELD_WEEKLY_KEY] = currentWeeklyKeyAR()
        if (snap.getString(FIELD_MONTHLY_KEY).isNullOrBlank()) updates[FIELD_MONTHLY_KEY] = currentMonthlyKeyAR()

        if (updates.isNotEmpty()) {
            ref.set(updates, SetOptions.merge()).await()
        }
    }

    suspend fun getWorkoutPromptAnsweredOn(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString(FIELD_WORKOUT_PROMPT_ANSWERED_ON)
    }

    suspend fun answerWorkoutPromptToday(todayKey: String, answerYes: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    FIELD_WORKOUT_PROMPT_ANSWER to answerYes,
                    FIELD_WORKOUT_PROMPT_ANSWERED_ON to todayKey,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }
}