package com.example.azizlumiere

import android.content.Context
import android.hardware.SensorEvent
import android.provider.Settings
import kotlinx.coroutines.delay

class BrightnessManager(
    activeProfile: Profile,
    private val config: UserPreferences,
    private val context: Context,
) {
    private val illuminationReader = IlluminationReader(context, ::onIlluminationEvent)
    private val brightnessMapper = BrightnessMapper(config.bufferSize, activeProfile)

    private fun onIlluminationEvent(event: SensorEvent) {
        val value = event.values[0]
        val timestamp = event.timestamp
        brightnessMapper.next(value, timestamp)
    }

    private fun startAggregation() {
        brightnessMapper.resetBuffer()
        illuminationReader.start()
    }

    private fun stopAggregation() {
        illuminationReader.stop()
    }

    private fun changeScreenBrightness(brightness: Int) {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        log("setting brightness to $brightness")
        Settings.System.putInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness
        )
    }

    suspend fun setBrightnessOneShot() {
        log("set brightness oneshot")
        startAggregation()
        while (brightnessMapper.currentBuffer().isEmpty()) {
            delay(100L)
        }
        val currentBuffer = brightnessMapper.currentBuffer()
        stopAggregation()
        brightnessMapper.getBufferMean(currentBuffer)?.let { brightness ->
            changeScreenBrightness(brightness)
        }
    }

    suspend fun setBrightnessAverage() {
        startAggregation()
        val stdDevAtMinIllumination = config.maxStdDevAtMinIllumination
        val stdDevAtMaxIllumination = config.maxStdDevAtMaxIllumination - stdDevAtMinIllumination
        val bufferSize = config.bufferSize
        val startTime = System.currentTimeMillis()
        val defaultCurrentStdDev = stdDevAtMaxIllumination + 1
        val defaultMaxStdDev = 0f
        var currentBuffer = brightnessMapper.currentBuffer()
        var currentBufferCount = currentBuffer.size
        var currentTime = startTime
        var currentStdDev = defaultCurrentStdDev
        var maxStdDev = defaultMaxStdDev
        while (currentBufferCount < bufferSize || currentStdDev > maxStdDev) {
            delay(1200L)
            currentBuffer = brightnessMapper.currentBuffer()
            currentBufferCount = currentBuffer.size
            currentStdDev = brightnessMapper.getBufferStandardDeviation(currentBuffer) ?: stdDevAtMaxIllumination + 1
            maxStdDev = brightnessMapper.getBufferMaxStandardDeviation(currentBuffer, stdDevAtMinIllumination, stdDevAtMaxIllumination) ?: 0f
            currentTime = System.currentTimeMillis()

            val bufferLog = "buffer count: $currentBufferCount"
            val deviationLog = "current deviation: $currentStdDev (max: $maxStdDev)"
            val timeDeltaLog =
                "time delta: ${currentTime - startTime}"
            log("$bufferLog, $deviationLog, $timeDeltaLog")
        }
        stopAggregation()
        brightnessMapper.getBufferMean(currentBuffer)?.let { brightness ->
            changeScreenBrightness(brightness)
        }
    }

    fun onCancel() {
        log("brightness manager onCancel hook")
        stopAggregation()
    }
}