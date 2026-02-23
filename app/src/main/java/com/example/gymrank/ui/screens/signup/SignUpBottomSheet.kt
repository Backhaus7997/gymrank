package com.example.gymrank.ui.screens.signup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.User
import com.example.gymrank.ui.components.*
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpBottomSheet(
    onDismiss: () -> Unit,
    onSignUpSuccess: (User) -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val activity = context as? Activity

    // Google Sign-In client
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // ✅ importante: default_web_client_id
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
            viewModel.signInWithGoogle(idToken) { user ->
                onDismiss()
                onSignUpSuccess(user)
            }
        }.onFailure { e ->
            onShowSnackbar(e.message ?: "No se pudo iniciar sesión con Google")
        }
    }

    // Block dismissal when loading
    LaunchedEffect(uiState.isLoading) { sheetState.isVisible }

    ModalBottomSheet(
        onDismissRequest = { if (!uiState.isLoading) onDismiss() },
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = GymRankColors.Outline,
                width = 48.dp,
                height = 4.dp
            )
        },
        containerColor = GymRankColors.Surface,
        contentColor = GymRankColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.lg)
                .padding(bottom = DesignTokens.Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Creá tu cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GymRankColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

            Text(
                text = "Sumate al ranking y competí con los mejores de Argentina.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = GymRankColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            AppTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = "Correo electrónico",
                trailingIcon = Icons.Filled.Email,
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            AppTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = "Contraseña",
                enabled = !uiState.isLoading,
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            AppTextField(
                value = uiState.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChange(it) },
                label = "Confirmá tu contraseña",
                enabled = !uiState.isLoading,
                isError = uiState.confirmPasswordError != null,
                errorMessage = uiState.confirmPasswordError,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                Text(
                    text = uiState.error ?: "",
                    color = GymRankColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            PrimaryButton(
                text = "CREAR CUENTA",
                onClick = {
                    viewModel.signUp { user ->
                        onDismiss()
                        onSignUpSuccess(user)
                    }
                },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            DividerWithText(text = "O continuá con")

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            // ✅ Solo Google, centrado
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SocialButton(
                    text = "Google",
                    onClick = {
                        if (uiState.isLoading) return@SocialButton
                        if (activity == null || googleClient == null) {
                            onShowSnackbar("No se pudo abrir Google Sign-In (Activity null)")
                            return@SocialButton
                        }
                        googleLauncher.launch(googleClient.signInIntent)
                    },
                    modifier = Modifier.widthIn(min = 220.dp),
                    enabled = !uiState.isLoading
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = "Cancelar",
                    fontSize = 15.sp,
                    color = GymRankColors.TextSecondary
                )
            }
        }
    }
}
