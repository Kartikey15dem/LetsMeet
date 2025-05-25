package com.myworldtech.meet.presentation.componenets

import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.myworldtech.meet.R
import androidx.camera.core.Preview
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import kotlinx.coroutines.launch


@Composable
fun MeetingPreviewScreen(
    viewModel: VideoCallViewModel,
    meetingCode: String,
    name: String,
    photoUrl: String,
    onBack: () -> Unit,
    onJoinClick: suspend () -> Boolean,
    isAskToJoin: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    var isMicEnabled by rememberSaveable { mutableStateOf(true) }
    var isCamEnabled by rememberSaveable { mutableStateOf(true) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            IconButton(onClick = {isMuted = !isMuted}) {
                Icon(
                    painter = painterResource(id = if (isMuted) R.drawable.speaker__1_ else R.drawable.speaker),
                    contentDescription = "Mute",
                    tint = Color.Black
                )
            }


        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = meetingCode, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(250.dp)
                .height(350.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if(isCamEnabled){
                CameraPreviewView(modifier = modifier.fillMaxSize())
            }else {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "User Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(id = R.drawable.account_circle_24)

                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {isCamEnabled = !isCamEnabled },
                    modifier = Modifier.weight(1f).wrapContentSize(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isCamEnabled) Color.Black.copy(alpha = 0.7f) else Color.White,
                        disabledContainerColor = if(isCamEnabled) Color.Black.copy(alpha = 0.7f).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = if(isCamEnabled) R.drawable.video_camera else R.drawable.video_camera_prohibited),
                        contentDescription = "Audio/Video Toggle",
                        tint = if(isCamEnabled) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Audio Toggle Button
                Button(
                    onClick = {isMicEnabled = !isMicEnabled },
                    modifier = Modifier.weight(1f).wrapContentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor= if(isMicEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = if (isMicEnabled) R.drawable.mic else R.drawable.mute),
                        contentDescription = "Audio Toggle",
                        tint = if(isMicEnabled) Color.White else Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (joining) {
            CircularProgressIndicator()
        }

        Button(
            onClick = {
                joining = true
                coroutineScope.launch {
                    if(!onJoinClick()) joining = false
                    else{
                        viewModel.toggleCamera(isCamEnabled)
                        viewModel.toggleMic(isMicEnabled)
                        viewModel.toggleSpeakerAudio(!isMuted)
                    }
                }
            },
            enabled = !joining,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val buttonText = if (!joining) {
                if (isAskToJoin) "Ask to Join" else "Join"
            }else if (isAskToJoin) "Asking to Join" else "Joining..."

            Text(buttonText)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Joining as")
        // User details
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "User Photo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                placeholder = painterResource(id = R.drawable.account_circle_24)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun CameraPreviewView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}




