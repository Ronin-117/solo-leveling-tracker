package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.local.WorkoutDatabase
import com.example.data.repository.WorkoutRepository
import com.example.ui.WorkoutViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PitchBlack

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database, Dao, and Repository
        val database = WorkoutDatabase.getDatabase(this)
        val repository = WorkoutRepository(database.workoutDao())
        
        // Instantiate ViewModel using the factory
        val viewModel: WorkoutViewModel by viewModels {
            WorkoutViewModel.Factory(repository)
        }

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val activeUser by viewModel.activeUser.collectAsState()
                val isSessionLoaded by viewModel.isSessionLoaded.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PitchBlack)
                ) {
                    if (isSessionLoaded) {
                        // Smooth transition between Login and Dashboard
                        Crossfade(
                            targetState = activeUser,
                            animationSpec = tween(durationMillis = 500),
                            label = "AuthScreenTransition"
                        ) { user ->
                            if (user == null) {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onLoginSuccess = {
                                        // Managed reactively via database session state
                                    }
                                )
                            } else {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onLogout = {
                                        // Managed reactively via database session state
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
