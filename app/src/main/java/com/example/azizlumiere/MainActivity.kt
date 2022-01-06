package com.example.azizlumiere

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.azizlumiere.UserPreferencesRepository.Companion.userPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var profileManager: ProfileManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private val profilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val profile = profileManager.addProfile(uri)
                    if (profile != null) {
                        loadProfileSpinner()
                        Toast
                            .makeText(
                                this,
                                "Imported ${profile.name}",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    } else {
                        Toast
                            .makeText(
                                this,
                                "Couldn't import profile",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        }

    private var serviceStarted = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasWriteSettingsPermission()) {
            changeWriteSettingsPermission()
        }

        userPreferencesRepository = UserPreferencesRepository(this)
        profileManager = ProfileManager(this)
        serviceStarted = getServiceState(this) == ForegroundServiceState.STARTED
        log("serviceStarted=$serviceStarted")

        findViewById<Button>(R.id.importProfileButton).let {
            it.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                }
                profilePickerLauncher.launch(intent)
            }
        }

        findViewById<Button>(R.id.toggleSensorButton).let {
            val button = it
            val startLabel = "start service"
            val stopLabel = "stop service"
            button.text = if (serviceStarted) stopLabel else startLabel
            it.setOnClickListener {
                serviceStarted = if (serviceStarted) {
                    actionOnService(ForegroundServiceActions.STOP)
                    button.text = startLabel
                    false
                } else {
                    actionOnService(ForegroundServiceActions.START)
                    button.text = stopLabel
                    true
                }
            }
        }

        findViewById<Button>(R.id.reloadServiceButton).let {
            it.setOnClickListener {
                actionOnService(ForegroundServiceActions.RELOAD_CONFIG)
            }
        }

        val profileSelectorSpinner = findViewById<Spinner>(R.id.profileSelectorSpinner).also {
            it.onItemSelectedListener = this
        }

        findViewById<Button>(R.id.openSettingsButton).let {
            it.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        loadProfileSpinner()

        lifecycleScope.launch {
            userPreferencesStore.data.collect { userPreferences ->
                val adapter = profileSelectorSpinner.adapter as ArrayAdapter<String>
                profileSelectorSpinner.setSelection(adapter.getPosition(userPreferences.activeProfile))
            }
        }
    }

    private fun actionOnService(action: ForegroundServiceActions) {
        if (getServiceState(this) == ForegroundServiceState.STOPPED && action == ForegroundServiceActions.STOP) return
        Intent(this, ForegroundService::class.java).also {
            it.action = action.name
            startForegroundService(it)
            return
        }
    }

    private fun loadProfileSpinner() {
        profileManager.loadProfiles()
        val loadedProfiles = profileManager.profiles
        val keys = loadedProfiles.keys.toList()

        lifecycleScope.launch {
            userPreferencesStore.data.first().let { userPreferences ->
                val profileSelectorSpinner: Spinner = findViewById(R.id.profileSelectorSpinner)
                ArrayAdapter(this@MainActivity, R.layout.support_simple_spinner_dropdown_item, keys)
                    .also { adapter ->
                        profileSelectorSpinner.adapter = adapter
                        if (userPreferences.activeProfile.isNotEmpty()) {
                            profileSelectorSpinner.setSelection(adapter.getPosition(userPreferences.activeProfile))
                        } else {
                            keys.getOrNull(0)?.let {
                                userPreferencesRepository.setActiveProfile(it)
                            }
                        }
                    }
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {
        log("didn't click spinner item")
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, index: Int, p3: Long) {
        log("clicked item $index: ${adapterView?.getItemAtPosition(index)}")
        adapterView?.let {
            profileManager.profiles[it.getItemAtPosition(index)]?.let { profile ->
                lifecycleScope.launch {
                    userPreferencesStore.data.first().let { userPreferences ->
                        if (userPreferences.activeProfile != profile.name) {
                            userPreferencesRepository.setActiveProfile(profile.name)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(this)
    }

    private fun changeWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(intent)
    }
}
