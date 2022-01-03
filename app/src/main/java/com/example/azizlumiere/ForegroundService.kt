package com.example.azizlumiere

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.*

private const val MAIN_JOB_DELAY = 5000L

class ForegroundService : Service() {
    private val screenIntentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
        addAction(Intent.ACTION_SCREEN_ON)
    }
    private val localBinder = LocalBinder()
    private var isServiceStarted = false
    private var mainJob: Job? = null
    private var lastWakeUpOneShot: Long? = null
    private lateinit var screenReceiver: ScreenReceiver
    private lateinit var brightnessManager: BrightnessManager
    private lateinit var profileProvider: ProfileProvider

    private fun startService() {
        if (isServiceStarted) return
        log("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ForegroundServiceState.STARTED)
        registerReceiver(screenReceiver, screenIntentFilter)

        sendBroadcast(Intent().also {
            it.action = ForegroundServiceBroadcastActions.STARTED.name
        })

        mainJob = GlobalScope.launch(Dispatchers.IO) {
            coroutineScope {
                try {
                    while (isServiceStarted) {
                        lastWakeUpOneShot?.let {
                            val timeDelta = System.currentTimeMillis() - it
                            if (timeDelta < MAIN_JOB_DELAY) {
                                log("offsetting current loop by ${MAIN_JOB_DELAY - timeDelta}ms")
                                delay(MAIN_JOB_DELAY - timeDelta)
                            }
                        }
                        if (screenReceiver.isScreenOn) {
                            withContext(Dispatchers.IO) {
                                brightnessManager.setBrightnessAverage()
                            }
                        }
                        delay(MAIN_JOB_DELAY)
                    }
                } finally {
                    brightnessManager.onCancel()
                }
            }
        }
    }

    private fun stopService() {
        if (!isServiceStarted) return
        log("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        mainJob?.let {
            log("cancelling main loop job")
            it.cancel()
        }
        try {
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ForegroundServiceState.STOPPED)
        unregisterReceiver(screenReceiver)

        sendBroadcast(Intent().also {
            it.action = ForegroundServiceBroadcastActions.STOPPED.name
        })

    }

    private fun createNotification(): Notification {
        val notificationChannelId = "AZIZ LUMIERE SERVICE CHANNEL"
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

    inner class LocalBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        log("Some component want to bind with the service")
        return localBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        intent?.let {
            val action = it.action
            log("using an intent with action $action")
            when (action) {
                ForegroundServiceActions.START.name -> startService()
                ForegroundServiceActions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
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
        val restartServiceIntent = Intent(applicationContext, ForegroundService::class.java).also {
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
}
