package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class GambleStep { INTRO, SPIN_WHEEL, ROLL_DICE, CHOOSE_DURATION, ACCEPTED }
private enum class Duration { DAILY, SIDE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GambleScreen(
    onBack: () -> Unit,
    onGoToChallenges: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF35F5A6) }

    var step by remember { mutableStateOf(GambleStep.INTRO) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var focus by remember { mutableStateOf(Focus.UPPER) }
    var duration by remember { mutableStateOf(Duration.SIDE) }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Desafíos", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Fondo sutil
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            )

            FateModal(
                step = step,
                accent = accent,
                surface = surface,
                input = input,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                difficulty = difficulty,
                focus = focus,
                duration = duration,
                onClose = onBack,
                onBegin = { step = GambleStep.SPIN_WHEEL },

                // ✅ se llama cuando termina la animación de la rueda
                onSpin = {
                    difficulty = Difficulty.values().random()
                    step = GambleStep.ROLL_DICE
                },

                // ✅ se llama cuando termina la animación del dado
                onRoll = {
                    focus = Focus.values().random()
                    step = GambleStep.CHOOSE_DURATION
                },

                onPickDuration = { duration = it },
                onRandomizeDuration = {
                    duration = if (Random.nextBoolean()) Duration.DAILY else Duration.SIDE
                },
                onContinueFromDuration = { step = GambleStep.ACCEPTED },
                onAcceptQuest = { onGoToChallenges() },
                onBackToIntro = { step = GambleStep.INTRO }
            )
        }
    }
}

