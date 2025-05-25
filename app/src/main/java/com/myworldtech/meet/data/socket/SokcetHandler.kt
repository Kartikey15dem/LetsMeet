package com.myworldtech.meet.data.socket

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder.AudioSource
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.myworldtech.meet.data.preferences.getUserInfo
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import io.github.crow_misia.webrtc.option.MediaConstraintsOption
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.VideoTrack
import java.net.URISyntaxException
import com.myworldtech.meet.data.util.CameraCapturerFactory
import com.myworldtech.meet.data.util.JsonUtils
import com.myworldtech.meet.data.util.PeerConnectionUtils
import com.myworldtech.meet.data.util.toJsonObject
import com.myworldtech.meet.domain.service.ServiceCallbackInterface
import com.myworldtech.meet.presentation.model.Participant
import io.github.crow_misia.mediasoup.Device
import io.github.crow_misia.mediasoup.Producer
import io.github.crow_misia.mediasoup.Consumer
import io.github.crow_misia.mediasoup.RecvTransport
import io.github.crow_misia.mediasoup.SendTransport
import io.github.crow_misia.mediasoup.Transport
import io.github.crow_misia.mediasoup.createDevice
import io.github.crow_misia.webrtc.RTCComponentFactory
import io.github.crow_misia.webrtc.RTCLocalAudioManager
import io.github.crow_misia.webrtc.RTCLocalVideoManager
import io.socket.client.Ack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.webrtc.CameraVideoCapturer
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.ranges.until
import kotlin.text.ifEmpty

