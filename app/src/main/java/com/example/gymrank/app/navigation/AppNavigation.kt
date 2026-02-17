package com.example.gymrank.app.navigation

import androidx.compose.runtime.Composable
import com.example.gymrank.ui.session.SessionViewModel

/**
 * ✅ Wrapper: este package es el que importa MainActivity.
 * Delegamos a la navegación real para evitar duplicados y confusiones.
 */
@Composable
fun AppNavigation(sessionViewModel: SessionViewModel) {
    com.example.gymrank.navigation.AppNavigation(sessionViewModel = sessionViewModel)
}
