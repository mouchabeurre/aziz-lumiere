package com.example.azizlumiere

import kotlin.math.*

private const val MIN_BRIGHTNESS = 0f
private const val MAX_BRIGHTNESS = 255f
private const val MIN_ILLUMINATION = 0f
private const val MAX_ILLUMINATION = 15000f

class BrightnessMapper(bufferSize: Int, private val profile: Profile) {
    private val brightnessBuffer: Array<BrightnessEvent?> = Array(bufferSize) { null }

    fun resetBuffer() {
        for (i in brightnessBuffer.indices) {
            brightnessBuffer[i] = null
        }
    }

    fun next(eventValue: Float, eventTime: Long) {
        val brightness = brightnessFromIllumination(eventValue) ?: return
        upsertBuffer(BrightnessEvent(brightness, eventValue, eventTime))
    }

    fun getBufferMedian(buffer: List<BrightnessEvent>): BrightnessEvent? {
        if (buffer.isEmpty()) {
            return null
        }
        return buffer.sortedBy { it.brightness }[buffer.size / 2]
    }

    fun getBufferStandardDeviation(buffer: List<BrightnessEvent>): Float {
        val brightnessValues = buffer
            .map { it.brightness.toFloat() }
        val mean = brightnessValues.sum() / buffer.size
        val sumOfSquare = brightnessValues.map { (it - mean).pow(2) }.sum()
        val content = currentBuffer().joinToString(", ") { "[${it.brightness} ${it.illumination}]" }
        log("buffer content: $content")
        return sqrt(sumOfSquare / buffer.size)
    }

    fun getBufferScaleToMaxValues(buffer: List<BrightnessEvent>): Float {
        val brightnessSum = buffer.map { it.brightness }.sum()
        return brightnessSum / (MAX_BRIGHTNESS * buffer.size)
    }

    fun currentBuffer(): List<BrightnessEvent> {
        return brightnessBuffer.filterNotNull()
    }

    private fun brightnessFromIllumination(luminosity: Float): Int? {
        val value = max(min(luminosity, MAX_ILLUMINATION), MIN_ILLUMINATION)
        val lowerBound = profile.data.findLast { it.illumination <= value } ?: return null
        val upperBound =
            profile.data.reversed().findLast { it.illumination >= value } ?: return null
        var fraction = 0f
        if (lowerBound != upperBound) {
            fraction =
                (value - lowerBound.illumination) / (upperBound.illumination - lowerBound.illumination)
        }
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