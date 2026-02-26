package com.example.gymrank.ui.screens.friendrequests

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.data.repository.FeedRepositoryFirestoreImpl
import com.example.gymrank.ui.theme.DesignTokens
import com.example.gymrank.ui.theme.GymRankColors
import kotlinx.coroutines.launch

class FriendRequestsViewModel(
    private val repo: FeedRepositoryFirestoreImpl = FeedRepositoryFirestoreImpl()
) : ViewModel() {

    // ✅ Tipo real: lista de objetos (no Pair)
    // Estos objetos tienen: fromUid y createdAtLabel
    val incoming = repo.observeIncomingPendingRequests()

    suspend fun accept(fromUid: String) = repo.acceptFriendRequest(fromUid)
    suspend fun reject(fromUid: String) = repo.rejectFriendRequest(fromUid)

    // Devuelve Pair(username, avatarUrl)
    suspend fun userMini(uid: String) = repo.getUserMini(uid)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    onBack: () -> Unit,
    vm: FriendRequestsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // ✅ NO asumimos Pair. Es la lista que venga del repo.
    val items by vm.incoming.collectAsState(initial = emptyList())

    // cache local: uid -> (username, avatarUrl)
    val miniCache = remember { mutableStateMapOf<String, Pair<String, String>>() }

    LaunchedEffect(items) {
        items.forEach { req ->
            val fromUid = req.fromUid
            if (!miniCache.containsKey(fromUid)) {
                runCatching { vm.userMini(fromUid) }
                    .onSuccess { mini -> miniCache[fromUid] = mini }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DesignTokens.Colors.BackgroundBase
                )
            )
        },
        containerColor = DesignTokens.Colors.BackgroundBase
    ) { pad ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tenés solicitudes nuevas.",
                    color = DesignTokens.Colors.TextSecondary
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = items,
                key = { it.fromUid } // ✅ key estable
            ) { req ->
                val fromUid = req.fromUid
                val createdAtLabel = req.createdAtLabel

                val mini = miniCache[fromUid]
                val username = mini?.first ?: "usuario"
                val avatar = mini?.second ?: "" // lo dejamos listo (si después querés mostrarlo)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DesignTokens.Colors.SurfaceElevated,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DesignTokens.Colors.SurfaceInputs, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = username,
                            color = DesignTokens.Colors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (createdAtLabel.isNotBlank()) {
                            Text(
                                text = createdAtLabel,
                                color = DesignTokens.Colors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { scope.launch { vm.accept(fromUid) } },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GymRankColors.PrimaryAccent
                                )
                            ) {
                                Text("Aceptar")
                            }

                            OutlinedButton(
                                onClick = { scope.launch { vm.reject(fromUid) } },
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                            ) {
                                Text("Rechazar", color = DesignTokens.Colors.TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}