package com.example.azizlumiere

import android.content.Context

class ProfileProvider(private val context: Context) {
    var activeProfile: Profile? = null
        private set

    fun loadSavedProfile(profileName: String) {
        val profile = ProfileManager.loadProfile(context, profileName) ?: return
        log("Active profile ${profile.name}:")
        profile.data.forEach { entry ->
            log("   ${entry.illumination} ${entry.brightness}")
        }
        activeProfile = profile
    }
}