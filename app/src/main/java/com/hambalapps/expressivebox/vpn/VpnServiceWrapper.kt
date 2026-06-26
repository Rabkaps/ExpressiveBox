package com.hambalapps.expressivebox.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.hambalapps.expressivebox.MainActivity
import com.hambalapps.expressivebox.R
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.ui.main.NodesPopupActivity
import com.hambalapps.expressivebox.data.deserializeSubscriptions
import com.hambalapps.expressivebox.data.Subscription
import com.hambalapps.expressivebox.Config
import io.nekohasekai.libbox.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.nio.charset.StandardCharsets

class VpnServiceWrapper : VpnService(), PlatformInterface, CommandServerHandler {

    companion object {
        const val ACTION_START = "com.hambalapps.expressivebox.START"
        const val ACTION_STOP = "com.hambalapps.expressivebox.STOP"
        private const val CHANNEL_ID = "vpn_service_channel_v2"
        private const val NOTIFICATION_ID = 101

        private val _vpnState = MutableStateFlow("DISCONNECTED")
        val vpnState: StateFlow<String> = _vpnState

        private val _vpnLogs = MutableStateFlow("")
        val vpnLogs: StateFlow<String> = _vpnLogs
        var debugLogFile: File? = null

        fun log(message: String) {
            android.util.Log.i("ExpressiveBox", message)
            debugLogFile?.let { file ->
                try {
                    file.appendText(message + "\n")
                } catch (e: Exception) {}
            }
            val combined = _vpnLogs.value + message + "\n"
            _vpnLogs.value = if (combined.length > 15000) {
                combined.takeLast(10000).substringAfter("\n", "")
            } else {
                combined
            }
        }

        fun clearLogs() {
            _vpnLogs.value = ""
        }

        fun checkAndLoadCrashLog(context: android.content.Context) {
            var logText = ""
            
            val crashFile = File(context.cacheDir, "crash.log")
            if (crashFile.exists()) {
                logText += "--- PREVIOUS JVM CRASH LOG ---\n" + crashFile.readText() + "\n------------------------------\n"
                try {
                    crashFile.delete()
                } catch (e: Exception) {}
            }

            val logDir = File(context.cacheDir, "logs")
            val errLogFile = File(logDir, "stderr.log")
            if (errLogFile.exists()) {
                val stderrText = errLogFile.readText()
                if (stderrText.trim().isNotEmpty()) {
                    logText += "--- PREVIOUS NATIVE LOG/CRASH ---\n" + stderrText + "\n---------------------------------\n"
                }
                try {
                    errLogFile.delete()
                } catch (e: Exception) {}
            }

            val crashReportFile = File(context.filesDir, "CrashReport-stderr.log")
            if (crashReportFile.exists()) {
                val stderrText = crashReportFile.readText()
                if (stderrText.trim().isNotEmpty()) {
                    logText += "--- PREVIOUS NATIVE LOG/CRASH ---\n" + stderrText + "\n---------------------------------\n"
                }
                try {
                    crashReportFile.delete()
                } catch (e: Exception) {}
            }

            if (logText.isNotEmpty()) {
                _vpnLogs.value = logText
            }
        }
    }

    private var commandServer: CommandServer? = null
    private var tunFd: ParcelFileDescriptor? = null
    private var tunFdInt: Int = -1
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logReaderJob: Job? = null
    private var defaultInterfaceListener: InterfaceUpdateListener? = null
    private var defaultNetworkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var lastSentPhysicalName: String? = null
    private var lastSentPhysicalIndex: Int = -1
    private var isForeground = false

    private var splitTunnelingEnabledVal = false
    private var splitTunnelingModeVal = "bypass"
    private var splitTunnelingAppsVal = emptySet<String>()
    private var showLiveNotificationVal = false
    private var activeProfileVal = ""

