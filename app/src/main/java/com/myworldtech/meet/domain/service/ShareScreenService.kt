package com.myworldtech.meet.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.myworldtech.meet.MainActivity
import com.myworldtech.meet.R
import com.myworldtech.meet.data.socket.ShareScreenSocketHandler
import com.myworldtech.meet.data.socket.SocketHandler
import com.myworldtech.meet.domain.service.VideoCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

class ShareScreenService : Service() {
    private lateinit var socketHandler: ShareScreenSocketHandler
    private val binder = ShareScreenBinder()

   val isScreenSharing = MutableStateFlow(false)

    companion object {
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "ShareScreenChannel"
    }

    inner class ShareScreenBinder : Binder() {
        fun getService(): ShareScreenService = this@ShareScreenService
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        return START_STICKY
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startScreenSharing(resultCode: Int, data: Intent,roomId: String,peerId:String) {
        Log.d("screenS", "startScreenSharing")
        try {
//            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            isScreenSharing.value = true
            socketHandler = ShareScreenSocketHandler(
                context = this,
                coroutineScope = CoroutineScope(Dispatchers.IO),
                mediaProjectionData = data
            )
            socketHandler.setupSocketConnection(roomId.trim(),peerId)
        } catch (e: Exception) {
            Log.e("screen", "Failed to start screen sharing: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun stopScreenSharing() {

        try {

            if (::socketHandler.isInitialized) {
                socketHandler.closeConnection()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isScreenSharing.value = false
        } catch (e: Exception) {
            Log.e("VideoCallService", "Failed to stop screen sharing: ${e.message}")
        }
    }




    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Share Screen Service",
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
            .setContentTitle("Screen Sharing")
            .setContentText("You are sharing your screen")
            .setSmallIcon(R.drawable.screencast)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

}