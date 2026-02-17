package com.example.gymrank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

/**
 * Reusable Components for GymRank UI
 */

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GymRankColors.Background,
                        GymRankColors.Surface,
                        GymRankColors.Background
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Subtle overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GymRankColors.OverlayTop,
                            GymRankColors.OverlayBottom
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        )
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = GymRankColors.PrimaryAccent,
            contentColor = GymRankColors.PrimaryAccentText,
            disabledContainerColor = GymRankColors.SurfaceAlt,
            disabledContentColor = GymRankColors.TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = GymRankColors.PrimaryAccentText,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.pill),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = GymRankColors.TextPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = GymRankColors.TextSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled) GymRankColors.Outline else GymRankColors.Outline.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null, // ✅ NUEVO
    singleLine: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    color = if (isError) GymRankColors.Error else GymRankColors.TextSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            singleLine = singleLine,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,

            // ⬅️ si querés seguir soportando icono a la izquierda, queda igual
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = GymRankColors.TextSecondary
                    )
                }
            },

            // ✅ Icono a la derecha:
            // - si es password: toggle lock (como ya tenías)
            // - si no es password y viene trailingIcon: lo muestra (ej: Email)
            trailingIcon = when {
                isPassword -> {
                    {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = GymRankColors.TextSecondary
                            )
                        }
                    }
                }
                trailingIcon != null -> {
                    {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = GymRankColors.TextSecondary
                        )
                    }
                }
                else -> null
            },

            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = GymRankColors.TextPrimary,
                unfocusedTextColor = GymRankColors.TextPrimary,
                disabledTextColor = GymRankColors.TextSecondary,
                errorTextColor = GymRankColors.TextPrimary,
                focusedContainerColor = GymRankColors.SurfaceAlt,
                unfocusedContainerColor = GymRankColors.SurfaceAlt,
                disabledContainerColor = GymRankColors.SurfaceAlt.copy(alpha = 0.5f),
                errorContainerColor = GymRankColors.SurfaceAlt,
                focusedBorderColor = GymRankColors.Outline,
                unfocusedBorderColor = GymRankColors.Outline,
                disabledBorderColor = GymRankColors.Outline.copy(alpha = 0.3f),
                errorBorderColor = GymRankColors.Error,
                focusedLabelColor = GymRankColors.TextSecondary,
                unfocusedLabelColor = GymRankColors.TextSecondary,
            ),
            shape = RoundedCornerShape(14.dp)
        )

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = GymRankColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = DesignTokens.Spacing.md)
            )
        }
    }
}


@Composable
fun SocialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.medium),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GymRankColors.SurfaceAlt,
            contentColor = GymRankColors.TextPrimary,
            disabledContainerColor = GymRankColors.SurfaceAlt.copy(alpha = 0.4f),
            disabledContentColor = GymRankColors.TextSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = DesignTokens.BorderWidth.thin,
            color = GymRankColors.Outline
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DividerWithText(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = GymRankColors.Outline
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md),
            style = MaterialTheme.typography.bodyMedium,
            color = GymRankColors.TextSecondary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = GymRankColors.Outline
        )
    }
}

@Composable
fun TrophyIcon(
    modifier: Modifier = Modifier,
    size: Int = 80
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(DesignTokens.CornerRadius.round))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GymRankColors.PrimaryAccent.copy(alpha = 0.15f),
                        GymRankColors.Surface,
                        GymRankColors.Background
                    )
                )
            )
            .border(
                width = 2.dp,
                color = GymRankColors.PrimaryAccent.copy(alpha = 0.4f),
                shape = RoundedCornerShape(DesignTokens.CornerRadius.round)
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OnboardingScaffold(
    currentStep: Int,
    totalSteps: Int,
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    hero: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DesignTokens.Colors.BackgroundBase,
                        DesignTokens.Colors.BackgroundBase.copy(alpha = 0.98f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.Spacing.lg)
                .padding(top = DesignTokens.Spacing.lg, bottom = DesignTokens.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = DesignTokens.Colors.TextPrimary)
                    }
                }
                Spacer(Modifier.width(DesignTokens.Spacing.md))
                StepperDots(currentStep = currentStep, totalSteps = totalSteps)
            }
            if (hero != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { hero() }
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DesignTokens.Colors.TextSecondary)
            }

            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg)
        ) { bottomBar() }
    }
}

@Composable
fun StepperDots(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { index ->
            val filled = index <= currentStep
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (filled) GymRankColors.PrimaryAccent else Color.Transparent)
                    .border(1.dp, DesignTokens.Colors.DividerSubtle, CircleShape)
            )
        }
    }
}

@Composable
fun PrimaryCtaButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.pillLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = GymRankColors.PrimaryAccent,
            contentColor = GymRankColors.PrimaryAccentText,
            disabledContainerColor = DesignTokens.Colors.DividerSubtle,
            disabledContentColor = DesignTokens.Colors.TextSecondary
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = DesignTokens.Colors.TextSecondary)
    }
}

@Composable
fun SelectableOptionCard(
    title: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) DesignTokens.Colors.SurfaceInputs else DesignTokens.Colors.SurfaceElevated
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerRadius.card))
            .clickable(onClick = onClick),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = DesignTokens.Colors.TextPrimary)
            }
            Text(title, color = DesignTokens.Colors.TextPrimary)
        }
    }
}

@Composable
fun IconListItemRow(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DesignTokens.Colors.SurfaceInputs),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GymRankColors.PrimaryAccent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DesignTokens.Colors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, color = DesignTokens.Colors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
