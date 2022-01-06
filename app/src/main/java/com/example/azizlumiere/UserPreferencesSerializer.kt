package com.example.azizlumiere

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferences> {
    //override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()
    override val defaultValue: UserPreferences = UserPreferences.newBuilder()
        .setMainJobInterval(5000)
        .setAggregateSensorValues(true)
        .setBufferSize(6)
        .setMinAggregationWindow(1000)
        .setBaseStandardDeviation(1.5f)
        .setExtraStandardDeviation(4f)
        .build()

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            return UserPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: UserPreferences, output: OutputStream) = t.writeTo(output)
}

