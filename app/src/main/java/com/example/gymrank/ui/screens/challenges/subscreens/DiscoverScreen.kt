package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.domain.model.ChallengeStatus
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

private data class ChallengeCard(
    val templateId: String,
    val title: String,
    val subtitle: String,
    val level: String,
    val durationValue: Int,
    val durationUnitShort: String,
    val imageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onBack: () -> Unit
) {
    val bg = runCatching { DesignTokens.Colors.BackgroundBase }.getOrElse { Color(0xFF000000) }
    val surface = runCatching { DesignTokens.Colors.SurfaceElevated }.getOrElse { Color(0xFF101010) }
    val input = runCatching { DesignTokens.Colors.SurfaceInputs }.getOrElse { Color(0xFF151515) }
    val textPrimary = runCatching { DesignTokens.Colors.TextPrimary }.getOrElse { Color.White }
    val textSecondary = runCatching { DesignTokens.Colors.TextSecondary }.getOrElse { Color(0xFF8E8E93) }
    val accent = runCatching { GymRankColors.PrimaryAccent }.getOrElse { Color(0xFF2EF2A0) }

    var tab by remember { mutableIntStateOf(0) } // 0=Descubrir, 1=Mi biblioteca
    var query by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    val repo = remember { ChallengeRepositoryFirestoreImpl() }
    val vm: DiscoverViewModel = viewModel(factory = DiscoverViewModelFactory(repo))
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    // ✅ templates -> cards
    val cardsAll = remember(state.templates) { state.templates.map { it.toUiCard() } }

    // ✅ IDs aceptados vienen desde Firestore (user_challenges)
    val acceptedIds: Set<String> = remember(state.userChallenges) {
        state.userChallenges
            .filter { it.status == ChallengeStatus.ACTIVE || it.status == ChallengeStatus.COMPLETED }
            .map { it.templateId }
            .filter { it.isNotBlank() }
            .toSet()
    }

    val baseList = remember(cardsAll, tab, acceptedIds) {
        if (tab == 0) {
            cardsAll.filter { it.templateId !in acceptedIds }
        } else {
            cardsAll.filter { it.templateId in acceptedIds }
        }
    }

    val filtered = remember(baseList, query) {
        if (query.isBlank()) baseList
        else baseList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Desafíos", color = textPrimary, fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SegBtn("Descubrir", tab == 0, accent) { tab = 0 }
                SegBtn("Mi biblioteca", tab == 1, accent) { tab = 1 }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary) },
                placeholder = { Text("Buscar", color = textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = input,
                    unfocusedContainerColor = input,
                    focusedBorderColor = accent.copy(alpha = 0.35f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    cursorColor = accent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Text("Total: ${filtered.size}", color = textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            when {
                state.loading -> {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 18.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = accent) }
                }

                state.error != null -> {
                    Surface(
                        color = surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("No se pudieron cargar los desafíos", color = textPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(state.error ?: "", color = textSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { vm.load() },
                                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.20f))
                            ) {
                                Text("Reintentar", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                else -> {
                    if (filtered.isEmpty()) {
                        val msg = if (tab == 0) "No hay desafíos para mostrar." else "Todavía no aceptaste desafíos."
                        Surface(
                            color = surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                msg,
                                color = textSecondary,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 18.dp)
                        ) {
                            items(filtered) { c ->
                                ChallengeListCard(
                                    card = c,
                                    surface = surface,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    accent = accent,
                                    onClick = { selectedTemplateId = c.templateId }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // ✅ MODAL DETALLE
        if (selectedTemplateId != null) {
            val t = state.templates.firstOrNull { it.id == selectedTemplateId }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            ChallengeDetailModal(
                template = t,
                accent = accent,
                surface = surface,
                input = input,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                alreadyAccepted = selectedTemplateId in acceptedIds,
                onClose = { selectedTemplateId = null },
                onAccept = {
                    val id = selectedTemplateId ?: return@ChallengeDetailModal

                    // ✅ Persistir (Firestore)
                    vm.acceptTemplate(id)

                    // ✅ Cambiar a biblioteca y cerrar modal
                    tab = 1
                    selectedTemplateId = null
                }
            )
        }
    }
}

private fun ChallengeTemplate.toUiCard(): ChallengeCard {
    val (value, unitShort) = if (durationDays >= 7 && durationDays % 7 == 0) {
        val weeks = (durationDays / 7).coerceAtLeast(1)
        weeks to "SEM"
    } else {
        durationDays.coerceAtLeast(1) to "DÍAS"
    }

    return ChallengeCard(
        templateId = id,
        title = title,
        subtitle = subtitle,
        level = level.ifBlank { "Principiante" },
        durationValue = value,
        durationUnitShort = unitShort,
        imageUrl = imageUrl
    )
}

@Composable
private fun RowScope.SegBtn(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent
    val border = if (selected) accent.copy(alpha = 0.55f) else Color.Transparent
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.weight(1f).clip(shape).clickable { onClick() },
        shape = shape,
        color = bg,
        border = if (selected) BorderStroke(1.dp, border) else null
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChallengeListCard(
    card: ChallengeCard,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(width = 132.dp, height = 86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val url = card.imageUrl
                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ImageIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = card.title,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = card.subtitle,
                    color = textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LevelPill(text = card.level, accent = accent)
                    DurationCircle(value = card.durationValue, unitShort = card.durationUnitShort, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun LevelPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = Modifier.heightIn(min = 32.dp).wrapContentWidth()
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun DurationCircle(value: Int, unitShort: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        modifier = Modifier.size(56.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = unitShort,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun ChallengeDetailModal(
    template: ChallengeTemplate?,
    accent: Color,
    surface: Color,
    input: Color,
    textPrimary: Color,
    textSecondary: Color,
    alreadyAccepted: Boolean,
    onClose: () -> Unit,
    onAccept: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        template?.title ?: "Desafío",
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (template?.subtitle?.isNotBlank() == true) {
                    Text(template.subtitle, color = textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                }

                val durationTxt = if (template != null) "${template.durationDays} días" else "—"
                val lvlTxt = template?.level?.ifBlank { "Principiante" } ?: "—"

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = input,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Duración", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(durationTxt, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Surface(
                        color = input,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Nivel", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(lvlTxt, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { if (!alreadyAccepted) onAccept() },
                    enabled = !alreadyAccepted,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        if (alreadyAccepted) "YA EN TU BIBLIOTECA" else "ACEPTAR DESAFÍO",
                        color = GymRankColors.PrimaryAccentText,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Al aceptar, se mueve a tu biblioteca.",
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}