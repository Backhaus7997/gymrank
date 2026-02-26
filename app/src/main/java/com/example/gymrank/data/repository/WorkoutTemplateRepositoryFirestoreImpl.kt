package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.WorkoutTemplate
import com.example.gymrank.domain.model.WorkoutTemplateDay
import com.example.gymrank.domain.model.WorkoutTemplateExercise
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WorkoutTemplateRepositoryFirestoreImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getTemplates(): List<WorkoutTemplate> {
        val snap = db.collection("workoutTemplates").get().await()

        return snap.documents.map { doc ->
            WorkoutTemplate(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                description = doc.getString("description").orEmpty(),
                visibility = doc.getString("visibility") ?: "official",
                isPro = doc.getBoolean("isPro") ?: false,
                weeks = (doc.getLong("weeks") ?: 0L).toInt(),
                level = doc.getString("level").orEmpty(),
                frequencyPerWeek = (doc.getLong("frequencyPerWeek") ?: 0L).toInt(),
                goalTags = (doc.get("goalTags") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                coverUrl = doc.getString("coverUrl")
            )
        }
    }

    suspend fun getTemplateById(templateId: String): WorkoutTemplate? {
        val doc = db.collection("workoutTemplates").document(templateId).get().await()
        if (!doc.exists()) return null

        return WorkoutTemplate(
            id = doc.id,
            title = doc.getString("title").orEmpty(),
            description = doc.getString("description").orEmpty(),
            visibility = doc.getString("visibility") ?: "official",
            isPro = doc.getBoolean("isPro") ?: false,
            weeks = (doc.getLong("weeks") ?: 0L).toInt(),
            level = doc.getString("level").orEmpty(),
            frequencyPerWeek = (doc.getLong("frequencyPerWeek") ?: 0L).toInt(),
            goalTags = (doc.get("goalTags") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            coverUrl = doc.getString("coverUrl")
        )
    }

    suspend fun getDays(templateId: String): List<WorkoutTemplateDay> {
        val snap = db.collection("workoutTemplates")
            .document(templateId)
            .collection("days")
            .orderBy("order")
            .get()
            .await()

        return snap.documents.map { doc ->
            val exercisesRaw = doc.get("exercises") as? List<*>
            val exercises = exercisesRaw?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                WorkoutTemplateExercise(
                    name = (m["name"] as? String).orEmpty(),
                    sets = ((m["sets"] as? Number)?.toInt()) ?: 0,
                    reps = ((m["reps"] as? Number)?.toInt()) ?: 0,
                    usesBodyweight = (m["usesBodyweight"] as? Boolean) ?: false,
                    weightKg = (m["weightKg"] as? Number)?.toInt(),
                    weekday = ((m["weekday"] as? Number)?.toInt()) ?: 1
                )
            }.orEmpty()

            WorkoutTemplateDay(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                description = doc.getString("description").orEmpty(),
                weekday = (doc.getLong("weekday") ?: 1L).toInt(),
                order = (doc.getLong("order") ?: 0L).toInt(),
                exercises = exercises
            )
        }
    }
}