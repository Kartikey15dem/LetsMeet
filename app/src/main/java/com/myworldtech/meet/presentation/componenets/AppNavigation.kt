package com.myworldtech.meet.presentation.componenets

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel

@Composable
fun AppNavigation(
    videoCallViewModel : VideoCallViewModel,
    authViewModel: AuthViewModel,
    authService: AuthService,
    startService: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        // Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("home") },
                onCreateAccount = { navController.navigate("createAccount")  },
                authService = authService
            ) { navController.navigate("emailLogin") }
        }

        // Create Account Screen
        composable("createAccount") {
            CreateAccountScreen(
                viewModel = authViewModel,
                authService = authService,
                onAccountCreated = { navController.navigate("home") }
            )
        }
        composable ("emailLogin"){
            EmailLoginScreen(
                onLoginSuccess = { navController.navigate("home") },
                authService = authService,
                viewModel = authViewModel,
            )
        }
        // Home Screen
        composable("home") {
            HomeScreen(
                onClick = { navController.navigate("videoCall")
                          startService()
                          },
            )
        }
        composable ("videoCall") {
            VideoCallScreen(
                viewModel = videoCallViewModel,
            )
        }
    }
}

