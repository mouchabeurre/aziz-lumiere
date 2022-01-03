package com.example.azizlumiere

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ForegroundServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context) == ForegroundServiceState.STARTED) {
            Intent(context, ForegroundService::class.java).also {
                it.action = ForegroundServiceActions.START.name
                log("Starting the service from a BroadcastReceiver")
                context.startForegroundService(it)
                return
            }
        }
    }
}