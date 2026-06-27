package com.hambalapps.expressivebox.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.data.deserializeSubscriptions
import com.hambalapps.expressivebox.data.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PREV_NODE = "com.hambalapps.expressivebox.vpn.ACTION_PREV_NODE"
        const val ACTION_NEXT_NODE = "com.hambalapps.expressivebox.vpn.ACTION_NEXT_NODE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_PREV_NODE || action == ACTION_NEXT_NODE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context.applicationContext)
                    
                    val subscriptionListStr = settingsManager.subscriptionList.first()
                    val activeSubId = settingsManager.activeSubId.first()
                    val activeProfile = settingsManager.activeProfile.first()
                    
                    val subscriptions = deserializeSubscriptions(subscriptionListStr)
                    val activeSub = subscriptions.find { it.id == activeSubId }
                        ?: if (activeSubId == "manual") {
                            val manualStr = settingsManager.manualServers.first()
                            Subscription(id = "manual", name = "Manual / Custom Configs", url = "local://manual", servers = manualStr)
                        } else {
                            subscriptions.firstOrNull()
                        }
                    
                    val serverList = activeSub?.servers?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
                    if (serverList.size > 1) {
                        val currentIndex = serverList.indexOf(activeProfile)
                        val nextIndex = when (action) {
                            ACTION_NEXT_NODE -> {
                                if (currentIndex == -1) 0 else (currentIndex + 1) % serverList.size
                            }
                            else -> { // ACTION_PREV_NODE
                                if (currentIndex == -1) serverList.size - 1 else (currentIndex - 1 + serverList.size) % serverList.size
                            }
                        }
                        val nextProfile = serverList[nextIndex]
                        settingsManager.setActiveProfile(nextProfile)
                        
                        VpnServiceWrapper.log("Notification action: Switched node to ${nextProfile.substringBefore("://")}")
                        
                        // Restart/reload the VPN service to apply the change
                        val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                            this.action = VpnServiceWrapper.ACTION_START
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(startIntent)
                        } else {
                            context.startService(startIntent)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExpressiveBox", "Failed to cycle node: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
