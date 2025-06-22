package com.myworldtech.meet.presentation.componenets

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.componenets.auth.CreateAccountScreen
import com.myworldtech.meet.presentation.componenets.auth.EmailLoginScreen
import com.myworldtech.meet.presentation.componenets.auth.LoginScreen
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    peerId : String,
    videoCallViewModel: VideoCallViewModel,
    authViewModel: AuthViewModel,
    authService: AuthService,
    startService:suspend (String,String,Boolean) -> Boolean,
    onCallEnd: () -> Unit,
    startVScreenSharing: () -> Unit,
    stopScreenSharing: () -> Unit,
    modifier: Modifier
) {
    val navController = rememberNavController()


    NavHost(navController = navController, startDestination = if(isLoggedIn) {
        if (videoCallViewModel.isServiceStarted) "videoCall" else "home"
    } else "login") {
        // Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCreateAccount = {
                    navController.navigate("createAccount") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                authService = authService
            ) { navController.navigate("emailLogin") }
        }

        // Create Account Screen
        composable("createAccount") {
            CreateAccountScreen(
                viewModel = authViewModel,
                authService = authService,
                onAccountCreated = { navController.navigate("home")
                    }
            )
        }
        composable ("emailLogin"){
            EmailLoginScreen(
                onLoginSuccess = { navController.navigate("home")
                    navController.popBackStack()},
                authService = authService,
                viewModel = authViewModel,
            )
        }
        // Home Screen
        composable("home") {

            HomeScreen(
                navController = navController
            )
        }
        composable(
            route = "meetingPreview/{meetingCode}/{name}/{photoUrl}/{isAskToJoin}",
            arguments = listOf(
                navArgument("meetingCode") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("photoUrl") { type = NavType.StringType },
                navArgument("isAskToJoin") { type = NavType.BoolType },
            )
        ) { backStackEntry ->
            val meetingCode = backStackEntry.arguments?.getString("meetingCode") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val photoUrl = backStackEntry.arguments?.getString("photoUrl") ?: ""
            val isAskToJoin = backStackEntry.arguments?.getBoolean("isAskToJoin") == true

            videoCallViewModel.setRoomId(meetingCode)
            videoCallViewModel.setPeerId(peerId)

            MeetingPreviewScreen(
                viewModel = videoCallViewModel,
                meetingCode = meetingCode,
                name = name,
                photoUrl = photoUrl,
                onBack = { navController.popBackStack()
                         onCallEnd()},
                onJoinClick = {
                    if(startService(meetingCode,peerId,!isAskToJoin)) {
                        Log.d("meet","nav")
                        navController.navigate("videoCall")
                        return@MeetingPreviewScreen true
                    } else { onCallEnd()
                        return@MeetingPreviewScreen false
                    }
                },
                isAskToJoin = isAskToJoin,
                navController = navController,
            )
        }

        composable("videoCall") {

            VideoCallScreen(
                viewModel = videoCallViewModel,
                onCallEnd = {
                    onCallEnd()
                    navController.popBackStack()
                    navController.popBackStack()
                },
                startVScreenSharing = { startVScreenSharing() },
                stopScreenSharing = stopScreenSharing,
                inCallMessages = { navController.navigate("inCallMessages") },
                participantList = { navController.navigate("participantList") },
            )
        }
        composable ("inCallMessages"){
            InCallMessagesScreen(
                viewModel = videoCallViewModel,
                onClose = { navController.popBackStack() },
                onSendMessage = {videoCallViewModel.sendMessage(it)}
            )
        }
        composable ("participantList"){
            ParticipantListScreen(
                viewModel = videoCallViewModel,
                onBack = {navController.popBackStack()}
            )
        }
    }
}


