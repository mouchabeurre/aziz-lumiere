package com.example.azizlumiere

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, SensorEventListener {

    private lateinit var profileManager: ProfileManager
    private lateinit var profileProvider: ProfileProvider
    private lateinit var brightnessManager: BrightnessManager

    private lateinit var sensorManager: SensorManager
    private var luminositySensor: Sensor? = null
    var sensorPaused = true

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasWriteSettingsPermission()) {
            changeWriteSettingsPermission()

        }

        profileManager = ProfileManager(this)
        profileProvider = ProfileProvider(this)
        brightnessManager = BrightnessManager(this, profileProvider)

        val importProfileButton: Button = findViewById(R.id.importProfileButton)
        importProfileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }
            startActivityForResult(intent, 1)
        }

        val toggleSensorButton: Button = findViewById(R.id.toggleSensorButton)
        toggleSensorButton.setOnClickListener {
            sensorPaused = if (sensorPaused) {
                Log.d("MY LOG", "resumed sensor")
                super.onResume()
                sensorManager.registerListener(
                    this,
                    luminositySensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                false
            } else {
                Log.d("MY LOG", "paused sensor")
                super.onPause()
                sensorManager.unregisterListener(this)
                true
            }
        }

        val profileSelectorSpinner: Spinner = findViewById(R.id.profileSelectorSpinner)
        profileSelectorSpinner.onItemSelectedListener = this

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        luminositySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        updateLoadedProfiles()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                result?.data?.let {
                    val profile = profileManager.addProfile(it)
                    if (profile != null) {
                        updateLoadedProfiles()
                        Toast
                            .makeText(this, "Imported ${profile.name}", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast
                            .makeText(this, "Couldn't import profile", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        Log.d("MY LOG", "didn't click spinner item")
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        Log.d("MY LOG", "clicked item $p2: ${p0?.getItemAtPosition(p2)}")
        if (p0 != null) {
            val profile = profileManager.profiles.get(p0.getItemAtPosition(p2))
            profileProvider.changeActiveProfile(profile)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("MY LOG", "accuracy changed")
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0 != null) {
            val value = p0.values[0]
            Log.d("MY LOG", "light sensor reading $value")
            brightnessManager.next(value)
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
