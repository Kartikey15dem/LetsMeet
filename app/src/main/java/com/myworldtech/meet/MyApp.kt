package com.myworldtech.meet

import android.app.Application
import android.util.Log
import io.github.crow_misia.mediasoup.MediasoupClient
import io.github.crow_misia.webrtc.log.LogHandler
import org.webrtc.Logging

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        MediasoupClient.initialize(
            context = this,
            logHandler = object : LogHandler {
                override fun log(
                    priority: Int, tag: String?,
                    t: Throwable?,
                    message: String?,
                    vararg args: Any?,
                ) {
                    Log.d("tim", "message = $message args = $args priority = $priority tag = $tag")
                }
            },
            loggableSeverity = Logging.Severity.LS_INFO,
        )
    }
}