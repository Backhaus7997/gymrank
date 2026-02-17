package com.example.gymrank.ui.screens.onboarding

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.components.AppTextField
import com.example.gymrank.ui.components.GlassCard
import com.example.gymrank.ui.components.PrimaryCtaButton
import com.example.gymrank.ui.components.SecondaryTextButton
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Height
import androidx.compose.foundation.BorderStroke




enum class SimpleStep { DOB, WEIGHT, HEIGHT, GENDER, EXPERIENCE }

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    Log.d("ONBOARDING_UI", "USING ui/screens/onboarding/OnboardingScreen.kt")

    var currentStep by remember { mutableStateOf(SimpleStep.DOB) }

    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }

    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF35F5A6) }

    val steps = remember {
        listOf(SimpleStep.DOB, SimpleStep.WEIGHT, SimpleStep.HEIGHT, SimpleStep.GENDER, SimpleStep.EXPERIENCE)
    }
    val stepIndex = steps.indexOf(currentStep).coerceAtLeast(0)
    val progress = ((stepIndex + 1).toFloat() / steps.size.toFloat()).coerceIn(0f, 1f)
    val isLast = currentStep == SimpleStep.EXPERIENCE

    fun goBack() {
        currentStep = when (currentStep) {
            SimpleStep.WEIGHT -> SimpleStep.DOB
            SimpleStep.HEIGHT -> SimpleStep.WEIGHT
            SimpleStep.GENDER -> SimpleStep.HEIGHT
            SimpleStep.EXPERIENCE -> SimpleStep.GENDER
            else -> SimpleStep.DOB
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Background base (vertical)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(bg, bg.copy(alpha = 0.92f))
                    )
                )
        )

        // Accent glow (radial)
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

        // Bottom depth gradient (para que no quede plano/negro)
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

        // Watermark
        Box(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = "GYM RANK",
                color = accent.copy(alpha = 0.12f),
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        // subtle scrim
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

            // Top area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))

                // Badge "GYM RANK" (sutil arriba)
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

                // Progress header
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

                // Hero icon
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
                            OnboardingStepContent(
                                step = step,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent,
                                dob = dob,
                                onDobChange = { dob = it },
                                weight = weight,
                                onWeightChange = { weight = it },
                                height = height,
                                onHeightChange = { height = it },
                                gender = gender,
                                onGenderPick = { gender = it },
                                experience = experience,
                                onExperiencePick = { experience = it }
                            )
                        }
                    }
                }
            }

            // Bottom CTA row (Volver SIEMPRE visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canGoBack = currentStep != SimpleStep.DOB

                // Mostramos siempre "Volver" para que no “desaparezca” la UI
                Box(modifier = Modifier.alpha(if (canGoBack) 1f else 0.45f)) {
                    SecondaryTextButton(
                        text = "Volver",
                        onClick = { if (canGoBack) goBack() }
                    )
                }

                Spacer(Modifier.weight(1f))

                PrimaryCtaButton(
                    text = if (isLast) "Finalizar" else "Continuar",
                    onClick = {
                        if (isLast) {
                            onFinished()
                        } else {
                            currentStep = when (currentStep) {
                                SimpleStep.DOB -> SimpleStep.WEIGHT
                                SimpleStep.WEIGHT -> SimpleStep.HEIGHT
                                SimpleStep.HEIGHT -> SimpleStep.GENDER
                                SimpleStep.GENDER -> SimpleStep.EXPERIENCE
                                SimpleStep.EXPERIENCE -> SimpleStep.EXPERIENCE
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepContent(
    step: SimpleStep,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    dob: String,
    onDobChange: (String) -> Unit,
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
        SimpleStep.DOB -> {
            title = "Fecha de nacimiento"
            subtitle = "¿Cuándo naciste?"
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
            subtitle = "Contanos tu experiencia"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = textSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(14.dp))

        when (step) {
            SimpleStep.DOB -> {
                AppTextField(
                    value = dob,
                    onValueChange = onDobChange,
                    label = "DD/MM/AAAA",
                    leadingIcon = Icons.Default.Person
                )
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
                ChoiceChipsRow(
                    options = listOf("Masculino", "Femenino", "Otro"),
                    selected = gender,
                    accent = accent,
                    onPick = onGenderPick
                )
                Spacer(Modifier.height(10.dp))
                if (gender.isNotBlank()) {
                    Text(
                        text = "Seleccionado: $gender",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SimpleStep.EXPERIENCE -> {
                ChoiceChipsRow(
                    options = listOf("Principiante", "Intermedio", "Avanzado"),
                    selected = experience,
                    accent = accent,
                    onPick = onExperiencePick
                )
                Spacer(Modifier.height(10.dp))
                if (experience.isNotBlank()) {
                    Text(
                        text = "Seleccionado: $experience",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceChipsRow(
    options: List<String>,
    selected: String,
    accent: Color,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            options.forEach { opt ->
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

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tocá una opción para continuar",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
