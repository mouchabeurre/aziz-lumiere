package com.example.azizlumiere

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlin.math.*

private const val MIN_BRIGHTNESS = 0f
private const val MAX_BRIGHTNESS = 255f
private const val MIN_LUMINOSITY = 0f
private const val MAX_LUMINOSITY = 15000f

class BrightnessManager(
    private val context: Context,
    private val profileProvider: ProfileProvider
) {
    private var lastSensorValue = 0f
    private var lastOutputValue = 0f

    fun next(rawValue: Float) {
        val value = max(min(rawValue, MAX_LUMINOSITY), MIN_LUMINOSITY)
        val profile = profileProvider.activeProfile ?: return
        val lowerBound = profile.data.findLast { it.lux < value } ?: return
        val upperBound = profile.data.reversed().findLast { it.lux >= value } ?: return
        val fraction = (value - lowerBound.lux) / (upperBound.lux - lowerBound.lux)
        val projection =
            lowerBound.brightness + (upperBound.brightness - lowerBound.brightness) * fraction
        changeScreenBrightness(
            max(min(projection, MAX_BRIGHTNESS), MIN_BRIGHTNESS).toInt()
        )
    }

    private fun changeScreenBrightness(brightness: Int) {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Log.d("MY LOG", "setting brightness to $brightness")
        Settings.System.putInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness
        )
    }
}