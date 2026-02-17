package com.example.gymrank.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymrank.ui.components.GymRankBottomBar
import com.example.gymrank.ui.screens.challenges.ChallengesScreen
import com.example.gymrank.ui.screens.challenges.subscreens.DiscoverScreen
import com.example.gymrank.ui.screens.challenges.subscreens.EquipmentScreen
import com.example.gymrank.ui.screens.challenges.subscreens.GambleScreen
import com.example.gymrank.ui.screens.challenges.subscreens.QuestsScreen
import com.example.gymrank.ui.screens.feed.FeedScreen
import com.example.gymrank.ui.screens.home.HomeScreen
import com.example.gymrank.ui.screens.login.LoginScreen
import com.example.gymrank.ui.screens.onboarding.OnboardingScreen
import com.example.gymrank.ui.screens.ranking.RankingRoutes
import com.example.gymrank.ui.screens.ranking.RankingScreen
import com.example.gymrank.ui.screens.ranking.subscreens.RankingDetailsScreen
import com.example.gymrank.ui.screens.selectgym.SelectGymScreen
import com.example.gymrank.ui.screens.welcome.WelcomeScreen
import com.example.gymrank.ui.screens.workout.WorkoutScreen
import com.example.gymrank.ui.screens.workout.subscreens.CoachAiScreen
import com.example.gymrank.ui.screens.workout.subscreens.CreateRoutineScreen
import com.example.gymrank.ui.screens.workout.subscreens.ExploreScreen
import com.example.gymrank.ui.screens.workout.subscreens.WorkoutHistoryScreen
import com.example.gymrank.ui.screens.workout.subscreens.WorkoutProgressScreen
import com.example.gymrank.ui.session.SessionViewModel
import kotlinx.coroutines.launch

private const val NAV_FLOW_KEY = "nav_flow"
private const val FLOW_LOGIN = "login"
private const val FLOW_SIGNUP = "signup"