class SocketHandler(
    private val serviceCallback: ServiceCallbackInterface,
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private lateinit var socket: Socket
    private lateinit var mediasoupDevice: Device
    private var sendTransport: SendTransport? = null
    private var recvTransport: RecvTransport? = null
    private lateinit var componentFactory: RTCComponentFactory
    private lateinit var localAudioManager: RTCLocalAudioManager
    private lateinit var localVideoManager: RTCLocalVideoManager
    private val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
    private var cameraName = "front"
    private var isUseDataChannel = true

    private var myPeerId = ""
    private var micProducer: Producer? = null

    private var camProducer: Producer? = null
    private lateinit var camCapturer: CameraVideoCapturer
    private lateinit var  peerConnectionFactory: PeerConnectionFactory
    private lateinit var mediaConstraintsOption: MediaConstraintsOption
    private lateinit var nProducers : String
    private lateinit var videoProducerId : String
    private lateinit var audioProducerId: String
    private var isSpeakerAudio = false

    fun toggleSpeakerAudio(isEnabled: Boolean){
        isSpeakerAudio = isEnabled
    }

    fun toggleCamera(isEnabled: Boolean) {
        coroutineScope.launch {
            if (isEnabled) {
                serviceCallback.toggleParticipantVideo(myPeerId,true)
                resumeVideo()
            } else {
                serviceCallback.toggleParticipantVideo(myPeerId,false)
                pauseVideo()
            }
        }
    }

    fun toggleMic(isEnabled: Boolean) {
        coroutineScope.launch {
            if (isEnabled) {
                serviceCallback.toggleParticipantAudio(myPeerId,true)
                resumeAudio()
            } else {
                serviceCallback.toggleParticipantAudio(myPeerId,false)
                pauseAudio()
            }
        }
    }

    suspend fun setupSocketConnection(roomId: String,peerId: String,isHost: Boolean): Boolean {

            try {
                myPeerId = peerId
                initializeSocket()
                val roomData = joinRoom(roomId,isHost)
                if(roomData.has("approved")) return false
                initializeMediaComponents()
                setupMediaSoupDevice(roomData)
                createTransports()
                removeParticipant()
                enableMediaIfPossible()
                consumeRemoteProducers()
                consumeNewProducer()
                pauseProducerOfPeer()
                resumeProducerOfPeer()
                receiveMessage()
                if (isHost) handlePeerRequest()

            } catch (e: Exception) {
                Log.e("socket", "Setup failed", e)
                return false
            }
        return true
    }

    private suspend fun initializeSocket() {
        try {

            socket = IO.socket("http://192.168.29.235:3000")
            socket.connect()
            Log.d("socket", "Socket connected: ${socket.connected()}")

            // Add error handling
            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("socket", "Connection error: ${args[0]}")
            }
            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("socket", "Socket disconnected")
            }
        } catch (e: URISyntaxException) {
            Log.e("socket", "Invalid server URL", e)
        }
    }

    private suspend fun joinRoom(roomId: String,isHost: Boolean): JSONObject {
        val roomRequest = JSONObject().put("roomId", roomId)
            .put("peerId", myPeerId)
            .put("isHost", isHost)
        socket.emit("join-room", roomRequest)
        Log.d("socket", "Socket join-room: $roomRequest")
        val approved = socket.awaitEvent("join-approved")
        Log.d("socket","approved : ${approved.getBoolean("approved")}")
        return if(approved.getBoolean("approved")) socket.awaitEvent("room-joined") else approved

    }

    private fun initializeMediaComponents() {
        // Camera setup
        camCapturer =
            CameraCapturerFactory.create(
                context,
                fixedResolution = false,
                preferenceFrontCamera = "front" == cameraName
            )!!


        // Create media constraints
        mediaConstraintsOption = MediaConstraintsOption().also {
            it.enableAudioDownstream()
            it.enableAudioUpstream()
            it.videoEncoderFactory = DefaultVideoEncoderFactory(
                PeerConnectionUtils.eglContext,
                true,
                true,
            )
            it.videoDecoderFactory = DefaultVideoDecoderFactory(
                PeerConnectionUtils.eglContext,
            )
            it.enableVideoDownstream(PeerConnectionUtils.eglContext)
            it.audioSource = AudioSource.VOICE_COMMUNICATION
            camCapturer.also { capturer ->
                it.enableVideoUpstream(capturer, PeerConnectionUtils.eglContext)
            }
        }

        componentFactory = RTCComponentFactory(mediaConstraintsOption)
        peerConnectionFactory = componentFactory.createPeerConnectionFactory(context) { _, _ -> }
        localAudioManager = componentFactory.createAudioManager()!!
        localVideoManager = componentFactory.createVideoManager()!!
        mediasoupDevice = peerConnectionFactory.createDevice()
    }

    private suspend fun setupMediaSoupDevice(roomData: JSONObject) {
        val routerRtpCapabilities = roomData.getJSONObject("routerRtpCapabilities").toString()
        val producers = roomData.getJSONArray("producers")
        Log.d("socket", "myPeerId $myPeerId")
        coroutineScope.launch(Dispatchers.Main) {
            serviceCallback.addMyParticipant(Participant(id = myPeerId, name = "You", photoUrl = getUserInfo(context).second))
        }
        nProducers = producers.toString()
        val peers = roomData.getJSONObject("peers")
        val jsonArray = JSONArray()
        peers.keys().forEach { key ->
            if (peers.getBoolean(key.toString())) {
                val jsonObject = JSONObject().put("key", key).put("value", true)
                jsonArray.put(jsonObject)
            }
        }
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i) // Get each JSONObject
            val peerId = jsonObject.getString("key") // Extract the "key" field as the peer ID

            if(peerId!=myPeerId) {
                Log.d("socket", "Peer ID: $peerId")
                // Process the peer information
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.addParticipant(
                        Participant(id = peerId, name = "Peer $i")
                    )
                }
            }
        }

        withContext(Dispatchers.Default) {
            if (!mediasoupDevice.loaded) {
                mediasoupDevice.load(routerRtpCapabilities, rtcConfig)
            }
        }
    }

    private suspend fun createTransports() = withContext(Dispatchers.IO) {
        val sctpCapabilities = if (isUseDataChannel) {
            mediasoupDevice.sctpCapabilities.toJsonObject()
        } else null


        createSendTransport(sctpCapabilities)

        createRecvTransport(sctpCapabilities)

    }

    private suspend fun createSendTransport(sctpCapabilities: JSONObject?) {
        try {
            // Request transport info from server
            val transportInfo = socket.emitAndAwait("create-send-transport")
            JsonUtils.jsonPut(transportInfo, "sctpCapabilities", sctpCapabilities)

            // Extract parameters
            val id = transportInfo.getString("id")
            val iceParameters = transportInfo.getJSONObject("iceParameters").toString()
            val iceCandidates = transportInfo.getJSONArray("iceCandidates").toString()
            val dtlsParameters = transportInfo.getJSONObject("dtlsParameters")
            val sctpParameters = transportInfo.optString("sctpParameters").ifEmpty { null }

            // Create transport
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
        } catch (e: Exception) {
            Log.e("socket", "Error creating send transport", e)
            throw e
        }
    }

    private suspend fun createRecvTransport(sctpCapabilities: JSONObject?) {
        try {
            val transportInfo = socket.emitAndAwait("create-recv-transport")
            JsonUtils.jsonPut(transportInfo, "sctpCapabilities", sctpCapabilities)

            // Extract parameters (similar to createSendTransport)
            val id = transportInfo.getString("id")
            val iceParameters = transportInfo.getJSONObject("iceParameters").toString()
            val iceCandidates = transportInfo.getJSONArray("iceCandidates").toString()
            val dtlsParameters = transportInfo.getJSONObject("dtlsParameters")
            val sctpParameters = transportInfo.optString("sctpParameters").ifEmpty { null }

            // Create transport
            recvTransport = mediasoupDevice.createRecvTransport(
                listener = recvTransportListener,
                id = id,
                iceParameters = iceParameters,
                iceCandidates = iceCandidates,
                dtlsParameters = dtlsParameters.toString(),
                sctpParameters = sctpParameters,
                appData = null,
                rtcConfig = rtcConfig
            )

            Log.d("socket", "Receive transport created: ${recvTransport?.id}")
        } catch (e: Exception) {
            Log.e("socket", "Error creating receive transport", e)
            throw e
        }
    }

    private suspend fun enableMediaIfPossible() {
        if ( mediasoupDevice.loaded) {
            val canSendMic = mediasoupDevice.canProduce("audio")
            val canSendCam = mediasoupDevice.canProduce("video")

            if(canSendCam) enableCam()
            if(canSendMic) enableMic()
        }
    }

    private suspend fun enableMic() {
        micProducer?.close()
        localAudioManager.dispose()
        micProducer = null
        // Implementation for enabling microphone
        localAudioManager.initTrack(peerConnectionFactory, mediaConstraintsOption)
        val track = localAudioManager.track ?: run {

            Log.d("socket","audio track null")
            return
        }
        withContext(Dispatchers.Main) {
            serviceCallback.updateParticipantAudio(myPeerId, track)
        }
        track.setEnabled(true)
        val micProducer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    Log.d("socket","micProducer = $micProducer")
                    micProducer?.also {

                        Log.d("socket","removeProducer = ${it.id}")
                        localAudioManager.dispose()
                        micProducer = null
                    }
                }
            },
            track = track,
            encodings = emptyList(),
            codecOptions = null,
            appData = null,
        )
        this.micProducer = micProducer

        Log.d("socket","addProducer = $micProducer")
    }

    private suspend fun enableCam() {
        camProducer?.close()
        camProducer = null
        localVideoManager.dispose()
        camCapturer.stopCapture()
        // Implementation for enabling camera
        localVideoManager.initTrack(peerConnectionFactory, mediaConstraintsOption, context)
        camCapturer.startCapture(640, 480, 30)
        val track = localVideoManager.track ?: run {

            Log.d("socket","video track null")
            return
        }
        track.setEnabled(true)
        withContext(Dispatchers.Main) {
            serviceCallback.updateParticipantVideo(myPeerId, track)
        }
        val camProducer = sendTransport?.produce(
            listener = object : Producer.Listener {
                override fun onTransportClose(producer: Producer) {
                    //
                    Log.d("socket","onTransportClose(), camProducer")
                    camProducer?.also {
                        localVideoManager.dispose()
                        camCapturer.stopCapture()
                        camProducer = null
                    }
                }
            },
            track = track,
            encodings = emptyList(),
            codecOptions = null,
            appData = null,
        )
        this.camProducer = camProducer
        Log.d("socket","addProducer = $camProducer")
    }
    fun switchCamera() {
        if (::camCapturer.isInitialized) {
            camCapturer.switchCamera(null)
            cameraName = if (cameraName == "front") "back" else "front"
            Log.d("SocketHandler", "Camera switched to $cameraName")
        } else {
            Log.e("SocketHandler", "Camera capturer not initialized")
        }
    }

    private val sendTransportListener = object : SendTransport.Listener {
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            Log.d("socket", "onConnect() - send transport")
            val jsonParameters = JSONObject(dtlsParameters)
            val payload = JSONObject().put("dtlsParameters", jsonParameters)

            coroutineScope.launch {
                try {
                    socket.emitAndAwait("connect-send-transport", payload)
                    Log.d("socket", "Send transport connected")
                } catch (e: Exception) {
                    Log.e("socket", "Failed to connect send transport", e)
                }
            }
        }

        override fun onConnectionStateChange(
            transport: Transport,
            newState: String
        ) {
            Log.d("socket", "Send transport connection state: $newState")
        }

        override fun onProduce(transport: Transport, kind: String, rtpParameters: String, appData: String?): String {
            Log.d("socket", "onProduce() $kind")
            val json = JSONObject().apply {
                put("transportId", transport.id)
                put("kind", kind)
                put("rtpParameters", rtpParameters.toJsonObject())
            }

            // We need to use runBlocking here because this interface function must return synchronously
            return runBlocking {
                try {
                    val response = socket.emitAndAwait("produce", json)
                    val producerId = response.optString("id", "")
                    if(kind=="audio") audioProducerId = producerId else videoProducerId = producerId
                    producerId
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
            TODO("Not yet implemented")
        }

        // Other listener methods remain the same
    }

    private val recvTransportListener = object : RecvTransport.Listener {
        override fun onConnect(transport: Transport, dtlsParameters: String) {
            Log.d("socket", "onConnect() - receive transport")
            val jsonParameters = JSONObject(dtlsParameters)
            val payload = JSONObject().put("dtlsParameters", jsonParameters)

            coroutineScope.launch {
                try {
                    socket.emitAndAwait("connect-recv-transport", payload)
                    Log.d("socket", "Receive transport connected")
                } catch (e: Exception) {
                    Log.e("socket", "Failed to connect receive transport", e)
                }
            }
        }

        override fun onConnectionStateChange(transport: Transport, newState: String) {
            Log.d("socket", "Receive transport connection state: $newState")
        }
    }
    suspend fun Socket.emitAndAwait(event: String, data: Any? = null): JSONObject {
        Log.d("socket", "emitAndAwait() $event")
        return suspendCancellableCoroutine { continuation ->
            val ackCallback = object : Ack {
                override fun call(vararg args: Any?) {
                    try {
                        if (args.isNotEmpty() && args[0] is JSONObject) {
                            continuation.resume(args[0] as JSONObject)
                        } else {
                            // Create a default success response if server doesn't return a JSONObject
                            continuation.resume(JSONObject().put("success", true))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            Exception("Invalid response from $event event")

                        )
                        Log.d("socket", "emitAndAwait() $e")
                    }
                }
            }

            // Handle differently based on whether data is null
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
                    Log.d("socket","event:$event")
                } else {
                    continuation.resumeWithException(
                        Exception("Invalid data for $event event")
                    )
                }
            }
        }
    }
    private suspend fun consumeRemoteProducers() {
        try {
            Log.d("socket", "nProducers: $nProducers")
            val producerJsonArray = JSONArray(nProducers)

            for (i in 0 until producerJsonArray.length()) {
                val producerJson = producerJsonArray.getJSONObject(i)
                val producerId = producerJson.getString("producerId")
                consumeMedia(producerId)
            }
        } catch (e: Exception) {
            Log.e("socket", "Error consuming producers", e)
        }
    }
    private  fun consumeNewProducer() {
        try {
            Log.d("socket", "newProducers ")
            socket.on("new-producer") { args ->
                val newProducer = args[0] as JSONObject
                val newPeerId = newProducer.getString("peerId")
                if(newPeerId != myPeerId + "share"){
                    coroutineScope.launch(Dispatchers.Main) {
                        serviceCallback.addParticipant(
                            Participant(newPeerId, name = "New Peer")
                        )
                        Log.d("socket", "New producer: $newProducer")
                        consumeMedia(newProducer.getString("producerId"))
                    }
                }

            }
        }catch (e: Exception) {
            Log.e("socket", "Error consuming new producer", e)
        }

    }

    private suspend fun consumeMedia(producerId: String) {
        val rtpCapabilities = mediasoupDevice.rtpCapabilities

        val requestData = JSONObject().apply {
            put("producerId", producerId)
            put("rtpCapabilities", JSONObject(rtpCapabilities))
        }
        Log.d("socket", "Consume request: $requestData")

        try {
            val responseData = socket.emitAndAwait("consume", requestData)

            val peerId = responseData.optString("peerId")
            val id = responseData.optString("id")?:""
            val kind = responseData.optString("kind")
            val rtpParameters = responseData.optString("rtpParameters")

            val consumer = recvTransport?.consume(
                listener = object : Consumer.Listener {
                    override fun onTransportClose(consumer: Consumer) {
                        Log.d("socket", "onTransportClose for consumer: ${consumer.id}")
                    }
                },
                id = id,
                producerId = producerId,
                kind = kind,
                rtpParameters = rtpParameters,
                appData = null
            ) ?: return


            if ("video" == consumer.kind) {
                val videoTrack = consumer.track as VideoTrack
                withContext(Dispatchers.Main) {
                    videoTrack.let { serviceCallback.updateParticipantVideo(peerId, it) }
                    serviceCallback.updateVideoConsumer(id)
                }
            }else {
                val audioTrack = consumer.track as AudioTrack
                withContext(Dispatchers.Main) {
                    audioTrack.setEnabled(isSpeakerAudio)
                    audioTrack.let { serviceCallback.updateParticipantAudio(peerId, it) }
                    serviceCallback.updateAudioConsumer(id)
                }
            }
        } catch (e: Exception) {
            Log.e("socket", "Error consuming media: ${e.message}")
        }
    }
    private fun pauseVideo(){
        val producerId = JSONObject().put("producerId", videoProducerId)
        socket.emit("pause-producer",producerId)
    }
    private fun resumeVideo(){
        val producerId = JSONObject().put("producerId", videoProducerId)
        socket.emit("resume-producer",producerId)
    }
    private fun pauseAudio(){
        val producerId = JSONObject().put("producerId", audioProducerId)
        socket.emit("pause-producer",producerId)
    }
    private fun resumeAudio(){
        val producerId = JSONObject().put("producerId", audioProducerId)
        socket.emit("resume-producer",producerId)
    }
    private fun pauseProducerOfPeer(){
        try{
            socket.on("producer-paused") { args ->
                val json = args[0] as JSONObject
                val peerId = json.getString("peerId")
                val kind = json.getString("kind")
                coroutineScope.launch(Dispatchers.Main) {

                    if(kind == "audio") {
                        val data = JSONObject().put("consumerId",serviceCallback.getAudioConsumer(peerId))
                        socket.emit("pause-consumer",data)
                        Log.d("socket","Consumer paused $data")
                        serviceCallback.toggleParticipantAudio(peerId, false)
                    } else {
                        val data = JSONObject().put("consumerId",serviceCallback.getVideoConsumer(peerId))
                        socket.emit("pause-consumer",data)
                        serviceCallback.toggleParticipantVideo(peerId,false)
                    }
                }
            }
        }catch (e: Exception) {
            Log.e("socket", "Error pausing producer: ${e.message}")
        }
    }
    private fun resumeProducerOfPeer(){
        try{
            socket.on("producer-resumed") { args ->
                val json = args[0] as JSONObject
                val peerId = json.getString("peerId")
                val kind = json.getString("kind")
                coroutineScope.launch(Dispatchers.Main) {
                    if(kind == "audio") {
                        val data = JSONObject().put("consumerId",serviceCallback.getAudioConsumer(peerId))
                        socket.emit("resume-consumer",data)
                        Log.d("socket","Consumer resumed $data")
                        serviceCallback.toggleParticipantAudio(peerId, true)
                    } else {
                        val data = JSONObject().put("consumerId",serviceCallback.getVideoConsumer(peerId))
                        socket.emit("resume-consumer",data)
                        serviceCallback.toggleParticipantVideo(peerId,true)
                    }
                }
            }
        }catch (e: Exception) {
            Log.e("socket", "Error resuming producer: ${e.message}")
        }
    }
    fun sendMessage(message: String){
        val message = JSONObject().put("text",message)
        try{
            socket.emit("message",message)
            Log.d("socket", "Message: $message")
        }catch (e: Exception) {
            Log.e("socket", "Error sending message: ${e.message}")
        }
    }
    private fun receiveMessage() {
        try {
            socket.on("receive-message") { args ->
                val data = args[0] as JSONObject
                val senderPeerId = data.getString("peerId")
                val text = data.getString("text")
                Log.d("socket", "Message received : $text")
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.addMessage(text, senderPeerId)
                }
            }
        }catch (e: Exception) {
                Log.e("socket", "Error receiving message: ${e.message}")
            }

    }
    private  fun removeParticipant(){
        try{
            socket.on("peer-disconnected") { args ->
                val data = args[0] as JSONObject
                coroutineScope.launch(Dispatchers.Main) {
                    serviceCallback.removeParticipant(data.getString("peerId"))
                }
            }
        }catch (e: Exception) {
            Log.e("socket", "Error getting disconnected peer", e)
        }
    }
    private fun handlePeerRequest(){
        Log.d("socket", "ask ")
        socket.on("ask-to-join") { args ->
            try {
                Log.d("socket", "asking ")
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val requesterPeerId = data.getString("requesterPeerId")
                    val requesterSocketId = data.getString("requesterSocketId")
                    serviceCallback.handlePeerRequest(requesterPeerId,requesterSocketId)
                    Log.d("socket","handlePeerRequest : $requesterPeerId")
                }
            } catch (e: Exception) {
                Log.e("SocketError", "Error handling ask-to-join: ${e.localizedMessage}", e)
            }
        }
    }
    fun approvePeer(approved: Boolean,requesterSocketId:String){
        val response = JSONObject().apply {
            put("approved", approved)
            put("to", requesterSocketId)
        }
        Log.d("socket","hRequest : $approved")
        socket.emit("ask-to-join-response", response)
    }




    fun closeConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendTransport?.close()
                recvTransport?.close()
                socket.disconnect()
            } catch (e: Exception) {
                Log.e("socket", "Error during endCall: ${e.message}")
            } finally {
                sendTransport = null
                recvTransport = null
                socket.close()
                Log.d("socket", "Connection fully closed")
            }
        }
    }

}

