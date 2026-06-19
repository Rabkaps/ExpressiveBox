package com.hambalapps.expressivebox.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

suspend fun measurePingDelay(host: String, port: Int): Int = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2000) // 2-second timeout
        }
        (System.currentTimeMillis() - startTime).toInt()
    } catch (e: Exception) {
        -1
    }
}

fun getHostAndPortFromLink(link: String): Pair<String, Int>? {
    try {
        val trimmed = link.trim()
        val rest = if (trimmed.contains("#")) trimmed.substring(0, trimmed.indexOf("#")) else trimmed
        val schemeIdx = rest.indexOf("://")
        val content = if (schemeIdx >= 0) rest.substring(schemeIdx + 3) else rest
        val queryIdx = content.indexOf("?")
        val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
        
        val serverPart = if (mainPart.contains("@")) mainPart.substring(mainPart.indexOf("@") + 1) else mainPart
        val colonIdx = serverPart.lastIndexOf(":")
        val host = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
        val portStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
        val port = portStr.toIntOrNull() ?: 443
        return Pair(host, port)
    } catch (e: Exception) {
        return null
    }
}
