package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.GlobalLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PointsRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val usersCol get() = db.collection("users")
    private val ledgerRoot get() = db.collection("points_ledger")

    // ============================
    // ✅ Campos users para ranking
    // ============================
    private val FIELD_POINTS = "points" // legacy total simple
    private val FIELD_WEEKLY_POINTS = "weeklyPoints"
    private val FIELD_MONTHLY_POINTS = "monthlyPoints"
    private val FIELD_TOTAL_POINTS = "totalPoints"
    private val FIELD_WEEKLY_KEY = "weeklyKey"
    private val FIELD_MONTHLY_KEY = "monthlyKey"
    private val FIELD_GLOBAL_POINTS = "globalPoints"
    private val FIELD_GLOBAL_LEVEL = "globalLevel"

    // ============================
    // ✅ Timezone AR
    // ============================
    private val arTz: TimeZone = TimeZone.getTimeZone("America/Argentina/Buenos_Aires")

    private fun currentWeeklyKeyAR(): String {
        val cal = Calendar.getInstance(arTz, Locale.getDefault())
        cal.firstDayOfWeek = Calendar.MONDAY
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')
        return "$year-W$week" // ej 2026-W10
    }

    private fun currentMonthlyKeyAR(): String {
        val cal = Calendar.getInstance(arTz, Locale.getDefault())
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return "$year-$month" // ej 2026-03
    }

    private fun todayKeyBuenosAires(): String {
        val cal = Calendar.getInstance(arTz, Locale.getDefault())
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d" // yyyy-MM-dd
    }

    // ============================
    // ✅ Award points idempotente
    // ✅ SUMA weekly/monthly/total
    // ✅ Resetea por semana/mes real
    // ============================
    suspend fun awardPointsIdempotent(
        eventId: String,
        sourceType: String, // "challenge" | "mission" | "gym_checkin"
        sourceId: String,
        points: Int
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (points <= 0) return

        val userRef = usersCol.document(uid)
        val entryRef = ledgerRoot.document(uid).collection("entries").document(eventId)

        val wkNow = currentWeeklyKeyAR()
        val mkNow = currentMonthlyKeyAR()

        db.runTransaction { tx ->
            // 1) idempotencia: si ya existe entry => no sumar de nuevo
            val existing = tx.get(entryRef)
            if (existing.exists()) return@runTransaction null

            // 2) leer user actual (para rollover + bases)
            val uSnap = tx.get(userRef)
            val wkStored = uSnap.getString(FIELD_WEEKLY_KEY)
            val mkStored = uSnap.getString(FIELD_MONTHLY_KEY)

            val prevLegacy = uSnap.getLong(FIELD_POINTS) ?: 0L
            val prevTotal = uSnap.getLong(FIELD_TOTAL_POINTS) ?: 0L
            val prevWeekly = uSnap.getLong(FIELD_WEEKLY_POINTS) ?: 0L
            val prevMonthly = uSnap.getLong(FIELD_MONTHLY_POINTS) ?: 0L

            val weeklyBase = if (!wkStored.isNullOrBlank() && wkStored == wkNow) prevWeekly else 0L
            val monthlyBase = if (!mkStored.isNullOrBlank() && mkStored == mkNow) prevMonthly else 0L

            val delta = points.toLong()

            val newLegacy = prevLegacy + delta
            val newTotal = prevTotal + delta
            val newWeekly = weeklyBase + delta
            val newMonthly = monthlyBase + delta

            // 3) crear entry en ledger (incluye keys para debugging/auditoría)
            tx.set(
                entryRef,
                mapOf(
                    "uid" to uid,
                    "eventId" to eventId,
                    "sourceType" to sourceType,
                    "sourceId" to sourceId,
                    "points" to points,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "weeklyKey" to wkNow,
                    "monthlyKey" to mkNow
                ),
                SetOptions.merge()
            )

            // 4) aplicar patch (merge) con rollover implícito via set de keys + nuevos acumulados
            val patch: Map<String, Any> = mapOf(
                "updatedAt" to FieldValue.serverTimestamp(),

                // ✅ compat
                FIELD_POINTS to newLegacy,

                // ✅ ranking
                FIELD_TOTAL_POINTS to newTotal,
                FIELD_WEEKLY_POINTS to newWeekly,
                FIELD_MONTHLY_POINTS to newMonthly,
                FIELD_WEEKLY_KEY to wkNow,
                FIELD_MONTHLY_KEY to mkNow
            )

            tx.set(userRef, patch, SetOptions.merge())

            null
        }.await()
    }

    // ============================
    // ✅ Check-in diario (AR)
    // ============================
    suspend fun awardGymCheckinPoints(points: Int = 20) {
        val uid = auth.currentUser?.uid ?: return
        val dayKey = todayKeyBuenosAires()
        val eventId = "gym_checkin_${uid}_$dayKey"
        awardPointsIdempotent(
            eventId = eventId,
            sourceType = "gym_checkin",
            sourceId = dayKey,
            points = points
        )
    }

    // overload si querés pasar dayKey (si tu UI lo usa)
    suspend fun awardGymCheckinPoints(todayKey: String, points: Int = 20) {
        val uid = auth.currentUser?.uid ?: return
        val eventId = "gym_checkin_${uid}_$todayKey"
        awardPointsIdempotent(
            eventId = eventId,
            sourceType = "gym_checkin",
            sourceId = todayKey,
            points = points
        )
    }

    // ============================
    // ✅ Sistema de puntos globales
    // ============================

    /**
     * Calcula y actualiza los puntos globales basado en los puntos mensuales
     * Regla: puntosGlobalesGanados = (puntosMensuales / 10) + bonusTopGym
     */
    suspend fun processMonthlyClose(monthKey: String) {
        val gyms = getActiveGyms()

        for (gymId in gyms) {
            val topUsers = getTopUsersForGym(gymId, monthKey, 3)

            // Procesar bonos por posición
            topUsers.forEachIndexed { index, userId ->
                val bonus = when (index) {
                    0 -> 100 // Top 1
                    1 -> 60  // Top 2
                    2 -> 30  // Top 3
                    else -> 0
                }

                if (bonus > 0) {
                    awardGlobalPointsWithBonus(userId, monthKey, bonus)
                }
            }
        }

        // Procesar usuarios sin gimnasio (solo puntos base, sin bonus)
        processUsersWithoutGym(monthKey)
    }

    private suspend fun getActiveGyms(): List<String> {
        return try {
            val snapshot = usersCol
                .whereNotEqualTo("gymId", null)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.getString("gymId") }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getTopUsersForGym(gymId: String, monthKey: String, limit: Int): List<String> {
        return try {
            val snapshot = usersCol
                .whereEqualTo("gymId", gymId)
                .whereEqualTo("monthlyKey", monthKey)
                .orderBy("monthlyPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun processUsersWithoutGym(monthKey: String) {
        try {
            val snapshot = usersCol
                .whereEqualTo("gymId", null)
                .whereEqualTo("monthlyKey", monthKey)
                .get()
                .await()

            for (doc in snapshot.documents) {
                awardGlobalPointsWithBonus(doc.id, monthKey, 0)
            }
        } catch (e: Exception) {
            // Log error pero continuar
        }
    }

    private suspend fun awardGlobalPointsWithBonus(userId: String, monthKey: String, bonus: Int) {
        val userRef = usersCol.document(userId)

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val monthlyPoints = userSnap.getLong("monthlyPoints") ?: 0L
            val currentGlobalPoints = userSnap.getLong(FIELD_GLOBAL_POINTS) ?: 0L

            // Calcular puntos ganados: (monthlyPoints / 10) + bonus
            val basePoints = (monthlyPoints / 10).toInt()
            val totalGained = basePoints + bonus

            if (totalGained > 0) {
                val newGlobalPoints = currentGlobalPoints + totalGained
                val newGlobalLevel = GlobalLevel.calculateLevel(newGlobalPoints.toInt())

                // Crear entrada en ledger para auditoría
                val entryRef = ledgerRoot.document(userId)
                    .collection("entries")
                    .document("monthly_close_${monthKey}")

                tx.set(entryRef, mapOf(
                    "uid" to userId,
                    "eventId" to "monthly_close_${monthKey}",
                    "sourceType" to "monthly_close",
                    "sourceId" to monthKey,
                    "points" to totalGained,
                    "basePoints" to basePoints,
                    "bonusPoints" to bonus,
                    "monthlyPoints" to monthlyPoints,
                    "createdAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge())

                // Actualizar usuario
                tx.set(userRef, mapOf(
                    FIELD_GLOBAL_POINTS to newGlobalPoints,
                    FIELD_GLOBAL_LEVEL to newGlobalLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge())
            }

            null
        }.await()
    }

    /**
     * Inicializa campos globales para usuarios existentes
     */
    suspend fun ensureGlobalFields(uid: String) {
        val userRef = usersCol.document(uid)
        val snap = userRef.get().await()

        if (!snap.exists()) return

        val updates = mutableMapOf<String, Any>()

        if (snap.getLong(FIELD_GLOBAL_POINTS) == null) {
            updates[FIELD_GLOBAL_POINTS] = 0L
        }

        if (snap.getLong(FIELD_GLOBAL_LEVEL) == null) {
            updates[FIELD_GLOBAL_LEVEL] = 1L
        }

        if (updates.isNotEmpty()) {
            userRef.set(updates, SetOptions.merge()).await()
        }
    }
}