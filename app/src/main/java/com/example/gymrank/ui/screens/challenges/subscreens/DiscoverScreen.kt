package com.example.gymrank.ui.screens.challenges.subscreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gymrank.data.repository.ChallengeRepositoryFirestoreImpl
import com.example.gymrank.domain.model.ChallengeTemplate
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

private data class ChallengeCard(
    val title: String,
    val subtitle: String,
    val level: String,
    val durationValue: Int,
    val durationUnitShort: String, // "SEM" o "DÍAS"
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

    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    // Repo + VM
    val repo = remember { ChallengeRepositoryFirestoreImpl() }
    val vm: DiscoverViewModel = viewModel(factory = DiscoverViewModelFactory(repo))
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    val cardsAll = remember(state.templates) {
        state.templates.map { it.toUiCard() }
    }

    val filtered = remember(cardsAll, query, tab) {
        val base = if (tab == 0) cardsAll else emptyList()
        if (query.isBlank()) base
        else base.filter {
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
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
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
                                accent = accent
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
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
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .clickable { onClick() },
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
    accent: Color
) {
    Surface(
        color = surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Imagen izquierda
            Box(
                Modifier
                    .size(width = 132.dp, height = 86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.03f)
                            )
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

            // Derecha
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
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
                    DurationCircle(
                        value = card.durationValue,
                        unitShort = card.durationUnitShort,
                        accent = accent
                    )
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
        modifier = Modifier
            .heightIn(min = 32.dp)
            .wrapContentWidth()
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
                text = unitShort, // "SEM" / "DÍAS"
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