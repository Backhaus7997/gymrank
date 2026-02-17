package com.example.gymrank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymrank.app.navigation.AppNavigation
import com.example.gymrank.ui.session.SessionViewModel
import com.example.gymrank.ui.theme.GymRankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GymRankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sessionViewModel: SessionViewModel = viewModel()
                    AppNavigation(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}
