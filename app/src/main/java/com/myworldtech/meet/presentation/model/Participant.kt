package com.myworldtech.meet.presentation.model

import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class Participant(
    val id: String,
    val name: String,
    val videoTrack: VideoTrack? = null,
    val audioTrack: AudioTrack? = null
)