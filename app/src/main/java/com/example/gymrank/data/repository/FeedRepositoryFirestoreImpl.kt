package com.example.gymrank.data.repository

import com.example.gymrank.ui.screens.feed.ExerciseSummary
import com.example.gymrank.ui.screens.feed.FeedPost
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FeedRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    data class UserDoc(
        val username: String = "",
        val experience: String = "Principiante",
        val avatarUrl: String? = null,
        val feedVisibility: String = "PUBLIC" // ✅ NUEVO (en perfil)
    )

    // ✅ workouts YA NO tienen visibility
    data class WorkoutDoc(
        val title: String = "",
        val imageUrl: String? = null,
        val createdAt: Any? = null,
        val exercises: List<Map<String, Any?>> = emptyList(),

        // extra detalle
        val description: String? = null,
        val durationMinutes: Long? = null,
        val intensity: String? = null,
        val muscles: List<String> = emptyList(),
        val notes: String? = null,
        val type: String? = null,
        val timestampMillis: Long? = null
    )

    private fun now() = System.currentTimeMillis()

    private fun createdAtMillis(v: Any?): Long? = when (v) {
        is Timestamp -> v.toDate().time
        is Long -> v
        is Double -> v.toLong()
        is Int -> v.toLong()
        else -> null
    }

    private fun timeLabel(ms: Long?): String {
        if (ms == null) return ""
        val diff = now() - ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 2 -> "Recién"
            minutes < 60 -> "Hace ${minutes} min"
            hours < 24 -> "Hace ${hours} h"
            days == 1L -> "Ayer"
            else -> "Hace ${days} días"
        }
    }

    suspend fun removeFriend(friendUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        if (friendUid == myUid) return

        db.collection("users")
            .document(myUid)
            .collection("friends")
            .document(friendUid)
            .delete()
            .await()
    }

    private fun levelFromExperience(exp: String): Int = when (exp.trim().lowercase()) {
        "principiante" -> 12
        "intermedio" -> 25
        "avanzado" -> 40
        else -> 10
    }

    suspend fun getMyFriendsUids(): List<String> {
        val myUid = auth.currentUser?.uid ?: return emptyList()
        val snap = db.collection("users")
            .document(myUid)
            .collection("friends")
            .get()
            .await()
        return snap.documents.map { it.id }
    }

    suspend fun searchUsersByUsernameExact(username: String): List<Pair<String, UserDoc>> {
        val q = username.trim()
        if (q.isEmpty()) return emptyList()

        val snap = db.collection("users")
            .whereEqualTo("username", q)
            .limit(10)
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            val u = d.toObject(UserDoc::class.java) ?: return@mapNotNull null
            d.id to u
        }
    }

    suspend fun addFriend(friendUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        if (friendUid == myUid) return

        db.collection("users")
            .document(myUid)
            .collection("friends")
            .document(friendUid)
            .set(mapOf("createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            .await()
    }

    private fun normalizeFeedVisibility(v: String?): String = v?.trim()?.uppercase() ?: "PUBLIC"

    /**
     * ✅ FEED PÚBLICO:
     * - Solo usuarios cuyo perfil esté en PUBLIC.
     * - No hay filtro por workout.visibility porque ya no existe.
     */
    suspend fun getPublicFeed(maxUsers: Int = 50, perUserLimit: Long = 5): List<FeedPost> {
        val myUid = auth.currentUser?.uid ?: ""

        val usersSnap = db.collection("users")
            .limit(maxUsers.toLong())
            .get()
            .await()

        val users = usersSnap.documents
            .filter { it.id != myUid }
            .mapNotNull { doc ->
                val u = doc.toObject(UserDoc::class.java) ?: return@mapNotNull null
                val vis = normalizeFeedVisibility(u.feedVisibility)
                if (vis != "PUBLIC") return@mapNotNull null // ✅ clave
                doc.id to u.copy(feedVisibility = vis)
            }

        val posts = mutableListOf<Pair<Long, FeedPost>>()

        for ((uid, user) in users) {
            val ws = db.collection("users").document(uid)
                .collection("workouts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(perUserLimit)
                .get()
                .await()

            ws.documents.forEach { wDoc ->
                val w = wDoc.toObject(WorkoutDoc::class.java) ?: return@forEach
                val ms = createdAtMillis(w.createdAt) ?: 0L

                val exercises = w.exercises.take(3).map { m ->
                    ExerciseSummary(
                        name = (m["name"] as? String).orEmpty(),
                        reps = (m["reps"] as? Number)?.toInt() ?: 0,
                        weightKg = (m["weightKg"] as? Number)?.toFloat(),
                        isBodyWeight = (m["usesBodyweight"] as? Boolean) == true
                    )
                }

                val post = FeedPost(
                    id = wDoc.id,
                    ownerUid = uid,
                    userName = user.username.ifBlank { "usuario" },
                    avatarUrl = user.avatarUrl?.takeIf { it.isNotBlank() } ?: "https://i.pravatar.cc/128?u=$uid",
                    level = levelFromExperience(user.experience),
                    workoutTitle = w.title,
                    workoutImageUrl = w.imageUrl
                        ?: "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=60",
                    visibility = "Público", // ✅ viene del perfil
                    timestampLabel = timeLabel(ms),
                    exercises = exercises
                )

                posts.add(ms to post)
            }
        }

        return posts.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * ✅ FEED AMIGOS:
     * - Si el amigo está en PRIVATE -> no se muestra nada
     * - Si está en FRIENDS o PUBLIC -> se muestra
     */
    suspend fun getFriendsFeed(friendUids: List<String>, perFriendLimit: Long = 10): List<FeedPost> {
        if (friendUids.isEmpty()) return emptyList()

        val posts = mutableListOf<Pair<Long, FeedPost>>()

        for (friendUid in friendUids) {
            val userSnap = db.collection("users").document(friendUid).get().await()
            val user = userSnap.toObject(UserDoc::class.java) ?: continue

            val vis = normalizeFeedVisibility(user.feedVisibility)
            if (vis == "PRIVATE") continue // ✅ clave

            val ws = db.collection("users").document(friendUid)
                .collection("workouts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(perFriendLimit)
                .get()
                .await()

            ws.documents.forEach { wDoc ->
                val w = wDoc.toObject(WorkoutDoc::class.java) ?: return@forEach
                val ms = createdAtMillis(w.createdAt) ?: 0L

                val exercises = w.exercises.take(3).map { m ->
                    ExerciseSummary(
                        name = (m["name"] as? String).orEmpty(),
                        reps = (m["reps"] as? Number)?.toInt() ?: 0,
                        weightKg = (m["weightKg"] as? Number)?.toFloat(),
                        isBodyWeight = (m["usesBodyweight"] as? Boolean) == true
                    )
                }

                val label = if (vis == "FRIENDS") "Amigos" else "Público"

                val post = FeedPost(
                    id = wDoc.id,
                    ownerUid = friendUid,
                    userName = user.username.ifBlank { "usuario" },
                    avatarUrl = user.avatarUrl?.takeIf { it.isNotBlank() }
                        ?: "https://i.pravatar.cc/128?u=$friendUid",
                    level = levelFromExperience(user.experience),
                    workoutTitle = w.title,
                    workoutImageUrl = w.imageUrl
                        ?: "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=60",
                    visibility = label,
                    timestampLabel = timeLabel(ms),
                    exercises = exercises
                )

                posts.add(ms to post)
            }
        }

        return posts.sortedByDescending { it.first }.map { it.second }
    }
}