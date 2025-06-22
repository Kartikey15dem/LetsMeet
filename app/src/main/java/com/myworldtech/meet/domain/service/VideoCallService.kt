package com.myworldtech.meet.domain.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.myworldtech.meet.MainActivity
import com.myworldtech.meet.R
import com.myworldtech.meet.data.socket.SocketHandler
import com.myworldtech.meet.presentation.model.Message
import com.myworldtech.meet.presentation.model.Participant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import kotlin.collections.removeFirstOrNull
import kotlin.text.get

class VideoCallService : Service() {
    private val binder = VideoCallBinder()
    private lateinit var socketHandler: SocketHandler

    // Replace List with Map for participants
    private val _participants = MutableStateFlow<Map<String, Participant>>(emptyMap())
    val participants: StateFlow<Map<String, Participant>> = _participants.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val _requestQueue = MutableStateFlow<ArrayDeque<Pair<String, String>>>(ArrayDeque())
    val requestQueue: StateFlow<ArrayDeque<Pair<String, String>>> = _requestQueue


    private val _roomId = mutableStateOf<String>("")
    val roomId: State<String> = _roomId

    private val _peerId = mutableStateOf<String>("")
    val peerId: State<String> = _peerId

    private val _focusedParticipant = MutableStateFlow<Participant?>(null)
    val focusedParticipant: StateFlow<Participant?> = _focusedParticipant.asStateFlow()

    val isMicEnabled = MutableStateFlow(true)
    val isCamEnabled = MutableStateFlow(true)
    val isMuted = MutableStateFlow(false)


