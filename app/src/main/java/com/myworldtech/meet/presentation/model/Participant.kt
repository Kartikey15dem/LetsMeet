package com.myworldtech.meet.presentation.model

import kotlinx.coroutines.CompletableDeferred
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class Participant(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val videoTrack: VideoTrack? = null,
    val audioTrack: AudioTrack? = null,
    val videoConsumerId : String? = null,
    val audioConsumerId : String? = null,
    var isMute : Boolean = false,
    var isVideoPaused : Boolean = false
)
data class Message(
    val senderName: String,
    val text: String,
    val isMe: Boolean
)


