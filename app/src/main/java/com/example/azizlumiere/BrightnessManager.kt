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
        brightnessMapper.getBufferMedian(currentBuffer)?.let { brightnessEvent ->
            changeScreenBrightness(brightnessEvent.brightness)
        }
    }

    suspend fun setBrightnessAverage() {
        startAggregation()
        val minAggregationWindow = config.minAggregationWindow
        val maxStandardDeviation = config.baseStandardDeviation
        val fluctuationMargin = config.extraStandardDeviation
        val bufferSize = config.bufferSize
        val startTime = System.currentTimeMillis()
        var currentBufferCount = 0
        var currentTime = startTime
        var currentDeviation = maxStandardDeviation + 1
        var fluctuationRatio = 0f
        while (
            (currentBufferCount < bufferSize || currentDeviation > maxStandardDeviation + fluctuationRatio * fluctuationMargin)
            && (currentTime - startTime <= minAggregationWindow || currentDeviation > maxStandardDeviation + fluctuationRatio * fluctuationMargin)
        ) {
            delay(1200L)
            val currentBuffer = brightnessMapper.currentBuffer()
            if (currentBuffer.isNotEmpty()) {
                currentBufferCount = currentBuffer.size
                currentDeviation = brightnessMapper.getBufferStandardDeviation(currentBuffer)
                fluctuationRatio =
                    brightnessMapper.getBufferScaleToMaxValues(currentBuffer)
            }
            currentTime = System.currentTimeMillis()

            val bufferLog = "buffer count: $currentBufferCount"
            val deviationLog = "current deviation: $currentDeviation (max: $maxStandardDeviation)"
            val extraCoefLog =
                "extra coef: $fluctuationRatio (${fluctuationRatio * fluctuationMargin})"
            val timeDeltaLog =
                "time delta: ${currentTime - startTime} (maxWindow: $minAggregationWindow)"
            log("$bufferLog, $deviationLog, $extraCoefLog, $timeDeltaLog")
        }
        val currentBuffer = brightnessMapper.currentBuffer()
        stopAggregation()
        brightnessMapper.getBufferMedian(currentBuffer)?.let { brightnessEvent ->
            changeScreenBrightness(brightnessEvent.brightness)
        }
    }

    fun onCancel() {
        log("brightness manager onCancel hook")
        stopAggregation()
    }
}