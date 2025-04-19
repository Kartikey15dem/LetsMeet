package com.myworldtech.meet.presentation.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.myworldtech.meet.presentation.model.Participant
import androidx.compose.runtime.State

class VideoCallViewModel : ViewModel() {
    private val _participants = mutableStateOf<List<Participant>>(emptyList())
    val participants: State<List<Participant>> = _participants

    // Socket client reference (initialize this in your ViewModel setup)
//    private lateinit var socketClient: SocketClient

    init {
        // Connect to socket and set up listeners
        setupSocketConnection()
    }

    private fun setupSocketConnection() {
        // Initialize socket client
        // socketClient = YourSocketClient()

        // Listen for audio/video toggle events
//        socketClient.on("participant_audio_toggled") { data ->
//            val participantId = data.getString("participantId")
//            val enabled = data.getBoolean("enabled")
//            updateParticipantAudio(participantId, enabled)
//        }
//
//        socketClient.on("participant_video_toggled") { data ->
//            val participantId = data.getString("participantId")
//            val enabled = data.getBoolean("enabled")
//            updateParticipantVideo(participantId, enabled)
//        }
    }

    // Update methods called by socket events
    fun updateParticipantAudio(id: String, enabled: Boolean) {
        updateParticipant(id) { it.copy(audioEnabled = enabled) }
    }

    fun updateParticipantVideo(id: String, enabled: Boolean) {
        updateParticipant(id) { it.copy(videoEnabled = enabled) }
    }

    // Helper to update a specific participant
    private fun updateParticipant(id: String, transform: (Participant) -> Participant) {
        _participants.value = _participants.value.map {
            if (it.id == id) transform(it) else it
        }
    }
}