package com.myworldtech.meet.presentation.componenets

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.myworldtech.meet.R
import com.myworldtech.meet.data.util.PeerConnectionUtils
import com.myworldtech.meet.presentation.model.Participant
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun ParticipantGridCell(
    modifier: Modifier,
    participant: Participant
) {
    // Remember participant ID for stable identity
    val participantId = remember(participant.id) { participant.id }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp), // Add padding around the cell
        contentAlignment = Alignment.Center
    ) {
        // Video handling
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

    // Track handling without recomposition
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

//object WebRTCUtils {
//    val eglBaseContext: EglBase.Context by lazy {
//        EglBase.create().eglBaseContext
//    }
//}