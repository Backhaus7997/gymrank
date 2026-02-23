package com.example.gymrank.ui.screens.onboarding

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.data.repository.UserRepositoryImpl
import com.example.gymrank.ui.components.AppTextField
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.components.PrimaryCtaButton
import com.example.gymrank.ui.components.SecondaryTextButton
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
enum class SimpleStep { USERNAME, DOB, WEIGHT, HEIGHT, GENDER, EXPERIENCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    Log.d("ONBOARDING_UI", "USING ui/screens/onboarding/OnboardingScreen.kt")

    val coroutineScope = rememberCoroutineScope()
    val userRepo = remember { UserRepositoryImpl() }

    var currentStep by remember { mutableStateOf(SimpleStep.USERNAME) }

    var username by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // "DD/MM/AAAA"
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // DatePicker state
    var showDobPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF35F5A6) }

    val steps = remember {
        listOf(
            SimpleStep.USERNAME,
            SimpleStep.DOB,
            SimpleStep.WEIGHT,
            SimpleStep.HEIGHT,
            SimpleStep.GENDER,
            SimpleStep.EXPERIENCE
        )
    }
    val stepIndex = steps.indexOf(currentStep).coerceAtLeast(0)
    val progress = ((stepIndex + 1).toFloat() / steps.size.toFloat()).coerceIn(0f, 1f)
    val isLast = currentStep == SimpleStep.EXPERIENCE

    fun goBack() {
        errorMsg = null
        currentStep = when (currentStep) {
            SimpleStep.DOB -> SimpleStep.USERNAME
            SimpleStep.WEIGHT -> SimpleStep.DOB
            SimpleStep.HEIGHT -> SimpleStep.WEIGHT
            SimpleStep.GENDER -> SimpleStep.HEIGHT
            SimpleStep.EXPERIENCE -> SimpleStep.GENDER
            else -> SimpleStep.USERNAME
        }
    }

    // ============================
    // ✅ Validaciones por step
    // ============================
    fun usernameIsValid(value: String): Boolean {
        val v = value.trim()
        if (v.length !in 3..20) return false
        // letras/números/_ y . (sin espacios), y no puede empezar/terminar con . o _
        val regex = Regex("""^(?![._])(?!.*[._]{2})[a-zA-Z0-9._]{3,20}(?<![._])$""")
        return regex.matches(v)
    }

    fun dobIsValid(value: String): Boolean {
        // DD/MM/AAAA y fecha real
        val regex = Regex("""^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\d{4}$""")
        if (!regex.matches(value.trim())) return false

        val parts = value.trim().split("/")
        val dd = parts[0].toInt()
        val mm = parts[1].toInt()
        val yyyy = parts[2].toInt()

        // validar con Calendar (incluye meses con 30/31 y febrero)
        val cal = Calendar.getInstance()
        cal.isLenient = false
        return runCatching {
            cal.set(Calendar.YEAR, yyyy)
            cal.set(Calendar.MONTH, mm - 1)
            cal.set(Calendar.DAY_OF_MONTH, dd)
            cal.time // fuerza validación
            true
        }.getOrElse { false }
    }

    fun weightIsValid(value: String): Boolean {
        val n = value.trim().toIntOrNull() ?: return false
        return n in 30..300
    }

    fun heightIsValid(value: String): Boolean {
        val n = value.trim().toIntOrNull() ?: return false
        return n in 120..250
    }

    fun genderIsValid(value: String) = value in listOf("Masculino", "Femenino", "Otro")
    fun expIsValid(value: String) = value in listOf("Principiante", "Intermedio", "Avanzado")

    val isStepValid = when (currentStep) {
        SimpleStep.USERNAME -> usernameIsValid(username)
        SimpleStep.DOB -> dobIsValid(dob)
        SimpleStep.WEIGHT -> weightIsValid(weight)
        SimpleStep.HEIGHT -> heightIsValid(height)
        SimpleStep.GENDER -> genderIsValid(gender)
        SimpleStep.EXPERIENCE -> expIsValid(experience)
    }

    fun stepErrorText(): String {
        return when (currentStep) {
            SimpleStep.USERNAME ->
                "Usuario inválido. 3-20 chars. Solo letras/números/._ y sin espacios."
            SimpleStep.DOB ->
                "Fecha inválida. Elegí una fecha válida (DD/MM/AAAA)."
            SimpleStep.WEIGHT ->
                "Peso inválido. Usá un número entre 30 y 300."
            SimpleStep.HEIGHT ->
                "Altura inválida. Usá un número entre 120 y 250."
            SimpleStep.GENDER ->
                "Seleccioná un género para continuar."
            SimpleStep.EXPERIENCE ->
                "Seleccioná tu experiencia para finalizar."
        }
    }

    fun goNext() {
        currentStep = when (currentStep) {
            SimpleStep.USERNAME -> SimpleStep.DOB
            SimpleStep.DOB -> SimpleStep.WEIGHT
            SimpleStep.WEIGHT -> SimpleStep.HEIGHT
            SimpleStep.HEIGHT -> SimpleStep.GENDER
            SimpleStep.GENDER -> SimpleStep.EXPERIENCE
            SimpleStep.EXPERIENCE -> SimpleStep.EXPERIENCE
        }
    }

    // ============================
    // ✅ DOB DatePickerDialog
    // ============================
    if (showDobPicker) {
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            // ✅ IMPORTANTE: usar UTC para no restar 1 día
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val dd = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                            val mm = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                            val yyyy = cal.get(Calendar.YEAR).toString()
                            dob = "$dd/$mm/$yyyy"
                            errorMsg = null
                        }
                        showDobPicker = false
                    }
                ) { Text("Listo") }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ============================
    // UI
    // ============================
    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(bg, bg.copy(alpha = 0.92f))))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                        radius = 950f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            accent.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = "GYM RANK",
                color = accent.copy(alpha = 0.12f),
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.14f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = "GYM RANK",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Armá tu perfil",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Paso ${stepIndex + 1} de ${steps.size}",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent.copy(alpha = 0.85f),
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Spacer(Modifier.height(18.dp))

                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.26f),
                    modifier = Modifier
                        .size(70.dp)
                        .alpha(0.9f)
                )

                Spacer(Modifier.height(10.dp))
            }

            // Main card
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) {
                        Crossfade(targetState = currentStep, label = "onboarding-step") { step ->
                            StepContent(
                                step = step,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent,

                                username = username,
                                onUsernameChange = { v ->
                                    username = v
                                    errorMsg = null
                                },

                                dob = dob,
                                onOpenDobPicker = {
                                    errorMsg = null
                                    showDobPicker = true
                                },

                                weight = weight,
                                onWeightChange = { v ->
                                    weight = v
                                    errorMsg = null
                                },

                                height = height,
                                onHeightChange = { v ->
                                    height = v
                                    errorMsg = null
                                },

                                gender = gender,
                                onGenderPick = { v ->
                                    gender = v
                                    errorMsg = null
                                },

                                experience = experience,
                                onExperiencePick = { v ->
                                    experience = v
                                    errorMsg = null
                                }
                            )
                        }

                        if (errorMsg != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMsg!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom CTAs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canGoBack = currentStep != SimpleStep.USERNAME

                Box(modifier = Modifier.alpha(if (canGoBack) 1f else 0.45f)) {
                    SecondaryTextButton(
                        text = "Volver",
                        onClick = { if (canGoBack && !isSaving) goBack() }
                    )
                }

                Spacer(Modifier.weight(1f))

                PrimaryCtaButton(
                    text = if (isLast) (if (isSaving) "Guardando..." else "Finalizar") else "Continuar",
                    onClick = {
                        if (!isStepValid) {
                            errorMsg = stepErrorText()
                            return@PrimaryCtaButton
                        }

                        errorMsg = null

                        if (!isLast) {
                            goNext()
                        } else {
                            coroutineScope.launch {
                                isSaving = true

                                runCatching {
                                    userRepo.saveOnboarding(
                                        username = username.trim(),
                                        dob = dob.trim(),
                                        weightKg = weight.trim().toInt(),
                                        heightCm = height.trim().toInt(),
                                        gender = gender,
                                        experience = experience
                                    )
                                }.onSuccess {
                                    onFinished()
                                }.onFailure { e ->
                                    errorMsg = e.message ?: "No se pudo guardar tu perfil"
                                }

                                isSaving = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    step: SimpleStep,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,

    username: String,
    onUsernameChange: (String) -> Unit,

    dob: String,
    onOpenDobPicker: () -> Unit,

    weight: String,
    onWeightChange: (String) -> Unit,

    height: String,
    onHeightChange: (String) -> Unit,

    gender: String,
    onGenderPick: (String) -> Unit,

    experience: String,
    onExperiencePick: (String) -> Unit
) {
    val title: String
    val subtitle: String

    when (step) {
        SimpleStep.USERNAME -> {
            title = "Nombre de usuario"
            subtitle = "Elegí cómo querés aparecer en el ranking"
        }
        SimpleStep.DOB -> {
            title = "Fecha de nacimiento"
            subtitle = "Elegí tu fecha (se guarda como DD/MM/AAAA)"
        }
        SimpleStep.WEIGHT -> {
            title = "Peso"
            subtitle = "Ingresá tu peso actual"
        }
        SimpleStep.HEIGHT -> {
            title = "Altura"
            subtitle = "Ingresá tu altura"
        }
        SimpleStep.GENDER -> {
            title = "Género"
            subtitle = "Seleccioná tu género"
        }
        SimpleStep.EXPERIENCE -> {
            title = "Experiencia"
            subtitle = "¿Hace cuánto entrenás?"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = textSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        when (step) {
            SimpleStep.USERNAME -> {
                AppTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "Ej: usuario123",
                    leadingIcon = Icons.Filled.Badge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tip: sin espacios. Podés usar . y _",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            SimpleStep.DOB -> {
                // Campo de DOB: solo lectura + abre DatePicker
                OutlinedTextField(
                    value = dob,
                    onValueChange = { /* readOnly */ },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("DD/MM/AAAA") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = onOpenDobPicker) { Text("Elegir") }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent.copy(alpha = 0.65f),
                        unfocusedBorderColor = accent.copy(alpha = 0.25f),
                        focusedLabelColor = Color.White.copy(alpha = 0.85f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.55f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onOpenDobPicker,
                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.18f)),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Abrir calendario", color = Color.White)
                }
            }

            SimpleStep.WEIGHT -> {
                AppTextField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = "Peso (kg)",
                    leadingIcon = Icons.Filled.MonitorWeight
                )
            }

            SimpleStep.HEIGHT -> {
                AppTextField(
                    value = height,
                    onValueChange = onHeightChange,
                    label = "Altura (cm)",
                    leadingIcon = Icons.Filled.Height
                )
            }

            SimpleStep.GENDER -> {
                CenteredChoiceCard(
                    accent = accent,
                    title = "Elegí una opción",
                    subtitle = "Esto ayuda a personalizar tu perfil"
                ) {
                    ChoiceChipsGrid(
                        options = listOf("Masculino", "Femenino", "Otro"),
                        selected = gender,
                        accent = accent,
                        onPick = onGenderPick
                    )
                }
            }

            SimpleStep.EXPERIENCE -> {
                CenteredChoiceCard(
                    accent = accent,
                    title = "Tu nivel",
                    subtitle = "No te preocupes, después lo podés ajustar"
                ) {
                    ChoiceChipsGrid(
                        options = listOf("Principiante", "Intermedio", "Avanzado"),
                        selected = experience,
                        accent = accent,
                        onPick = onExperiencePick
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredChoiceCard(
    accent: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            content()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tocá una opción para continuar",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChoiceChipsGrid(
    options: List<String>,
    selected: String,
    accent: Color,
    onPick: (String) -> Unit
) {
    // 2 filas prolijas (centradas)
    val rows = options.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { opt ->
                    val isSelected = opt == selected

                    FilterChip(
                        selected = isSelected,
                        onClick = { onPick(opt) },
                        label = {
                            Text(
                                text = opt,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = Color.White.copy(alpha = 0.78f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.20f),
                            borderWidth = 1.dp
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
