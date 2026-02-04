package com.example.gymrank.ui.screens.selectgym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.domain.model.Gym
import com.example.gymrank.domain.model.GymUi
import com.example.gymrank.ui.components.GradientBackground
import com.example.gymrank.ui.components.GymCard
import com.example.gymrank.ui.theme.GymRankColors

@Composable
fun SelectGymScreen(
    onGymSelected: (Gym) -> Unit,
    viewModel: SelectGymViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Elegí tu gimnasio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Seleccioná el gym donde entrenás",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    ) { paddingValues ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar por nombre o ciudad...",
                            color = Color(0xFF8E8E93)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8E8E93)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = Color(0xFF2C2C2E),
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        cursorColor = GymRankColors.PrimaryAccent
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GymRankColors.PrimaryAccent
                            )
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.error ?: "Error desconocido",
                                    color = GymRankColors.Error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.retryLoadGyms() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GymRankColors.PrimaryAccent,
                                        contentColor = Color(0xFF000000)
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    uiState.filteredGyms.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron gimnasios",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(uiState.filteredGyms) { gym ->
                                GymCard(
                                    gym = gym.toGymUi(),
                                    onJoin = { onGymSelected(gym) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extension to convert Gym to GymUi with mock image URLs
 */
private fun Gym.toGymUi(): GymUi {
    // Mock image URLs - replace with real URLs from your backend
    val mockImages = listOf(
        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
        "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=800",
        "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=800",
        "https://images.unsplash.com/photo-1576678927484-cc907957898c?w=800",
        "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=800",
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800"
    )

    return GymUi(
        id = this.id,
        name = this.name,
        location = "${this.city}, Argentina",
        imageUrl = mockImages[this.id.hashCode().mod(mockImages.size)],
        isHighCompetition = listOf("Iron Temple", "Titan Gym", "Beast Factory").contains(this.name)
    )
}
