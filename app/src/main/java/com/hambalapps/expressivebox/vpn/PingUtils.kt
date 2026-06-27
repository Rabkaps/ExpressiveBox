package com.hambalapps.expressivebox.vpn

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import android.content.Context
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap


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

fun tryBase64Decode(str: String): String? {
    val flags = listOf(
        Base64.DEFAULT,
        Base64.URL_SAFE,
        Base64.NO_PADDING,
        Base64.NO_WRAP,
        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    for (flag in flags) {
        try {
            val decoded = Base64.decode(str, flag)
            val decodedStr = String(decoded, StandardCharsets.UTF_8).trim()
            if (decodedStr.isNotEmpty()) {
                return decodedStr
            }
        } catch (e: Exception) {
            // continue
        }
    }
    return null
}

fun getHostAndPortFromLink(link: String): Pair<String, Int>? {
    try {
        val trimmed = link.trim()
        val rest = if (trimmed.contains("#")) trimmed.substring(0, trimmed.indexOf("#")) else trimmed
        val schemeIdx = rest.indexOf("://")
        val scheme = if (schemeIdx >= 0) rest.substring(0, schemeIdx).lowercase() else ""
        val content = if (schemeIdx >= 0) rest.substring(schemeIdx + 3) else rest
        val queryIdx = content.indexOf("?")
        val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content

        if (scheme == "vmess") {
            val decoded = tryBase64Decode(mainPart)
            if (decoded != null && decoded.startsWith("{")) {
                val vmessJson = JSONObject(decoded)
                val add = vmessJson.optString("add")
                val portVal = vmessJson.opt("port")
                val port = when (portVal) {
                    is Number -> portVal.toInt()
                    is String -> portVal.toIntOrNull() ?: 443
                    else -> 443
                }
                if (add.isNotEmpty()) {
                    return Pair(add, port)
                }
            }
        }

        if (scheme == "ss") {
            val atIdx = mainPart.indexOf("@")
            if (atIdx < 0) {
                val decoded = tryBase64Decode(mainPart)
                if (decoded != null && decoded.contains("@")) {
                    val parts = decoded.split("@")
                    val serverPart = parts[1]
                    val colonIdx = serverPart.lastIndexOf(":")
                    val h = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
                    val pStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
                    return Pair(h, pStr.toIntOrNull() ?: 443)
                }
            }
        }
        
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

object ProxyNameResolver {
    private val nameCache = ConcurrentHashMap<String, String>()

    fun getProxyName(link: String, context: Context): String {
        val cached = nameCache[link]
        if (cached != null) return cached

        val trimmed = link.trim()
        val hashIdx = trimmed.indexOf("#")
        if (hashIdx >= 0) {
            val name = try {
                URLDecoder.decode(trimmed.substring(hashIdx + 1), "UTF-8")
            } catch (e: Exception) {
                trimmed.substring(hashIdx + 1)
            }
            nameCache[link] = name
            return name
        }

        if (trimmed.startsWith("vmess://")) {
            try {
                val mainPart = trimmed.substring(8)
                val decoded = tryBase64Decode(mainPart)
                if (decoded != null && decoded.startsWith("{")) {
                    val json = JSONObject(decoded)
                    val ps = json.optString("ps")
                    if (ps.isNotEmpty()) {
                        nameCache[link] = ps
                        return ps
                    }
                    val add = json.optString("add")
                    if (add.isNotEmpty()) {
                        val cleanHost = if (add.length > 20) add.take(20) + "..." else add
                        val name = "VMESS ($cleanHost)"
                        nameCache[link] = name
                        return name
                    }
                }
            } catch (e: Exception) {}
        }

        if (trimmed.startsWith("ss://")) {
            try {
                val mainPart = trimmed.substring(5).substringBefore("#").substringBefore("?")
                if (!mainPart.contains("@")) {
                    val decoded = tryBase64Decode(mainPart)
                    if (decoded != null && decoded.contains("@")) {
                        val host = decoded.substringAfter("@").substringBefore(":")
                        val cleanHost = if (host.length > 20) host.take(20) + "..." else host
                        val name = "SS ($cleanHost)"
                        nameCache[link] = name
                        return name
                    }
                }
            } catch (e: Exception) {}
        }

        val name = try {
            val schemeIdx = trimmed.indexOf("://")
            val scheme = if (schemeIdx >= 0) trimmed.substring(0, schemeIdx).uppercase() else "VPN"
            val rest = if (schemeIdx >= 0) trimmed.substring(schemeIdx + 3) else trimmed
            val host = if (rest.contains("@")) {
                rest.substringAfter("@").substringBefore(":")
            } else {
                rest.substringBefore(":")
            }
            val cleanHost = if (host.length > 20) host.take(20) + "..." else host
            "$scheme ($cleanHost)"
        } catch (e: Exception) {
            context.getString(com.hambalapps.expressivebox.R.string.notif_unnamed)
        }
        nameCache[link] = name
        return name
    }
}

