package com.example.gymrank.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymrank.data.repository.UserRepositoryImpl
import com.example.gymrank.data.repository.WorkoutRepositoryFirestoreImpl
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.Workout
import com.example.gymrank.domain.model.WorkoutExercise
import com.example.gymrank.ui.components.GymRankBottomBar
import com.example.gymrank.ui.screens.challenges.ChallengesScreen
import com.example.gymrank.ui.screens.challenges.subscreens.DiscoverScreen
import com.example.gymrank.ui.screens.challenges.subscreens.EquipmentScreen
import com.example.gymrank.ui.screens.challenges.subscreens.GambleScreen
import com.example.gymrank.ui.screens.challenges.subscreens.QuestsScreen
import com.example.gymrank.ui.screens.feed.FeedScreen
import com.example.gymrank.ui.screens.feed.subscreens.WorkoutDetailScreen
import com.example.gymrank.ui.screens.friendrequests.FriendRequestsScreen
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
import com.example.gymrank.ui.screens.workout.subscreens.ProgramDetailScreen
import com.example.gymrank.ui.screens.workout.subscreens.RecoveryDetailsScreen
import com.example.gymrank.ui.screens.workout.subscreens.WorkoutHistoryScreen
import com.example.gymrank.ui.screens.workout.subscreens.WorkoutProgressScreen
import com.example.gymrank.ui.session.SessionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.gymrank.ui.screens.home.profile.ProfileScreen
import com.example.gymrank.ui.screens.challenges.subscreens.ChallengeTemplateDetailScreen

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

    // ✅ Flow estable
    var flowState by rememberSaveable { mutableStateOf(FLOW_LOGIN) }

    // ✅ FLAG CLAVE: onboarding SOLO cuando venís de SIGNUP
    var shouldShowOnboarding by rememberSaveable { mutableStateOf(false) }

    fun setFlow(flow: String) {
        flowState = flow
        Log.d("NavFlow", "setFlow=$flow (route=$currentRoute)")
    }

    fun doLogout() {
        FirebaseAuth.getInstance().signOut()
        flowState = FLOW_LOGIN
        shouldShowOnboarding = false

        navController.navigate(Screen.Welcome.route) {
            popUpTo(Screen.Welcome.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToCreateRoutine() {
        navController.navigate("workout/create_routine") { launchSingleTop = true }
    }

    fun navigateToRankingTab() {
        navController.navigate("rank") {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // ✅ Parche silencioso: si falta feedVisibility, lo setea a PUBLIC
    suspend fun ensureFeedVisibilityDefault(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        if (snap.getString("feedVisibility").isNullOrBlank()) {
            ref.set(
                mapOf(
                    "feedVisibility" to "PUBLIC",
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    // ✅ Gate: si ya hay gym guardado -> Home, sino -> SelectGym
    suspend fun navigateAfterAuth(userRepo: UserRepositoryImpl) {
        val gym: Gym? = runCatching<Gym?> { userRepo.getSelectedGym() }.getOrNull()

        if (gym != null) {
            runCatching { sessionViewModel.selectGym(gym) }
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

            // ✅ WELCOME
            composable(Screen.Welcome.route) {
                val userRepo = remember { UserRepositoryImpl() }
                val firebaseUser = FirebaseAuth.getInstance().currentUser

                LaunchedEffect(firebaseUser?.uid) {
                    if (firebaseUser != null) {
                        setFlow(FLOW_LOGIN)
                        shouldShowOnboarding = false

                        runCatching { ensureFeedVisibilityDefault(firebaseUser.uid) }
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
                            shouldShowOnboarding = true
                            navController.navigate("login_signup") { launchSingleTop = true }
                        },
                        onNavigateToLogin = {
                            setFlow(FLOW_LOGIN)
                            shouldShowOnboarding = false
                            navController.navigate(Screen.Login.route) { launchSingleTop = true }
                        },
                        onSignUpSuccessNavigate = {
                            setFlow(FLOW_SIGNUP)
                            shouldShowOnboarding = true
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
                        shouldShowOnboarding = false

                        coroutineScope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            if (uid.isNotBlank()) runCatching { ensureFeedVisibilityDefault(uid) }
                            navigateAfterAuth(userRepo)
                        }
                    },
                    onSignUpSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        shouldShowOnboarding = true

                        coroutineScope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            if (uid.isNotBlank()) runCatching { ensureFeedVisibilityDefault(uid) }
                            navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                        }
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
                        shouldShowOnboarding = false

                        coroutineScope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            if (uid.isNotBlank()) runCatching { ensureFeedVisibilityDefault(uid) }
                            navigateAfterAuth(userRepo)
                        }
                    },
                    onSignUpSuccess = { user ->
                        sessionViewModel.setUser(user)
                        setFlow(FLOW_SIGNUP)
                        shouldShowOnboarding = true

                        coroutineScope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            if (uid.isNotBlank()) runCatching { ensureFeedVisibilityDefault(uid) }
                            navController.navigate(Screen.SelectGym.route) { launchSingleTop = true }
                        }
                    }
                )
            }

            // ✅ SELECT GYM
            composable(Screen.SelectGym.route) {
                val userRepo = remember { UserRepositoryImpl() }

                // ✅ Decide por DB (profileCompleted), no por flags en memoria
                suspend fun destinationAfterGymChoice(): String {
                    return runCatching {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        if (uid.isBlank()) return@runCatching Screen.Home.route

                        val snap = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()

                        val completed = snap.getBoolean("profileCompleted") == true
                        if (completed) Screen.Home.route else Screen.Onboarding.route
                    }.getOrElse {
                        // si falla por red o lo que sea, por default mostramos onboarding
                        // (mejor que dejar al user sin completar perfil)
                        Screen.Onboarding.route
                    }
                }

                SelectGymScreen(
                    onGymSelected = { gym: Gym ->
                        coroutineScope.launch {
                            runCatching { userRepo.saveSelectedGym(gym) }
                                .onFailure { e -> Log.e("SelectGym", "Error guardando gym", e) }

                            runCatching { sessionViewModel.selectGym(gym) }

                            val destination = destinationAfterGymChoice()

                            navController.navigate(destination) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onContinueWithoutGym = {
                        coroutineScope.launch {
                            // ✅ NO limpiar acá (ya se limpia en SelectGymScreen -> viewModel.continueWithoutGym)

                            val destination = destinationAfterGymChoice()

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
                        shouldShowOnboarding = false
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackToSelectGym = {
                        // ✅ volver a elegir gym (sin tocar Welcome)
                        navController.navigate(Screen.SelectGym.route) {
                            popUpTo(Screen.SelectGym.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ✅ MAIN TABS
            composable(Screen.Home.route) {
                HomeScreen(
                    sessionViewModel = sessionViewModel,
                    onLogWorkout = { navigateToCreateRoutine() },
                    onOpenRanking = { navigateToRankingTab() },
                    onLogout = { doLogout() },
                    onOpenFriendRequests = { navController.navigate("friends/requests") },
                    onOpenProfile = { navController.navigate("profile") }
                )
            }

            // ✅ FEED TAB
            composable("feed") {
                FeedScreen(
                    onOpenWorkoutDetail = { ownerUid, workoutId ->
                        navController.navigate("feed/workout_detail/$ownerUid/$workoutId")
                    }
                )
            }

            // ✅ NUEVO: detalle por usuario (LIST MODE: últimos 5)
            composable("feed/workout_detail/{ownerUid}/{workoutId}") { entry ->
                val ownerUid = entry.arguments?.getString("ownerUid").orEmpty()
                val workoutId = entry.arguments?.getString("workoutId").orEmpty()

                WorkoutDetailScreen(
                    ownerUid = ownerUid,
                    workoutId = workoutId,
                    onBack = { navController.popBackStack() }
                )
            }


            // ✅ CHALLENGES
            composable("challenges") {
                ChallengesScreen(
                    onOpenDiscover = { navController.navigate("challenges/discover") },
                    onOpenQuests = { navController.navigate("challenges/quests") },
                    onOpenGamble = { navController.navigate("challenges/gamble") },
                    onOpenEquipment = { navController.navigate("challenges/equipment") },
                    onOpenUserChallengeDetail = { _, templateId ->
                        navController.navigate("challenges/template_detail/$templateId")
                    }
                )
            }

            composable("challenges/template_detail/{templateId}") { entry ->
                val templateId = entry.arguments?.getString("templateId").orEmpty()
                ChallengeTemplateDetailScreen(
                    templateId = templateId,
                    onBack = { navController.popBackStack() },
                    onAcceptedGoToActive = {
                        navController.navigate("challenges") {
                            popUpTo("challenges") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
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

            // ✅ FRIEND REQUESTS
            composable("friends/requests") {
                FriendRequestsScreen(onBack = { navController.popBackStack() })
            }

            composable("profile") {
                ProfileScreen(
                    onClose = { navController.popBackStack() },
                    onOpenFriendRequests = { navController.navigate("friends/requests") },
                    onLogout = { doLogout() }
                )
            }

            // ✅ SUBSCREENS CHALLENGES
            composable("challenges/discover") {
                DiscoverScreen(
                    onBack = { navController.popBackStack() }
                )
            }
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
                RecoveryDetailsScreen(onBack = { navController.popBackStack() })
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
                            title = draft.name.trim(),
                            description = draft.description.trim().ifBlank { null },
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

            // ✅ (si usás este route desde workout/history u otros)
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