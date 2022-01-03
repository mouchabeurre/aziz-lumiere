package com.example.azizlumiere

import android.content.*
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.IntentFilter
import kotlinx.coroutines.Job

class QuickTileService : TileService() {
    private var isTileStarted = false
    private var updatesReceiver: BroadcastReceiver? = null

    private enum class TileState {
        ACTIVE,
        INACTIVE,
        UNAVAILABLE
    }

    private inner class UpdatesReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            log("tile receiver got $intent")
            intent?.action?.let {
                when (it) {
                    ForegroundServiceBroadcastActions.STARTED.name -> setTile(TileState.ACTIVE)
                    ForegroundServiceBroadcastActions.STOPPED.name -> setTile(TileState.INACTIVE)
                }
            }
        }
    }

    private fun setTile(state: TileState) {
        if (isTileStarted) {
            when (state) {
                TileState.ACTIVE -> qsTile.state = Tile.STATE_ACTIVE
                TileState.INACTIVE -> qsTile.state = Tile.STATE_INACTIVE
                TileState.UNAVAILABLE -> qsTile.state = Tile.STATE_UNAVAILABLE
            }
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        log("tile clicked")
        val serviceStarted = getServiceState(this) == ForegroundServiceState.STARTED
        setTile(TileState.UNAVAILABLE)
        Intent(this, ForegroundService::class.java).also {
            it.action =
                if (serviceStarted) ForegroundServiceActions.STOP.name else ForegroundServiceActions.START.name
            startForegroundService(it)
            return
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        log("tile service started listening")
        isTileStarted = true

        val filter = IntentFilter().also {
            it.addAction(ForegroundServiceBroadcastActions.STARTED.name)
            it.addAction(ForegroundServiceBroadcastActions.STOPPED.name)
        }
        updatesReceiver = UpdatesReceiver()
        registerReceiver(updatesReceiver, filter)

        if (getServiceState(this) == ForegroundServiceState.STARTED) {
            qsTile.state = Tile.STATE_ACTIVE
        } else {
            qsTile.state = Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        log("tile service stopped listening")
        unregisterReceiver(updatesReceiver)
        isTileStarted = false
    }
}