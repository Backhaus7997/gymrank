package com.example.gymrank.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.ui.components.*
import com.example.gymrank.ui.screens.signup.SignUpBottomSheet
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    val focusManager = LocalFocusManager.current
    var showSignUpSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
            viewModel.resetUiState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GymRankColors.Background,
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GymRankColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GymRankColors.Background
                    )
                )
            }
        }
    ) { paddingValues ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = DesignTokens.Spacing.lg)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))

                // Trophy Icon
                TrophyIcon(size = 80)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                // Title
                Text(
                    text = "Gym Rank",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymRankColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                // Subtitle
                Text(
                    text = "¿Listo para escalar el ranking argentino?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    color = GymRankColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))

                // Email Field
                AppTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = "Correo o usuario",
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = uiState !is LoginUiState.Loading
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

                // Password Field
                AppTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = "Contraseña",
                    isPassword = true,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onLoginClick()
                        }
                    ),
                    enabled = uiState !is LoginUiState.Loading
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                // Forgot Password
                TextButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Próximamente")
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Te olvidaste la contraseña?",
                        fontSize = 14.sp,
                        color = GymRankColors.PrimaryAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                // Login Button
                PrimaryButton(
                    text = "ENTRAR →",
                    onClick = { viewModel.onLoginClick() },
                    enabled = uiState !is LoginUiState.Loading,
                    isLoading = uiState is LoginUiState.Loading
                )

                // Error message
                if (uiState is LoginUiState.Error) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = GymRankColors.Error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                // Or Divider
                DividerWithText(text = "O continuá con")

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                // Social Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
                ) {
                    SocialButton(
                        text = "Google",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Próximamente")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is LoginUiState.Loading
                    )

                    SocialButton(
                        text = "Apple",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Próximamente")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is LoginUiState.Loading
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

                // Sign Up Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿No tenés cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp,
                        color = GymRankColors.TextSecondary
                    )
                    TextButton(onClick = { showSignUpSheet = true }) {
                        Text(
                            text = "Registrate",
                            fontSize = 15.sp,
                            color = GymRankColors.PrimaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))
            }
        }

        // SignUp Bottom Sheet
        if (showSignUpSheet) {
            SignUpBottomSheet(
                onDismiss = { showSignUpSheet = false },
                onSignUpSuccess = onSignUpSuccess,
                onShowSnackbar = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            )
        }
    }
}
