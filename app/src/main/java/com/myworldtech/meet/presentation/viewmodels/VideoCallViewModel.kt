package com.myworldtech.meet.presentation.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myworldtech.meet.domain.service.VideoCallService
import com.myworldtech.meet.presentation.model.Participant
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import java.lang.ref.WeakReference

class VideoCallViewModel : ViewModel() {
    private val _participants = mutableStateOf<List<Participant>>(emptyList())
    val participants: State<List<Participant>> = _participants

    private val _roomId = mutableStateOf<String>("")
    val roomId: State<String> = _roomId

    // Service connection
    private var _videoCallServiceRef: WeakReference<VideoCallService?> = WeakReference(null)
    private val videoCallService: VideoCallService?
        get() = _videoCallServiceRef.get()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VideoCallService.VideoCallBinder
            val videoCallService = binder.getService()
            _videoCallServiceRef = WeakReference(videoCallService)

            // Start collecting the participants from the service
            viewModelScope.launch {
                videoCallService.participants.collectLatest { serviceParticipants ->
                    _participants.value = serviceParticipants
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _videoCallServiceRef = WeakReference(null)
        }
    }

    // This method is no longer needed since we now collect from the flow
    // fun refreshParticipantsFromService() {
    //     videoCallService?.let {
    //         _participants.value = it.getParticipants()
    //     }
    // }

    fun bindToService(context: Context) {
        val intent = Intent(context, VideoCallService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindFromService(context: Context) {
        try {
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Service not bound, ignore
        }
    }

    fun setRoomId(roomId: String) {
        _roomId.value = roomId
    }

    override fun onCleared() {
        super.onCleared()
        // Don't call closeConnection here as the service should keep running
    }

    // Remove these commented out methods completely
}