package com.myworldtech.meet.data.util

import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

object PeerConnectionUtils {
    private val eglBase = EglBase.create()
    internal val eglContext: EglBase.Context = eglBase.eglBaseContext

}
