package com.myworldtech.meet.presentation.componenets

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.myworldtech.meet.R
import com.myworldtech.meet.data.util.PeerConnectionUtils
import com.myworldtech.meet.presentation.model.Participant
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun ParticipantGridCell(
    modifier: Modifier,
    participant: Participant,
    viewModel: VideoCallViewModel
) {
        val focusedParticipant by viewModel.focusedParticipant
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .pointerInput(focusedParticipant?.id) {
                    detectTapGestures(
                        onDoubleTap = {
                           if (focusedParticipant != null) viewModel.setFocusedParticipant(null) else viewModel.setFocusedParticipant(participant)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Video handling
            if (!participant.isVideoPaused) {
                WebRTCVideoRenderer(
                    videoTrack = participant.videoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = participant.photoUrl,
                    contentDescription = "No video available",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center),
                    placeholder = painterResource(id = R.drawable.baseline_account_circle_24),
                    error = painterResource(id = R.drawable.baseline_account_circle_24)
                )
            }

            // Audio handling
            if (participant.isMute) {

                Image(
                    painter = painterResource(id = R.drawable.mute),
                    contentDescription = "Audio not available",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    colorFilter = ColorFilter.tint(Color.Red)
                )
            }

            Text(
                text = participant.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }


@Composable
fun WebRTCVideoRenderer(
    modifier: Modifier = Modifier,
    videoTrack: VideoTrack?,
) {
    val context = LocalContext.current

    val renderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(Unit) {
        renderer.init(PeerConnectionUtils.eglContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setEnableHardwareScaler(true)

        onDispose {
            renderer.release()
        }
    }

    DisposableEffect(videoTrack) {
        videoTrack?.addSink(renderer)
        onDispose {
            videoTrack?.removeSink(renderer)
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier,
        update = { /* No-op - renderer is stable */ }
    )
}

