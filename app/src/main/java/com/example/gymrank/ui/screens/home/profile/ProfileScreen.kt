@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.gymrank.ui.screens.home.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onClose: () -> Unit,
    onOpenFriendRequests: () -> Unit,
    onLogout: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                vm.setSavingState(true)
                val base64 = runCatching {
                    compressImageToBase64Jpeg(context, uri, targetMaxBytes = 220_000)
                }.getOrNull()

                if (base64 == null) {
                    vm.setSavingState(false)
                    vm.setError("No se pudo procesar la imagen. Probá con otra.")
                } else {
                    vm.onPhotoBase64Picked(base64)
                    vm.setSavingState(false)
                }
            }
        }
    }

    val bg = Brush.verticalGradient(listOf(Color(0xFF0B0B0B), Color(0xFF000000)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = DesignTokens.Colors.SurfaceElevated,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Perfil",
                        color = DesignTokens.Colors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = DesignTokens.Colors.TextPrimary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Avatar + username + experience + cambiar foto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color(0xFF232323),
                        border = BorderStroke(1.dp, DesignTokens.Colors.SurfaceInputs)
                    ) {
                        val dataUrl = ui.photoBase64?.let { "data:image/jpeg;base64,$it" }
                        if (!dataUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = dataUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Box(Modifier.fillMaxSize())
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = ui.username.ifBlank { "usuario" },
                            color = DesignTokens.Colors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ui.experienceLabel,
                            color = DesignTokens.Colors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, GymRankColors.PrimaryAccent.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GymRankColors.PrimaryAccent.copy(alpha = 0.10f),
                            contentColor = GymRankColors.PrimaryAccent
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("Cambiar foto", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Username (tu "Nombre")
                Text("Nombre", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                DarkField(
                    value = ui.username,
                    onValueChange = vm::onUsernameChanged,
                    placeholder = "Tu nombre"
                )

                Spacer(Modifier.height(12.dp))

                // Experiencia
                Text("Experiencia", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                DarkDropdown(
                    value = ui.experienceLabel,
                    options = ExperienceOptions.labels,
                    onPick = { vm.onExperienceChanged(ExperienceOptions.toValue(it)) }
                )

                Spacer(Modifier.height(12.dp))

                // Género
                Text("Género", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                DarkDropdown(
                    value = ui.genderLabel,
                    options = GenderOptions.labels,
                    onPick = { vm.onGenderChanged(GenderOptions.toValue(it)) }
                )

                Spacer(Modifier.height(12.dp))

                // Visibilidad (feedVisibility en Firestore)
                Text("Visibilidad", color = DesignTokens.Colors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                DarkDropdown(
                    value = ui.visibilityLabel,
                    options = VisibilityOptions.labels,
                    onPick = { vm.onVisibilityChanged(VisibilityOptions.toValue(it)) }
                )

                Spacer(Modifier.height(16.dp))

                // Guardar
                Button(
                    onClick = vm::save,
                    enabled = !ui.isSaving,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymRankColors.PrimaryAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = GymRankColors.PrimaryAccent.copy(alpha = 0.6f),
                        disabledContentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    if (ui.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("GUARDANDO…", fontWeight = FontWeight.Black)
                    } else {
                        Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Black)
                    }
                }

                if (!ui.error.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(ui.error ?: "", color = GymRankColors.Error, fontSize = 12.sp)
                }
                if (ui.savedOk) {
                    Spacer(Modifier.height(10.dp))
                    Text("Cambios guardados ✅", color = GymRankColors.PrimaryAccent, fontSize = 12.sp)
                }

                Spacer(Modifier.height(14.dp))

                // Card solicitudes
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = DesignTokens.Colors.SurfaceElevated,
                    border = BorderStroke(1.dp, DesignTokens.Colors.SurfaceInputs)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Solicitudes de amistad", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ui.friendRequestsHint,
                            color = DesignTokens.Colors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenFriendRequests,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DesignTokens.Colors.SurfaceInputs),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DesignTokens.Colors.SurfaceElevated,
                                contentColor = DesignTokens.Colors.TextPrimary
                            )
                        ) { Text("Ver solicitudes") }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Card logout
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onLogout() },
                    shape = RoundedCornerShape(18.dp),
                    color = DesignTokens.Colors.SurfaceElevated,
                    border = BorderStroke(1.dp, DesignTokens.Colors.SurfaceInputs)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = DesignTokens.Colors.TextPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text("Cerrar sesión", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val shape = RoundedCornerShape(14.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .border(1.dp, DesignTokens.Colors.SurfaceInputs, shape),
        placeholder = { Text(placeholder, color = DesignTokens.Colors.TextSecondary.copy(alpha = 0.7f)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DesignTokens.Colors.SurfaceElevated,
            unfocusedContainerColor = DesignTokens.Colors.SurfaceElevated,
            disabledContainerColor = DesignTokens.Colors.SurfaceElevated,
            focusedTextColor = DesignTokens.Colors.TextPrimary,
            unfocusedTextColor = DesignTokens.Colors.TextPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DesignTokens.Colors.TextPrimary
        ),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkDropdown(
    value: String,
    options: List<String>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(shape)
                .border(1.dp, DesignTokens.Colors.SurfaceInputs, shape),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DesignTokens.Colors.SurfaceElevated,
                unfocusedContainerColor = DesignTokens.Colors.SurfaceElevated,
                focusedTextColor = DesignTokens.Colors.TextPrimary,
                unfocusedTextColor = DesignTokens.Colors.TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DesignTokens.Colors.SurfaceElevated)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = DesignTokens.Colors.TextPrimary) },
                    onClick = {
                        onPick(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Convierte una imagen elegida (Uri) a JPEG comprimido (Base64) para guardar en Firestore sin Storage.
 * targetMaxBytes: aprox. 200-250KB para estar holgado con el límite del doc.
 */
private suspend fun compressImageToBase64Jpeg(
    context: Context,
    uri: Uri,
    targetMaxBytes: Int
): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer imagen")

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("No se pudo decodificar imagen")

    // Comprimir bajando quality hasta llegar a target
    var quality = 88
    var out = compressJpeg(bitmap, quality)

    while (out.size > targetMaxBytes && quality > 35) {
        quality -= 8
        out = compressJpeg(bitmap, quality)
    }

    Base64.encodeToString(out, Base64.NO_WRAP)
}

private fun compressJpeg(bitmap: android.graphics.Bitmap, quality: Int): ByteArray {
    val bos = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, bos)
    return bos.toByteArray()
}