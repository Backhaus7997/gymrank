package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

private const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeTemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    onAcceptedGoToActive: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    // look
    val cardShape = RoundedCornerShape(22.dp)
    val softBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    val cardBg = surface.copy(alpha = 0.72f)
    val cardGradient = Brush.verticalGradient(listOf(cardBg, cardBg.copy(alpha = 0.58f)))

    val repo = remember { ChallengeRepositoryFirestoreImpl() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var template by remember { mutableStateOf<ChallengeTemplate?>(null) }
    var myActive by remember { mutableStateOf<UserChallenge?>(null) }
    var busy by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                val t = repo.getChallengeTemplateById(templateId)
                val active = if (uid.isNotBlank()) {
                    repo.getUserChallenges(uid, listOf(ChallengeStatus.ACTIVE))
                        .firstOrNull { it.templateId == templateId }
                } else null
                t to active
            }.onSuccess { (t, active) ->
                template = t
                myActive = active
                loading = false
            }.onFailure { e ->
                loading = false
                error = e.message ?: "Error cargando desafío"
            }
        }
    }

    LaunchedEffect(templateId) { reload() }

    // ticker progreso
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(myActive?.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Desafío", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            val scroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 170.dp)
            ) {
                when {
                    loading -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }

                    error != null -> {
                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = softBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("No se pudo cargar", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(error ?: "", color = textSecondary, fontSize = 13.sp)
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { reload() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.20f))
                                ) {
                                    Text("Reintentar", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    template == null -> Text("Desafío no encontrado", color = textPrimary)

                    else -> {
                        val t = template!!
                        val uc = myActive

                        val duration = t.durationDays.coerceAtLeast(1)
                        val start = uc?.startedAt
                        val elapsedDays = if (start == null) 0 else
                            floor(((now - start).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()).toInt()

                        val dayNumber = (elapsedDays + 1).coerceIn(1, duration)
                        val remaining = (duration - elapsedDays).coerceAtLeast(0)
                        val progress = (elapsedDays.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                        // HERO
                        Surface(
                            color = cardBg,
                            shape = cardShape,
                            border = softBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardGradient, cardShape)
                                    .padding(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.035f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = accent.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
                                        ) {
                                            Icon(
                                                Icons.Filled.Flag,
                                                contentDescription = null,
                                                tint = accent.copy(alpha = 0.75f),
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    t.title,
                                    color = textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    t.subtitle,
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatPill(
                                        label = "Nivel",
                                        value = t.level.ifBlank { "Facil" },
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatPill(
                                        label = "Duración",
                                        value = "${duration} Días",
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatPill(
                                        label = "Restan",
                                        value = "${if (uc == null) duration else remaining} Días",
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // PROGRESO
                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(18.dp),
                            border = softBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardGradient, RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Text("Progreso", color = textPrimary, fontWeight = FontWeight.SemiBold)

                                Spacer(Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { if (uc == null) 0f else progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = accent.copy(alpha = 0.65f),
                                    trackColor = Color.White.copy(alpha = 0.10f)
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = if (uc == null) "Todavía no lo aceptaste"
                                    else "Día $dayNumber de $duration • Restan $remaining",
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )

                                if (uc != null && elapsedDays < duration) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Se habilita “Completado” cuando pasen todos los días.",
                                        color = textSecondary.copy(alpha = 0.92f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FOOTER
            Surface(
                color = bg.copy(alpha = 0.94f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                val t = template
                val uc = myActive

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when {
                        uid.isBlank() -> {
                            Text("Tenés que estar logueado para aceptar desafíos.", color = textSecondary, fontSize = 13.sp)
                        }

                        t != null && uc == null -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        runCatching { repo.acceptChallenge(uid, templateId) }
                                            .onSuccess {
                                                busy = false
                                                onAcceptedGoToActive()
                                            }
                                            .onFailure { e ->
                                                busy = false
                                                error = e.message ?: "Error aceptando desafío"
                                            }
                                    }
                                },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.22f)),
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(if (busy) "Aceptando..." else "Aceptar desafío", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        uc != null && t != null -> {
                            val duration = t.durationDays.coerceAtLeast(1)
                            val start = uc.startedAt
                            val elapsedDays = if (start == null) 0 else
                                floor(((System.currentTimeMillis() - start).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()).toInt()
                            val canComplete = elapsedDays >= duration

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            runCatching { repo.updateUserChallengeStatus(uid, uc.id, ChallengeStatus.CANCELED) }
                                                .onSuccess { busy = false; onBack() }
                                                .onFailure { e -> busy = false; error = e.message }
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.06f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Abandonar", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            runCatching { repo.updateUserChallengeStatus(uid, uc.id, ChallengeStatus.COMPLETED) }
                                                .onSuccess { busy = false; onBack() }
                                                .onFailure { e -> busy = false; error = e.message ?: "Error completando" }
                                        }
                                    },
                                    enabled = canComplete && !busy,
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent.copy(alpha = 0.28f),
                                        disabledContainerColor = Color.White.copy(alpha = 0.06f)
                                    )
                                ) {
                                    Text(
                                        "Completado",
                                        color = if (canComplete) Color.White else Color.White.copy(alpha = 0.45f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ FIX DEL “CORTADO”:
 * - NO height fijo, usamos heightIn(min=...)
 * - Ajuste de padding vertical y spacer
 */
@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.10f)),
        modifier = modifier.heightIn(min = 64.dp) // <- clave
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp), // <- clave
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}