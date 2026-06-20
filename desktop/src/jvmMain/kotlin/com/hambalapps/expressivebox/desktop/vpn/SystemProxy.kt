package com.hambalapps.expressivebox.desktop.vpn

object SystemProxy {
    private const val REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"

    fun enable(host: String, port: Int): Boolean {
        return try {
            // Setup mixed proxy format for Windows
            val serverValue = "http=$host:$port;https=$host:$port;socks=$host:$port"
            
            runCommand("reg", "add", REG_KEY, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", "1", "/f")
            runCommand("reg", "add", REG_KEY, "/v", "ProxyServer", "/t", "REG_SZ", "/d", serverValue, "/f")
            runCommand("reg", "add", REG_KEY, "/v", "ProxyOverride", "/t", "REG_SZ", "/d", "<local>", "/f")
            
            // Notify Windows user32 subsystems that internet settings have changed
            runCommand("rundll32.exe", "user32.dll", "UpdatePerUserSystemParameters")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disable(): Boolean {
        return try {
            runCommand("reg", "add", REG_KEY, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", "0", "/f")
            runCommand("rundll32.exe", "user32.dll", "UpdatePerUserSystemParameters")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun runCommand(vararg args: String) {
        try {
            val process = ProcessBuilder(*args).start()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
