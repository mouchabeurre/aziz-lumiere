package com.example.azizlumiere

import android.content.Context
import android.hardware.SensorEvent
import android.provider.Settings
import kotlinx.coroutines.delay

private const val BUFFER_SIZE = 6
private const val MAX_AGGREGATION_WINDOW = 1000L
private const val MAX_STANDARD_DEVIATION = 1.5f
private const val EXTRA_STANDARD_DEVIATION = 4f

class BrightnessManager(
    profileProvider: ProfileProvider,
    private val context: Context,
) {

    private val brightnessMapper = BrightnessMapper(BUFFER_SIZE, profileProvider)
    private val luminosityReader = LuminosityReader(context, ::onLuminosityEvent)

    private fun onLuminosityEvent(event: SensorEvent) {
        val value = event.values[0]
        val timestamp = event.timestamp
        brightnessMapper.next(value, timestamp)
    }

    private fun startAggregation() {
        brightnessMapper.resetBuffer()
        luminosityReader.start()
    }

    private fun stopAggregation() {
        luminosityReader.stop()
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
        val brightness = brightnessMapper.getBufferMedian(currentBuffer)?.brightness ?: return
        changeScreenBrightness(brightness)
    }

    suspend fun setBrightnessAverage(
        maxWindowMillis: Long = MAX_AGGREGATION_WINDOW,
        maxDeviation: Float = MAX_STANDARD_DEVIATION
    ) {
        startAggregation()
        val startTime = System.currentTimeMillis()
        var currentBufferCount = 0
        var currentTime = startTime
        var currentDeviation = MAX_STANDARD_DEVIATION
        var currentExtraDeviationCoef = 0f
        while (
            (currentBufferCount < BUFFER_SIZE || currentDeviation > maxDeviation + currentExtraDeviationCoef * EXTRA_STANDARD_DEVIATION)
            && (currentTime - startTime <= maxWindowMillis || currentDeviation > maxDeviation + currentExtraDeviationCoef * EXTRA_STANDARD_DEVIATION)
        ) {
            delay(1200L)
            val currentBuffer = brightnessMapper.currentBuffer()
            currentBufferCount = currentBuffer.size
            currentDeviation = brightnessMapper.getBufferStandardDeviation(currentBuffer)
            currentExtraDeviationCoef = brightnessMapper.getBufferScaleToMaxValues(currentBuffer)
            currentTime = System.currentTimeMillis()

            val bufferLog = "buffer count: $currentBufferCount"
            val deviationLog = "current deviation: $currentDeviation (max: $maxDeviation)"
            val extraCoefLog =
                "extra coef: $currentExtraDeviationCoef (${currentExtraDeviationCoef * EXTRA_STANDARD_DEVIATION})"
            val timeDeltaLog =
                "time delta: ${currentTime - startTime} (maxWindow: $maxWindowMillis)"
            log("$bufferLog, $deviationLog, $extraCoefLog, $timeDeltaLog")
        }
        val currentBuffer = brightnessMapper.currentBuffer()
        stopAggregation()
        val brightness = brightnessMapper.getBufferMedian(currentBuffer)?.brightness ?: return
        changeScreenBrightness(brightness)
    }
}