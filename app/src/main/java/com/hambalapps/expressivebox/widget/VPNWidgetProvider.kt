package com.hambalapps.expressivebox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.hambalapps.expressivebox.R
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VPNWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.hambalapps.expressivebox.widget.ACTION_TOGGLE"
        const val ACTION_STATE_CHANGED = "com.hambalapps.expressivebox.widget.ACTION_STATE_CHANGED"
        
        @Volatile
        private var lastVpnState = "DISCONNECTED"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        if (action == ACTION_TOGGLE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context.applicationContext)
                    val activeProfile = settingsManager.activeProfile.first()
                    val showLiveNotification = settingsManager.showLiveNotification.first()
                    
                    val toggleIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                        action = if (lastVpnState == "CONNECTED") {
                            VpnServiceWrapper.ACTION_STOP
                        } else {
                            VpnServiceWrapper.ACTION_START
                        }
                        putExtra("active_profile", activeProfile)
                        putExtra("show_live_notification", showLiveNotification)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(toggleIntent)
                    } else {
                        context.startService(toggleIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExpressiveBox", "Widget failed to toggle VPN: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_STATE_CHANGED) {
            val state = intent.getStringExtra("state") ?: "DISCONNECTED"
            lastVpnState = state
            
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VPNWidgetProvider::class.java))
            updateAllWidgets(context, manager, ids)
        }
    }

    private fun updateAllWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsManager = SettingsManager(context.applicationContext)
                val activeProfile = settingsManager.activeProfile.first()
                
                val stateText = when (lastVpnState) {
                    "CONNECTED" -> "SECURED"
                    "CONNECTING" -> "SHIELD ACTIVE..."
                    "DISCONNECTING" -> "DISCONNECTING..."
                    else -> "UNPROTECTED"
                }

                val statusColor = when (lastVpnState) {
                    "CONNECTED" -> 0xFF624FBE.toInt()
                    "CONNECTING" -> 0xFFD03A60.toInt()
                    else -> 0xFF808080.toInt()
                }

                val nodeName = if (activeProfile.isNotEmpty()) {
                    val hashIdx = activeProfile.indexOf("#")
                    if (hashIdx >= 0) {
                        try {
                            java.net.URLDecoder.decode(activeProfile.substring(hashIdx + 1), "UTF-8")
                        } catch (e: Exception) {
                            activeProfile.substring(hashIdx + 1)
                        }
                    } else {
                        val clean = activeProfile.substringAfter("://").substringBefore("@").substringBefore(":")
                        if (clean.length > 24) clean.take(24) + "..." else clean
                    }
                } else {
                    "No active node selected"
                }

                for (id in ids) {
                    val views = RemoteViews(context.packageName, R.layout.vpn_widget)
                    views.setTextViewText(R.id.widget_status, stateText)
                    views.setTextColor(R.id.widget_status, statusColor)
                    views.setTextViewText(R.id.widget_node_name, nodeName)
                    
                    val buttonBgColor = when (lastVpnState) {
                        "CONNECTED" -> 0xFF624FBE.toInt()
                        "CONNECTING" -> 0xFFD03A60.toInt()
                        else -> 0xFF353A42.toInt()
                    }
                    views.setInt(R.id.widget_button_toggle, "setBackgroundColor", buttonBgColor)

                    val toggleIntent = Intent(context, VPNWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE
                    }
                    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pi = PendingIntent.getBroadcast(context, 0, toggleIntent, flag)
                    views.setOnClickPendingIntent(R.id.widget_button_toggle, pi)

                    manager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpressiveBox", "Widget update failed: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