    override fun onCreate() {
        super.onCreate()
        debugLogFile = File(cacheDir, "app_debug.log").apply {
            try {
                if (exists()) delete()
                createNewFile()
            } catch (e: Exception) {}
        }
        android.util.Log.i("ExpressiveBox", "VpnServiceWrapper onCreate called")
        createNotificationChannel()
        
        serviceScope.launch {
            _vpnState.collect { state ->
                if (isForeground) {
                    updateNotification(state)
                }
            }
        }

        serviceScope.launch {
            val settingsManager = SettingsManager(applicationContext)
            settingsManager.settings.collect { settings ->
                showLiveNotificationVal = settings.showLiveNotification
                activeProfileVal = settings.activeProfile
                if (isForeground) {
                    updateNotification(_vpnState.value)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        android.util.Log.i("ExpressiveBox", "VpnServiceWrapper onStartCommand called with action: $action")
        if (intent != null) {
            if (intent.hasExtra("active_profile")) {
                activeProfileVal = intent.getStringExtra("active_profile") ?: ""
            }
            if (intent.hasExtra("show_live_notification")) {
                showLiveNotificationVal = intent.getBooleanExtra("show_live_notification", false)
            }
        }
        if (action == ACTION_START) {
            if (_vpnState.value == "CONNECTED") {
                reloadVpnEngine()
            } else {
                _vpnState.value = "CONNECTING"
                startForegroundServiceNotification()
                startVpnEngine()
            }
        } else if (action == ACTION_STOP) {
            stopVpnEngine()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(state: String): android.app.Notification {
        val settingsManager = SettingsManager(applicationContext)
        val nodeName = if (activeProfileVal.isEmpty()) {
            getString(R.string.notif_no_node)
        } else if (activeProfileVal.startsWith("{")) {
            getString(R.string.notif_custom)
        } else {
            ProxyNameResolver.getProxyName(activeProfileVal, applicationContext)
        }
        
        val contentText = when (state) {
            "CONNECTED" -> getString(R.string.notif_connected, nodeName)
            "CONNECTING" -> getString(R.string.notif_connecting, nodeName)
            "DISCONNECTING" -> getString(R.string.notif_disconnecting)
            else -> getString(R.string.notif_disconnected)
        }
        
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Prev Node Intent
        val prevIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_PREV_NODE
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            prevIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Next Node Intent
        val nextIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NEXT_NODE
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // List Intent targeting translucent Dialog NodesPopupActivity
        val listIntent = Intent(applicationContext, NodesPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val listPendingIntent = PendingIntent.getActivity(
            applicationContext,
            4,
            listIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Disconnect Intent
        val stopIntent = Intent(applicationContext, VpnServiceWrapper::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            applicationContext,
            3,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            getString(R.string.notif_action_prev),
            prevPendingIntent
        ).build()
        
        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            getString(R.string.notif_action_next),
            nextPendingIntent
        ).build()
        
        val disconnectAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notif_action_disconnect),
            stopPendingIntent
        ).build()
        
        val listAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_agenda,
            getString(R.string.notif_action_list),
            listPendingIntent
        ).build()
        
        val showLiveNotif = showLiveNotificationVal
        
        val channelToUse = if (showLiveNotif) "vpn_service_channel_live" else CHANNEL_ID
        val priority = NotificationCompat.PRIORITY_DEFAULT
        
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelToUse)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
        if (showLiveNotif) {
            notificationBuilder.addAction(nextAction)
        }
        notificationBuilder.addAction(listAction)
        notificationBuilder.addAction(disconnectAction)
            
        if (showLiveNotif) {
            notificationBuilder.extras.putBoolean("android.requestPromotedOngoing", true)
            val shortText = when (state) {
                "CONNECTED" -> {
                    val shortNode = nodeName.substringBefore(" ").take(7)
                    shortNode.ifEmpty { getString(R.string.active_node) }
                }
                "CONNECTING" -> getString(R.string.state_connecting)
                else -> getString(R.string.app_name)
            }
            notificationBuilder.extras.putCharSequence("android.shortCriticalText", shortText)
        }
        
        return notificationBuilder.build()
    }

    private fun updateNotification(state: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (state == "DISCONNECTED") {
            manager?.cancel(NOTIFICATION_ID)
            return
        }
        val notification = buildNotification(state)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun startForegroundServiceNotification() {
        log("startForegroundServiceNotification called")
        val showLiveNotif = showLiveNotificationVal
        log("showLiveNotification toggle: $showLiveNotif")
        if (showLiveNotif && Build.VERSION.SDK_INT >= 36) {
            val manager = getSystemService(NotificationManager::class.java)
            val allowed = manager?.canPostPromotedNotifications() ?: false
            log("canPostPromotedNotifications check: $allowed")
        }
        val notification = buildNotification(_vpnState.value)
        
        log("Calling startForeground with ID $NOTIFICATION_ID")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
        android.util.Log.i("ExpressiveBox", "startForeground finished execution")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val channelLow = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_chan_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            manager?.createNotificationChannel(channelLow)
            
            val channelLive = NotificationChannel(
                "vpn_service_channel_live",
                getString(R.string.notif_chan_live),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            manager?.createNotificationChannel(channelLive)
        }
    }

    private fun startVpnEngine() {
        android.util.Log.i("ExpressiveBox", "startVpnEngine called, launching coroutine")
        serviceScope.launch {
            try {
                log("Starting VPN Engine...")
                copyDatabasesFromAssets()
                val settingsManager = SettingsManager(applicationContext)
                
                val activeSubId = settingsManager.activeSubId.first()
                val autoConnectSubs = settingsManager.autoConnectSubs.first()
                var rawProfile = settingsManager.activeProfile.first()

                if (autoConnectSubs.contains(activeSubId)) {
                    log("Auto-Connect is enabled for active subscription. Finding best node...")
                    val subscriptionListStr = settingsManager.subscriptionList.first()
                    val subscriptions = deserializeSubscriptions(subscriptionListStr)
                    val activeSub = subscriptions.find { it.id == activeSubId }
                        ?: if (activeSubId == "manual") {
                            val manualStr = settingsManager.manualServers.first()
                            Subscription(id = "manual", name = "Manual", url = "local://manual", servers = manualStr)
                        } else if (activeSubId == "special_default") {
                            Subscription(id = "special_default", name = "Dedicated", url = "local://special_default", servers = Config.DEFAULT_PROFILE)
                        } else null
                    
                    val servers = activeSub?.servers?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    if (servers.isNotEmpty()) {
                        log("Pinging ${servers.size} servers in parallel...")
                        val jobs = servers.map { server ->
                            async(Dispatchers.IO) {
                                val hostPort = getHostAndPortFromLink(server)
                                val delay = if (hostPort != null) {
                                    measurePingDelay(hostPort.first, hostPort.second)
                                } else {
                                    -1
                                }
                                server to delay
                            }
                        }
                        val results = jobs.awaitAll()
                        val bestServer = results.filter { it.second >= 0 }.minByOrNull { it.second }
                        if (bestServer != null) {
                            log("Selected best server: ${ProxyNameResolver.getProxyName(bestServer.first, applicationContext)} with delay ${bestServer.second} ms")
                            settingsManager.setActiveProfile(bestServer.first)
                            rawProfile = bestServer.first
                        } else {
                            log("All pings timed out. Using default active profile.")
                        }
                    } else {
                        log("No servers available for Auto-Connect.")
                    }
                }

                if (rawProfile.trim().isEmpty()) {
                    log("No profile selected. Aborting connection.")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            applicationContext,
                            getString(R.string.notif_no_node),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    _vpnState.value = "DISCONNECTED"
                    stopSelf()
                    return@launch
                }
                
                // Read configurations from Preferences DataStore
                val bypassIranVal = settingsManager.bypassIran.first()
                val secureDnsVal = settingsManager.secureDns.first()
                val tunStackVal = settingsManager.tunStack.first()
                val enableFragmentVal = settingsManager.enableFragment.first()
                val fragmentLengthVal = settingsManager.fragmentLength.first()
                val fragmentIntervalVal = settingsManager.fragmentInterval.first()
                val enableMuxVal = settingsManager.enableMux.first()
                val bypassLanVal = settingsManager.bypassLan.first()
                val vpnModeVal = settingsManager.vpnMode.first()
                var warpPrivateKeyVal = settingsManager.warpPrivateKey.first()
                var warpPublicKeyVal = settingsManager.warpPublicKey.first()
                var warpIpAddressVal = settingsManager.warpIpAddress.first()
                var warpClientIdVal = settingsManager.warpClientId.first()

                if (vpnModeVal == "ai_bypass" && warpPrivateKeyVal.isNotEmpty() && warpClientIdVal.isEmpty()) {
                    log("Upgrading WARP credentials to retrieve Client ID (Reserved Bytes)...")
                    val creds = registerWarpAccount()
                    if (creds != null) {
                        settingsManager.setWarpCredentials(
                            creds.privateKey,
                            creds.publicKey,
                            creds.ipAddress,
                            creds.clientId
                        )
                        warpPrivateKeyVal = creds.privateKey
                        warpPublicKeyVal = creds.publicKey
                        warpIpAddressVal = creds.ipAddress
                        warpClientIdVal = creds.clientId
                        log("WARP credentials upgraded successfully. Client ID: $warpClientIdVal")
                    } else {
                        log("Failed to auto-register WARP credentials for Client ID upgrade.")
                    }
                }

                val vpnModeTunnelGamesVal = settingsManager.vpnModeTunnelGames.first()
                val warpDetourModeVal = settingsManager.warpDetourMode.first()
                val warpPortVal = settingsManager.warpPort.first()
                splitTunnelingEnabledVal = settingsManager.splitTunnelingEnabled.first()
                splitTunnelingModeVal = settingsManager.splitTunnelingMode.first()
                splitTunnelingAppsVal = settingsManager.splitTunnelingApps.first()

                val injectorSettings = InjectorSettings(
                    bypassIran = bypassIranVal,
                    secureDns = secureDnsVal,
                    tunStack = tunStackVal,
                    enableFragment = enableFragmentVal,
                    fragmentLength = fragmentLengthVal,
                    fragmentInterval = fragmentIntervalVal,
                    enableMux = enableMuxVal,
                    bypassLan = bypassLanVal,
                    vpnMode = vpnModeVal,
                    warpPrivateKey = warpPrivateKeyVal,
                    warpPublicKey = warpPublicKeyVal,
                    warpIpAddress = warpIpAddressVal,
                    warpClientId = warpClientIdVal,
                    vpnModeTunnelGames = vpnModeTunnelGamesVal,
                    warpDetourMode = warpDetourModeVal,
                    warpPort = warpPortVal
                )

                // Inject our custom bypass-Iran rules, split DNS, and advanced parameters
                val configJson = ConfigInjector.injectConfig(
                    applicationContext,
                    rawProfile,
                    injectorSettings
                )

                log("Generated Config JSON:\n$configJson")
                log("Configuration ready. Setting up environment...")
                
                // Redirect standard error of Go core to filesDir/CrashReport-stderr.log automatically via Setup
                val crashReportFile = File(filesDir, "CrashReport-stderr.log")
                if (crashReportFile.exists()) {
                    try { crashReportFile.delete() } catch (e: Exception) {}
                }
                
                val vpnLogFile = File(cacheDir, "vpn.log")
                startLogReader(vpnLogFile)

                log("Instantiating SetupOptions...")
                val setupOptions = SetupOptions()
                log("Setting BasePath...")
                setupOptions.setBasePath(filesDir.absolutePath)
                log("Setting WorkingPath...")
                setupOptions.setWorkingPath(filesDir.absolutePath)
                log("Setting TempPath...")
                setupOptions.setTempPath(cacheDir.absolutePath)
                log("Setting FixAndroidStack...")
                setupOptions.setFixAndroidStack(true)
                log("Setting CrashReportSource...")
                setupOptions.setCrashReportSource("stderr")
                log("Setting CommandServerListenPort...")
                setupOptions.setCommandServerListenPort(3000)
                log("Calling Libbox.setup...")
                try {
                    Libbox.setup(setupOptions)
                    log("Libbox.setup finished successfully.")
                } catch (e: Throwable) {
                    log("Libbox setup warning: ${e.message}")
                }

                log("Creating CommandServer...")
                commandServer = Libbox.newCommandServer(this@VpnServiceWrapper, this@VpnServiceWrapper)
                
                log("Starting CommandServer...")
                commandServer?.start()

                log("Starting sing-box service...")
                val overrideOptions = OverrideOptions().apply {
                    autoRedirect = false
                }
                commandServer?.startOrReloadService(configJson, overrideOptions)

                _vpnState.value = "CONNECTED"
                log("VPN Connected successfully.")
                downloadDatabasesIfMissing()
            } catch (e: Throwable) {
                log("Failed to start VPN: ${e.message}")
                e.printStackTrace()
                stopVpnEngine()
            }
        }
    }

    private fun reloadVpnEngine() {
        serviceScope.launch {
            try {
                log("Reconnecting VPN Engine (Teardown & Re-initialize)...")
                _vpnState.value = "CONNECTING"
                
                // Teardown core service
                try {
                    commandServer?.closeService()
                } catch (e: Exception) {
                    log("Error closing service during reconnect: ${e.message}")
                }
                
                try {
                    commandServer?.close()
                } catch (e: Exception) {}
                commandServer = null
                
                try {
                    tunFd?.close()
                } catch (e: Exception) {}
                tunFd = null

                if (tunFdInt != -1) {
                    try {
                        log("Adopting and closing TUN file descriptor $tunFdInt during reconnect...")
                        ParcelFileDescriptor.adoptFd(tunFdInt).close()
                    } catch (e: Exception) {
                        log("Error closing adopted FD during reconnect: ${e.message}")
                    }
                    tunFdInt = -1
                }
                
                stopLogReader()
                lastSentPhysicalName = null
                lastSentPhysicalIndex = -1
                
                // Wait a moment for interface teardown to stabilize
                kotlinx.coroutines.delay(500)
                
                log("Re-initializing VPN core...")
                startForegroundServiceNotification()
                startVpnEngine()
            } catch (e: Throwable) {
                log("Failed to reconnect VPN: ${e.message}")
                e.printStackTrace()
                stopVpnEngine()
            }
        }
    }

    private fun stopVpnEngine() {
        serviceScope.launch {
            try {
                log("Stopping VPN Engine...")
                _vpnState.value = "DISCONNECTING"
                
                log("Stopping core service...")
                try {
                    commandServer?.closeService()
                } catch (e: Exception) {
                    log("Error closing service: ${e.message}")
                }
                
                log("Stopping CommandServer...")
                commandServer?.close()
                commandServer = null
                
                try {
                    tunFd?.close()
                } catch (e: Exception) {
                    // Ignore detached close warnings
                }
                tunFd = null

                if (tunFdInt != -1) {
                    try {
                        log("Adopting and closing TUN file descriptor $tunFdInt...")
                        ParcelFileDescriptor.adoptFd(tunFdInt).close()
                        log("TUN file descriptor closed.")
                    } catch (e: Exception) {
                        log("Error closing adopted FD: ${e.message}")
                    }
                    tunFdInt = -1
                }

                log("VPN Engine stopped.")
            } catch (e: Throwable) {
                log("Error stopping VPN: ${e.message}")
            } finally {
                stopLogReader()
                lastSentPhysicalName = null
                lastSentPhysicalIndex = -1
                _vpnState.value = "DISCONNECTED"
                val manager = getSystemService(NotificationManager::class.java)
                manager?.cancel(NOTIFICATION_ID)
                stopForeground(true)
                isForeground = false
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLogReader()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        serviceScope.cancel()
    }

    private fun startLogReader(logFile: File) {
        logReaderJob = serviceScope.launch {
            try {
                _vpnLogs.value = ""
                var readOffset = 0L
                while (isActive) {
                    if (logFile.exists()) {
                        val len = logFile.length()
                        if (len > readOffset) {
                            logFile.inputStream().use { stream ->
                                stream.skip(readOffset)
                                val bytes = stream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    val newLogs = String(bytes, StandardCharsets.UTF_8)
                                    val combined = _vpnLogs.value + newLogs
                                    _vpnLogs.value = if (combined.length > 15000) {
                                        combined.takeLast(10000).substringAfter("\n", "")
                                    } else {
                                        combined
                                    }
                                }
                                readOffset = len
                            }
                        }
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                log("Log reader error: ${e.message}")
            }
        }
    }

    private fun stopLogReader() {
        logReaderJob?.cancel()
        logReaderJob = null
    }

    // --- PlatformInterface Implementation ---

    override fun openTun(options: TunOptions): Int {
        try {
            log("openTun called by sing-box core (MTU: ${options.getMTU()}, AutoRoute: ${options.getAutoRoute()})")
            val builder = Builder()
                .setSession("ExpressiveBox")
                .setMtu(options.getMTU().let { if (it > 0) it else 1500 })

            // 0. Configure Split Tunneling (Per-App Routing)
            if (splitTunnelingEnabledVal) {
                log("Applying Split Tunneling. Mode: $splitTunnelingModeVal, Apps: $splitTunnelingAppsVal")
                for (pkg in splitTunnelingAppsVal) {
                    if (pkg.isNotEmpty() && pkg != packageName) {
                        try {
                            if (splitTunnelingModeVal == "bypass") {
                                builder.addDisallowedApplication(pkg)
                            } else {
                                builder.addAllowedApplication(pkg)
                            }
                        } catch (e: Exception) {
                            log("Failed to apply split tunneling for package $pkg: ${e.message}")
                        }
                    }
                }
                if (splitTunnelingModeVal != "bypass") {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        log("Failed to allow own package: ${e.message}")
                    }
                }
            }

            var addedIpv4Address = false
            var addedIpv6Address = false

            // 1. Configure Local Address
            val ipv4Addresses = options.getInet4Address()
            if (ipv4Addresses != null && ipv4Addresses.hasNext()) {
                while (ipv4Addresses.hasNext()) {
                    val prefix = ipv4Addresses.next()
                    try {
                        builder.addAddress(prefix.address(), prefix.prefix())
                        addedIpv4Address = true
                    } catch (e: Exception) {
                        log("Failed to add IPv4 address ${prefix.address()}: ${e.message}")
                    }
                }
            }
            
            if (!addedIpv4Address) {
                try {
                    builder.addAddress("172.19.0.1", 30)
                    addedIpv4Address = true
                } catch (e: Exception) {
                    log("Failed to add default IPv4 address: ${e.message}")
                }
            }

            val ipv6Addresses = options.getInet6Address()
            if (ipv6Addresses != null) {
                while (ipv6Addresses.hasNext()) {
                    val prefix = ipv6Addresses.next()
                    try {
                        builder.addAddress(prefix.address(), prefix.prefix())
                        addedIpv6Address = true
                    } catch (e: Exception) {
                        log("Failed to add IPv6 address ${prefix.address()}: ${e.message}")
                    }
                }
            }

            // 2. Configure Route redirects
            val ipv4Routes = options.getInet4RouteAddress()
            if (ipv4Routes != null && ipv4Routes.hasNext()) {
                while (ipv4Routes.hasNext()) {
                    val prefix = ipv4Routes.next()
                    try {
                        builder.addRoute(prefix.address(), prefix.prefix())
                    } catch (e: Exception) {
                        log("Failed to add IPv4 route ${prefix.address()}: ${e.message}")
                    }
                }
            } else if (options.getAutoRoute() && addedIpv4Address) {
                try {
                    builder.addRoute("0.0.0.0", 0)
                } catch (e: Exception) {
                    log("Failed to add default IPv4 route: ${e.message}")
                }
            }

            val ipv6Routes = options.getInet6RouteAddress()
            if (ipv6Routes != null && ipv6Routes.hasNext()) {
                while (ipv6Routes.hasNext()) {
                    val prefix = ipv6Routes.next()
                    try {
                        builder.addRoute(prefix.address(), prefix.prefix())
                    } catch (e: Exception) {
                        log("Failed to add IPv6 route ${prefix.address()}: ${e.message}")
                    }
                }
            } else if (options.getAutoRoute() && addedIpv6Address) {
                try {
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    log("Failed to add default IPv6 route: ${e.message}")
                }
            }

            // 3. Configure DNS Servers
            val dnsServer = options.getDNSServerAddress()
            if (dnsServer != null && dnsServer.getValue().isNotEmpty()) {
                try {
                    builder.addDnsServer(dnsServer.getValue())
                } catch (e: Exception) {
                    log("Failed to add DNS server ${dnsServer.getValue()}: ${e.message}")
                }
                // Add one more IPv4 address for robust DNS hijacking
                try {
                    if (dnsServer.getValue() != "8.8.8.8") {
                        builder.addDnsServer("8.8.8.8")
                    } else {
                        builder.addDnsServer("1.1.1.1")
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                try {
                    builder.addDnsServer("172.19.0.1")
                    builder.addDnsServer("8.8.8.8")
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // 4. Establish TUN Interface and detach file descriptor
            val pfd = try {
                builder.establish()
            } catch (e: Exception) {
                log("VpnService establish threw: ${e.message}")
                null
            } ?: return -1 // Do NOT throw exception to Go thread (crashes JVM), return -1
            
            tunFd = pfd
            val fd = pfd.detachFd()
            tunFdInt = fd
            log("TUN interface established. FD: $fd")
            return fd
        } catch (t: Throwable) {
            log("Fatal error in openTun JNI callback: ${t.message}")
            t.printStackTrace()
            return -1
        }
    }

    override fun autoDetectInterfaceControl(fd: Int) {
        val success = protect(fd)
        if (!success) {
            log("Failed to protect socket FD: $fd")
        }
    }
    override fun clearDNSCache() {}
    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        log("closeDefaultInterfaceMonitor called")
        defaultInterfaceListener = null
        val callback = defaultNetworkCallback
        if (callback != null) {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {}
            defaultNetworkCallback = null
        }
    }
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String?,
        sourcePort: Int,
        destinationAddress: String?,
        destinationPort: Int
    ): ConnectionOwner {
        return ConnectionOwner().apply {
            userId = -1
        }
    }
    private fun createStringIterator(list: List<String>): StringIterator {
        return object : StringIterator {
            private var index = 0
            override fun hasNext(): Boolean = index < list.size
            override fun len(): Int = list.size
            override fun next(): String {
                if (index >= list.size) return ""
                return list[index++]
            }
        }
    }

    class PhysicalNetworkInfo(
        val name: String,
        val index: Int,
        val metered: Boolean,
        val constrained: Boolean
    )

    private fun getNetworkInterfaceByName(name: String): java.net.NetworkInterface? {
        try {
            val enumeration = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
            while (enumeration.hasMoreElements()) {
                val javaIf = enumeration.nextElement() ?: continue
                if (javaIf.name == name) {
                    return javaIf
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun getFallbackPhysicalInterfaceName(): String? {
        try {
            val enumeration = java.net.NetworkInterface.getNetworkInterfaces()
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    val javaIf = enumeration.nextElement() ?: continue
                    val name = javaIf.name ?: continue
                    val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
                    if (isLoopback) continue
                    
                    val lower = name.lowercase()
                    if (lower.startsWith("tun") || lower.startsWith("vpn") || lower.startsWith("ppp") || 
                        lower.startsWith("lo") || lower.startsWith("dummy") || lower.startsWith("tap")) {
                        continue
                    }
                    return name
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun getActivePhysicalNetworkInfo(cm: android.net.ConnectivityManager?): PhysicalNetworkInfo? {
        if (cm == null) return null
        try {
            val networks = cm.allNetworks
            for (network in networks) {
                val capabilities = cm.getNetworkCapabilities(network) ?: continue
                
                // Must NOT be a VPN
                val isVpn = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
                if (isVpn) continue
                
                val isWifi = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                val isEthernet = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                
                if (isWifi || isCellular || isEthernet) {
                    val lp = cm.getLinkProperties(network) ?: continue
                    val name = lp.interfaceName ?: continue
                    val javaIf = getNetworkInterfaceByName(name)
                    val index = javaIf?.index ?: 0
                    val metered = !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val constrained = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                    } else {
                        false
                    }
                    return PhysicalNetworkInfo(name, index, metered, constrained)
                }
            }
            
            // Fallback 1: system-wide active network if not VPN
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                if (capabilities != null && !capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) {
                    val lp = cm.getLinkProperties(activeNetwork)
                    val name = lp?.interfaceName
                    if (name != null) {
                        val javaIf = getNetworkInterfaceByName(name)
                        val index = javaIf?.index ?: 0
                        val metered = !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        val constrained = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                        } else {
                            false
                        }
                        return PhysicalNetworkInfo(name, index, metered, constrained)
                    }
                }
            }

            // Fallback 2: Scan local network interfaces to find physical interface name
            val fallbackName = getFallbackPhysicalInterfaceName()
            if (fallbackName != null) {
                val javaIf = getNetworkInterfaceByName(fallbackName)
                if (javaIf != null) {
                    return PhysicalNetworkInfo(javaIf.name, javaIf.index, false, false)
                }
            }
        } catch (e: Exception) {
            log("Error in getActivePhysicalNetworkInfo: ${e.message}")
        }
        return null
    }

    private fun buildLibboxInterface(
        javaIf: java.net.NetworkInterface,
        cm: android.net.ConnectivityManager?,
        metered: Boolean
    ): io.nekohasekai.libbox.NetworkInterface {
        val libboxIf = io.nekohasekai.libbox.NetworkInterface()
        libboxIf.setName(javaIf.name)
        libboxIf.setIndex(javaIf.index)
        
        val mtu = try { javaIf.mtu } catch (e: Exception) { 1500 }
        libboxIf.setMTU(mtu)
        
        var ifFlags = 0
        val isUp = try { javaIf.isUp } catch (e: Exception) { true }
        if (isUp) ifFlags = ifFlags or 1
        val isPointToPoint = try { javaIf.isPointToPoint } catch (e: Exception) { false }
        if (!isPointToPoint) ifFlags = ifFlags or 2
        val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
        if (isLoopback) ifFlags = ifFlags or 4
        if (isPointToPoint) ifFlags = ifFlags or 8
        val supportsMulticast = try { javaIf.supportsMulticast() } catch (e: Exception) { false }
        if (supportsMulticast) ifFlags = ifFlags or 16
        libboxIf.setFlags(ifFlags)

        val addrs = mutableListOf<String>()
        try {
            val addressesEnum = javaIf.inetAddresses
            while (addressesEnum.hasMoreElements()) {
                val addr = addressesEnum.nextElement() ?: continue
                if (!addr.isLoopbackAddress) {
                    var hostAddr = addr.hostAddress ?: continue
                    if (hostAddr.contains("%")) {
                        hostAddr = hostAddr.substringBefore("%")
                    }
                    var prefixLength = 24
                    try {
                        val interfaceAddresses = javaIf.interfaceAddresses
                        for (ifAddr in interfaceAddresses) {
                            if (ifAddr.address == addr) {
                                prefixLength = ifAddr.networkPrefixLength.toInt()
                                break
                            }
                        }
                    } catch (e: Exception) {}
                    addrs.add("$hostAddr/$prefixLength")
                }
            }
        } catch (e: Exception) {}
        libboxIf.setAddresses(createStringIterator(addrs))

        val dnsList = mutableListOf<String>()
        if (cm != null) {
            try {
                val networks = cm.allNetworks
                for (net in networks) {
                    val lp = cm.getLinkProperties(net)
                    if (lp?.interfaceName == javaIf.name) {
                        lp.dnsServers.forEach { dnsAddr ->
                            val dnsHost = dnsAddr.hostAddress
                            if (dnsHost != null) {
                                dnsList.add(dnsHost.substringBefore("%"))
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {}
        }
        
        return libboxIf
    }

    override fun getInterfaces(): NetworkInterfaceIterator? {
        val list = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        
        try {
            val enumeration = java.net.NetworkInterface.getNetworkInterfaces()
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    val javaIf = enumeration.nextElement() ?: continue
                    try {
                        val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
                        if (isLoopback) continue
                        
                        // Determine if metered
                        var isMetered = false
                        if (cm != null) {
                            try {
                                val networks = cm.allNetworks
                                for (net in networks) {
                                    val lp = cm.getLinkProperties(net)
                                    if (lp?.interfaceName == javaIf.name) {
                                        val caps = cm.getNetworkCapabilities(net)
                                        isMetered = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
                                        break
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        
                        val libboxIf = buildLibboxInterface(javaIf, cm, isMetered)
                        list.add(libboxIf)
                    } catch (e: Exception) {
                        log("Error processing interface ${javaIf.name}: ${e.message}")
                    }
                }
            }

            // Always ensure the active physical network interface is in the list
            try {
                val physicalInfo = getActivePhysicalNetworkInfo(cm)
                if (physicalInfo != null) {
                    val alreadyInList = list.any { it.getName() == physicalInfo.name }
                    if (!alreadyInList) {
                        val javaIf = getNetworkInterfaceByName(physicalInfo.name)
                        if (javaIf != null) {
                            val libboxIf = buildLibboxInterface(javaIf, cm, physicalInfo.metered)
                            list.add(libboxIf)
                        }
                    }
                }
            } catch (e: Exception) {
                log("Error adding active physical interface fallback: ${e.message}")
            }
        } catch (e: Exception) {
            log("Error getting network interfaces: ${e.message}")
        }

        val nameList = list.map { it.getName() }
        log("getInterfaces returning list: $nameList")

        return object : NetworkInterfaceIterator {
            private var idx = 0
            override fun hasNext(): Boolean = idx < list.size
            override fun next(): io.nekohasekai.libbox.NetworkInterface? {
                if (idx >= list.size) return null
                return list[idx++]
            }
        }
    }

    override fun includeAllNetworks(): Boolean = false
    override fun readWIFIState(): WIFIState? = null

    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun registerMyInterface(name: String?) {}
    override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}
    override fun systemCertificates(): StringIterator? = null
    override fun sendNotification(notification: Notification) {}

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        log("startDefaultInterfaceMonitor called")
        defaultInterfaceListener = listener
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
        
        // Resolve initial physical default interface
        val info = getActivePhysicalNetworkInfo(cm)
        if (info != null) {
            log("Sending initial default interface: name=${info.name}, index=${info.index}, metered=${info.metered}, constrained=${info.constrained}")
            try {
                listener.updateDefaultInterface(info.name, info.index, info.metered, info.constrained)
                lastSentPhysicalName = info.name
                lastSentPhysicalIndex = info.index
            } catch (e: Exception) {
                log("Error updating default interface: ${e.message}")
            }
        } else {
            log("No initial physical default interface found")
        }

        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                updatePhysicalInterface()
            }

            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                updatePhysicalInterface()
            }

            override fun onLinkPropertiesChanged(network: android.net.Network, lp: android.net.LinkProperties) {
                updatePhysicalInterface()
            }

            override fun onLost(network: android.net.Network) {
                updatePhysicalInterface()
            }

            private fun updatePhysicalInterface() {
                val currentInfo = getActivePhysicalNetworkInfo(cm)
                if (currentInfo != null) {
                    if (currentInfo.name != lastSentPhysicalName || currentInfo.index != lastSentPhysicalIndex) {
                        log("Default physical interface updated: name=${currentInfo.name}, index=${currentInfo.index}, metered=${currentInfo.metered}, constrained=${currentInfo.constrained}")
                        try {
                            defaultInterfaceListener?.updateDefaultInterface(currentInfo.name, currentInfo.index, currentInfo.metered, currentInfo.constrained)
                            lastSentPhysicalName = currentInfo.name
                            lastSentPhysicalIndex = currentInfo.index
                            
                            // Hot-reload configuration to pick up new system DNS servers
                            reloadVpnEngine()
                        } catch (e: Exception) {
                            log("Error sending default interface update: ${e.message}")
                        }
                    }
                } else {
                    log("No active physical network found during callback update")
                }
            }
        }
        defaultNetworkCallback = callback
        try {
            val builder = android.net.NetworkRequest.Builder()
            cm.registerNetworkCallback(builder.build(), callback)
            log("Successfully registered global network callback")
        } catch (e: Exception) {
            log("Failed to register global network callback, falling back to default network callback: ${e.message}")
            try {
                cm.registerDefaultNetworkCallback(callback)
            } catch (e2: Exception) {
                log("Failed to register default network callback: ${e2.message}")
            }
        }
    }
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun useProcFS(): Boolean = false

    // --- CommandServerHandler Implementation ---

    override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus().apply {
        available = false
        enabled = false
    }

    override fun serviceReload() {
        log("Core service reloaded.")
    }

    override fun serviceStop() {
        log("CommandServer requested service stop.")
        stopVpnEngine()
    }

    override fun triggerNativeCrash() {
        log("triggerNativeCrash called")
    }

    override fun writeDebugMessage(message: String?) {
        log("Debug: $message")
    }

    override fun setSystemProxyEnabled(enabled: Boolean) {}

    private fun downloadDatabasesIfMissing() {
        serviceScope.launch {
            try {
                val geoipFile = File(filesDir, "geoip-ir.srs")
                val geositeFile = File(filesDir, "geosite-ir.srs")
                
                if (!geoipFile.exists()) {
                    log("Background downloading geoip-ir.srs from Chocolate4U...")
                    downloadFile("https://raw.githubusercontent.com/Chocolate4U/Iran-sing-box-rules/rule-set/geoip-ir.srs", geoipFile)
                    log("geoip-ir.srs downloaded successfully.")
                }
                if (!geositeFile.exists()) {
                    log("Background downloading geosite-ir.srs from Chocolate4U...")
                    downloadFile("https://raw.githubusercontent.com/Chocolate4U/Iran-sing-box-rules/rule-set/geosite-ir.srs", geositeFile)
                    log("geosite-ir.srs downloaded successfully.")
                }
            } catch (e: Exception) {
                log("Background assets download failed: ${e.message}")
            }
        }
    }

    private fun downloadFile(urlStr: String, destFile: File) {
        val url = java.net.URL(urlStr)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.connect()
        
        if (connection.responseCode == 200) {
            val tempFile = File(destFile.parentFile, destFile.name + ".tmp")
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.renameTo(destFile)
        } else {
            throw java.io.IOException("HTTP error ${connection.responseCode} downloading $urlStr")
        }
    }

    private fun copyDatabasesFromAssets() {
        try {
            // Cleanup legacy .db files if they exist to save space
            val legacyGeoip = File(filesDir, "geoip.db")
            val legacyGeosite = File(filesDir, "geosite.db")
            if (legacyGeoip.exists()) legacyGeoip.delete()
            if (legacyGeosite.exists()) legacyGeosite.delete()

            val geoipFile = File(filesDir, "geoip-ir.srs")
            val geositeFile = File(filesDir, "geosite-ir.srs")
            
            if (!geoipFile.exists()) {
                log("Copying geoip-ir.srs from assets...")
                assets.open("geoip-ir.srs").use { input ->
                    geoipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                log("geoip-ir.srs copied from assets.")
            }
            if (!geositeFile.exists()) {
                log("Copying geosite-ir.srs from assets...")
                assets.open("geosite-ir.srs").use { input ->
                    geositeFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                log("geosite-ir.srs copied from assets.")
            }
        } catch (e: Exception) {
            log("Error copying databases from assets: ${e.message}")
            e.printStackTrace()
        }
    }
}
