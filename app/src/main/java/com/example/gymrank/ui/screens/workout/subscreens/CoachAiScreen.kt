package com.example.gymrank.ui.screens.workout.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachAiScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Step 1/5")
            Spacer(Modifier.height(12.dp))
            Text("Premium only - Free to preview")
            Spacer(Modifier.height(20.dp))
            Text("How often would you like to train?")
            Spacer(Modifier.height(12.dp))

            listOf("2 times per week","3 times per week","4 times per week","5 times per week","6 times per week").forEach {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) { Text(it) }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
