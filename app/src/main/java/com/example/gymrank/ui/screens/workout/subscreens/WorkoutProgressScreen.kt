package com.example.gymrank.ui.screens.workout.subscreens

import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.ui.theme.DesignTokens
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb

// ---------- Token helper ----------
private fun tokenColor(value: () -> Any?, fallback: Color): Color {
    return (runCatching { value() }.getOrNull() as? Color) ?: fallback
}

// ---------- UI Metric ----------
private enum class Metric(val label: String) { Weight("Peso"), Reps("Reps"), Volume("Volumen") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutProgressScreen(
    onBack: () -> Unit,
    vm: WorkoutProgressViewModel = viewModel()
) {
    val bg = tokenColor({ DesignTokens.Colors.BackgroundBase }, Color(0xFF000000))
    val textPrimary = tokenColor({ DesignTokens.Colors.TextPrimary }, Color.White)
    val textSecondary = tokenColor({ DesignTokens.Colors.TextSecondary }, Color(0xFF8E8E93))
    val cardBg = Color(0xFF101012)

    val ui by vm.ui.collectAsState()

    val handleBack: () -> Unit = {
        if (ui.selectedMuscle != null) vm.clearSelection() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso", color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = textSecondary)
                    }
                }
            )
        },
        containerColor = bg
    ) { innerPadding ->

        when {
            ui.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            ui.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text(ui.error ?: "Error", color = textSecondary) }
            }

            ui.workouts.isEmpty() -> {
                EmptyProgressState(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    textSecondary = textSecondary
                )
            }

            // ✅ NUEVO: si NO hay músculo seleccionado => lista tipo foto 2
            ui.selectedMuscle == null -> {
                MuscleListScreen(
                    muscles = ui.availableMuscles,
                    onClick = { vm.selectMuscle(it) },
                    innerPadding = innerPadding,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg
                )
            }

            // ✅ si hay músculo seleccionado => pantalla de ejercicios (como ya tenías)
            else -> {
                ExerciseProgressScreen(
                    selectedMuscle = ui.selectedMuscle!!,
                    list = ui.exerciseProgress,
                    innerPadding = innerPadding,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg
                )
            }
        }
    }
}

// ---------- Pantalla: Lista de músculos (foto 2) ----------
@Composable
private fun MuscleListScreen(
    muscles: List<String>,
    onClick: (String) -> Unit,
    innerPadding: PaddingValues,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Elegí un músculo",
                color = textSecondary,
                fontSize = 13.sp
            )
        }

        if (muscles.isEmpty()) {
            item {
                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "No hay músculos cargados todavía.",
                        color = textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(muscles) { m ->
                MuscleRowCard(
                    name = m,
                    onClick = { onClick(m) },
                    textPrimary = textPrimary,
                    cardBg = cardBg
                )
            }
        }
    }
}

@Composable
private fun MuscleRowCard(
    name: String,
    onClick: () -> Unit,
    textPrimary: Color,
    cardBg: Color
) {
    Surface(
        color = cardBg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = textPrimary.copy(alpha = 0.5f)
            )
        }
    }
}

// ---------- Pantalla: Ejercicios ----------
@Composable
private fun ExerciseProgressScreen(
    selectedMuscle: String,
    list: List<ExerciseProgress>,
    innerPadding: PaddingValues,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            selectedMuscle,
            color = textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(10.dp))

        val visibleExercises = list.filter { it.points.size >= 2 }

        if (visibleExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Todavía no hay progreso suficiente",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Necesitás al menos 2 entrenamientos\ndel mismo ejercicio para ver evolución.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Ejercicios (${list.size})",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            }
            items(
                list.filter { it.points.size >= 2 }
            ) { ex ->
                ExerciseProgressCardPro(
                    ex = ex,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg
                )
            }
        }
    }
}

// ---------- Empty state ----------
@Composable
private fun EmptyProgressState(
    modifier: Modifier,
    textSecondary: Color
) {
    Box(modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Todavía no hay datos de progreso",
                color = textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Empezá a registrar entrenamientos para ver tu evolución.",
                color = textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ---------- Card por ejercicio (igual que venías usando) ----------
@Composable
private fun ExerciseProgressCardPro(
    ex: ExerciseProgress,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color
) {
    var metric by remember { mutableStateOf(Metric.Weight) }

    val series = remember(ex, metric) { metricSeries(ex, metric) }
    val arrow = remember(series) { trendArrow(series) }
    val last = metricLast(ex, metric)
    val best = metricBest(ex, metric)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ex.exerciseName,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${ex.points.size} sesiones • tendencia $arrow",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Últ: ${formatMetric(metric, last)}",
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "PR: ${formatMetric(metric, best)}",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (series.size >= 2) {
                Spacer(Modifier.height(12.dp))
                MetricSelector(
                    selected = metric,
                    onSelected = { metric = it }
                )
            }

            Spacer(Modifier.height(12.dp))

            val hasChart = series.size >= 2

            // Solo mostrar gráfico si hay al menos 2 puntos
            if (series.size >= 2) {
                Spacer(Modifier.height(12.dp))

                LineChart(
                    series = series,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    lineColor = Color(0xFF00E5A8),
                    axisColor = Color.White.copy(alpha = 0.22f),
                    showDots = true
                )
            }
        }
    }
}

// ---------- Selector de métrica ----------
@Composable
private fun MetricSelector(
    selected: Metric,
    onSelected: (Metric) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = Metric.entries
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            divider = {}
        ) {
            tabs.forEachIndexed { index, metric ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onSelected(metric) },
                    text = { Text(text = metric.label, maxLines = 1) }
                )
            }
        }
    }
}

