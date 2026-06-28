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
            
            // Notify Windows wininet subsystems that internet proxy settings have changed
            refreshWindowsProxy()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disable(): Boolean {
        return try {
            runCommand("reg", "add", REG_KEY, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", "0", "/f")
            refreshWindowsProxy()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun refreshWindowsProxy() {
        val script = """
            ${'$'}sig = '[DllImport("wininet.dll")] public static extern bool InternetSetOption(int h, int o, int b, int l);'
            # Generate unique class name to avoid collision in PowerShell session
            ${'$'}name = 'ProxyHelper' + [System.Guid]::NewGuid().ToString().Replace('-', '')
            ${'$'}type = Add-Type -MemberDefinition ${'$'}sig -Name ${'$'}name -PassThru
            ${'$'}type::InternetSetOption(0, 39, 0, 0)
            ${'$'}type::InternetSetOption(0, 37, 0, 0)
        """.trimIndent()
        runCommand("powershell", "-WindowStyle", "Hidden", "-Command", script)
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
