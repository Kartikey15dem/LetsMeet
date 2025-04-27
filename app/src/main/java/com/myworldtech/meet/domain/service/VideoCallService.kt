package com.myworldtech.meet.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat.getSystemService
import com.myworldtech.meet.MainActivity
import com.myworldtech.meet.R
import com.myworldtech.meet.data.socket.SocketHandler
import com.myworldtech.meet.presentation.model.Participant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import android.app.Activity
import android.content.pm.ServiceInfo
import android.util.Log
import android.media.projection.MediaProjectionManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoCallService : Service() {
    private val binder = VideoCallBinder()
    private lateinit var socketHandler: SocketHandler
    // Use StateFlow instead of mutableStateOf for better interop with ViewModel
    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants.asStateFlow()


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

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create and display notification immediately
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Call Active")
            .setContentText("Tap to return to call")
            .setSmallIcon(R.drawable.baseline_add_call_24) // Make sure this exists
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        // Extract media projection data
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        // Initialize socket with room ID
        val roomId = intent?.getStringExtra("roomId") ?: "123456"
        initializeSocketHandler(roomId)

        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                // Store this for when you implement screen sharing
            } catch (e: Exception) {
                Log.e("VideoCallService", "Failed to get media projection: ${e.message}")
            }
        }

        return START_STICKY
    }

    private fun initializeSocketHandler(roomId: String) {
        socketHandler = SocketHandler(
            serviceCallback = createServiceCallback(),
            context = this,
            coroutineScope = CoroutineScope(Dispatchers.IO)
        )
        socketHandler.setupSocketConnection(roomId)
    }

    private fun createServiceCallback() = object : ServiceCallbackInterface {
        override fun addParticipant(participant: Participant) {
            _participants.update { currentList ->
                if (currentList.none { it.id == participant.id }) {
                    currentList + participant
                } else {
                    currentList
                }
            }
        }

        override fun updateParticipantVideo(id: String, videoTrack: VideoTrack?) {
            _participants.update { currentList ->
                currentList.map { participant ->
                    if (participant.id == id) participant.copy(videoTrack = videoTrack)
                    else participant
                }
            }
        }

        override fun updateParticipantAudio(id: String, audioTrack: AudioTrack?) {
            _participants.update { currentList ->
                currentList.map { participant ->
                    if (participant.id == id) participant.copy(audioTrack = audioTrack)
                    else participant
                }
            }
        }
    }

    fun getParticipants(): List<Participant> = _participants.value

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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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



    fun closeConnection() {
        socketHandler.closeConnection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}