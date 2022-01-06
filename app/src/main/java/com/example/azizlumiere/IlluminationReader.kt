package com.example.azizlumiere

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class IlluminationReader(
    context: Context,
    private val onEvent: (event: SensorEvent) -> Unit,
) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    fun start() {
        log("start sensor")
        sensorManager.registerListener(
            this,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        log("stop sensor")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { onEvent(it) }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }


}