@Composable
private fun FateModal(
    step: GambleStep,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color,
    difficulty: Difficulty,
    focus: Focus,
    duration: Duration,
    onClose: () -> Unit,
    onBegin: () -> Unit,
    onSpin: () -> Unit,
    onRoll: () -> Unit,
    onPickDuration: (Duration) -> Unit,
    onRandomizeDuration: () -> Unit,
    onContinueFromDuration: () -> Unit,
    onAcceptQuest: () -> Unit,
    onBackToIntro: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // rueda
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {

                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, tint = accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                when (step) {
                                    GambleStep.INTRO -> "APUESTA DEL DESTINO"
                                    GambleStep.SPIN_WHEEL -> "GIRÁ LA RUEDA"
                                    GambleStep.ROLL_DICE -> "TIRÁ LOS DADOS"
                                    GambleStep.CHOOSE_DURATION -> "ELEGÍ DURACIÓN"
                                    GambleStep.ACCEPTED -> "MISIÓN ACEPTADA"
                                },
                                color = textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                when (step) {
                                    GambleStep.INTRO -> "Dejá que el destino elija tu desafío"
                                    GambleStep.SPIN_WHEEL -> "Definí el nivel del desafío"
                                    GambleStep.ROLL_DICE -> "Descubrí tu objetivo"
                                    GambleStep.CHOOSE_DURATION -> "Seleccioná el tiempo"
                                    GambleStep.ACCEPTED -> "Tu destino está sellado"
                                },
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                when (step) {
                    GambleStep.INTRO -> {
                        FateRowCard("Girás la rueda", "Define la dificultad", accent, input)
                        Spacer(Modifier.height(10.dp))
                        FateRowCard("Tirás los dados", "Define el foco del cuerpo", accent, input)
                        Spacer(Modifier.height(10.dp))
                        FateRowCard("Elegís duración", "Diario o Misión corta", accent, input)

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onBegin,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                "EMPEZAR",
                                color = GymRankColors.PrimaryAccentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    GambleStep.SPIN_WHEEL -> {
                        val labels = listOf("FÁCIL", "MEDIO", "DIFÍCIL", "INSANO")
                        var preview by remember { mutableStateOf(labels[1]) } // MEDIO default

                        // mientras gira, actualiza preview leyendo rotation.value
                        LaunchedEffect(spinning) {
                            if (!spinning) return@LaunchedEffect
                            while (spinning) {
                                val idx = wheelSegmentIndex(rotation.value, labels.size)
                                preview = labels[idx]
                                delay(35)
                            }
                        }

                        WheelOfFate(
                            accent = accent,
                            surface = input,
                            rotationDeg = rotation.value,
                            previewText = preview
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (spinning) return@Button
                                spinning = true

                                val extraSpins = 4 * 360f
                                val randomStop = Random.nextFloat() * 360f
                                val target = rotation.value + extraSpins + randomStop

                                scope.launch {
                                    rotation.animateTo(
                                        targetValue = target,
                                        animationSpec = tween(durationMillis = 1400)
                                    )
                                    spinning = false
                                    onSpin() // ✅ avanza después de animar
                                }
                            },
                            enabled = !spinning,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                if (spinning) "GIRANDO..." else "¡GIRAR!",
                                color = GymRankColors.PrimaryAccentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onBackToIntro,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                        ) { Text("Volver", color = textPrimary) }
                    }

                    GambleStep.ROLL_DICE -> {
                        var rollNonce by remember { mutableIntStateOf(0) }
                        var rolling by remember { mutableStateOf(false) }

                        // preview que va cambiando mientras “rueda”
                        var previewFocus by remember { mutableStateOf(focus) }

                        DicePlaceholder(
                            accent = accent,
                            surface = input,
                            current = previewFocus, // ✅ mostramos preview
                            rollNonce = rollNonce
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (rolling) return@Button
                                rolling = true
                                rollNonce++ // ✅ dispara animación del dado

                                scope.launch {
                                    // durante la animación, ciclar el texto "Actual"
                                    val t0 = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - t0 < 700) {
                                        previewFocus = Focus.values().random()
                                        delay(70)
                                    }

                                    rolling = false
                                    onRoll() // ✅ recién ahora fija y avanza
                                }
                            },
                            enabled = !rolling,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, tint = GymRankColors.PrimaryAccentText)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (rolling) "TIRANDO..." else "TIRAR",
                                color = GymRankColors.PrimaryAccentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onBackToIntro,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                        ) { Text("Volver", color = textPrimary) }
                    }

                    GambleStep.CHOOSE_DURATION -> {
                        var randomizing by remember { mutableStateOf(false) }

                        // preview que va cambiando mientras "ALEATORIO" está corriendo
                        var previewDuration by remember { mutableStateOf(duration) }

                        DurationPickerV2(
                            accent = accent,
                            surface = input,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            selected = previewDuration, // ✅ mostramos el preview
                            onPick = { picked ->
                                if (randomizing) return@DurationPickerV2
                                previewDuration = picked
                                onPickDuration(picked)
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (randomizing) return@Button
                                randomizing = true

                                scope.launch {
                                    // ✅ “ruletita” entre DIARIO y CORTA por ~700ms
                                    val t0 = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - t0 < 700) {
                                        previewDuration = if (previewDuration == Duration.DAILY) Duration.SIDE else Duration.DAILY
                                        delay(90)
                                    }

                                    // ✅ frena: elegimos uno final y lo guardamos
                                    val final = if (Random.nextBoolean()) Duration.DAILY else Duration.SIDE
                                    previewDuration = final
                                    onPickDuration(final)

                                    // ✅ y avanzamos automáticamente
                                    randomizing = false
                                    onContinueFromDuration()
                                }
                            },
                            enabled = !randomizing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = GymRankColors.PrimaryAccentText)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (randomizing) "ELIGIENDO..." else "ALEATORIO",
                                color = GymRankColors.PrimaryAccentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // ✅ sacamos "Continuar" y ponemos "Volver" como rueda/dado
                        OutlinedButton(
                            onClick = onBackToIntro,
                            enabled = !randomizing,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                        ) { Text("Volver", color = textPrimary) }
                    }


                    GambleStep.ACCEPTED -> {
                        AcceptedView(
                            accent = accent,
                            surface = input,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            difficulty = difficulty,
                            focus = focus,
                            duration = duration
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onAcceptQuest,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                "INICIAR MISIÓN",
                                color = GymRankColors.PrimaryAccentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FateRowCard(title: String, subtitle: String, accent: Color, surface: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}

private fun wheelSegmentIndex(rotationDeg: Float, count: Int): Int {
    val sweep = 360f / count
    val rot = ((rotationDeg % 360f) + 360f) % 360f
    val pointerAngle = 270f // -90 equivalente
    val angle = (pointerAngle - rot + 360f) % 360f
    return (angle / sweep).toInt().coerceIn(0, count - 1)
}

/**
 * ✅ RUEDA (Canvas) + puntero arriba + textos adentro + preview “Actual”
 */
@Composable
private fun WheelOfFate(
    accent: Color,
    surface: Color,
    rotationDeg: Float,
    previewText: String
) {
    val labels = listOf("FÁCIL", "MEDIO", "DIFÍCIL", "INSANO")
    val colors = listOf(
        accent.copy(alpha = 0.18f),
        accent.copy(alpha = 0.28f),
        accent.copy(alpha = 0.38f),
        accent.copy(alpha = 0.50f)
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val strokeW = 10f
                val diameter = size.minDimension
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val wheelSize = Size(diameter, diameter)
                val center = Offset(size.width / 2f, size.height / 2f)
                val wheelRadius = diameter / 2f

                // base sutil
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = wheelRadius
                )

                val sweep = 360f / labels.size

                // rueda rotando
                rotate(rotationDeg, pivot = center) {
                    // segmentos
                    labels.forEachIndexed { idx, _ ->
                        drawArc(
                            color = colors[idx],
                            startAngle = -90f + idx * sweep,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = topLeft,
                            size = wheelSize
                        )
                    }

                    // divisores
                    labels.indices.forEach { i ->
                        val angle = Math.toRadians((-90.0 + i * sweep).toDouble())
                        val x = center.x + cos(angle).toFloat() * wheelRadius
                        val y = center.y + sin(angle).toFloat() * wheelRadius
                        drawLine(
                            color = Color.White.copy(alpha = 0.10f),
                            start = center,
                            end = Offset(x, y),
                            strokeWidth = 2f
                        )
                    }

                    // textos dentro
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = (diameter * 0.07f)
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                        alpha = (0.85f * 255).toInt()
                    }

                    labels.forEachIndexed { idx, text ->
                        val midAngle = -90f + idx * sweep + sweep / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val rText = diameter * 0.32f

                        val x = center.x + cos(rad).toFloat() * rText
                        val y = center.y + sin(rad).toFloat() * rText

                        drawContext.canvas.nativeCanvas.save()
                        drawContext.canvas.nativeCanvas.rotate(midAngle + 90f, x, y)
                        drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
                        drawContext.canvas.nativeCanvas.restore()
                    }
                }

                // borde externo
                drawCircle(
                    color = accent.copy(alpha = 0.35f),
                    radius = wheelRadius - strokeW / 2f,
                    style = Stroke(width = strokeW)
                )

                // centro
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = diameter * 0.18f
                )
                drawCircle(
                    color = accent.copy(alpha = 0.25f),
                    radius = diameter * 0.18f,
                    style = Stroke(width = 3f)
                )

                // ✅ PUNTERO “PIN” hacia abajo, dentro de la rueda (NO rota)
                run {
                    // donde “entra” el pin: arriba del círculo, un poquito hacia adentro
                    val tipY = center.y - wheelRadius + (diameter * 0.06f)
                    val tip = Offset(center.x, tipY)

                    val pinW = diameter * 0.10f      // ancho del pin
                    val pinH = diameter * 0.12f      // alto del pin
                    val baseY = tipY - pinH          // base arriba, punta abajo

                    // triangulo invertido (punta abajo)
                    val pinPath = Path().apply {
                        moveTo(tip.x, tip.y)                          // punta (abajo)
                        lineTo(tip.x - pinW / 2f, baseY)              // esquina izq arriba
                        lineTo(tip.x + pinW / 2f, baseY)              // esquina der arriba
                        close()
                    }

                    // sombra suave
                    drawPath(
                        path = pinPath,
                        color = Color.Black.copy(alpha = 0.35f)
                    )

                    // relleno
                    drawPath(
                        path = pinPath,
                        color = accent.copy(alpha = 0.98f)
                    )

                    // borde
                    drawPath(
                        path = pinPath,
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = 2.5f)
                    )

                    // “cap” redondito arriba del pin (como ruleta real)
                    drawCircle(
                        color = accent.copy(alpha = 0.95f),
                        radius = diameter * 0.018f,
                        center = Offset(tip.x, baseY)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
            ) {
                Text(
                    text = "Actual: $previewText",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Tip: girá para elegir la dificultad",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}


@Composable
private fun DicePlaceholder(
    accent: Color,
    surface: Color,
    current: Focus,
    rollNonce: Int
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    // 🎲 cara del dado (1..6). Mientras gira vamos ciclando.
    var face by remember { mutableIntStateOf(1) }

    LaunchedEffect(rollNonce) {
        if (rollNonce == 0) return@LaunchedEffect

        face = (1..6).random()

        val extra = Random.nextInt(0, 120)
        val target = rotation.value + 900f + extra

        scope.launch {
            scale.snapTo(1f)
            scale.animateTo(1.08f, tween(120))
            scale.animateTo(1f, tween(220))
        }

        val flipJob = scope.launch {
            while (true) {
                face = (1..6).random()
                delay(70)
            }
        }

        rotation.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 700)
        )

        flipJob.cancel()
        face = (1..6).random()
    }

    val label = when (current) {
        Focus.UPPER -> "Upper"
        Focus.LOWER -> "Lower"
        Focus.ABS -> "Abs"
        Focus.CARDIO -> "Cardio"
    }

    val iconText = when (current) {
        Focus.UPPER -> "💪"
        Focus.LOWER -> "🦵"
        Focus.ABS -> "🔥"
        Focus.CARDIO -> "⚡"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            rotationZ = rotation.value
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF0F0F10).copy(alpha = 0.55f))
                        .border(2.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DiceFace(
                        value = face,
                        dotColor = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
            ) {
                Text(
                    text = "Actual: $iconText  $label",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Superior", "Inferior", "Abdomen", "Cardio").forEach {
                    Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DurationPickerV2(
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    selected: Duration,
    onPick: (Duration) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            val gap = 12.dp

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardW = (maxWidth - gap) / 2

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    DurationCardV2(
                        title = "DIARIO",
                        subtitle = "24 horas",
                        selected = selected == Duration.DAILY,
                        accent = accent,
                        icon = Icons.Filled.Schedule,
                        onClick = { onPick(Duration.DAILY) },
                        modifier = Modifier
                            .width(cardW)
                            .height(150.dp)
                    )

                    DurationCardV2(
                        title = "CORTA",
                        subtitle = "3 horas",
                        selected = selected == Duration.SIDE,
                        accent = accent,
                        icon = Icons.Filled.Bolt,
                        onClick = { onPick(Duration.SIDE) },
                        modifier = Modifier
                            .width(cardW)
                            .height(150.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Seleccionado: ${if (selected == Duration.DAILY) "Diario" else "Corta"}",
                color = textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationCardV2(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val border = if (selected) accent.copy(alpha = 0.70f) else accent.copy(alpha = 0.20f)
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)
    val iconBg = if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
    val iconTint = if (selected) accent else Color.White.copy(alpha = 0.70f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.height(14.dp))

            Text(title, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AcceptedView(
    accent: Color,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    difficulty: Difficulty,
    focus: Focus,
    duration: Duration
) {
    val diffLabel = difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
    val focusLabel = focus.name.lowercase().replaceFirstChar { it.uppercase() }
    val durLabel = if (duration == Duration.DAILY) "Diario" else "Corta"

    // ✅ Animaciones de entrada (una sola vez)
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(220))
        // pop + bounce
        scale.animateTo(1.10f, animationSpec = tween(180))
        scale.animateTo(0.98f, animationSpec = tween(140))
        scale.animateTo(1.00f, animationSpec = tween(140))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha.value }
    ) {
        // ✅ Badge “premium” con pop
        Box(
            Modifier
                .size(118.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .border(2.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "¡Tu misión te espera!",
            color = textPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SmallPill(diffLabel, accent)
                    Spacer(Modifier.width(8.dp))
                    SmallPill(focusLabel, accent)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SmallPill(durLabel, accent)
                    Spacer(Modifier.width(8.dp))
                    SmallPill("Hasta +3 ELO", accent)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "Dificultad, foco y duración fueron elegidos por el destino.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
private fun SmallPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DiceFace(
    value: Int,
    dotColor: Color = Color.White
) {
    val size = 10.dp

    val positions = when (value) {
        1 -> listOf(Alignment.Center)
        2 -> listOf(Alignment.TopStart, Alignment.BottomEnd)
        3 -> listOf(Alignment.TopStart, Alignment.Center, Alignment.BottomEnd)
        4 -> listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
        5 -> listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.Center, Alignment.BottomStart, Alignment.BottomEnd)
        else -> listOf(Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        positions.forEach { align ->
            Box(
                modifier = Modifier
                    .size(size)
                    .align(align)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
        }
    }
}
