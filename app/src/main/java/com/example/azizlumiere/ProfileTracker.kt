package com.example.azizlumiere

import android.content.Context
import android.content.SharedPreferences

private const val name = "ACTIVE_PROFILE_KEY"
private const val key = "ACTIVE_PROFILE_NAME"

fun setActiveProfileName(context: Context, profile: Profile) {
    val sharedPrefs = getPreferences(context)
    log("saving active profile to ${profile.name}")
    sharedPrefs.edit().let {
        it.putString(key, profile.name)
        it.apply()
    }
}

fun getActiveProfileName(context: Context): String? {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getString(key, null)
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}
