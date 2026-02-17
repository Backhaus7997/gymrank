package com.example.gymrank.ui.screens.loadworkout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.graphics.StrokeCap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.ui.screens.loadworkout.LoadWorkoutViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoadWorkoutScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LoadWorkoutViewModel = viewModel()
) {
    val context = LocalContext.current

    // Minimal local state for MVP
    var timestampMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var durationMinutes by remember { mutableStateOf("60") }
    var type by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Expanded muscles list
    val musclesAll = listOf(
        "Pecho", "Espalda", "Piernas", "Hombros", "Bíceps", "Tríceps", "Abdomen", "Glúteos",
        "Gemelos", "Antebrazos", "Trapecios", "Dorsales", "Lumbar", "Cuádriceps", "Isquios", "Aductores",
        "Cardio", "Full body"
    )
    var musclesSelected by remember { mutableStateOf(setOf<String>()) }

    fun validate(): Boolean {
        val d = durationMinutes.toIntOrNull() ?: 0
        return d > 0 && type.isNotBlank() && musclesSelected.isNotEmpty()
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cargar entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000), titleContentColor = Color(0xFFFFFFFF), navigationIconContentColor = Color(0xFFFFFFFF))
            )
        },
        containerColor = Color(0xFF000000),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Fixed bottom bar actions with uniform background and insets
            Surface(color = Color(0xFF000000)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp)
                    ) { Text("Cancelar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    Button(
                        onClick = {
                            if (!validate()) {
                                Toast.makeText(context, "Completá tipo, duración y al menos 1 músculo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val workout = Workout(
                                timestampMillis = System.currentTimeMillis(),
                                durationMinutes = durationMinutes.toIntOrNull() ?: 0,
                                type = type,
                                muscles = musclesSelected.toList(),
                                intensity = intensity.ifBlank { "" },
                                notes = notes.ifBlank { null }
                            )
                            viewModel.save(workout) {
                                Toast.makeText(context, "Entrenamiento guardado", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { Text("Guardar entrenamiento", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // Reduce bottom padding to align neatly with fixed bottom bar and avoid empty space
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Duración card
                SectionCard(title = "Duración (min)") {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(onClick = { durationMinutes = ((durationMinutes.toIntOrNull() ?: 0) + 5).toString() }) { Text("+5") }
                        ElevatedButton(onClick = { durationMinutes = ((durationMinutes.toIntOrNull() ?: 0) + 15).toString() }) { Text("+15") }
                        ElevatedButton(onClick = { val v = (durationMinutes.toIntOrNull() ?: 0) - 5; durationMinutes = maxOf(v, 0).toString() }) { Text("-5") }
                    }
                }
            }

            item {
                // Tipo de entrenamiento (wrap chips to avoid cutting)
                SectionCard(title = "Tipo de entrenamiento") {
                    val types = listOf("Fuerza", "Hipertrofia", "Cardio", "Funcional", "Cross", "Otro")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { t ->
                            SelectChip(text = t, selected = type == t, onClick = { type = t })
                        }
                    }
                }
            }

            item {
                // Músculos entrenados (wrap chips)
                SectionCard(title = "Músculos entrenados") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        musclesAll.forEach { m ->
                            SelectChip(
                                text = m,
                                selected = musclesSelected.contains(m),
                                onClick = {
                                    musclesSelected = if (musclesSelected.contains(m)) musclesSelected - m else musclesSelected + m
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Intensidad
                SectionCard(title = "Intensidad") {
                    val intensities = listOf("Baja", "Media", "Alta")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(intensities) { i ->
                            SelectChip(text = i, selected = intensity == i, onClick = { intensity = i })
                        }
                    }
                }
            }

            item {
                // Notas — expand slightly to balance visual and remove any empty block
                SectionCard(title = "Notas (opcional)") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        singleLine = false,
                        // increase minLines to occupy space, keeping it optional
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF38383A), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFFFFF))
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E)
    val border = if (selected) Color(0xFFD0FD3E) else Color(0xFF38383A)
    val txt = if (selected) Color(0xFFFFFFFF) else Color(0xFF8E8E93)
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = txt,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

class LoadWorkoutViewModel(private val app: android.app.Application) : androidx.lifecycle.AndroidViewModel(app) {
    private val repo = com.example.gymrank.data.repository.WorkoutRepositoryImpl(app)
    fun save(workout: Workout, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveWorkout(workout)
            onDone()
        }
    }
}
