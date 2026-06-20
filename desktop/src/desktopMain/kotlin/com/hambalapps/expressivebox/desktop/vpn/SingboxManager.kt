package com.hambalapps.expressivebox.desktop.vpn

import com.hambalapps.expressivebox.desktop.data.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SingboxManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    
    private val _vpnState = MutableStateFlow("DISCONNECTED")
    val vpnState: StateFlow<String> = _vpnState.asStateFlow()

    private val _vpnLogs = MutableStateFlow("")
    val vpnLogs: StateFlow<String> = _vpnLogs.asStateFlow()

    private val _trafficStats = MutableStateFlow(Pair(0L, 0L)) // upload, download in bytes/sec
    val trafficStats: StateFlow<Pair<Long, Long>> = _trafficStats.asStateFlow()

    val workingDir = File(System.getProperty("user.home"), ".expressivebox").apply { mkdirs() }
    val exeFile = File(workingDir, "sing-box.exe")
    val geoip = File(workingDir, "geoip-ir.srs")
    val geosite = File(workingDir, "geosite-ir.srs")
    val configFile = File(workingDir, "config.json")
    val logFile = File(workingDir, "vpn.log")

    private var statsJob: Job? = null

    init {
        // Extract assets from JAR resources to working directory
        extractResource("sing-box.exe", exeFile)
        extractResource("geoip-ir.srs", geoip)
        extractResource("geosite-ir.srs", geosite)
        
        // Add shutdown hook to disable proxy and kill sing-box on exit
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    private fun extractResource(name: String, dest: File) {
        try {
            val stream: InputStream? = SingboxManager::class.java.classLoader.getResourceAsStream(name)
            if (stream != null) {
                Files.copy(stream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                stream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start(rawProfile: String, settingsManager: SettingsManager): Boolean {
        if (_vpnState.value == "CONNECTED" || _vpnState.value == "CONNECTING") {
            stop()
        }

        _vpnState.value = "CONNECTING"
        _vpnLogs.value = "Starting sing-box...\n"
        
        try {
            // 1. Generate Config
            val configJson = ConfigInjector.injectConfig(
                rawProfile = rawProfile,
                settings = settingsManager.currentSettings,
                geoipPath = geoip.absolutePath.replace("\\", "/"),
                geositePath = geosite.absolutePath.replace("\\", "/"),
                logPath = logFile.absolutePath.replace("\\", "/")
            )
            configFile.writeText(configJson)

            // 2. Clear old log file
            if (logFile.exists()) logFile.delete()

            // 3. Launch Subprocess
            val pb = ProcessBuilder(exeFile.absolutePath, "run", "-c", configFile.absolutePath)
            pb.directory(workingDir)
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            process = proc

            // Start log reading
            scope.launch {
                proc.inputStream.bufferedReader().use { reader ->
                    while (proc.isAlive) {
                        val line = reader.readLine() ?: break
                        log(line)
                    }
                }
            }

            // 4. Configure Windows System Proxy (Standard user mode)
            // Port 2080 is the default inbound port in our generated config
            if (!settingsManager.currentSettings.enableTun) {
                val proxyPort = 2080
                SystemProxy.enable("127.0.0.1", proxyPort)
            } else {
                SystemProxy.disable()
            }

            _vpnState.value = "CONNECTED"
            log("VPN Connected successfully.")

            // 5. Start Traffic Stats polling (Clash API / HTTP)
            startStatsPolling()

            return true
        } catch (e: Exception) {
            log("Failed to start VPN: ${e.message}")
            e.printStackTrace()
            stop()
            return false
        }
    }

    fun stop() {
        _vpnState.value = "DISCONNECTING"
        
        // Disable Windows Proxy
        SystemProxy.disable()
        
        // Stop stats polling
        statsJob?.cancel()
        statsJob = null
        _trafficStats.value = Pair(0L, 0L)

        // Kill Process
        process?.let {
            it.destroy()
            try {
                it.destroyForcibly()
            } catch (e: Exception) {}
        }
        process = null
        
        _vpnState.value = "DISCONNECTED"
        log("VPN Disconnected.")
    }

    private fun log(message: String) {
        val line = message + "\n"
        val current = _vpnLogs.value
        _vpnLogs.value = if (current.length > 15000) {
            current.takeLast(10000).substringAfter("\n") + line
        } else {
            current + line
        }
    }

    fun clearLogs() {
        _vpnLogs.value = ""
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                delay(1000)
                try {
                    val url = URL("http://127.0.0.1:9090/traffic")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        // text format: {"up":1234,"down":5678}
                        val up = text.substringAfter("\"up\":").substringBefore(",").trim().toLongOrNull() ?: 0L
                        val down = text.substringAfter("\"down\":").substringBefore("}").trim().toLongOrNull() ?: 0L
                        _trafficStats.value = Pair(up, down)
                    } else {
                        _trafficStats.value = Pair(0L, 0L)
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    _trafficStats.value = Pair(0L, 0L)
                }
            }
        }
    }
}
