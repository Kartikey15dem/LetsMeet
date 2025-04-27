package com.myworldtech.meet.domain.service

import com.myworldtech.meet.presentation.model.Participant
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

interface ServiceCallbackInterface {
    fun addParticipant(participant: Participant)
    fun updateParticipantVideo(id: String, videoTrack: VideoTrack?)
    fun updateParticipantAudio(id: String, audioTrack: AudioTrack?)
}