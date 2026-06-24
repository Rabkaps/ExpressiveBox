package com.hambalapps.expressivebox.vpn

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator

data class WarpCredentials(
    val privateKey: String,
    val publicKey: String,
    val ipAddress: String,
    val clientId: String
)

suspend fun registerWarpAccount(): WarpCredentials? = withContext(Dispatchers.IO) {
    try {
        // 1. Generate X25519 KeyPair using Android KeyPairGenerator (API 33+)
        val kpg = KeyPairGenerator.getInstance("X25519")
        val kp = kpg.generateKeyPair()
        
        // Extract raw 32-byte keys from PKCS#8 (48 bytes) and X.509 SubjectPublicKeyInfo (44 bytes) formats
        val privateEncoded = kp.private.encoded
        val publicEncoded = kp.public.encoded
        
        val privateKeyRaw = if (privateEncoded.size >= 32) {
            privateEncoded.takeLast(32).toByteArray()
        } else {
            privateEncoded
        }
        
        val publicKeyRaw = if (publicEncoded.size >= 32) {
            publicEncoded.takeLast(32).toByteArray()
        } else {
            publicEncoded
        }
        
        val privateKeyBase64 = Base64.encodeToString(privateKeyRaw, Base64.NO_WRAP).trim()
        val publicKeyBase64 = Base64.encodeToString(publicKeyRaw, Base64.NO_WRAP).trim()
        
        // 2. Register via Cloudflare API
        val url = URL("https://api.cloudflareclient.com/v0a1925/reg")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("User-Agent", "okhttp/3.12.1")
        
        val requestJson = JSONObject().apply {
            put("install_id", "")
            put("tos", "2020-09-01T00:00:00.000+02:00")
            put("key", publicKeyBase64)
            put("fcm_token", "")
            put("referrer", "")
            put("warp_enabled", true)
        }
        
        conn.outputStream.use { os ->
            os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
        }
        
        val responseCode = conn.responseCode
        if (responseCode == 200 || responseCode == 201) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseText)
            
            val config = responseJson.getJSONObject("config")
            val clientId = config.optString("client_id", "")
            val interfaceObj = config.getJSONObject("interface")
            val addresses = interfaceObj.getJSONObject("addresses")
            val v4Address = addresses.optString("v4", "")
            
            val finalIp = if (v4Address.isNotEmpty()) v4Address else "172.16.0.2/32"
            
            WarpCredentials(
                privateKey = privateKeyBase64,
                publicKey = publicKeyBase64,
                ipAddress = finalIp,
                clientId = clientId
            )
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
