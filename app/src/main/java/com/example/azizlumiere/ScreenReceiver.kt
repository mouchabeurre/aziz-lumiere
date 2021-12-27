package com.example.azizlumiere

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenReceiver(
    isScreenOnDefault: Boolean,
    private val screenOnCallback: (() -> Unit)? = null,
    private val screenOffCallback: (() -> Unit)? = null
) : BroadcastReceiver() {
    var isScreenOn = isScreenOnDefault
        private set

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_ON) {
            log("screen is ON")
            screenOnCallback?.let {
                log("calling screen ON callback")
                it()
            }
            isScreenOn = true
        } else if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            log("screen is OFF")
            screenOffCallback?.let {
                log("calling screen OFF callback")
                it()
            }
            isScreenOn = false
        }
    }
}