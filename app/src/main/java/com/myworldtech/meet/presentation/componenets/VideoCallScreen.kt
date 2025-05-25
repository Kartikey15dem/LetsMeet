package com.myworldtech.meet.presentation.componenets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.myworldtech.meet.R
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun VideoCallScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoCallViewModel,
    onCallEnd: () -> Unit,
    startVScreenSharing: () -> Unit,
    stopScreenSharing: () -> Unit,
    inCallMessages: () -> Unit,
    participantList: () -> Unit,
    ) {

    val context = LocalContext.current
    var isScreenSharing by viewModel.isScreenSharing
    val participantsMap by viewModel.participants.collectAsState()
    val participants = participantsMap.values.toList()
    val focusedParticipant by viewModel.focusedParticipant
    val requestQueue by viewModel.requestQueue.collectAsState()
    val isPipMode by viewModel.isPipMode


    Column(modifier = Modifier.fillMaxSize()
        .background(color = Color.Black),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Takes up the remaining space above the bottom item
            contentAlignment = Alignment.TopStart
        ) {

            if (participants.isNotEmpty()) {
                if(isPipMode){
                    Log.d("pip","entered pip mode")
                    AsyncImage(
                        model = if (focusedParticipant != null) focusedParticipant!!.photoUrl else participants[0].photoUrl,
                        contentDescription = "PiP participant",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .align(Alignment.Center),
                        placeholder = painterResource(id = R.drawable.baseline_account_circle_24),
                        error = painterResource(id = R.drawable.baseline_account_circle_24)
                    )
                }else {
                    if (isScreenSharing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You are sharing your screen with everyone",
                                color = Color.White,
                            )
                        }
                    } else {
                        if (focusedParticipant != null) {
                            ParticipantGridCell(
                                modifier = Modifier.fillMaxSize(),
                                participant = focusedParticipant!!,
                                viewModel = viewModel
                            )
                        } else {
                            if (participants.size <= 3) {

                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (i in participants.size - 1 downTo 0) {
                                        ParticipantGridCell(
                                            modifier = Modifier.weight(1f),
                                            participant = participants[i],
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                            if (participants.size == 5) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ParticipantGridCell(
                                        modifier = Modifier.weight(1f),
                                        participant = participants[4],
                                        viewModel = viewModel
                                    )
                                    for (i in 3 downTo 1) {
                                        if (i == 2) continue
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ParticipantGridCell(
                                                modifier = Modifier.weight(1f),
                                                participant = participants[i],
                                                viewModel = viewModel
                                            )
                                            ParticipantGridCell(
                                                modifier = Modifier.weight(1f),
                                                participant = participants[i - 1],
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            }
                            if (participants.size == 7) {
                                Row(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (i in 4..6) {
                                            ParticipantGridCell(
                                                modifier = Modifier.weight(1f),
                                                participant = participants[i],
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (i in 3 downTo 0) {
                                            ParticipantGridCell(
                                                modifier.weight(1f),
                                                participants[i],
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            }
                            if (participants.size == 6 || participants.size == 8 || participants.size == 4) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (i in participants.size / 2 downTo 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ParticipantGridCell(
                                                modifier = Modifier.weight(1f),
                                                participant = participants[2 * i - 1],
                                                 viewModel = viewModel
                                            )
                                            ParticipantGridCell(
                                                modifier = Modifier.weight(1f),
                                                participant = participants[2 * i - 2],
                                                 viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }else    Text(text = "No participants")
            MeetingControlRow(
                viewModel = viewModel,
                modifier = modifier.align(Alignment.TopCenter),
                onMuteToggle = { isMuted -> viewModel.toggleSpeakerAudio(!isMuted)},
                onCameraToggle = { viewModel.switchCamera() },
                onMeetingClick = participantList
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp, horizontal = 2.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CallControlRow(
                viewModel = viewModel,
                onVideoToggle = { viewModel.toggleCamera(it) },
                onAudioToggle = { viewModel.toggleMic(it) },
                onScreenShare = { isScreenShare->
                    if(isScreenShare) {
                        startVScreenSharing()
                        viewModel.toggleCamera(false)
                        isScreenSharing = true
                    }
                    else {
                        stopScreenSharing()
                        viewModel.toggleCamera(true)
                        isScreenSharing = false
                    }
                },
                onInCallMessages = inCallMessages,
                onLeaveCall = onCallEnd
            )
        }
    }
    var pendingRequest by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var requesterName by rememberSaveable { mutableStateOf("Unknown") }
    var coroutineScope = rememberCoroutineScope()

    LaunchedEffect(requestQueue) {
        if (pendingRequest == null && requestQueue.isNotEmpty()) {
            val request = viewModel.dequeRequest() ?: return@LaunchedEffect
            Log.d("socket", "peeeer: ${request.first}")

            coroutineScope.launch {
                val userData = viewModel.getDataFromFirestore(request.first.trim())
                Log.d("socket", "User data: $userData")
                requesterName = userData?.get("name") as? String ?: "Unknown"
                pendingRequest = request
                showDialog = true
            }
        }
    }

    if (showDialog && pendingRequest != null) {
        ShowJoinDialog(requesterName) { userDecision ->
            viewModel.approvePeer(userDecision, pendingRequest!!.second)
            showDialog = false
            pendingRequest = null
        }
    }
}
//@SuppressLint("UnusedBoxWithConstraintsScope")
//@Composable
//fun FloatingWindowWithMyGridCell(
//    myGridCellContent: @Composable () -> Unit
//) {
//    var offsetX by remember { mutableFloatStateOf(0f) }
//    var offsetY by remember { mutableFloatStateOf(0f) }
//    val pad = 16
//    BoxWithConstraints(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
//        val parentWidth = constraints.maxWidth.toFloat()
//        val parentHeight = constraints.maxHeight.toFloat()
//        val windowWidth = with(LocalDensity.current) { (100 + pad).dp.toPx() }
//        val windowHeight = with(LocalDensity.current) { (160 + pad).dp.toPx() }
//
//        // Initialize the box at the bottom end
//        offsetX = parentWidth - windowWidth
//        offsetY = parentHeight - windowHeight
//
//        Box(
//            modifier = Modifier
//                .offset {
//                    IntOffset(
//                        offsetX.coerceIn(100f, parentWidth - windowWidth ).roundToInt(),
//                        offsetY.coerceIn(100f, parentHeight - windowHeight ).roundToInt()
//                    )
//                }
//                .size(100.dp, 160.dp)
//                .clip(RoundedCornerShape(8.dp))
//                .background(Color.Black)
//                .pointerInput(Unit) {
//                    detectDragGestures { change, dragAmount ->
//                        change.consume()
//                        offsetX += dragAmount.x
//                        offsetY += dragAmount.y
//                    }
//                }
//        ) {
//            myGridCellContent()
//        }
//    }
//}


@Composable
fun MeetingControlRow(
    modifier: Modifier,
    onMuteToggle: (Boolean) -> Unit,
    onCameraToggle: () -> Unit,
    onMeetingClick: () -> Unit,
    viewModel: VideoCallViewModel
) {

    var isMuted by rememberSaveable { mutableStateOf(viewModel.isMuted.value) }
    var meetingLink by viewModel.roomId

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(95.dp)
            .padding(vertical = 24.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(3.dp))
        // Meeting Link Button
        Button(
            onClick = onMeetingClick,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = meetingLink,
                color = Color.White,
                fontSize = 17.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Mute Button
        Button(
            onClick = {
                isMuted = !isMuted
                onMuteToggle(isMuted)
            },
            modifier = Modifier.size(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = if (isMuted) R.drawable.speaker__1_ else R.drawable.speaker),
                contentDescription = "Mute",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Camera Toggle Button
        Button(
            onClick = {
                onCameraToggle()
            },
            modifier = Modifier.size(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.front_camera),
                contentDescription = "Toggle Camera",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
    }
}
@Composable
fun CallControlRow(
    onVideoToggle: (Boolean) -> Unit,
    onAudioToggle: (Boolean) -> Unit,
    onScreenShare: (Boolean) -> Unit,
    onInCallMessages: () -> Unit,
    onLeaveCall: () -> Unit,
    viewModel: VideoCallViewModel
) {
//    var isCamEnabled by rememberSaveable { mutableStateOf(viewModel.isCamEnabled.value) }
//    var isMicEnabled by rememberSaveable { mutableStateOf(viewModel.isMicEnabled.value) }
//    var isShareEnabled by rememberSaveable {mutableStateOf(viewModel.isScreenSharing.value) }
    var isCamEnabled by viewModel.isCamEnabled
    var isMicEnabled by viewModel.isMicEnabled
    var isShareEnabled by viewModel.isScreenSharing
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Audio/Video Toggle Button
        Spacer(modifier = Modifier.width(4.dp))
        Button(
            onClick = {
                onVideoToggle(!isCamEnabled)},
            modifier = Modifier.weight(1f).fillMaxHeight(),

            shape = CircleShape,
            enabled = !isShareEnabled,
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
            onClick = {
                onAudioToggle(!isMicEnabled)},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(isMicEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = if (isMicEnabled) R.drawable.mic else R.drawable.mute),
                contentDescription = "Audio Toggle",
                tint = if(isMicEnabled) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Screen Share Button
        Button(
            onClick = {
                onScreenShare(!isShareEnabled)
                isCamEnabled = !isShareEnabled},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(!isShareEnabled) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.screencast),
                contentDescription = "Screen Share",
                tint = if(!isShareEnabled) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // In-Call Messages Button
        Button(
            onClick = onInCallMessages,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= Color.Black.copy(alpha = 0.7f)),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat),
                contentDescription = "In-Call Messages",

                )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Leave Call Button (Red Color)
        Button(
            onClick = onLeaveCall,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= Color.Red),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.end_call),
                contentDescription = "Leave Call",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
    }
}
@Composable
fun ShowJoinDialog(name: String, onResult: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResult(false) },
        title = { Text("$name is asking to join") },
        confirmButton = {
            Button(onClick = { onResult(true) }) {
                Text("Accept")
            }
        },
        dismissButton = {
            Button(onClick = { onResult(false) }) {
                Text("Reject")
            }
        }
    )
}