// ---------- Data helpers ----------
private fun metricSeries(ex: ExerciseProgress, metric: Metric): List<Float> = when (metric) {
    Metric.Weight -> ex.points.map { it.maxWeight.toFloat() }
    Metric.Reps -> ex.points.map { it.maxReps.toFloat() }
    Metric.Volume -> ex.points.map { it.volume.toFloat() }
}

private fun metricLast(ex: ExerciseProgress, metric: Metric): Float =
    metricSeries(ex, metric).lastOrNull() ?: 0f

private fun metricBest(ex: ExerciseProgress, metric: Metric): Float =
    metricSeries(ex, metric).maxOrNull() ?: 0f

private fun trendArrow(series: List<Float>): String {
    if (series.size < 2) return "→"
    val a = series[series.size - 2]
    val b = series.last()
    return when {
        b > a -> "↑"
        b < a -> "↓"
        else -> "→"
    }
}

private fun formatMetric(metric: Metric, v: Float): String {
    if (v <= 0f) return "—"
    return when (metric) {
        Metric.Weight -> "${((v * 10f).roundToInt() / 10f)} kg"
        Metric.Reps -> "${v.roundToInt()} reps"
        Metric.Volume -> "${v.roundToInt()} vol"
    }
}

// ---------- Chart con grid + labels (la versión que ya arreglamos) ----------
@Composable
private fun LineChart(
    series: List<Float>,
    modifier: Modifier,
    lineColor: Color,
    axisColor: Color,
    showDots: Boolean = true
) {
    Canvas(modifier = modifier) {
        if (series.isEmpty()) return@Canvas

        val padLeft = 80f
        val padRight = 20f
        val padTop = 20f
        val padBottom = 55f
        val xEdgeInset = 18f

        val w = size.width
        val h = size.height

        val chartW = (w - padLeft - padRight).coerceAtLeast(1f)
        val chartH = (h - padTop - padBottom).coerceAtLeast(1f)

        val rawMin = series.minOrNull() ?: 0f
        val rawMax = series.maxOrNull() ?: 0f

        val min = if (rawMin == rawMax) rawMin - 1f else rawMin
        val max = if (rawMin == rawMax) rawMax + 1f else rawMax
        val range = (max - min).takeIf { it > 0.0001f } ?: 1f

        fun xOf(i: Int): Float {
            if (series.size == 1) return padLeft + chartW / 2f
            val usableW = (chartW - 2f * xEdgeInset).coerceAtLeast(1f)
            val dx = usableW / (series.size - 1)
            return padLeft + xEdgeInset + dx * i
        }

        fun yOf(v: Float): Float {
            val t = (v - min) / range
            return padTop + (1f - t) * chartH
        }

        val x0 = padLeft
        val y0 = padTop + chartH
        val xMax = padLeft + chartW
        val yTop = padTop

        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            color = axisColor.copy(alpha = 0.82f).toArgb()
        }

        val gridLines = 4
        val gridColor = axisColor.copy(alpha = 0.14f)

        for (i in 0..gridLines) {
            val y = yTop + (chartH / gridLines) * i

            drawLine(
                color = gridColor,
                start = Offset(x0, y),
                end = Offset(xMax, y),
                strokeWidth = 1.5f
            )

            val value = max - (range / gridLines) * i
            val label = formatAxisValue(value)
            val textW = labelPaint.measureText(label)

            drawContext.canvas.nativeCanvas.drawText(
                label,
                x0 - 12f - textW,
                y + (labelPaint.textSize * 0.35f),
                labelPaint
            )
        }

        drawLine(
            color = axisColor.copy(alpha = 0.45f),
            start = Offset(x0, yTop),
            end = Offset(x0, y0),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor.copy(alpha = 0.45f),
            start = Offset(x0, y0),
            end = Offset(xMax, y0),
            strokeWidth = 2f
        )

        val xLabelY = y0 + 22f
        val n = series.size
        for (idx in 0 until n) {
            val x = xOf(idx)
            val lbl = (idx + 1).toString()
            val textW = labelPaint.measureText(lbl)
            drawContext.canvas.nativeCanvas.drawText(
                lbl,
                x - textW / 2f,
                xLabelY,
                labelPaint
            )
        }

        if (series.size == 1) {
            val x = xOf(0)
            val y = yOf(series[0])
            if (showDots) {
                drawCircle(color = lineColor, radius = 6.5f, center = Offset(x, y))
                drawCircle(color = Color(0xFF0B0B0C), radius = 3f, center = Offset(x, y))
            }
            return@Canvas
        }

        val path = Path()
        series.forEachIndexed { idx, v ->
            val x = xOf(idx)
            val y = yOf(v)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 4.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        if (showDots) {
            series.forEachIndexed { idx, v ->
                val x = xOf(idx)
                val y = yOf(v)
                drawCircle(color = lineColor, radius = 5.5f, center = Offset(x, y))
                drawCircle(color = Color(0xFF0B0B0C), radius = 2.6f, center = Offset(x, y))
            }
        }
    }
}

private fun formatAxisValue(v: Float): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1000f -> "${(v / 100f).roundToInt() * 100}"
        abs >= 100f -> "${(v / 10f).roundToInt() * 10}"
        abs >= 10f -> "${(v * 10f).roundToInt() / 10f}"
        else -> "${(v * 100f).roundToInt() / 100f}"
    }
}