    val isServiceStarted = MutableStateFlow(false)
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VideoCallChannel"
    }

    inner class VideoCallBinder : Binder() {
        fun getService(): VideoCallService = this@VideoCallService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start with camera|microphone only
        val notification = createCallNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        return START_STICKY
    }
    fun setPeerId(peerId: String) {
        _peerId.value = peerId
    }
    fun setRoomId(roomId: String) {
        _roomId.value = roomId
    }

    suspend fun initializeSocketHandler(roomId:String,peerId:String,isHost: Boolean) : Boolean{
        socketHandler = SocketHandler(
            serviceCallback = createServiceCallback(),
            context = this,
            coroutineScope = CoroutineScope(Dispatchers.IO)
        )
        return socketHandler.setupSocketConnection(roomId.trim(),peerId,isHost)
    }

    private fun createServiceCallback() = object : ServiceCallbackInterface {
        override suspend fun addParticipant(participant: Participant) {
            val userData = getUserDataFromFirestore(participant.id)
            val newParticipant = participant.copy(
                name = userData?.get("name") as? String ?:  participant.name,
                photoUrl = userData?.get("profilePhotoUrl") as? String ?: participant.photoUrl
            )
            _participants.update { current ->
                current + (newParticipant.id to newParticipant)
            }
        }

        override fun addMyParticipant(participant: Participant) {
            _participants.update { current ->
                current + (participant.id to participant)
            }
        }

        override fun removeParticipant(participantId: String) {
            _participants.update { current ->
                current - participantId
            }
        }
        override fun updateParticipant(participant: Participant) {
            _participants.update { current ->
                current - participant.id
                current + (participant.id to participant)
            }
        }

        override fun updateParticipantVideo(id: String, videoTrack: VideoTrack?) {
            _participants.update { current ->
                current[id]?.let {
                    current + (id to it.copy(videoTrack = videoTrack))
                } ?: current
            }
        }

        override fun updateParticipantAudio(id: String, audioTrack: AudioTrack?) {
            _participants.update { current ->
                current[id]?.let {
                    current + (id to it.copy(audioTrack = audioTrack))
                } ?: current
            }
        }

        override fun toggleParticipantVideo(id: String, enabled: Boolean) {
            _participants.update { current ->
                current[id]?.let {
                    it.videoTrack?.setEnabled(enabled)
                    current + (id to it.copy(videoTrack = it.videoTrack, isVideoPaused = !enabled))
                } ?: current
            }
        }

        override fun toggleParticipantAudio(id: String, enabled: Boolean) {
            _participants.update { current ->
                current[id]?.let {
                    it.audioTrack?.setEnabled(enabled)
                    current + (id to it.copy(audioTrack = it.audioTrack, isMute = !enabled))
                } ?: current
            }
        }

        override fun updateAudioConsumer(id: String) {
            _participants.update { current ->
                current[id]?.let {
                    current + (id to it.copy(audioConsumerId = id))
                } ?: current
            }
        }

        override fun updateVideoConsumer(id: String) {
            _participants.update { current ->
                current[id]?.let {
                    current + (id to it.copy(videoConsumerId = id))
                } ?: current
            }
        }

        override fun getAudioConsumer(id: String): String? {
            return _participants.value[id]?.audioConsumerId
        }

        override fun getVideoConsumer(id: String): String? {
            return _participants.value[id]?.videoConsumerId
        }

        override fun addMessage(message: String, id: String) {
            val participant = _participants.value[id]
            val name = participant?.name ?: "Unknown"
            val newMessage = Message(text = message, senderName = name,isMe = false)
            _messages.update { current -> current + newMessage }
        }

        override fun handlePeerRequest(peerId: String, socketId: String) {
            val updatedQueue = ArrayDeque(_requestQueue.value)
            updatedQueue.addLast(peerId to socketId)
            _requestQueue.value = updatedQueue
        }
    }
    fun approvePeer(approved : Boolean, socketId:String){
        if (::socketHandler.isInitialized) {
            socketHandler.approvePeer(approved,socketId)
        } else {
            Log.e("VideoCallService", "SocketHandler not initialized")
        }
    }
    fun dequeueRequest(): Pair<String, String>? {
        val updatedQueue = ArrayDeque(_requestQueue.value)
        val removed = updatedQueue.removeFirstOrNull()
        _requestQueue.value = updatedQueue
        return removed
    }

    fun sendMessage(message: String) {
        if (::socketHandler.isInitialized) {
            socketHandler.sendMessage(message)
            val newMessage = Message(text = message, senderName = "You", isMe = true)
            _messages.update { current -> (current + newMessage).toList() }
            Log.d("VideoCallService", "Sending message: $message")
            Log.d("VideoCallService", " messages: ${messages.value.size}")
        } else {
            Log.e("VideoCallService", "SocketHandler not initialized")
        }
    }
    fun switchCamera(){
        if (::socketHandler.isInitialized) {
            socketHandler.switchCamera()
        } else {
            Log.e("VideoCallService", "SocketHandler not initialized")
        }
    }

    fun setCameraEnabled(isEnabled: Boolean) {
        if (::socketHandler.isInitialized) {
            socketHandler.toggleCamera(isEnabled)
            isCamEnabled.value = isEnabled
        } else {
            Log.e("VideoCallService", "SocketHandler not initialized")
        }
    }

    fun setMicEnabled(isEnabled: Boolean) {
        if (::socketHandler.isInitialized) {
            socketHandler.toggleMic(isEnabled)
            isMicEnabled.value = isEnabled
        } else {
            Log.e("VideoCallService", "SocketHandler not initialized")
        }
    }
    fun toggleSpeakerAudio(isEnabled: Boolean) {
        isMuted.value = !isEnabled
        _participants.value.forEach { (id, participant) ->
            participant.audioTrack?.setEnabled(isEnabled)
            if (::socketHandler.isInitialized) {
                socketHandler.toggleSpeakerAudio(isEnabled)
            }
            // Optionally update the participant's isMute state
            _participants.update { current ->
                current[id]?.let {
                    current + (id to it.copy(audioTrack = participant.audioTrack))
                } ?: current
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Video Call Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createCallNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ongoing Call")
            .setContentText("Tap to return to your meeting")
            .setSmallIcon(R.drawable.baseline_add_call_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    fun setFocusedParticipant(participant: Participant?) {
        _focusedParticipant.value = participant
    }

    suspend fun getUserDataFromFirestore(peerId: String): Map<String, Any>? {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(peerId)
                .get()
                .await()

            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            Log.d("socket","firebase $e")
            null
        }
    }



    fun closeConnection() {
        _participants.update { emptyMap() }
        if (::socketHandler.isInitialized) {
            socketHandler.closeConnection()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}