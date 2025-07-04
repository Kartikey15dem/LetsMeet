package com.myworldtech.meet

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.ui.theme.MeetTheme
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.myworldtech.meet.domain.service.ShareScreenService
import com.myworldtech.meet.domain.service.VideoCallService
import com.myworldtech.meet.presentation.componenets.AppNavigation
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.webrtc.PeerConnectionFactory
import java.io.IOException
import androidx.datastore.preferences.preferencesDataStore
import com.myworldtech.meet.data.preferences.getPeerId
import com.myworldtech.meet.data.preferences.isUserLoggedIn
import `in`.gauthama.network_monitor.NetworkStateMonitorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var authService: AuthService
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: VideoCallViewModel  by viewModels()

    private val REQUEST_CODE_SCREEN_CAPTURE = 1001
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        val networkStateMonitor = NetworkStateMonitorFactory.create(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@launch
            }
            networkStateMonitor.observeNetworkChanges().collect { networkState ->
                val nw = networkState.downloadBandwidthKbps
                Log.d("network", "Network: $nw")

            }

        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        authService = AuthService(this)

        // Configure Google and Microsoft Sign-In
        authService.configureGoogleSignIn()
//        authService.configureMicrosoftSignIn(this)

        val isLoggedIn = runBlocking { isUserLoggedIn(this@MainActivity) }
        val peerId = runBlocking { getPeerId(this@MainActivity) }

        Log.d("socket","peer : $peerId")


        setContent {
            MeetTheme {
                Scaffold(
                    modifier = Modifier.navigationBarsPadding()
                ) { innerPadding ->
                    AppNavigation(
                        isLoggedIn = isLoggedIn,
                        peerId = peerId.toString(),
                        videoCallViewModel = viewModel,
                        authViewModel = authViewModel,
                        authService = authService,
                        startService = { roomId, peerId, isHost ->
                            initialize()
                            return@AppNavigation startVideoCallService(roomId, peerId, isHost)
                        },
                        onCallEnd = { viewModel.endCall(this) },
                        startVScreenSharing = { startVScreenSharing() },
                        stopScreenSharing = { stopScreenSharing() },
                        // 👇 You need to update AppNavigation to accept a `modifier`
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }

    private suspend fun startVideoCallService(roomId:String,peerId:String,isHost: Boolean) : Boolean{
        val serviceIntent = Intent(this, VideoCallService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        viewModel.bindToService(this)
        viewModel.setServiceStartedValue(true)
        setAudioToSpeaker(this)
        return viewModel.initializeSocket(roomId,peerId,isHost)
    }

    private fun startVScreenSharing() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stopScreenSharing(){
        viewModel.stopScreenSharing()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ShareScreenService::class.java)
            val roomId = intent.getStringExtra("roomId") ?: "023"
            serviceIntent.putExtra("roomId", roomId)
            ContextCompat.startForegroundService(this, serviceIntent)
            lifecycleScope.launch {
                viewModel.bindToShareServiceAwait(this@MainActivity)
                viewModel.startScreenSharing(resultCode, data)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }
    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )
    }
    private fun enterPipManually() {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
    }

    override fun onPictureInPictureModeChanged(isInPipMode: Boolean, config: Configuration) {
        super.onPictureInPictureModeChanged(isInPipMode, config)
        Log.d("pip", "onPictureInPictureModeChanged: $isInPipMode")
        viewModel.isPipMode.value = isInPipMode
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
    override fun onUserLeaveHint() {
        if (viewModel.isServiceStarted) {
            super.onUserLeaveHint()
            enterPipManually()
        }
    }



}
fun setAudioToSpeaker(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = true
}

