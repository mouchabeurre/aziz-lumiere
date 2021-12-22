package com.example.azizlumiere

import android.content.Context
import android.util.Log

class ProfileProvider(private val context: Context) {
    var activeProfile: Profile? = null
        private set
    var oneshot = false

    fun changeActiveProfile(profile: Profile?) {
        activeProfile = profile
        if (!oneshot && profile != null) {
            profile.data.forEach { entry ->
                Log.d("MY LOG", "${entry.lux} ${entry.brightness}")
            }
            oneshot = true
        }
    }
}