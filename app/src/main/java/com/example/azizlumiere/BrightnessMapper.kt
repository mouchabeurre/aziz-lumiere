package com.example.azizlumiere

import kotlin.math.*

private const val MIN_BRIGHTNESS = 0f
private const val MAX_BRIGHTNESS = 255f
private const val MIN_LUMINOSITY = 0f
private const val MAX_LUMINOSITY = 15000f

class BrightnessMapper(private val profileProvider: ProfileProvider) {
    private val brightnessBuffer: Array<BrightnessEvent?> = Array(11) { null }

    fun resetBuffer() {
        for (i in brightnessBuffer.indices) {
            brightnessBuffer[i] = null
        }
    }

    fun getBufferCount(): Int {
        return getBufferValues().size
    }

    fun next(eventValue: Float, eventTime: Long) {
        val brightness = brightnessFromLuminosity(eventValue) ?: return
        upsertBuffer(BrightnessEvent(brightness, eventTime))
    }

    fun getBufferMedian(): BrightnessEvent? {
        val bufferValues = getBufferValues()
        if (bufferValues.isEmpty()) {
            return null
        }
        return bufferValues.sortedBy { it.brightness }[bufferValues.size / 2]
    }

    fun getBufferVariance(): Float {
        val brightnessValues = getBufferValues()
            .map { it.brightness.toFloat() }
        val mean = brightnessValues.sum() / getBufferValues().size
        val sumOfSquare = brightnessValues.map { (it - mean).pow(2) }.sum()
        val yes = brightnessBuffer.joinToString(", ") { "${it?.brightness}"}
        log("buffer content: $yes")
        return sqrt(sumOfSquare / getBufferValues().size)
    }

    private fun getBufferValues(): List<BrightnessEvent> {
        return brightnessBuffer.filterNotNull()
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
}