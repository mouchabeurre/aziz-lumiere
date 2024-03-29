package com.example.azizlumiere

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.example.azizlumiere.UserPreferencesRepository.Companion.userPreferencesStore
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
                    it.setOnBindEditTextListener { editText ->
                        editText.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    }
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toLongOrNull()?.let { value ->
                            if (value > 0) {
                                lifecycleScope.launch {
                                    userPreferencesRepository.setMainJobInterval(value)
                                }
                            } else {
                                showValidationError(
                                    getString(R.string.main_job_interval_preference_validation_positive)
                                )
                            }
                        } ?: run {
                            showValidationError(
                                getString(R.string.main_job_interval_preference_validation_type)
                            )
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
                    it.setOnBindEditTextListener { editText ->
                        editText.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    }
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toIntOrNull()?.let { value ->
                            if (value > 0) {
                                lifecycleScope.launch {
                                    userPreferencesRepository.setBufferSize(value)
                                }
                            } else {
                                showValidationError(
                                    getString(R.string.buffer_size_preference_validation_positive)
                                )
                            }
                        } ?: run {
                            showValidationError(
                                getString(R.string.buffer_size_preference_validation_type)
                            )
                        }
                        false
                    }
                }
            val maxStdDevMinIlluminationPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.max_std_dev_at_min_illumination_key))
                ?.also {
                    it.setOnBindEditTextListener { editText ->
                        editText.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    }
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toFloatOrNull()?.let { value ->
                            if (value >= 0) {
                                lifecycleScope.launch {
                                    userPreferencesRepository.setMinStdDevAtMinIllumination(value)
                                }
                            } else {
                                showValidationError(
                                    getString(R.string.max_std_dev_at_min_illumination_positive)
                                )
                            }
                        } ?: run {
                            showValidationError(
                                getString(R.string.max_std_dev_at_min_illumination_type)
                            )
                        }
                        false
                    }
                }
            val maxStdDevMaxIlluminationPreference = preferenceScreen
                .findPreference<EditTextPreference>(getString(R.string.max_std_dev_at_max_illumination_key))
                ?.also {
                    it.setOnBindEditTextListener { editText ->
                        editText.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    }
                    it.setOnPreferenceChangeListener { _, newValue ->
                        (newValue as String).toFloatOrNull()?.let { value ->
                            if (value >= 0) {

                                lifecycleScope.launch {
                                    userPreferencesRepository.setMaxStdDevAtMaxIllumination(
                                        value
                                    )
                                }
                            } else {
                                showValidationError(
                                    getString(R.string.max_std_dev_at_max_illumination_positive)
                                )
                            }
                        } ?: run {
                            showValidationError(
                                getString(R.string.max_std_dev_at_max_illumination_type)
                            )
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
                    maxStdDevMinIlluminationPreference?.text =
                        userPreferences.maxStdDevAtMinIllumination.toString()
                    maxStdDevMaxIlluminationPreference?.text =
                        userPreferences.maxStdDevAtMaxIllumination.toString()
                    log("updated preference screen preferences")
                }
            }
        }

        private fun showValidationError(message: String) {
            val errorMessage = "${getString(R.string.preference_validation_error)}: $message"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}