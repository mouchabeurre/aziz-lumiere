package com.example.azizlumiere

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import kotlinx.coroutines.delay

class LuminosityListener(
    private val context: Context,
    private val profileProvider: ProfileProvider
) : SensorEventListener {

    private val brightnessMapper = BrightnessMapper(profileProvider)
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val luminositySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    suspend fun setBrightnessOneShot() {
        log("set brightness oneshot")
        startSensor()
        while (brightnessMapper.getBufferCount() < 1) {
            delay(100L)
        }
        stopSensor()
        val brightness = brightnessMapper.getBufferMedian()?.brightness ?: return
        changeScreenBrightness(brightness)
    }

    suspend fun setBrightnessAverage(maxWindowMillis: Long = 1000L, maxVariance: Float = 1.5f) {
        startSensor()
        val startTime = System.currentTimeMillis()
        var currentBufferCount = 0
        var currentTime = startTime
        var currentVariance = Float.MAX_VALUE
        while (
            (currentBufferCount < 11 || currentVariance > maxVariance)
            && (currentTime - startTime <= maxWindowMillis || currentVariance > maxVariance)
        ) {
            delay(1200L)
            currentBufferCount = brightnessMapper.getBufferCount()
            currentVariance = brightnessMapper.getBufferVariance()
            currentTime = System.currentTimeMillis()
            log("buffer count: $currentBufferCount, current variance : $currentVariance (max variance: $maxVariance), time delta: ${currentTime - startTime} (maxWindow: $maxWindowMillis)")
        }
        stopSensor()
        val brightness = brightnessMapper.getBufferMedian()?.brightness ?: return
        changeScreenBrightness(brightness)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val value = event.values[0]
            val timestamp = event.timestamp
            brightnessMapper.next(value, timestamp)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //log("sensor accuracy changed")
        return
    }

    private fun startSensor() {
        log("start sensor")
        sensorManager.registerListener(
            this,
            luminositySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        brightnessMapper.resetBuffer()
    }

    private fun stopSensor() {
        log("stop sensor")
        sensorManager.unregisterListener(this)
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
}