package com.example.azizlumiere

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.example.azizlumiere.UserPreferencesRepository.Companion.userPreferencesStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        userPreferencesRepository = UserPreferencesRepository(this)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(userPreferencesRepository))
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment(
        private val userPreferencesRepository: UserPreferencesRepository
    ) : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val mainJobIntervalPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.main_job_interval_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toLongOrNull()?.let { value ->
                            lifecycleScope.launch {
                                userPreferencesRepository.setMainJobInterval(value)
                            }
                        }
                        false
                    }
                }
            val aggregateSensorValuesPreference = preferenceScreen
                .findPreference<SwitchPreference>(getString(R.string.aggregate_sensor_values_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        if (newValue is Boolean) {
                            lifecycleScope.launch {
                                userPreferencesRepository.setAggregateSensorValues(newValue)
                            }
                        }
                        false
                    }
                }
            val bufferSizePreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.buffer_size_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toIntOrNull()?.let { value ->
                            lifecycleScope.launch {
                                userPreferencesRepository.setBufferSize(value)
                            }
                        }
                        false
                    }
                }
            val minAggregationWindowPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.min_aggregation_window_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toLongOrNull()?.let { value ->
                            lifecycleScope.launch {
                                userPreferencesRepository.setMinAggregationWindow(value)
                            }
                        }
                        false
                    }
                }
            val maxStandardDeviationPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.max_standard_deviation_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toFloatOrNull()?.let { value ->
                            lifecycleScope.launch {
                                userPreferencesRepository.setMaxStandardDeviation(value)
                            }
                        }
                        false
                    }
                }
            val standardDeviationFluctuationMarginPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.standard_deviation_fluctuation_margin_preference_key))
                ?.also {
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toFloatOrNull()?.let { value ->
                            lifecycleScope.launch {
                                userPreferencesRepository.setStandardDeviationFluctuationMargin(
                                    value
                                )
                            }
                        }
                        false
                    }
                }

            lifecycleScope.launch {
                context?.userPreferencesStore?.data?.collect { userPreferences ->
                    mainJobIntervalPreference?.text = userPreferences.mainJobInterval.toString()
                    aggregateSensorValuesPreference?.isChecked =
                        userPreferences.aggregateSensorValues
                    bufferSizePreference?.text = userPreferences.bufferSize.toString()
                    minAggregationWindowPreference?.text =
                        userPreferences.minAggregationWindow.toString()
                    maxStandardDeviationPreference?.text =
                        userPreferences.maxStandardDeviation.toString()
                    standardDeviationFluctuationMarginPreference?.text =
                        userPreferences.standardDeviationFluctuationMargin.toString()
                    log("updated preference screen preferences")
                }
            }

        }
    }
}