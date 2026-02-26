package com.example.gymrank.ui.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.User
import com.example.gymrank.ui.components.*
import com.example.gymrank.ui.screens.signup.SignUpBottomSheet
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (User) -> Unit,
    onSignUpSuccess: (User) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    openSignUpOnStart: Boolean = false
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

    // ✅ Context/Activity
    val context = LocalContext.current
    val activity = context as? Activity

    // ✅ Google Sign-In client (igual que en signup)
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(com.example.gymrank.R.string.default_web_client_id))
            .build()
    }

    val googleClient = remember(activity) {
        if (activity != null) GoogleSignIn.getClient(activity, gso) else null
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        runCatching {
            val account = task.getResult(Exception::class.java)
            val idToken = account.idToken ?: error("Google idToken null (revisá default_web_client_id)")

            // ✅ Login con Google (no signup)
            viewModel.signInWithGoogle(idToken)

        }.onFailure { e ->
            scope.launch {
                snackbarHostState.showSnackbar(e.message ?: "No se pudo iniciar sesión con Google")
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val user = (uiState as LoginUiState.Success).user
            onLoginSuccess(user)
            viewModel.resetUiState()
        }
    }

    LaunchedEffect(openSignUpOnStart) {
        if (openSignUpOnStart) showSignUpSheet = true
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

                TrophyIcon(size = 80)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                Text(
                    text = "Fit Rank",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymRankColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

                Text(
                    text = "¿Listo para escalar el ranking argentino?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    color = GymRankColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xxl))

                AppTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = "Correo o usuario",
                    trailingIcon = Icons.Filled.Email,
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

                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendPasswordResetEmail { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontSize = 14.sp,
                        color = GymRankColors.PrimaryAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                PrimaryButton(
                    text = "ENTRAR →",
                    onClick = { viewModel.onLoginClick() },
                    enabled = uiState !is LoginUiState.Loading,
                    isLoading = uiState is LoginUiState.Loading
                )

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

                DividerWithText(text = "O continuá con")

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

                // ✅ Solo Google, centrado (igual que signup)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SocialButton(
                        text = "Google",
                        onClick = {
                            if (uiState is LoginUiState.Loading) return@SocialButton
                            if (activity == null || googleClient == null) {
                                scope.launch { snackbarHostState.showSnackbar("No se pudo abrir Google Sign-In (Activity null)") }
                                return@SocialButton
                            }

                            // ✅ Fuerza a que aparezca la UI (selector)
                            googleClient.revokeAccess().addOnCompleteListener {
                                googleLauncher.launch(googleClient.signInIntent)
                            }
                        },
                        modifier = Modifier.widthIn(min = 220.dp),
                        enabled = uiState !is LoginUiState.Loading
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xl))

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

        if (showSignUpSheet) {
            SignUpBottomSheet(
                onDismiss = { showSignUpSheet = false },
                onSignUpSuccess = { user: User -> onSignUpSuccess(user) },
                onShowSnackbar = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }
    }
}