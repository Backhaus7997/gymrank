package com.example.gymrank.ui.screens.loadworkout

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.gymrank.data.repository.WorkoutRepositoryImpl
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoadWorkoutScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LoadWorkoutViewModel = viewModel(
        factory = LoadWorkoutViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current

    var timestampMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var durationMinutes by remember { mutableStateOf("60") }
    var type by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // ✅ IMPORTANTE: estos strings tienen que matchear el mapper del HomeViewModel
    val musclesAll = listOf(
        "Pecho", "Espalda", "Hombros", "Trapecios",
        "Bíceps", "Tríceps", "Antebrazos",
        "Abdomen", "Oblicuos",
        "Cuádriceps", "Isquios", "Glúteos", "Gemelos"
    )
    var musclesSelected by remember { mutableStateOf(setOf<String>()) }

    fun validate(): Boolean {
        val d = durationMinutes.toIntOrNull() ?: 0
        return d > 0 && type.isNotBlank() && musclesSelected.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cargar entrenamiento", color = DesignTokens.Colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = DesignTokens.Colors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DesignTokens.Colors.BackgroundBase)
            )
        },
        containerColor = DesignTokens.Colors.BackgroundBase,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(color = DesignTokens.Colors.BackgroundBase) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.TextPrimary)
                    ) {
                        Text("Cancelar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    Button(
                        onClick = {
                            if (!validate()) {
                                Toast.makeText(
                                    context,
                                    "Completá tipo, duración y al menos 1 músculo",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val workout = Workout(
                                timestampMillis = timestampMillis,
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
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GymRankColors.PrimaryAccent,
                            contentColor = GymRankColors.PrimaryAccentText
                        )
                    ) {
                        Text("Guardar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard(title = "Duración (min)") {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymRankColors.PrimaryAccent,
                            unfocusedBorderColor = DesignTokens.Colors.DividerSubtle,
                            focusedContainerColor = DesignTokens.Colors.SurfaceInputs,
                            unfocusedContainerColor = DesignTokens.Colors.SurfaceInputs,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary,
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(onClick = { durationMinutes = ((durationMinutes.toIntOrNull() ?: 0) + 5).toString() }) { Text("+5") }
                        ElevatedButton(onClick = { durationMinutes = ((durationMinutes.toIntOrNull() ?: 0) + 15).toString() }) { Text("+15") }
                        ElevatedButton(onClick = {
                            val v = (durationMinutes.toIntOrNull() ?: 0) - 5
                            durationMinutes = maxOf(v, 0).toString()
                        }) { Text("-5") }
                    }
                }
            }

            item {
                SectionCard(title = "Tipo de entrenamiento") {
                    val types = listOf("Fuerza", "Hipertrofia", "Cardio", "Funcional", "Cross", "Otro")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t ->
                            SelectChip(text = t, selected = type == t, onClick = { type = t })
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Músculos entrenados") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        musclesAll.forEach { m ->
                            SelectChip(
                                text = m,
                                selected = musclesSelected.contains(m),
                                onClick = {
                                    musclesSelected =
                                        if (musclesSelected.contains(m)) musclesSelected - m else musclesSelected + m
                                }
                            )
                        }
                    }
                }
            }

            item {
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
                SectionCard(title = "Notas (opcional)") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymRankColors.PrimaryAccent,
                            unfocusedBorderColor = DesignTokens.Colors.DividerSubtle,
                            focusedContainerColor = DesignTokens.Colors.SurfaceInputs,
                            unfocusedContainerColor = DesignTokens.Colors.SurfaceInputs,
                            focusedTextColor = DesignTokens.Colors.TextPrimary,
                            unfocusedTextColor = DesignTokens.Colors.TextPrimary,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.SurfaceElevated),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, shape)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = DesignTokens.Colors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) DesignTokens.Colors.SurfaceInputs else DesignTokens.Colors.SurfaceElevated
    val border = if (selected) GymRankColors.PrimaryAccent else DesignTokens.Colors.SurfaceInputs
    val txt = if (selected) DesignTokens.Colors.TextPrimary else DesignTokens.Colors.TextSecondary

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
        Text(text = text, color = txt, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

class LoadWorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepositoryImpl()

    fun save(workout: Workout, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.saveWorkout(workout) }
                .onSuccess { onDone() }
        }
    }
}

class LoadWorkoutViewModelFactory(
    private val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadWorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoadWorkoutViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
