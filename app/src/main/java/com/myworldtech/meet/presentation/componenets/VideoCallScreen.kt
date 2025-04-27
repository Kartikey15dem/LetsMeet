package com.myworldtech.meet.presentation.componenets

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.myworldtech.meet.presentation.model.Participant
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import com.myworldtech.meet.ui.theme.MeetTheme
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.myworldtech.meet.R

@Composable
fun VideoCallScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoCallViewModel
//    participants : List<Int> = listOf(1, 2,3,4,5,6,7,8)
) {
    val participants by viewModel.participants
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (participants.isNotEmpty()) {
            Text(text = "No participants")
            if (participants.size <= 4) {

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..participants.size - 1) {
                        ParticipantGridCell(modifier.weight(1f), participants[i])
                    }
                }
                MyGridCell(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 26.dp, bottom = 26.dp),
                    height = 170,
                    width = 100,
                    participant = participants[0]
                )
            }
            if (participants.size == 5) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    ParticipantGridCell(modifier.weight(1f), participants[participants.size - 1])
                    for (i in 2 downTo 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ParticipantGridCell(modifier.weight(1f), participants[2 * i - 1])
                            ParticipantGridCell(modifier.weight(1f), participants[i - 1])
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
                            ParticipantGridCell(modifier.weight(1f), participants[i])
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 3 downTo 0) {
                            ParticipantGridCell(modifier.weight(1f), participants[i])
                        }
                    }
                }
            }
            if (participants.size == 6 || participants.size == 8) {
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
                            ParticipantGridCell(modifier.weight(1f), participants[2 * i - 1])
                            ParticipantGridCell(modifier.weight(1f), participants[i - 1])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyGridCell(
    modifier: Modifier,
    height: Int,
    width: Int,
    participant: Participant,
) {
    // Remember participant ID for stable identity
    val participantId = remember(participant.id) { participant.id }

        Box(
            modifier = modifier.width(width.dp)
                .height(height.dp)
                .padding(8.dp)
        ) {

            participant.videoTrack?.let { track ->
                WebRTCVideoRenderer(
                    videoTrack = track,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Image(
                painter = painterResource(id = R.drawable.baseline_account_circle_24), // Replace with your placeholder image resource
                contentDescription = "No video available",
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )

            // Audio handling
            if (participant.audioTrack == null) {
                Image(
                    painter = painterResource(id = R.drawable.mute), // Replace with your "cut mic" drawable resource
                    contentDescription = "Audio not available",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp) // Add padding for the "cut mic" icon
                )
            } else {
                DisposableEffect(participant.audioTrack) {
                    participant.audioTrack?.setEnabled(true)
                    onDispose { participant.audioTrack.setEnabled(false) }
                }
            }

            // Display participant name
            Text(
                text = participant.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

}
@Composable
fun CallControlRow(
    onVideoToggle: () -> Unit,
    onAudioToggle: () -> Unit,
    onScreenShare: () -> Unit,
    onInCallMessages: () -> Unit,
    onLeaveCall: () -> Unit,
) {
    var isClicked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Audio/Video Toggle Button
        Spacer(modifier = Modifier.width(3.dp))
        Button(
            onClick = {onVideoToggle()
                isClicked = !isClicked
            },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(!isClicked) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = if(!isClicked) R.drawable.video_camera else R.drawable.video_camera_prohibited),
                contentDescription = "Audio/Video Toggle",
                tint = if(!isClicked) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Audio Toggle Button
        Button(
            onClick = {onAudioToggle()
                isClicked = !isClicked},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(!isClicked) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = if (!isClicked) R.drawable.mic else R.drawable.mute),
                contentDescription = "Audio Toggle",
                tint = if(!isClicked) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        // Screen Share Button
        Button(
            onClick = {onScreenShare()
                isClicked = !isClicked},
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor= if(!isClicked) Color.Black.copy(alpha = 0.7f) else Color.White),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.screencast),
                contentDescription = "Screen Share",
                tint = if(!isClicked) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
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

@Preview
@Composable
fun Prev(modifier: Modifier = Modifier) {
    MeetTheme {
        CallControlRow({},{},{},{},{})
    }
}