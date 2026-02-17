package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.components.AppTextField
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlin.math.max

data class RoutineDraft(
    val name: String,
    val description: String,
    val exercises: List<RoutineExerciseDraft>
)

data class RoutineExerciseDraft(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float?,       // null => peso corporal
    val isBodyWeight: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    onBack: () -> Unit,
    onCreate: (RoutineDraft) -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF35F5A6) }

    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }

    var exercises by remember {
        mutableStateOf(
            listOf(
                RoutineExerciseDraft(
                    name = "",
                    sets = 3,
                    reps = 10,
                    weightKg = 0f,
                    isBodyWeight = false
                )
            )
        )
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var submitError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        nameError = null
        submitError = null

        if (routineName.trim().isEmpty()) {
            nameError = "Poné un nombre para la rutina."
            return false
        }

        val hasAtLeastOneExercise = exercises.any { it.name.trim().isNotEmpty() }
        if (!hasAtLeastOneExercise) {
            submitError = "Agregá al menos 1 ejercicio con nombre."
            return false
        }

        return true
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Crear rutina",
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Armá tu plantilla y repetila",
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            // ✅ CTA fijo abajo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                bg.copy(alpha = 0.92f),
                                bg
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val glow = Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glow)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Button(
                        onClick = {
                            if (!validate()) return@Button

                            val cleaned = RoutineDraft(
                                name = routineName.trim(),
                                description = routineDescription.trim(),
                                exercises = exercises
                                    .filter { it.name.trim().isNotEmpty() }
                                    .map {
                                        it.copy(
                                            name = it.name.trim(),
                                            sets = max(1, it.sets),
                                            reps = max(1, it.reps),
                                            weightKg = if (it.isBodyWeight) null else (it.weightKg ?: 0f)
                                        )
                                    }
                            )

                            onCreate(cleaned)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = GymRankColors.PrimaryAccentText
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "GUARDAR RUTINA",
                            color = GymRankColors.PrimaryAccentText,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp) // espacio para el CTA fijo
        ) {
            item {
                GlassCard {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "Detalles",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        AppTextField(
                            value = routineName,
                            onValueChange = {
                                routineName = it
                                if (nameError != null) nameError = null
                            },
                            label = "Nombre de la rutina",
                            isError = nameError != null,
                            errorMessage = nameError,
                            enabled = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        Spacer(Modifier.height(10.dp))

                        AppTextField(
                            value = routineDescription,
                            onValueChange = { routineDescription = it },
                            label = "Descripción (opcional)",
                            enabled = true,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions.Default
                        )

                        if (submitError != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = submitError ?: "",
                                color = GymRankColors.Error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ejercicios",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Plantilla editable (sets/reps/peso)",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            exercises = exercises + RoutineExerciseDraft(
                                name = "",
                                sets = 3,
                                reps = 10,
                                weightKg = 0f,
                                isBodyWeight = false
                            )
                            if (submitError != null) submitError = null
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            itemsIndexed(exercises) { idx, ex ->
                ExerciseEditorCard(
                    index = idx,
                    value = ex,
                    accent = accent,
                    surface = surface,
                    input = input,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onChange = { updated ->
                        exercises = exercises.toMutableList().also { it[idx] = updated }
                        if (submitError != null) submitError = null
                    },
                    onRemove = {
                        exercises = exercises.toMutableList().also { it.removeAt(idx) }
                        if (submitError != null) submitError = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    index: Int,
    value: RoutineExerciseDraft,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color,
    onChange: (RoutineExerciseDraft) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ejercicio ${index + 1}",
                    color = textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = Color.White.copy(alpha = 0.70f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            AppTextField(
                value = value.name,
                onValueChange = { onChange(value.copy(name = it)) },
                label = "Nombre (ej: Press banca)",
                enabled = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniNumberField(
                    modifier = Modifier.weight(1f),
                    label = "Sets",
                    value = value.sets,
                    input = input,
                    accent = accent,
                    textPrimary = textPrimary,
                    onValueChange = { onChange(value.copy(sets = it)) }
                )
                MiniNumberField(
                    modifier = Modifier.weight(1f),
                    label = "Reps",
                    value = value.reps,
                    input = input,
                    accent = accent,
                    textPrimary = textPrimary,
                    onValueChange = { onChange(value.copy(reps = it)) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = input,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Peso corporal", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Sin kg", color = textSecondary, fontSize = 11.sp)
                        }

                        Switch(
                            checked = value.isBodyWeight,
                            onCheckedChange = {
                                onChange(
                                    value.copy(
                                        isBodyWeight = it,
                                        weightKg = if (it) null else (value.weightKg ?: 0f)
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accent,
                                checkedTrackColor = accent.copy(alpha = 0.30f)
                            )
                        )
                    }
                }

                if (!value.isBodyWeight) {
                    MiniWeightField(
                        modifier = Modifier.widthIn(min = 120.dp),
                        label = "Kg",
                        value = value.weightKg ?: 0f,
                        input = input,
                        accent = accent,
                        textPrimary = textPrimary,
                        onValueChange = { onChange(value.copy(weightKg = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniNumberField(
    modifier: Modifier,
    label: String,
    value: Int,
    input: Color,
    accent: Color,
    textPrimary: Color,
    onValueChange: (Int) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = input,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { onValueChange(max(1, value - 1)) },
                modifier = Modifier.size(28.dp)
            ) { Icon(Icons.Filled.Close, contentDescription = "-", tint = textPrimary) }

            Text(value.toString(), color = textPrimary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))

            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(28.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "+", tint = textPrimary) }
        }
    }
}

@Composable
private fun MiniWeightField(
    modifier: Modifier,
    label: String,
    value: Float,
    input: Color,
    accent: Color,
    textPrimary: Color,
    onValueChange: (Float) -> Unit
) {
    var text by remember(value) { mutableStateOf(if (value == 0f) "" else value.toString()) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = input,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))

            TextField(
                value = text,
                onValueChange = {
                    text = it
                    val parsed = it.replace(",", ".").toFloatOrNull()
                    if (parsed != null) onValueChange(parsed)
                    if (it.isBlank()) onValueChange(0f)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.width(90.dp)
            )
        }
    }
}
