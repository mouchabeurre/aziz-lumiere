package com.example.azizlumiere

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.*

class EndlessService : Service() {

    private val screenIntentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
        addAction(Intent.ACTION_SCREEN_ON)
    }
    private var isServiceStarted = false
    private var mainLoop: Job? = null
    private var lastWakeUpOneShot: Long? = null
    private lateinit var screenReceiver: ScreenReceiver
    private lateinit var brightnessManager: BrightnessManager
    private lateinit var profileProvider: ProfileProvider

    override fun onBind(intent: Intent): IBinder? {
        log("Some component want to bind with the service")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                ServiceActions.START.name -> startService()
                ServiceActions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log("with a null intent. It has been probably restarted by the system.")
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        profileProvider = ProfileProvider(this)
        brightnessManager = BrightnessManager(profileProvider, this)
        screenReceiver = ScreenReceiver(true, {
            GlobalScope.launch(Dispatchers.IO) {
                brightnessManager.setBrightnessOneShot()
                lastWakeUpOneShot = System.currentTimeMillis()
            }
        })
        log("The service has been created".uppercase())
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent =
            PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT)
        applicationContext.getSystemService(Context.ALARM_SERVICE)
        val alarmService: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)
        registerReceiver(screenReceiver, screenIntentFilter)
        val mainLoopDelay = 5000L

        mainLoop = GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                lastWakeUpOneShot?.let {
                    val timeDelta = System.currentTimeMillis() - it
                    if (timeDelta < mainLoopDelay) {
                        log("offsetting current loop by ${mainLoopDelay - timeDelta}ms")
                        delay(mainLoopDelay - timeDelta)
                    }
                }
                runBlocking {
                    if (screenReceiver.isScreenOn) {
                        brightnessManager.setBrightnessAverage()
                    }
                }
                delay(mainLoopDelay)
            }
            log("End of the loop for the service")
        }
    }

    private fun stopService() {
        log("Stopping the foreground service")
        mainLoop?.let {
            log("cancelling main loop job")
            it.cancel()
        }
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
        unregisterReceiver(screenReceiver)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "AZIZ LUMIERE SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notificationChannelId,
            "Aziz Lumière Service notifications channel",
            NotificationManager.IMPORTANCE_LOW
        ).let {
            it.description = "Aziz Lumière Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val builder: Notification.Builder = Notification.Builder(
            this,
            notificationChannelId
        )

        return builder
            .setContentTitle("Aziz Lumière Service")
            .setContentText("Automatic brightness service currently running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setTicker("Ticker text")
            .build()
    }
}