@Composable
fun AppNavigation(sessionViewModel: SessionViewModel) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // ✅ Bottom bar SOLO en tabs principales
    // Ojo: details usa route "ranking_details/..." => NO debe mostrar bottom bar.
    val showBottomBar = when {
        currentRoute == Screen.Home.route -> true
        currentRoute == "feed" -> true
        currentRoute == "challenges" -> true
        currentRoute == "workout" -> true
        currentRoute == "rank" -> true
        currentRoute == Screen.Ranking.route -> true
        else -> false
    }

    fun setFlow(flow: String) {
        runCatching {
            val entry = navController.getBackStackEntry(Screen.Welcome.route)
            entry.savedStateHandle[NAV_FLOW_KEY] = flow
            Log.d("NavFlow", "setFlow=$flow (route=$currentRoute)")
        }.onFailure {
            navController.currentBackStackEntry?.savedStateHandle?.set(NAV_FLOW_KEY, flow)
            Log.d("NavFlow", "setFlow (fallback current)=$flow (route=$currentRoute)")
        }
    }

    fun getFlow(): String? {
        val fromWelcome = runCatching {
            navController.getBackStackEntry(Screen.Welcome.route)
                .savedStateHandle
                .get<String>(NAV_FLOW_KEY)
        }.getOrNull()
        if (fromWelcome != null) return fromWelcome
        val fromCurrent = navController.currentBackStackEntry?.savedStateHandle?.get<String>(NAV_FLOW_KEY)
        val fromPrev = navController.previousBackStackEntry?.savedStateHandle?.get<String>(NAV_FLOW_KEY)
        return fromCurrent ?: fromPrev
    }

    Scaffold(
        bottomBar = { if (showBottomBar) GymRankBottomBar(navController) }
    ) { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ✅ WELCOME
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onStartSignUp = {
                        Log.d("NavFlow", "Welcome → Login (signup)")
                        setFlow(FLOW_SIGNUP)
                        navController.navigate("login_signup") { launchSingleTop = true }
                    },
                    onNavigateToLogin = {
                        Log.d("NavFlow", "Welcome → Login (login)")
                        setFlow(FLOW_LOGIN)
                        navController.navigate(Screen.Login.route) { launchSingleTop = true }
                    },
                    onSignUpSuccessNavigate = {
                        Log.d("NavFlow", "Welcome → SelectGym (signup)")
                        setFlow(FLOW_SIGNUP)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    }
                )
            }

            // ✅ LOGIN normal
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { user ->
                        Log.d("NavFlow", "Login success → SelectGym (login)")
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_LOGIN)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    },
                    onSignUpSuccess = { user ->
                        Log.d("NavFlow", "SignUp success (from login) → SelectGym (signup)")
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    }
                )
            }

            // ✅ LOGIN con signup abierto
            composable("login_signup") {
                LoginScreen(
                    openSignUpOnStart = true,
                    onLoginSuccess = { user ->
                        Log.d("NavFlow", "Login success (from login_signup) → SelectGym (login)")
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_LOGIN)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    },
                    onSignUpSuccess = { user ->
                        Log.d("NavFlow", "SignUp success → SelectGym (signup)")
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    }
                )
            }

            // ✅ SELECT GYM
            composable(Screen.SelectGym.route) {
                SelectGymScreen(
                    onGymSelected = { gym ->
                        Log.d("NavFlow", "SelectGym → gym selected")
                        runCatching { sessionViewModel.selectGym(gym) }

                        val flow = getFlow()
                        Log.d("NavFlow", "SelectGym → flow=$flow")

                        coroutineScope.launch {
                            when (flow) {
                                FLOW_SIGNUP -> {
                                    Log.d("NavFlow", "SelectGym → Onboarding (signup flow)")
                                    navController.navigate(Screen.Onboarding.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                FLOW_LOGIN -> {
                                    Log.d("NavFlow", "SelectGym → Home (login flow)")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                else -> {
                                    Log.d("NavFlow", "SelectGym → Home (fallback)")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // ✅ ONBOARDING
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        Log.d("NavFlow", "Onboarding finished → Home")
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ✅ MAIN TABS
            composable(Screen.Home.route) { HomeScreen(sessionViewModel = sessionViewModel) }
            composable("feed") { FeedScreen() }

            composable("challenges") {
                ChallengesScreen(
                    onOpenDiscover = { navController.navigate("challenges/discover") },
                    onOpenQuests = { navController.navigate("challenges/quests") },
                    onOpenGamble = { navController.navigate("challenges/gamble") },
                    onOpenEquipment = { navController.navigate("challenges/equipment") }
                )
            }

            // ✅ WORKOUT (TAB)
            composable("workout") {
                WorkoutScreen(
                    onExploreClick = { navController.navigate("workout/explore") },
                    onCoachClick = { navController.navigate("workout/coach_ai") },
                    onHistoryClick = { navController.navigate("workout/history") },
                    onProgressClick = { navController.navigate("workout/progress") },
                    onCreateRoutineClick = { navController.navigate("workout/create_routine") }
                )
            }

            // ✅ RANK (TAB)
            composable("rank") {
                RankingScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Ranking.route) {
                RankingScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ DETAILS REAL (match exact route used by RankingRoutes.detailsRoute(...))
            // ✅ DETAILS REAL (match exact route used by RankingRoutes.detailsRoute(...))
            composable(
                route = RankingRoutes.Details,
                arguments = RankingRoutes.detailsArgs
            ) { entry ->
                val gymName = entry.arguments?.getString("gymName").orEmpty()
                val gymLocation = entry.arguments?.getString("gymLocation").orEmpty()
                val periodLabel = entry.arguments?.getString("periodLabel").orEmpty()
                val myPosition = entry.arguments?.getInt("myPosition") ?: 0
                val myPoints = entry.arguments?.getInt("myPoints") ?: 0

                RankingDetailsScreen(
                    gymName = gymName,
                    gymLocation = gymLocation,
                    periodLabel = periodLabel,
                    myPosition = myPosition,
                    myPoints = myPoints,
                    onBack = { navController.popBackStack() }
                )
            }



            // ✅ (si lo usás)
            composable(Screen.LoadWorkout.route) { PlaceholderScreen("Cargar entrenamiento") }

            // ✅ SUBSCREENS CHALLENGES (sin bottom bar)
            composable("challenges/discover") { DiscoverScreen(onBack = { navController.popBackStack() }) }
            composable("challenges/quests") { QuestsScreen(onBack = { navController.popBackStack() }) }
            composable("challenges/gamble") {
                GambleScreen(
                    onBack = { navController.popBackStack() },
                    onGoToChallenges = {
                        navController.navigate("challenges") {
                            popUpTo("challenges") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("challenges/equipment") { EquipmentScreen(onBack = { navController.popBackStack() }) }

            // ✅ SUBSCREENS WORKOUT (sin bottom bar)
            composable("workout/explore") { ExploreScreen(onBack = { navController.popBackStack() }) }
            composable("workout/coach_ai") { CoachAiScreen(onBack = { navController.popBackStack() }) }
            composable("workout/history") { WorkoutHistoryScreen(onBack = { navController.popBackStack() }) }
            composable("workout/progress") { WorkoutProgressScreen(onBack = { navController.popBackStack() }) }

            // ✅ CREATE ROUTINE
            composable("workout/create_routine") {
                CreateRoutineScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { _ ->
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
