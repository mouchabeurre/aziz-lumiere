package com.example.azizlumiere

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var profileManager: ProfileManager
    private lateinit var profileProvider: ProfileProvider
    private val profilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val profile = profileManager.addProfile(uri)
                    if (profile != null) {
                        updateLoadedProfiles()
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

        profileManager = ProfileManager(this)
        profileProvider = ProfileProvider(this)
        serviceStarted = getServiceState(this) == ServiceState.STARTED
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
                    actionOnService(ServiceActions.STOP)
                    button.text = startLabel
                    false
                } else {
                    actionOnService(ServiceActions.START)
                    button.text = stopLabel
                    true
                }
            }
        }

        findViewById<Spinner>(R.id.profileSelectorSpinner).let {
            it.onItemSelectedListener = this
        }

        updateLoadedProfiles()
    }

    private fun actionOnService(action: ServiceActions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == ServiceActions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            startForegroundService(it)
            return
        }
    }

    private fun updateLoadedProfiles() {
        profileManager.loadProfiles()
        val loadedProfiles = profileManager.profiles
        val keys = loadedProfiles.keys.toList()

        val profileSelectorSpinner: Spinner = findViewById(R.id.profileSelectorSpinner)
        ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, keys)
            .also { adapter ->
                //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                profileSelectorSpinner.adapter = adapter
            }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        log("didn't click spinner item")
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        log("clicked item $p2: ${p0?.getItemAtPosition(p2)}")
        if (p0 != null) {
            val profile = profileManager.profiles[p0.getItemAtPosition(p2)] ?: return
            setActiveProfileName(this, profile)
            profileProvider.loadSavedProfile()
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
