package com.example.gymrank.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SelectGym : Screen("select_gym")
    object Login : Screen("login")
    object Home : Screen("home")
}
