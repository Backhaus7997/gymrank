package com.example.gymrank.ui.screens.signup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.ui.components.*
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpBottomSheet(
    onDismiss: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Block dismissal when loading
    LaunchedEffect(uiState.isLoading) {
        sheetState.isVisible
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!uiState.isLoading) {
                onDismiss()
            }
        },
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
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),
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
            // Title
            Text(
                text = "Creá tu cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GymRankColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

            // Subtitle
            Text(
                text = "Sumate al ranking y competí con los mejores de Argentina.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = GymRankColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

            // Email Field
            AppTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = "Correo electrónico",
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            // Password Field
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

            // Confirm Password Field
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

            // General Error
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

            // Sign Up Button
            PrimaryButton(
                text = "CREAR CUENTA",
                onClick = {
                    viewModel.signUp {
                        onDismiss()
                        onSignUpSuccess()
                    }
                },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))

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
                    onClick = { onShowSnackbar("Próximamente") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                )

                SocialButton(
                    text = "Apple",
                    onClick = { onShowSnackbar("Próximamente") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

            // Cancel Button
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
