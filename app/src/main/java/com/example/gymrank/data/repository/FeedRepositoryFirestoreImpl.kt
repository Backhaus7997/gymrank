package com.example.gymrank.data.repository

import com.example.gymrank.ui.screens.feed.ExerciseSummary
import com.example.gymrank.ui.screens.feed.FeedPost
import com.example.gymrank.ui.screens.feed.FeedWorkoutItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FeedRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // =========================
    // MODELOS
    // =========================

    data class UserDoc(
        val username: String = "",
        val experience: String = "Principiante",
        val avatarUrl: String? = null,
        val feedVisibility: String = "PUBLIC"
    )

    data class WorkoutDoc(
        val title: String = "",
        val imageUrl: String? = null,
        val createdAt: Any? = null,
        val exercises: List<Map<String, Any?>> = emptyList(),
        val description: String? = null,
        val durationMinutes: Long? = null,
        val intensity: String? = null,
        val muscles: List<String> = emptyList(),
        val notes: String? = null,
        val type: String? = null,
        val timestampMillis: Long? = null
    )

    // ✅ Friend Requests
    data class FriendRequestDoc(
        val fromUid: String = "",
        val toUid: String = "",
        val status: String = "pending", // pending | accepted | rejected | canceled
        val createdAt: Any? = null,
        val updatedAt: Any? = null
    )

    data class FriendRequestItem(
        val requestId: String,
        val fromUid: String,
        val fromUsername: String,
        val fromAvatarUrl: String,
        val createdAtLabel: String
    )

    // =========================
    // HELPERS
    // =========================

    private fun now() = System.currentTimeMillis()

    // ✅ ESTE ES EL FIX DEL ERROR "Field 'createdAt' is not a java.lang.Number"
    private fun createdAtMillis(v: Any?): Long? = when (v) {
        is Timestamp -> v.toDate().time
        is Long -> v
        is Double -> v.toLong()
        is Int -> v.toLong()
        is String -> v.toLongOrNull()
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

    private fun normalizeFeedVisibility(v: String?): String = v?.trim()?.uppercase() ?: "PUBLIC"

    private fun requestId(fromUid: String, toUid: String) = "${fromUid}_${toUid}"

    // =========================
    // AMIGOS
    // =========================

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

    suspend fun getMyFriendsUids(): List<String> {
        val myUid = auth.currentUser?.uid ?: return emptyList()
        val snap = db.collection("users")
            .document(myUid)
            .collection("friends")
            .get()
            .await()
        return snap.documents.map { it.id }
    }

    // ✅ compat: esto antes agregaba directo, ahora ENVÍA SOLICITUD
    suspend fun addFriend(friendUid: String) {
        sendFriendRequest(friendUid)
    }

    // =========================
    // ✅ SOLICITUDES
    // =========================

    suspend fun sendFriendRequest(toUid: String) {
        val fromUid = auth.currentUser?.uid ?: return
        if (toUid.isBlank() || fromUid == toUid) return

        // 1) Si ya son amigos
        val alreadyFriend = db.collection("users")
            .document(fromUid)
            .collection("friends")
            .document(toUid)
            .get()
            .await()
            .exists()
        if (alreadyFriend) return

        // 2) Si ya hay pending entrante (to->from), no crear otra
        val incomingId = requestId(toUid, fromUid)
        val incomingSnap = db.collection("friend_requests").document(incomingId).get().await()
        val incomingStatus = incomingSnap.getString("status")?.lowercase()
        if (incomingSnap.exists() && incomingStatus == "pending") return

        // 3) Creo/actualizo mi outgoing
        val outId = requestId(fromUid, toUid)
        val ref = db.collection("friend_requests").document(outId)

        val existing = ref.get().await()
        val existingStatus = existing.getString("status")?.lowercase()

        if (existing.exists() && existingStatus == "pending") return

        val payload = hashMapOf(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "status" to "pending",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!existing.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        ref.set(payload).await()
    }

    suspend fun cancelFriendRequest(toUid: String) {
        val fromUid = auth.currentUser?.uid ?: return
        if (toUid.isBlank() || fromUid == toUid) return

        val id = requestId(fromUid, toUid)
        val ref = db.collection("friend_requests").document(id)
        val snap = ref.get().await()
        if (!snap.exists()) return

        val status = snap.getString("status")?.lowercase()
        if (status != "pending") return

        ref.set(
            mapOf(
                "status" to "canceled",
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun acceptFriendRequest(fromUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        if (fromUid.isBlank() || fromUid == myUid) return

        val id = requestId(fromUid, myUid)
        val reqRef = db.collection("friend_requests").document(id)

        db.runTransaction { tx ->
            val snap = tx.get(reqRef)
            if (!snap.exists()) return@runTransaction null

            val status = snap.getString("status")?.lowercase()
            if (status != "pending") return@runTransaction null

            tx.set(
                reqRef,
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            val myFriendRef = db.collection("users").document(myUid)
                .collection("friends").document(fromUid)

            val otherFriendRef = db.collection("users").document(fromUid)
                .collection("friends").document(myUid)

            tx.set(myFriendRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            tx.set(otherFriendRef, mapOf("createdAt" to FieldValue.serverTimestamp()))

            null
        }.await()
    }

    suspend fun rejectFriendRequest(fromUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        if (fromUid.isBlank() || fromUid == myUid) return

        val id = requestId(fromUid, myUid)
        val ref = db.collection("friend_requests").document(id)
        val snap = ref.get().await()
        if (!snap.exists()) return

        val status = snap.getString("status")?.lowercase()
        if (status != "pending") return

        ref.set(
            mapOf(
                "status" to "rejected",
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    fun observeIncomingPendingRequestsCount(): Flow<Int> = callbackFlow {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val reg = db.collection("friend_requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.size() ?: 0)
            }

        awaitClose { reg.remove() }
    }

    fun observeIncomingPendingRequests(): Flow<List<FriendRequestItem>> = callbackFlow {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val reg = db.collection("friend_requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents.orEmpty()

                val basic = docs.map { d ->
                    val r = d.toObject(FriendRequestDoc::class.java) ?: FriendRequestDoc()
                    FriendRequestItem(
                        requestId = d.id,
                        fromUid = r.fromUid,
                        fromUsername = "",
                        fromAvatarUrl = "",
                        createdAtLabel = timeLabel(createdAtMillis(r.createdAt))
                    )
                }

                trySend(basic)
            }

        awaitClose { reg.remove() }
    }

    suspend fun getUserMini(uid: String): Pair<String, String> {
        val snap = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()

        val username = snap.getString("username")
            ?: snap.getString("name")
            ?: snap.getString("displayName")
            ?: "usuario"

        val avatarUrl = snap.getString("avatarUrl")
            ?: snap.getString("photoUrl")
            ?: ""

        return username to avatarUrl
    }

    // =========================
    // BÚSQUEDA DE USUARIOS
    // =========================

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

    private fun levelFromExperience(exp: String): Int = when (exp.trim().lowercase()) {
        "principiante" -> 12
        "intermedio" -> 25
        "avanzado" -> 40
        else -> 10
    }

    // =========================
    // ✅ NUEVO: últimos entrenos por usuario (para expand en Feed)
    // =========================
    suspend fun getRecentWorkoutsForUser(ownerUid: String, limit: Long = 5): List<FeedWorkoutItem> {
        if (ownerUid.isBlank()) return emptyList()

        val ws = db.collection("users")
            .document(ownerUid)
            .collection("workouts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        return ws.documents.mapNotNull { doc ->
            val w = doc.toObject(WorkoutDoc::class.java) ?: return@mapNotNull null
            val createdMs = createdAtMillis(w.createdAt)

            val exercises = w.exercises.mapNotNull { m ->
                val name = (m["name"] as? String).orEmpty()
                if (name.isBlank()) return@mapNotNull null
                ExerciseSummary(
                    name = name,
                    reps = (m["reps"] as? Number)?.toInt() ?: 0,
                    weightKg = (m["weightKg"] as? Number)?.toFloat(),
                    isBodyWeight = (m["usesBodyweight"] as? Boolean) == true
                )
            }

            FeedWorkoutItem(
                id = doc.id,
                title = w.title,
                durationMinutes = w.durationMinutes?.toInt(),
                intensity = w.intensity,
                type = w.type,
                muscles = w.muscles,
                description = w.description,
                notes = w.notes,
                timestampLabel = timeLabel(createdMs),
                exercises = exercises
            )
        }
    }

    /**
     * FEED PÚBLICO
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
                if (vis != "PUBLIC") return@mapNotNull null
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
                    visibility = "Público",
                    timestampLabel = timeLabel(ms),
                    exercises = exercises
                )

                posts.add(ms to post)
            }
        }

        return posts.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * FEED AMIGOS
     */
    suspend fun getFriendsFeed(friendUids: List<String>, perFriendLimit: Long = 10): List<FeedPost> {
        if (friendUids.isEmpty()) return emptyList()

        val posts = mutableListOf<Pair<Long, FeedPost>>()

        for (friendUid in friendUids) {
            val userSnap = db.collection("users").document(friendUid).get().await()
            val user = userSnap.toObject(UserDoc::class.java) ?: continue

            val vis = normalizeFeedVisibility(user.feedVisibility)
            if (vis == "PRIVATE") continue

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
                    avatarUrl = user.avatarUrl?.takeIf { it.isNotBlank() } ?: "https://i.pravatar.cc/128?u=$friendUid",
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