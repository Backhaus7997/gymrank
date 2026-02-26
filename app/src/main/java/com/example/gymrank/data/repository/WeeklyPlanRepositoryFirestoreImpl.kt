package com.example.gymrank.data.repository

import android.util.Log
import com.example.gymrank.ui.screens.workout.WeeklyPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar
import java.util.Locale

class WeeklyPlanRepositoryFirestoreImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * ✅ IMPORTANTE:
     * Cambiá esto si tu plan está guardado en otro path.
     * Por defecto:
     * users/{uid}/plans/current
     */
    private fun planDoc(uid: String) =
        db.collection("users").document(uid).collection("plans").document("current")

    fun getWeeklyPlan(): Flow<WeeklyPlan?> = callbackFlow {
        var reg: ListenerRegistration? = null

        fun attach(uid: String) {
            reg?.remove()

            reg = planDoc(uid).addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("WeeklyPlanRepo", "snapshot error", err)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val data = snap.data.orEmpty()

                fun parseMuscles(value: Any?): List<String> {
                    return when (value) {
                        is List<*> -> value.mapNotNull { it as? String }.map { it.trim() }.filter { it.isNotBlank() }
                        is Map<*, *> -> {
                            val inner = value["muscles"]
                            if (inner is List<*>) inner.mapNotNull { it as? String }.map { it.trim() }.filter { it.isNotBlank() }
                            else emptyList()
                        }
                        else -> emptyList()
                    }
                }

                fun keyToDow(keyRaw: String): Int? {
                    val k = keyRaw.trim().lowercase(Locale.getDefault())
                    return when (k) {
                        // Español
                        "lunes", "l" -> Calendar.MONDAY
                        "martes" -> Calendar.TUESDAY
                        "miercoles", "miércoles", "x" -> Calendar.WEDNESDAY
                        "jueves", "j" -> Calendar.THURSDAY
                        "viernes", "v" -> Calendar.FRIDAY
                        "sabado", "sábado", "s" -> Calendar.SATURDAY
                        "domingo", "d" -> Calendar.SUNDAY

                        // Inglés
                        "mon" -> Calendar.MONDAY
                        "tue", "tues" -> Calendar.TUESDAY
                        "wed" -> Calendar.WEDNESDAY
                        "thu", "thur", "thurs" -> Calendar.THURSDAY
                        "fri" -> Calendar.FRIDAY
                        "sat" -> Calendar.SATURDAY
                        "sun" -> Calendar.SUNDAY

                        // Si vos usabas los fields mon/tue/wed...
                        "mon", "tue", "wed", "thu", "fri", "sat", "sun" -> null

                        else -> null
                    }
                }

                // También soporta formato mon/tue/wed... (por si lo tenés así)
                fun englishShortToDow(k: String): Int? = when (k) {
                    "mon" -> Calendar.MONDAY
                    "tue" -> Calendar.TUESDAY
                    "wed" -> Calendar.WEDNESDAY
                    "thu" -> Calendar.THURSDAY
                    "fri" -> Calendar.FRIDAY
                    "sat" -> Calendar.SATURDAY
                    "sun" -> Calendar.SUNDAY
                    else -> null
                }

                val map = linkedMapOf<Int, List<String>>()

                // 1) intenta leer claves directas del documento
                data.forEach { (key, value) ->
                    val k = key.trim().lowercase(Locale.getDefault())
                    val dow = keyToDow(k) ?: englishShortToDow(k)
                    if (dow != null) {
                        map[dow] = parseMuscles(value)
                    }
                }

                // 2) fallback si el documento venía con mon/tue/wed...
                if (map.isEmpty()) {
                    val keys = listOf("mon","tue","wed","thu","fri","sat","sun")
                    keys.forEach { k ->
                        val dow = englishShortToDow(k) ?: return@forEach
                        map[dow] = parseMuscles(data[k])
                    }
                }

                val plan = if (map.values.any { it.isNotEmpty() }) WeeklyPlan(map) else null
                trySend(plan)
            }
        }

        val authListener = FirebaseAuth.AuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != null) attach(uid) else {
                reg?.remove()
                reg = null
                trySend(null)
            }
        }

        auth.addAuthStateListener(authListener)
        auth.currentUser?.uid?.let { attach(it) } ?: trySend(null)

        awaitClose {
            reg?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }
}