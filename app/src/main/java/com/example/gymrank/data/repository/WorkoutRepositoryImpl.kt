package com.example.gymrank.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "gymrank_prefs")

class WorkoutRepositoryImpl(private val context: Context) : WorkoutRepository {
    private val LAST_WORKOUT = stringPreferencesKey("last_workout_json")

    override suspend fun saveWorkout(workout: Workout) {
        val json = JSONObject().apply {
            put("timestampMillis", workout.timestampMillis)
            put("durationMinutes", workout.durationMinutes)
            put("type", workout.type)
            put("intensity", workout.intensity)
            put("notes", workout.notes ?: "")
            put("muscles", JSONArray(workout.muscles))
        }.toString()
        context.dataStore.edit { prefs ->
            prefs[LAST_WORKOUT] = json
        }
    }

    override fun getLastWorkout(): Flow<Workout?> = context.dataStore.data.map { prefs ->
        val json = prefs[LAST_WORKOUT] ?: return@map null
        runCatching {
            val obj = JSONObject(json)
            val musclesJson = obj.optJSONArray("muscles") ?: JSONArray()
            val muscles = (0 until musclesJson.length()).map { i -> musclesJson.optString(i) }
            Workout(
                timestampMillis = obj.optLong("timestampMillis"),
                durationMinutes = obj.optInt("durationMinutes"),
                type = obj.optString("type"),
                muscles = muscles,
                intensity = obj.optString("intensity"),
                notes = obj.optString("notes").takeIf { it.isNotEmpty() }
            )
        }.getOrNull()
    }
}
