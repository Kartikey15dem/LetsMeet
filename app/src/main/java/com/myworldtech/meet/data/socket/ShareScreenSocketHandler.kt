package com.myworldtech.meet.data.socket

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import com.myworldtech.meet.data.util.JsonUtils
import com.myworldtech.meet.data.util.PeerConnectionUtils
import com.myworldtech.meet.data.util.toJsonObject
import io.github.crow_misia.webrtc.option.MediaConstraintsOption
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.webrtc.*
import io.github.crow_misia.mediasoup.*
import io.github.crow_misia.webrtc.RTCComponentFactory
import io.github.crow_misia.webrtc.RTCLocalAudioManager
import io.github.crow_misia.webrtc.RTCLocalVideoManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class ShareScreenSocketHandler(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val mediaProjectionData: Intent, // Pass this from your service/activity
) {
    private var socket: Socket? = null
    private lateinit var mediasoupDevice: Device
    private var sendTransport: SendTransport? = null
    private val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
    private var myPeerId = ""

    private var micProducer: Producer? = null
    private var screenProducer: Producer? = null

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var mediaConstraintsOption: MediaConstraintsOption
    private lateinit var componentFactory: RTCComponentFactory

    // WebRTC objects for screen capture
    private lateinit var screenCapturer: VideoCapturer
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null

    fun setupSocketConnection(roomId: String, peerId:String) = coroutineScope.launch {
        myPeerId = peerId + "share"
        try {
            initializeSocket()
            val roomData = joinRoom("023")
            initializeMediaComponents()
            setupMediaSoupDevice(roomData)
            createTransports()
            enableMediaIfPossible()
        } catch (e: Exception) {
            Log.e("socket", "Setup failed", e)
        }
    }

    private suspend fun initializeSocket() {
        socket = IO.socket("http://192.168.29.235:3000")

        socket?.connect()
        Log.d("socket", "Socket connected: ${socket?.connected()}")
        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("socket", "Connection error: ${args[0]}")
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d("socket", "Socket disconnected")
        }
    }

    private suspend fun joinRoom(roomId: String): JSONObject {
        val roomRequest = JSONObject().put("roomId", roomId)
            .put("peerId", myPeerId)
        socket?.emit("join-room", roomRequest)
        Log.d("socket", "Socket join-room: $roomRequest")
        return socket?.awaitEvent("room-joined") ?: JSONObject()
    }

    private fun initializeMediaComponents() {
        // Create PeerConnectionFactory and component factory
        screenCapturer = createScreenCapturer()
        mediaConstraintsOption = MediaConstraintsOption().also {
            it.enableAudioUpstream()
            it.enableVideoUpstream(screenCapturer, null)
        }
        componentFactory = RTCComponentFactory(mediaConstraintsOption)
        peerConnectionFactory = componentFactory.createPeerConnectionFactory(context) { _, _ -> }
        mediasoupDevice = peerConnectionFactory.createDevice()
    }

    private suspend fun setupMediaSoupDevice(roomData: JSONObject) {
        val routerRtpCapabilities = roomData.getJSONObject("routerRtpCapabilities").toString()

        withContext(Dispatchers.Default) {
            if (!mediasoupDevice.loaded) {
                mediasoupDevice.load(routerRtpCapabilities, rtcConfig)
            }
        }
    }

    private suspend fun createTransports() = withContext(Dispatchers.IO) {
        val sctpCapabilities = mediasoupDevice.sctpCapabilities.toJsonObject()
        createSendTransport(sctpCapabilities)
    }

    private suspend fun createSendTransport(sctpCapabilities: JSONObject?) {
        val transportInfo = socket?.emitAndAwait("create-send-transport") ?: return
        JsonUtils.jsonPut(transportInfo, "sctpCapabilities", sctpCapabilities)
        val id = transportInfo.getString("id")
        val iceParameters = transportInfo.getJSONObject("iceParameters").toString()
        val iceCandidates = transportInfo.getJSONArray("iceCandidates").toString()
        val dtlsParameters = transportInfo.getJSONObject("dtlsParameters")
        val sctpParameters = transportInfo.optString("sctpParameters").ifEmpty { null }
        sendTransport = mediasoupDevice.createSendTransport(
            listener = sendTransportListener,
            id = id,
            iceParameters = iceParameters,
            iceCandidates = iceCandidates,
            dtlsParameters = dtlsParameters.toString(),
            sctpParameters = sctpParameters,
            appData = null,
            rtcConfig = rtcConfig
        )
        Log.d("socket", "Send transport created: ${sendTransport?.id}")
    }

    private fun enableMediaIfPossible() {
        if (mediasoupDevice.loaded) {
            val canSendMic = mediasoupDevice.canProduce("audio")
            val canSendScreen = mediasoupDevice.canProduce("video")
            if (canSendScreen) enableScreen()
//            if (canSendMic) enableAudio()
        }
    }

    // --- Screen sharing video track ---
    private  fun enableScreen() {

        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", PeerConnectionUtils.eglContext)
        videoSource = peerConnectionFactory.createVideoSource(false)
        screenCapturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        screenCapturer.startCapture(640, 480, 30)
        val videoTrack = peerConnectionFactory.createVideoTrack("SCREEN_VIDEO_TRACK", videoSource)

        // Send video track as producer
        val producer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    videoSource?.dispose()
                    surfaceTextureHelper?.dispose()
                    screenCapturer.stopCapture()
                    screenProducer = null
                }
            },
            track = videoTrack,
            encodings = emptyList(),
            codecOptions = null,
            appData = null,
        )
        screenProducer = producer
        Log.d("socket", "Screen producer created: $producer")
    }

    private fun createScreenCapturer(): VideoCapturer {
        // Use ScreenCapturerAndroid for screen sharing
        val callback = object : MediaProjection.Callback() {}
        return ScreenCapturerAndroid(mediaProjectionData, callback)
    }

    // --- Audio track for screen sharing (mic/system audio) ---
    private fun enableAudio() {
        // For mic audio (default)
        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val audioTrack = peerConnectionFactory.createAudioTrack("SCREEN_AUDIO_TRACK", audioSource)

        // For system audio (Android 10+), use AudioPlaybackCaptureConfiguration (not shown here)

        val producer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    audioSource?.dispose()
                    micProducer = null
                }
            },
            track = audioTrack,
            encodings = emptyList(),
            codecOptions = null,
            appData = null,
        )
        micProducer = producer
        Log.d("socket", "Audio producer created: $producer")
    }

    private val sendTransportListener = object : SendTransport.Listener {
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            val jsonParameters = JSONObject(dtlsParameters)
            val payload = JSONObject().put("dtlsParameters", jsonParameters)
            coroutineScope.launch {
                try {
                    socket?.emitAndAwait("connect-send-transport", payload)
                    Log.d("socket", "Send transport connected")
                } catch (e: Exception) {
                    Log.e("socket", "Failed to connect send transport", e)
                }
            }
        }
        override fun onConnectionStateChange(transport: Transport, newState: String) {
            Log.d("socket", "Send transport connection state: $newState")
        }
        override fun onProduce(transport: Transport, kind: String, rtpParameters: String, appData: String?): String {
            val json = JSONObject().apply {
                put("transportId", transport.id)
                put("kind", kind)
                put("rtpParameters", rtpParameters.toJsonObject())
            }
            return kotlinx.coroutines.runBlocking {
                try {
                    val response = socket?.emitAndAwait("produce", json)
                    response?.optString("id", "") ?: ""
                } catch (e: Exception) {
                    Log.e("socket", "Failed to produce", e)
                    ""
                }
            }
        }
        override fun onProduceData(
            transport: Transport,
            sctpStreamParameters: String,
            label: String,
            protocol: String,
            appData: String?
        ): String {
            return ""
        }
    }

    suspend fun Socket.emitAndAwait(event: String, data: Any? = null): JSONObject {
        return suspendCancellableCoroutine { continuation ->
            val ackCallback = object : io.socket.client.Ack {
                override fun call(vararg args: Any?) {
                    try {
                        if (args.isNotEmpty() && args[0] is JSONObject) {
                            continuation.resume(args[0] as JSONObject)
                        } else {
                            continuation.resume(JSONObject().put("success", true))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(Exception("Invalid response from $event event"))
                    }
                }
            }
            if (data == null) {
                emit(event, ackCallback)
            } else {
                emit(event, data, ackCallback)
            }
        }
    }
    suspend fun Socket.awaitEvent(event: String): JSONObject {
        return suspendCancellableCoroutine { continuation ->
            once(event) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    continuation.resume(args[0] as JSONObject)
                    Log.d("socket", "event:$event")
                } else {
                    continuation.resumeWithException(Exception("Invalid data for $event event"))
                }
            }
        }
    }

    fun closeConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendTransport?.close()
                socket?.disconnect()
            } catch (e: Exception) {
                Log.e("socket", "Error during endCall: ${e.message}")
            } finally {
                sendTransport = null
                socket?.close()
                Log.d("socket", "Connection fully closed")
            }
        }
    }
}

