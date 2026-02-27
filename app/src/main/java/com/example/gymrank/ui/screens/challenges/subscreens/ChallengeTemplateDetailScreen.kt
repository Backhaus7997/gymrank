package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.domain.model.UserChallenge
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Detalle", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                error != null -> {
                    Surface(
                        color = surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
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

                template == null -> {
                    Text("Desafío no encontrado", color = textPrimary)
                }

                else -> {
                    val t = template!!

                    Surface(
                        color = surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(t.title, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(t.subtitle, color = textSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Pill("Nivel: ${t.level.ifBlank { "Principiante" }}", accent)
                                Pill("Duración: ${t.durationDays} días", accent)
                            }

                            Spacer(Modifier.height(14.dp))

                            // “Qué hay que hacer” (por ahora lo armamos con tags; luego podemos agregar fields reales)
                            Text("Qué hay que hacer", color = textPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))

                            val tasks = if (t.tags.isNotEmpty()) t.tags else listOf("Seguí las instrucciones del desafío", "Completalo dentro del tiempo")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                tasks.forEach { s ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            "• $s",
                                            color = textPrimary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // CTA: aceptar o gestionar estado
                    if (uid.isBlank()) {
                        Text("Tenés que estar logueado para aceptar desafíos.", color = textSecondary, fontSize = 13.sp)
                    } else {
                        if (myActive == null) {
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
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(if (busy) "Aceptando..." else "Aceptar desafío", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text("Estado actual: ACTIVO", color = textSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val uc = myActive ?: return@Button
                                        scope.launch {
                                            busy = true
                                            runCatching { repo.updateUserChallengeStatus(uid, uc.id, ChallengeStatus.COMPLETED) }
                                                .onSuccess { myActive = null; busy = false; onBack() }
                                                .onFailure { e -> busy = false; error = e.message }
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.22f))
                                ) { Text("Completar", color = Color.White, fontWeight = FontWeight.SemiBold) }

                                OutlinedButton(
                                    onClick = {
                                        val uc = myActive ?: return@OutlinedButton
                                        scope.launch {
                                            busy = true
                                            runCatching { repo.updateUserChallengeStatus(uid, uc.id, ChallengeStatus.CANCELED) }
                                                .onSuccess { myActive = null; busy = false; onBack() }
                                                .onFailure { e -> busy = false; error = e.message }
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                                ) { Text("Cancelar", color = Color.White, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}