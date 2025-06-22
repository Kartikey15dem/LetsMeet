package com.myworldtech.meet.domain.service

import com.myworldtech.meet.presentation.model.Participant
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

interface ServiceCallbackInterface {
   suspend fun addParticipant(participant: Participant)
    fun addMyParticipant(participant: Participant)
    fun removeParticipant(participantId:String)
    fun updateParticipant(participant: Participant)
    fun updateParticipantVideo(id: String, videoTrack: VideoTrack?)
    fun updateParticipantAudio(id: String, audioTrack: AudioTrack?)
    fun toggleParticipantVideo(id: String,enabled: Boolean)
    fun toggleParticipantAudio(id: String,enabled: Boolean)
    fun updateAudioConsumer(id:String)
    fun updateVideoConsumer(id:String)
    fun getAudioConsumer(id:String):String?
    fun getVideoConsumer(id:String):String?
    fun addMessage(message: String, id: String)
    fun handlePeerRequest(peerId:String,socketId:String)

}