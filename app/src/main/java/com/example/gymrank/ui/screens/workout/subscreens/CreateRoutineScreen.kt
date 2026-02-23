package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

// ============================
// MODELS
// ============================

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

// ============================
// EXERCISE CATALOG (muscle -> exercises)
// ============================

private val EXERCISES_BY_MUSCLE: Map<String, List<String>> = mapOf(
    "Pecho" to listOf(
        "Press plano con barra",
        "Press plano con mancuernas",
        "Press inclinado con barra",
        "Press inclinado con mancuernas",
        "Press declinado con barra",
        "Press declinado con mancuernas",
        "Press en máquina",
        "Press inclinado en máquina",
        "Press Hammer",
        "Press convergente",
        "Press en Smith plano",
        "Press en Smith inclinado",
        "Fondos en paralelas para pecho",
        "Flexiones",
        "Flexiones inclinadas",
        "Flexiones declinadas",
        "Aperturas con mancuernas planas",
        "Aperturas con mancuernas inclinadas",
        "Aperturas en peck deck (contractor)",
        "Cruces en polea alta (cable cross)",
        "Cruces en polea media",
        "Cruces en polea baja",
        "Aperturas en polea de pie",
        "Aperturas en polea acostado",
        "Pullover con mancuerna",
        "Pullover en polea",
        "Press con agarre neutro",
        "Press tipo Guillotine"
    ),
    "Espalda" to listOf(
        "Dominadas pronas",
        "Dominadas supinas",
        "Dominadas neutras",
        "Jalón al pecho en polea (agarre ancho)",
        "Jalón al pecho (agarre cerrado)",
        "Jalón al pecho (agarre neutro)",
        "Jalón tras nuca",
        "Remo con barra",
        "Remo Pendlay",
        "Remo con mancuerna a una mano",
        "Remo en banco inclinado (pecho apoyado)",
        "Remo en polea baja (sentado)",
        "Remo en máquina (remadora)",
        "Remo Hammer",
        "Remo en T",
        "Remo en T con apoyo de pecho",
        "Remo en Smith",
        "Jalón con brazos rectos (pullover en polea)",
        "Pull-over en máquina",
        "Peso muerto convencional",
        "Peso muerto estilo sumo",
        "Rack pull",
        "Buenos días",
        "Hiperextensiones lumbares",
        "Remo invertido",
        "Face pull",
        "Encogimientos escapulares en barra"
    ),
    "Femorales" to listOf(
        "Peso muerto rumano (barra)",
        "Peso muerto rumano (mancuernas)",
        "Peso muerto rumano a una pierna",
        "Buenos días (barra)",
        "Curl femoral acostado (máquina)",
        "Curl femoral sentado (máquina)",
        "Curl femoral parado (máquina)",
        "Curl nórdico (Nordic curl)",
        "Glute ham raise",
        "Pull-through en polea",
        "Kettlebell swing",
        "Hip hinge con banda elástica",
        "Curl femoral con fitball",
        "Curl femoral con deslizadores",
        "Peso muerto con piernas rígidas",
        "Buenos días en Smith",
        "Curl femoral en polea con tobillera",
        "RDL en Smith"
    ),
    "Hombros" to listOf(
        "Press militar con barra",
        "Press militar sentado con barra",
        "Press con mancuernas sentado",
        "Press con mancuernas parado",
        "Arnold press",
        "Press en máquina de hombros",
        "Press Hammer de hombros",
        "Press en Smith",
        "Elevaciones laterales con mancuernas",
        "Elevaciones laterales sentado",
        "Elevaciones laterales en polea",
        "Elevaciones laterales en máquina",
        "Elevaciones frontales con mancuernas",
        "Elevaciones frontales con disco",
        "Elevaciones frontales en polea",
        "Pájaros (deltoide posterior) con mancuernas",
        "Pájaros en peck deck inverso",
        "Pájaros en polea (cruce posterior)",
        "Face pull",
        "Remo al mentón (upright row)",
        "Encogimiento + press (push press)",
        "Y-raises en banco inclinado"
    ),
    "Bíceps" to listOf(
        "Curl con barra recta",
        "Curl con barra EZ",
        "Curl alternado con mancuernas",
        "Curl simultáneo con mancuernas",
        "Curl martillo",
        "Curl martillo cruzado",
        "Curl inclinado con mancuernas",
        "Curl predicador en banco Scott (barra EZ)",
        "Curl predicador con mancuerna",
        "Curl predicador en máquina",
        "Curl en polea baja (barra)",
        "Curl en polea con soga",
        "Curl concentración",
        "Curl araña (spider curl)",
        "Curl 21s",
        "Curl en banco inclinado con barra EZ",
        "Dominadas supinas (chin-up) enfocadas en bíceps",
        "Curl con banda elástica",
        "Curl en máquina (biceps machine)"
    ),
    "Tríceps" to listOf(
        "Press cerrado con barra",
        "Press cerrado en Smith",
        "Fondos en paralelas",
        "Fondos en banco (bench dips)",
        "Extensión de tríceps en polea (pushdown) con barra",
        "Extensión de tríceps en polea con soga",
        "Pushdown agarre inverso",
        "Extensión por encima de la cabeza con mancuerna (a dos manos)",
        "Extensión por encima de la cabeza con mancuerna (una mano)",
        "Extensión por encima de la cabeza en polea (con soga)",
        "Rompecráneos / press francés (skull crushers) con barra EZ",
        "Press francés sentado (barra EZ)",
        "Patada de tríceps con mancuerna (kickback)",
        "Patada de tríceps en polea",
        "Extensión acostado con mancuernas",
        "Flexiones diamante",
        "JM press",
        "Extensión en máquina de tríceps"
    ),
    "Abdomen" to listOf(
        "Crunch",
        "Crunch en máquina",
        "Crunch en polea",
        "Crunch en banco declinado",
        "Crunch con disco en el pecho",
        "Elevación de piernas colgado",
        "Elevación de rodillas colgado",
        "Elevación de piernas en paralelas",
        "Reverse crunch",
        "Plancha (plank)",
        "Plancha lateral",
        "Rueda abdominal (ab wheel)",
        "Dead bug",
        "Hollow hold",
        "Bicycle crunch",
        "Mountain climbers",
        "Toques de talón (heel taps)",
        "V-ups",
        "Russian twist",
        "Woodchopper en polea",
        "Pallof press en polea/banda"
    ),
    "Glúteos" to listOf(
        "Hip thrust con barra",
        "Hip thrust en máquina",
        "Hip thrust en Smith",
        "Puente de glúteos (glute bridge)",
        "Puente a una pierna",
        "Patada de glúteo en polea (tobillera)",
        "Patada de glúteo en máquina",
        "Abducción de cadera en máquina",
        "Abducción con banda (mini band)",
        "Sentadilla (barra)",
        "Sentadilla en Smith",
        "Sentadilla sumo",
        "Peso muerto sumo",
        "Zancadas caminando",
        "Zancadas atrás",
        "Búlgaras (Bulgarian split squat)",
        "Step-up (subidas al banco)",
        "Pull-through en polea",
        "Buenos días (enfocado en glúteo/hinge)",
        "Cable kickback cruzado"
    ),
    "Cuádriceps" to listOf(
        "Sentadilla con barra",
        "Sentadilla frontal",
        "Sentadilla goblet",
        "Sentadilla en Smith",
        "Hack squat (máquina)",
        "Prensa 45° (leg press)",
        "Prensa horizontal",
        "Extensión de piernas (máquina)",
        "Sissy squat",
        "Zancadas (lunges)",
        "Zancadas caminando",
        "Zancadas atrás",
        "Búlgaras",
        "Step-up",
        "Sentadilla en caja",
        "Sentadilla con pausa",
        "Wall sit",
        "Sentadilla sumo",
        "Trineo / empuje de trineo"
    ),
    "Gemelos" to listOf(
        "Gemelos de pie en máquina",
        "Gemelos sentado en máquina",
        "Gemelos en prensa",
        "Gemelos a una pierna",
        "Gemelos en Smith",
        "Donkey calf raise",
        "Saltar la soga",
        "Elevaciones excéntricas de gemelos",
        "Gemelos con mancuerna",
        "Gemelos en multipower"
    ),
    "Trapecios" to listOf(
        "Encogimientos con barra",
        "Encogimientos con mancuernas",
        "Encogimientos en Smith",
        "Encogimientos en máquina",
        "Farmer walk (caminata del granjero)",
        "Rack pull",
        "Peso muerto",
        "High pull",
        "Remo al mentón",
        "Face pull (trapecio medio/alto)",
        "Remo con barra (espalda alta)",
        "Y-raises / W-raises (trapecio medio)"
    ),
    "Antebrazos" to listOf(
        "Curl de muñeca con barra",
        "Curl de muñeca con mancuernas",
        "Curl de muñeca inverso",
        "Curl inverso con barra",
        "Curl martillo",
        "Farmer walk",
        "Colgarse de la barra",
        "Pinza con discos",
        "Pronación/supinación con mancuerna",
        "Wrist roller (rodillo de muñeca)",
        "Apretar hand gripper",
        "Extensión de dedos con banda elástica"
    )
)

