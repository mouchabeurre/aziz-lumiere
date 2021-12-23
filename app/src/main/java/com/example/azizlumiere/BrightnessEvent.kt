package com.example.azizlumiere

private const val DEFAULT_BRIGHTNESS = 4

data class BrightnessEvent(val brightness: Int, val timestamp: Long) {
    constructor() : this(DEFAULT_BRIGHTNESS, System.currentTimeMillis())
}