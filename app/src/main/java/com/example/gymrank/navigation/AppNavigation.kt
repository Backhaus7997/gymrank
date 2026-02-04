package com.example.gymrank.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymrank.ui.screens.home.HomeScreen
import com.example.gymrank.ui.screens.login.LoginScreen
import com.example.gymrank.ui.screens.selectgym.SelectGymScreen
import com.example.gymrank.ui.screens.welcome.WelcomeScreen
import com.example.gymrank.ui.session.SessionViewModel

@Composable
fun AppNavigation(
    sessionViewModel: SessionViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToSelectGym = {
                    navController.navigate(Screen.SelectGym.route)
                }
            )
        }

        composable(Screen.SelectGym.route) {
            SelectGymScreen(
                onGymSelected = { gym ->
                    sessionViewModel.selectGym(gym)
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                sessionViewModel = sessionViewModel
            )
        }
    }
}


