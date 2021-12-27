package com.example.azizlumiere

import android.content.Context
import android.util.Log

class ProfileProvider(private val context: Context) {
    var activeProfile: Profile? = null
        private set

    init {
        loadSavedProfile()
    }

    fun loadSavedProfile() {
        val profileName = getActiveProfileName(context) ?: return
        val profile = ProfileManager.loadProfile(context, profileName) ?: return
        log("Active profile ${profile.name}:")
        profile.data.forEach { entry ->
            log("   ${entry.lux} ${entry.brightness}")
        }
        activeProfile = profile
    }
}