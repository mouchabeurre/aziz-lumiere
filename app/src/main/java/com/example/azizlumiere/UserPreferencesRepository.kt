package com.example.azizlumiere

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

private const val DATA_STORE_FILE_NAME = "user_prefs.proto"

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
            fileName = DATA_STORE_FILE_NAME,
            serializer = UserPreferencesSerializer
        )
    }

    suspend fun setActiveProfile(profileName: String) {
        context.userPreferencesStore.updateData { store ->
            log("updating store profile with $profileName")
            store.toBuilder()
                .setActiveProfile(profileName)
                .build()
        }
    }

    suspend fun setMainJobInterval(interval: Long) {
        context.userPreferencesStore.updateData { store ->
            log("updating store main job interval with $interval")
            store.toBuilder()
                .setMainJobInterval(interval)
                .build()
        }
    }

    suspend fun setAggregateSensorValues(aggregate: Boolean) {
        context.userPreferencesStore.updateData { store ->
            log("updating store aggregate sensor values with $aggregate")
            store.toBuilder()
                .setAggregateSensorValues(aggregate)
                .build()
        }
    }

    suspend fun setBufferSize(size: Int) {
        context.userPreferencesStore.updateData { store ->
            log("updating store buffer size with $size")
            store.toBuilder()
                .setBufferSize(size)
                .build()
        }
    }

    suspend fun setMinAggregationWindow(interval: Long) {
        context.userPreferencesStore.updateData { store ->
            log("updating store min aggregation window with $interval")
            store.toBuilder()
                .setMinAggregationWindow(interval)
                .build()
        }
    }

    suspend fun setBaseStandardDeviation(threshold: Float) {
        context.userPreferencesStore.updateData { store ->
            log("updating store max std deviation with $threshold")
            store.toBuilder()
                .setBaseStandardDeviation(threshold)
                .build()
        }
    }

    suspend fun setExtraStandardDeviation(margin: Float) {
        context.userPreferencesStore.updateData { store ->
            log("updating store std deviation fluctuation margin with $margin")
            store.toBuilder()
                .setExtraStandardDeviation(margin)
                .build()
        }
    }
}