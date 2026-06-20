package com.hambalapps.expressivebox.vpn

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

import com.hambalapps.expressivebox.data.SettingsManager
import kotlinx.coroutines.flow.first

class VpnQuickSettingsTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            VpnServiceWrapper.vpnState.collectLatest { state ->
                updateTileState(state)
            }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTileState(state: String) {
        val tile = qsTile ?: return
        when (state) {
            "CONNECTED" -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "ExpressiveBox: Secured"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connected"
                }
            }
            "CONNECTING" -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ExpressiveBox: Connecting"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connecting..."
                }
            }
            "DISCONNECTING" -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ExpressiveBox: Disconnecting"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnecting..."
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ExpressiveBox VPN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentState = VpnServiceWrapper.vpnState.value
        if (currentState == "CONNECTED" || currentState == "CONNECTING") {
            stopVpnService()
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val settingsManager = SettingsManager(applicationContext)
        serviceScope.launch {
            val currentSettings = withContext(Dispatchers.IO) {
                settingsManager.settings.first()
            }
            val intent = Intent(this@VpnQuickSettingsTileService, VpnServiceWrapper::class.java).apply {
                action = VpnServiceWrapper.ACTION_START
                putExtra("active_profile", currentSettings.activeProfile)
                putExtra("show_live_notification", currentSettings.showLiveNotification)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, VpnServiceWrapper::class.java).apply {
            action = VpnServiceWrapper.ACTION_STOP
        }
        startService(intent)
    }
}
