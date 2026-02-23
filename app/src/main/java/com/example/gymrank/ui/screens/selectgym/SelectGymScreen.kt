package com.example.gymrank.ui.screens.selectgym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.GymUi
import com.example.gymrank.ui.components.GradientBackground
import com.example.gymrank.ui.components.GymCard

@Composable
fun SelectGymScreen(
    onGymSelected: (Gym) -> Unit,
    viewModel: SelectGymViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ✅ search local
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadGyms()
    }

    GradientBackground {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}")
                }
            }

            else -> {
                // ✅ filtrar gyms por query (name o city)
                val filteredGyms = remember(uiState.gyms, query) {
                    val q = query.trim().lowercase()
                    if (q.isEmpty()) uiState.gyms
                    else uiState.gyms.filter { g ->
                        g.name.lowercase().contains(q) || g.city.lowercase().contains(q)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 18.dp)
                ) {
                    Text(
                        text = "Elegí tu gimnasio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Seleccioná el gym donde entrenás",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        placeholder = { Text("Buscar por nombre o ciudad...") }
                    )

                    Spacer(Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredGyms) { gym ->
                            val gymUi = gym.toGymUi()

                            // ✅ USAR TU CARD LINDA (como la screenshot 2)
                            GymCard(
                                gym = gymUi,
                                onJoin = { onGymSelected(gym) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Gym.toGymUi(): GymUi {
    return GymUi(
        id = id,
        name = name,
        location = "$city, Argentina",
        imageUrl = imageForGymName(name),
        isHighCompetition = name in setOf("Iron Temple", "Titan Gym", "Beast Factory")
    )
}

/**
 * ✅ Una imagen ESPECÍFICA por gimnasio (sin pool).
 */
private fun imageForGymName(name: String): String {
    return when (name.trim()) {
        "Olympia Gym" ->
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1400&auto=format&fit=crop&q=75"

        "Fuerza Sur" ->
            "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=1400&auto=format&fit=crop&q=75"

        "Power House" ->
            "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=1400&auto=format&fit=crop&q=75"

        "Sparta Fitness" ->
            "https://images.unsplash.com/photo-1599058917765-a780eda07a3e?w=1400&auto=format&fit=crop&q=75"

        "Iron Temple" ->
            "https://images.pexels.com/photos/29526371/pexels-photo-29526371.jpeg"

        "Titan Gym" ->
            "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=1400&auto=format&fit=crop&q=75"

        "Peak Performance" ->
            "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=1400&auto=format&fit=crop&q=75"

        "Alpha Training" ->
            "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=1400&auto=format&fit=crop&q=75"

        "Iron Paradise" ->
            "https://images.pexels.com/photos/29224211/pexels-photo-29224211.jpeg"

        "Evolution Gym" ->
            "https://images.pexels.com/photos/29392546/pexels-photo-29392546.jpeg"

        "Warrior Zone" ->
            "https://images.pexels.com/photos/31000553/pexels-photo-31000553.jpeg"

        else -> ""
    }
}
