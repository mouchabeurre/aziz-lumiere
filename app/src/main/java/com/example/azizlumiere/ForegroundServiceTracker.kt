package com.example.azizlumiere

import android.content.Context
import android.content.SharedPreferences

enum class ForegroundServiceState {
    STARTED,
    STOPPED,
}

private const val name = "FOREGROUND_SERVICE_KEY"
private const val key = "FOREGROUND_SERVICE_STATE"

fun setServiceState(context: Context, state: ForegroundServiceState) {
    val sharedPrefs = getPreferences(context)
    log("setting service state to ${state.name}")
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ForegroundServiceState? {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ForegroundServiceState.STOPPED.name) ?: return null
    return ForegroundServiceState.valueOf(value)
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}