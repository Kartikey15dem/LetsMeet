package com.myworldtech.meet.presentation.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myworldtech.meet.domain.service.ShareScreenService
import com.myworldtech.meet.domain.service.VideoCallService
import com.myworldtech.meet.presentation.model.Message
import com.myworldtech.meet.presentation.model.Participant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoCallViewModel : ViewModel() {
    // Replace List with Map for participants
    private val _participants = MutableStateFlow<Map<String, Participant>>(emptyMap())
    val participants: StateFlow<Map<String, Participant>> = _participants.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val _requestQueue = MutableStateFlow<ArrayDeque<Pair<String, String>>>(ArrayDeque())
    val requestQueue: StateFlow<ArrayDeque<Pair<String, String>>> = _requestQueue

     val roomId = mutableStateOf<String>("")

     val peerId = mutableStateOf<String>("")

    private val _focusedParticipant = mutableStateOf<Participant?>(null)
    val focusedParticipant: State<Participant?> = _focusedParticipant

    val isMicEnabled = mutableStateOf(true)
    val isCamEnabled = mutableStateOf(true)
    val isMuted = mutableStateOf(false)
    val isScreenSharing = mutableStateOf(false)

    val isPipMode = mutableStateOf(false)

    var isServiceStarted = false
    // Service connection
    private var _videoCallServiceRef: WeakReference<VideoCallService?> = WeakReference(null)
    private val videoCallService: VideoCallService?
        get() = _videoCallServiceRef.get()

    private var _screenShareServiceRef: WeakReference<ShareScreenService?> = WeakReference(null)
    private val screenShareService: ShareScreenService?
        get() = _screenShareServiceRef.get()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VideoCallService.VideoCallBinder
            val videoCallService = binder.getService()
            _videoCallServiceRef = WeakReference(videoCallService)
            videoCallServiceReady.complete(Unit)
            Log.d("VideoCallService", " service connected}")


            viewModelScope.launch {
                videoCallService.participants.collectLatest { serviceParticipants ->
                    _participants.value = serviceParticipants
                    Log.d("VideoCallService", " mic: ${_messages.value.size}")
                }

            }
            viewModelScope.launch {
                videoCallService.messages.collectLatest { messages ->
                    _messages.value = messages
                    Log.d("VideoCallService", " messagesV: ${_messages.value.size}")
                }
            }
            viewModelScope.launch {
                videoCallService.focusedParticipant.collectLatest { serviceFocusedParticipant->
                    _focusedParticipant.value = serviceFocusedParticipant
                }
            }
            viewModelScope.launch {
                videoCallService.requestQueue.collectLatest { serviceRequestQueue->
                    _requestQueue.value = serviceRequestQueue
                }
            }
            viewModelScope.launch {
                videoCallService.isServiceStarted.collectLatest { serviceIsServiceStarted->
                    isServiceStarted = serviceIsServiceStarted
                }
            }
            viewModelScope.launch {
                videoCallService.isMicEnabled.collectLatest { serviceIsMicEnabled->
                    isMicEnabled.value = serviceIsMicEnabled
                }
            }
            viewModelScope.launch {
                videoCallService.isCamEnabled.collectLatest { serviceIsCamEnabled->
                    isCamEnabled.value = serviceIsCamEnabled
                }
            }
            viewModelScope.launch {
                videoCallService.isMuted.collectLatest { serviceIsMuted ->
                    isMuted.value = serviceIsMuted
                }
            }


        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _videoCallServiceRef = WeakReference(null)
            videoCallServiceReady = CompletableDeferred()
        }
    }


    private var screenShareServiceReady = CompletableDeferred<Unit>()
    private var videoCallServiceReady = CompletableDeferred<Unit>()

    private val screenShareServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShareScreenService.ShareScreenBinder
            _screenShareServiceRef = WeakReference(binder.getService())
            screenShareServiceReady.complete(Unit)
            viewModelScope.launch {
                screenShareService?.isScreenSharing?.collectLatest { screenSharing->
                    isScreenSharing.value = screenSharing
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            _screenShareServiceRef = WeakReference(null)
            screenShareServiceReady = CompletableDeferred() // Reset for next bind
        }
    }

    suspend fun bindToShareServiceAwait(context: Context) {
        Log.d("screenS", "bindToShareService")
        screenShareServiceReady = CompletableDeferred()
        val intent = Intent(context, ShareScreenService::class.java)
        context.bindService(intent, screenShareServiceConnection, Context.BIND_AUTO_CREATE)
        screenShareServiceReady.await()
    }


    suspend fun bindToService(context: Context) {
        videoCallServiceReady = CompletableDeferred()
        val intent = Intent(context, VideoCallService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        videoCallServiceReady.await()
    }
    suspend  fun initializeSocket(roomId:String,peerId:String,isHost: Boolean):Boolean{
       return videoCallService?.initializeSocketHandler(roomId,peerId,isHost) == true
    }
    fun setPeerId(peerId: String) {
        videoCallService?.setPeerId(peerId)
        this.peerId.value = peerId
    }
    fun setRoomId(roomId: String) {
        videoCallService?.setRoomId(roomId)
        this.roomId.value = roomId
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startScreenSharing(resultCode: Int, data: Intent){
        screenShareService?.startScreenSharing(resultCode,data,roomId.value,peerId.value)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun stopScreenSharing(){
        screenShareService?.stopScreenSharing()
    }

    // Set the focused participant for PiP mode
    fun setFocusedParticipant(participant: Participant?) {
        _focusedParticipant.value = participant
        videoCallService!!.setFocusedParticipant(participant)
    }

    fun approvePeer(approved : Boolean, socketId:String){
        videoCallService?.approvePeer(approved,socketId)
    }
    fun dequeRequest(): Pair<String, String>?{
       return videoCallService?.dequeueRequest()
    }


    override fun onCleared() {
        super.onCleared()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun endCall(context: Context){
        videoCallService?.closeConnection()
        screenShareService?.stopScreenSharing()
        _participants.value = emptyMap()
        _focusedParticipant.value = null
        roomId.value = ""
        videoCallService?.isServiceStarted?.value = false
        try {
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Service not bound, ignore
        }
    }
    fun switchCamera(){
        videoCallService?.switchCamera()
    }

    fun toggleCamera(isEnabled: Boolean) {
        videoCallService?.setCameraEnabled(isEnabled)
        isCamEnabled.value = isEnabled
    }

    fun toggleMic(isEnabled: Boolean) {
        videoCallService?.setMicEnabled(isEnabled)
        isMicEnabled.value = isEnabled
    }

    fun sendMessage(message: String) {
        videoCallService?.sendMessage(message)
    }
    fun toggleSpeakerAudio(isEnabled: Boolean) {
        videoCallService?.toggleSpeakerAudio(isEnabled)
        isMuted.value = !isEnabled
    }
    fun setServiceStartedValue(value: Boolean) {
        isServiceStarted = true
        videoCallService?.isServiceStarted?.value = value
    }
    suspend fun getDataFromFirestore(peerId: String): Map<String, Any>? {
        val userData = videoCallService?.getUserDataFromFirestore(peerId)
        Log.d("socket", "firestore $userData")
        return userData
    }
    // Remove these commented out methods completely
}