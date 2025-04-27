package com.myworldtech.meet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.componenets.CreateAccountScreen
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.ui.theme.MeetTheme
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.myworldtech.meet.domain.service.VideoCallService
import com.myworldtech.meet.presentation.componenets.AppNavigation
import com.myworldtech.meet.presentation.componenets.VideoCallScreen
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import org.webrtc.PeerConnectionFactory

class MainActivity : ComponentActivity() {
    private lateinit var authService: AuthService
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: VideoCallViewModel  by viewModels()
    private val REQUEST_CODE_SCREEN_CAPTURE = 1001
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        authService = AuthService(this)

        // Configure Google and Microsoft Sign-In
        authService.configureGoogleSignIn()
//        authService.configureMicrosoftSignIn(this)

        setContent {
            MeetTheme {
                AppNavigation(
                    videoCallViewModel = viewModel,
                    authViewModel = authViewModel,
                    authService = authService,
                    startService = {
                        initialize()

                        // Start service and bind to it
                        val roomId = intent.getStringExtra("roomId") ?: "123456"
                        viewModel.setRoomId(roomId)

                        startVideoCallService()
                    }
                )

            }
        }
    }

    private fun startVideoCallService() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Pass the projection data to your service
            val serviceIntent = Intent(this, VideoCallService::class.java)
            val roomId = "123456"
            serviceIntent.putExtra("roomId", roomId)
            serviceIntent.putExtra("resultCode", resultCode)
            serviceIntent.putExtra("data", data)

            ContextCompat.startForegroundService(this, serviceIntent)
            viewModel.bindToService(this)
        }
    }

    override fun onResume() {
        super.onResume()
//        // Refresh participants data when activity comes to foreground
//        viewModel.refreshParticipantsFromService()
    }

    override fun onDestroy() {
        viewModel.unbindFromService(this)
        super.onDestroy()
    }
    fun initialize() {
        // Call this from Application.onCreate()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )
    }

//    fun startMicrosoftSignIn(authService: AuthService) {
//        authService.createAccountWithMicrosoft(
//            onSuccess = { message ->
//                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
//            },
//            onFailure = { error ->
//                Toast.makeText(
//                    this@MainActivity,
//                    "Microsoft Sign-In Error: $error",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        )
//    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeetTheme {
        Greeting("Android")
    }
}