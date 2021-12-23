package com.example.azizlumiere

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.util.*
import kotlin.math.*

private const val MIN_BRIGHTNESS = 0f
private const val MAX_BRIGHTNESS = 255f
private const val MIN_LUMINOSITY = 0f
private const val MAX_LUMINOSITY = 15000f

class BrightnessManager(
    private val context: Context,
    private val profileProvider: ProfileProvider
) {
    private val brightnessBuffer = Array(11) { BrightnessEvent() }

    private var updateTimer: Timer? = null

    fun next(eventValue: Float, eventTime: Long) {
        val brightness = brightnessFromLuminosity(eventValue) ?: return
        upsertBuffer(BrightnessEvent(brightness, eventTime))
        val variance = getBufferVariance()
        Log.d("MY LOG", "variance is $variance")
    }

    fun startTimer() {
        if (updateTimer == null) {
            updateTimer = Timer().also { timer ->
                timer.scheduleAtFixedRate(UpdateBrightnessTask(), 0, 2000)
            }
        }
    }

    fun stopTimer() {
        updateTimer?.cancel()
        updateTimer = null
    }

    private fun brightnessFromLuminosity(luminosity: Float): Int? {
        val value = max(min(luminosity, MAX_LUMINOSITY), MIN_LUMINOSITY)
        val profile = profileProvider.activeProfile ?: return null
        val lowerBound = profile.data.findLast { it.lux < value } ?: return null
        val upperBound = profile.data.reversed().findLast { it.lux >= value } ?: return null
        val fraction = (value - lowerBound.lux) / (upperBound.lux - lowerBound.lux)
        val projection =
            lowerBound.brightness + (upperBound.brightness - lowerBound.brightness) * fraction
        return max(min(projection, MAX_BRIGHTNESS), MIN_BRIGHTNESS).toInt()
    }

    private fun upsertBuffer(value: BrightnessEvent) {
        for (i in (brightnessBuffer.size - 2) downTo 0) {
            brightnessBuffer[i + 1] = brightnessBuffer[i]
        }
        brightnessBuffer[0] = value
    }

    private fun getBufferMedian(): BrightnessEvent {
        return brightnessBuffer.asIterable().sortedBy { it.brightness }[brightnessBuffer.size / 2]
    }

    private fun getBufferVariance(): Float {
        val brightnessValues = brightnessBuffer
            .map { it.brightness.toFloat() }
        val mean = brightnessValues.sum() / brightnessBuffer.size
        val sumOfSquare = brightnessValues.map { (it - mean).pow(2) }.sum()
        return sqrt(sumOfSquare / brightnessBuffer.size)
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

    private inner class UpdateBrightnessTask : TimerTask() {
        override fun run() {
            Log.d("MY LOG", "in task ${brightnessBuffer.joinToString(", ") { "${it.brightness}" }}")
            if (getBufferVariance() < 2) {
                changeScreenBrightness(getBufferMedian().brightness)
            }
        }
    }
}