private fun availableExercisesFor(selectedMuscles: Set<String>): List<String> {
    val muscles = selectedMuscles
        .filter { it != "Oblicuos" }
        .toSet()

    val source = if (muscles.isEmpty()) {
        EXERCISES_BY_MUSCLE.values.flatten()
    } else {
        muscles.flatMap { m -> EXERCISES_BY_MUSCLE[m].orEmpty() }
    }

    return source.distinct().sorted()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineScreen(
    onBack: () -> Unit,
    onCreate: (RoutineDraft, List<String>) -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF35F5A6) }

    val nameFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // ✅ FIX: state de lista para poder scrollear al top sin crashear
    val listState = rememberLazyListState()
    var shouldFocusName by remember { mutableStateOf(false) }
    var shouldScrollTop by remember { mutableStateOf(false) }

    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }

    val musclesAll = listOf(
        "Pecho", "Espalda", "Femorales", "Hombros",
        "Bíceps", "Tríceps", "Abdomen", "Glúteos",
        "Cuádriceps", "Gemelos", "Trapecios", "Antebrazos",
        "Oblicuos"
    )
    var musclesSelected by remember { mutableStateOf(setOf<String>()) }

    val availableExercises = remember(musclesSelected) {
        availableExercisesFor(musclesSelected)
    }

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
            nameError = "Poné un nombre para el entrenamiento."
            return false
        }

        val hasAtLeastOneExercise = exercises.any { it.name.trim().isNotEmpty() }
        if (!hasAtLeastOneExercise) {
            submitError = "Agregá al menos 1 ejercicio con nombre."
            return false
        }

        return true
    }

    // ✅ FIX: scroll al top (sin crashear)
    LaunchedEffect(shouldScrollTop) {
        if (!shouldScrollTop) return@LaunchedEffect
        runCatching { listState.animateScrollToItem(0) }
        shouldScrollTop = false
    }

    // ✅ FIX: focus + teclado en el próximo frame (evita IllegalStateException del FocusRequester)
    LaunchedEffect(shouldFocusName) {
        if (!shouldFocusName) return@LaunchedEffect

        delay(16) // 1 frame aprox

        runCatching {
            nameFocusRequester.requestFocus()
            keyboard?.show()
        }

        shouldFocusName = false
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Crear entrenamiento",
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

        // ✅ BOTÓN AGREGAR FLOTANTE
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
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
                containerColor = accent,
                contentColor = GymRankColors.PrimaryAccentText,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.padding(bottom = 84.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar ejercicio", fontWeight = FontWeight.ExtraBold)
            }
        },

        bottomBar = {
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
                            if (!validate()) {
                                // ✅ FIX: en vez de crashear, scrollea y enfoca
                                if (nameError != null) {
                                    shouldScrollTop = true
                                    shouldFocusName = true
                                } else {
                                    // error por ejercicios -> al menos lo llevamos arriba
                                    shouldScrollTop = true
                                }
                                return@Button
                            }

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

                            onCreate(cleaned, musclesSelected.toList())
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
                            text = "GUARDAR ENTRENAMIENTO",
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
            state = listState, // ✅ CLAVE: para animateScrollToItem(0)
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            // ----------------------------
            // DETALLES
            // ----------------------------
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester)
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

            // ----------------------------
            // MÚSCULOS
            // ----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.SurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Músculos del día",
                                    color = DesignTokens.Colors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Filtra los ejercicios sugeridos",
                                    color = DesignTokens.Colors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            TextButton(
                                onClick = { musclesSelected = emptySet() },
                                enabled = musclesSelected.isNotEmpty()
                            ) {
                                Text(
                                    "Limpiar",
                                    color = if (musclesSelected.isNotEmpty()) accent else DesignTokens.Colors.TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        MuscleGridPicker(
                            muscles = musclesAll.filter { it != "Oblicuos" },
                            selected = musclesSelected,
                            onToggle = { m ->
                                musclesSelected =
                                    if (musclesSelected.contains(m)) musclesSelected - m else musclesSelected + m
                            },
                            accent = accent
                        )
                    }
                }
            }

            // ----------------------------
            // EJERCICIOS
            // ----------------------------
            item {
                Column {
                    Text(
                        text = "Ejercicios",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Seleccioná el ejercicio (sets/reps/peso)",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            itemsIndexed(exercises) { idx, ex ->
                ExerciseEditorCard(
                    index = idx,
                    value = ex,
                    availableExercises = availableExercises,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleGridPicker(
    muscles: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    accent: Color
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2
    ) {
        muscles.forEach { muscle ->
            val isSelected = selected.contains(muscle)

            val bg = if (isSelected) accent.copy(alpha = 0.12f) else DesignTokens.Colors.SurfaceInputs
            val border = if (isSelected) accent.copy(alpha = 0.65f) else DesignTokens.Colors.SurfaceInputs
            val icon = if (isSelected) Icons.Filled.Check else Icons.Filled.Add

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onToggle(muscle) },
                shape = RoundedCornerShape(999.dp),
                color = bg,
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, if (isSelected) accent else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) accent else DesignTokens.Colors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = muscle,
                        color = DesignTokens.Colors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    index: Int,
    value: RoutineExerciseDraft,
    availableExercises: List<String>,
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

            ExerciseDropdownField(
                label = "Ejercicio",
                value = value.name,
                options = availableExercises,
                input = input,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onSelect = { chosen ->
                    onChange(value.copy(name = chosen))
                }
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
                            Text(
                                "Peso corporal",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
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
            ) { Icon(Icons.Filled.Remove, contentDescription = "-", tint = textPrimary) }

            Text(
                value.toString(),
                color = textPrimary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDropdownField(
    label: String,
    value: String,
    options: List<String>,
    input: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onSelect: (String) -> Unit,
    helperText: String = "Filtrado por músculos seleccionados"
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(expanded) { mutableStateOf("") }

    val display = if (value.isBlank()) "Seleccionar ejercicio" else value

    val filtered = remember(options, query) {
        if (query.isBlank()) options
        else {
            val q = query.trim().lowercase()
            options.filter { it.lowercase().contains(q) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = {
                Text(
                    text = if (options.isEmpty()) "No hay ejercicios para esos músculos." else helperText,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = textSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.List,
                    contentDescription = null,
                    tint = accent
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = accent.copy(alpha = 0.55f),
                unfocusedBorderColor = accent.copy(alpha = 0.20f),
                focusedLabelColor = textSecondary,
                unfocusedLabelColor = textSecondary,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = accent,
                containerColor = input
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DesignTokens.Colors.SurfaceElevated)
                .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
        ) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Elegí un ejercicio",
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${filtered.size}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = {
                            Text("Buscar (ej: press, curl, remo...)", color = textSecondary)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = input,
                            unfocusedContainerColor = input,
                            disabledContainerColor = input,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = accent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )

            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay ejercicios para esos músculos.", color = textSecondary) },
                    onClick = { expanded = false }
                )
                return@DropdownMenu
            }

            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin resultados para \"$query\"", color = textSecondary) },
                    onClick = { /* no-op */ }
                )
                return@DropdownMenu
            }

            filtered.forEach { item ->
                val isSelected = item == value

                DropdownMenuItem(
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = item,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.10f) else Color.Transparent)
                )
            }
        }
    }
}