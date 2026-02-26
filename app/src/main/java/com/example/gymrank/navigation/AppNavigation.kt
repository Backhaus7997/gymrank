package com.example.gymrank.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymrank.data.repository.UserRepositoryImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.ui.components.GymRankBottomBar
import com.example.gymrank.ui.screens.challenges.ChallengesScreen
import com.example.gymrank.ui.screens.challenges.subscreens.DiscoverScreen
import com.example.gymrank.ui.screens.challenges.subscreens.EquipmentScreen
import com.example.gymrank.ui.screens.challenges.subscreens.GambleScreen
import com.example.gymrank.ui.screens.challenges.subscreens.QuestsScreen
import com.example.gymrank.ui.screens.feed.FeedScreen
import com.example.gymrank.ui.screens.home.HomeScreen
import com.example.gymrank.ui.screens.loadworkout.LoadWorkoutScreen
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
import com.example.gymrank.ui.screens.workout.subscreens.RecoveryDetailsScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.gymrank.ui.screens.workout.subscreens.ProgramDetailScreen
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.model.WorkoutExercise
import com.example.gymrank.ui.screens.workout.subscreens.CreateRoutineScreen
import com.example.gymrank.ui.screens.feed.subscreens.WorkoutDetailScreen


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
    val showBottomBar = when (currentRoute) {
        Screen.Home.route,
        "feed",
        "challenges",
        "workout",
        "rank",
        Screen.Ranking.route -> true
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

    // ✅ Logout centralizado
    fun doLogout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        navController.navigate(Screen.Welcome.route) {
            popUpTo(Screen.Welcome.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    // ✅ FIX: ir a CreateRoutine (no a cualquier lado)
    fun navigateToCreateRoutine() {
        navController.navigate("workout/create_routine") {
            launchSingleTop = true
        }
    }

    // ✅ FIX: ir al TAB Ranking (como si tocaras el bottom bar)
    fun navigateToRankingTab() {
        navController.navigate("rank") {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // ✅ Gate: si ya hay gym guardado -> Home, sino -> SelectGym
    suspend fun navigateAfterAuth(userRepo: UserRepositoryImpl) {
        val gym: Gym? = runCatching<Gym?> { userRepo.getSelectedGym() }.getOrNull()

        if (gym != null) {
            runCatching<Unit> { sessionViewModel.selectGym(gym) }

            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(Screen.SelectGym.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = { if (showBottomBar) GymRankBottomBar(navController) }
    ) { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ✅ WELCOME (auto-redirect si ya hay sesión iniciada)
            composable(Screen.Welcome.route) {
                val userRepo = remember { UserRepositoryImpl() }
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                LaunchedEffect(firebaseUser?.uid) {
                    if (firebaseUser != null) {
                        navigateAfterAuth(userRepo)
                    }
                }

                if (firebaseUser != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Cargando…")
                    }
                } else {
                    WelcomeScreen(
                        onStartSignUp = {
                            setFlow(FLOW_SIGNUP)
                            navController.navigate("login_signup") { launchSingleTop = true }
                        },
                        onNavigateToLogin = {
                            setFlow(FLOW_LOGIN)
                            navController.navigate(Screen.Login.route) { launchSingleTop = true }
                        },
                        onSignUpSuccessNavigate = {
                            setFlow(FLOW_SIGNUP)
                            navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                        }
                    )
                }
            }

            // ✅ LOGIN normal
            composable(Screen.Login.route) {
                val userRepo = remember { UserRepositoryImpl() }

                LoginScreen(
                    onLoginSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_LOGIN)

                        coroutineScope.launch {
                            navigateAfterAuth(userRepo)
                        }
                    },
                    onSignUpSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    }
                )
            }

            // ✅ LOGIN con signup abierto
            composable("login_signup") {
                val userRepo = remember { UserRepositoryImpl() }

                LoginScreen(
                    openSignUpOnStart = true,
                    onLoginSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_LOGIN)

                        coroutineScope.launch {
                            navigateAfterAuth(userRepo)
                        }
                    },
                    onSignUpSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                    }
                )
            }

            // ✅ SELECT GYM
            composable(Screen.SelectGym.route) {
                val userRepo = remember { UserRepositoryImpl() }

                SelectGymScreen(
                    onGymSelected = { gym: Gym ->
                        coroutineScope.launch {
                            runCatching<Unit> { userRepo.saveSelectedGym(gym) }
                                .onFailure { e -> Log.e("SelectGym", "Error guardando gym", e) }

                            runCatching<Unit> { sessionViewModel.selectGym(gym) }

                            val flow = getFlow()
                            val destination =
                                if (flow == FLOW_SIGNUP) Screen.Onboarding.route else Screen.Home.route

                            navController.navigate(destination) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            // ✅ ONBOARDING
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ✅ MAIN TABS
            composable(Screen.Home.route) {
                HomeScreen(
                    sessionViewModel = sessionViewModel,

                    // ✅ FIX: NOMBRES CORRECTOS (según tu HomeScreen)
                    // "Cargar entreno" -> CreateRoutineScreen
                    onLogWorkout = { navigateToCreateRoutine() },

                    // "Ver ranking" -> Tab Ranking
                    onOpenRanking = { navigateToRankingTab() },

                    onLogout = { doLogout() }
                )
            }

            composable("feed") {
                FeedScreen(
                    onOpenWorkout = { ownerUid, workoutId ->
                        navController.navigate("workout/detail/$ownerUid/$workoutId")
                    }
                )
            }

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
                    onCreateRoutineClick = { navController.navigate("workout/create_routine") },
                    onRecoveryDetailsClick = { navController.navigate("workout/recovery") }
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

            // ✅ DETAILS REAL
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

            composable(Screen.LoadWorkout.route) {
                LoadWorkoutScreen(
                    onCancel = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // ✅ SUBSCREENS CHALLENGES
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

            // ✅ SUBSCREENS WORKOUT
            composable("workout/explore") {
                ExploreScreen(
                    onBack = { navController.popBackStack() },
                    onViewProgram = { templateId ->
                        navController.navigate("workout/program_detail/$templateId")
                    }
                )
            }
            composable("workout/program_detail/{templateId}") { entry ->
                val templateId = entry.arguments?.getString("templateId").orEmpty()
                ProgramDetailScreen(
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("workout/coach_ai") { CoachAiScreen(onBack = { navController.popBackStack() }) }
            composable("workout/history") {
                WorkoutHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onGoToCreate = { navController.navigate("workout/create_routine") }
                )
            }
            composable("workout/progress") { WorkoutProgressScreen(onBack = { navController.popBackStack() }) }

            composable("workout/recovery") {
                RecoveryDetailsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ CREATE ROUTINE
            composable(route = "workout/create_routine") {

                val workoutRepo = remember { WorkoutRepositoryFirestoreImpl() }
                val scope = rememberCoroutineScope()

                CreateRoutineScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { draft, muscles, weekday ->

                        val workout = Workout(
                            durationMinutes = 60,
                            type = draft.name,
                            muscles = muscles,
                            intensity = "Media",
                            notes = draft.description.trim().ifBlank { null },

                            // ✅ lo uso como “nombre” del entrenamiento guardado
                            title = draft.name.trim(),
                            description = draft.description.trim().ifBlank { null },

                            // ✅ weekday adentro de cada exercise
                            exercises = draft.exercises.map { ex ->
                                WorkoutExercise(
                                    name = ex.name,
                                    sets = ex.sets,
                                    reps = ex.reps,
                                    usesBodyweight = ex.isBodyWeight,
                                    weightKg = ex.weightKg?.toInt(),
                                    weekday = weekday
                                )
                            }
                        )

                        scope.launch {
                            workoutRepo.saveWorkout(workout)
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable("workout/detail/{ownerUid}/{workoutId}") { entry ->
                val ownerUid = entry.arguments?.getString("ownerUid").orEmpty()
                val workoutId = entry.arguments?.getString("workoutId").orEmpty()

                WorkoutDetailScreen(
                    ownerUid = ownerUid,
                    workoutId = workoutId,
                    onBack = { navController.popBackStack() }
                )
            }

        }
    }
}