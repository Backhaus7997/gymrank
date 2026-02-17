package com.example.gymrank.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.components.GradientBackground
import com.example.gymrank.ui.components.PrimaryButton
import com.example.gymrank.ui.components.SecondaryButton
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.example.gymrank.ui.screens.signup.SignUpBottomSheet
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onStartSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccessNavigate: () -> Unit
) {
    // Estado local para abrir/cerrar el sheet de registro en la misma pantalla
    var showSignUpSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GymRankColors.Background
    ) { paddingValues ->
        GradientBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top country chip (optional)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = DesignTokens.Spacing.xl),
                    color = GymRankColors.SurfaceAlt.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(DesignTokens.CornerRadius.pill)
                ) {
                    Text(
                        text = "🇦🇷 ARGENTINA",
                        modifier = Modifier.padding(
                            horizontal = DesignTokens.Spacing.md,
                            vertical = DesignTokens.Spacing.sm
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GymRankColors.TextSecondary
                    )
                }

                // Center content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = DesignTokens.Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(0.3f))

                    // Logo/Brand circle
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(DesignTokens.CornerRadius.round))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GymRankColors.PrimaryAccent.copy(alpha = 0.25f),
                                        GymRankColors.PrimaryAccent.copy(alpha = 0.1f),
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
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💪",
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 80.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                    // Title with accent on "RANK"
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = GymRankColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("FIT ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = GymRankColors.PrimaryAccent,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append("RANK")
                            }
                        },
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                    // Subtitle in Argentinian Spanish
                    Text(
                        text = "Dominá el ranking. Subí de nivel.\nLa comunidad fitness más competitiva de Argentina.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        color = GymRankColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.lg)
                    )

                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Bottom buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.lg)
                        .padding(bottom = DesignTokens.Spacing.xl)
                ) {
                    PrimaryButton(
                        text = "EMPEZAR",
                        onClick = { showSignUpSheet = true }
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                    SecondaryButton(
                        text = "¿Ya tenés cuenta?",
                        onClick = onNavigateToLogin
                    )

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                    // Footer text
                    Text(
                        text = "v1.0 · HECHO PARA LOS QUE COMPITEN",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = GymRankColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Render del BottomSheet de registro (mismo que la pantalla SignUp)
    if (showSignUpSheet) {
        SignUpBottomSheet(
            onDismiss = { showSignUpSheet = false },
            onSignUpSuccess = {
                showSignUpSheet = false
                onSignUpSuccessNavigate()
            },
            onShowSnackbar = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }
}
