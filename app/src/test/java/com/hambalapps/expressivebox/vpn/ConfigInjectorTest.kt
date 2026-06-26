package com.hambalapps.expressivebox.vpn

import android.content.Context
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlinx.coroutines.runBlocking

class ConfigInjectorTest {
    @Test
    fun testRealityConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir")))

        val rawUri = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.nl.netlume.ir:29757?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=t_9lyts8KkYowHc3eDr22L7DuzRUnjRnodNhd1lspAE&sid=462333e748f7577e#%F0%9F%87%B3%F0%9F%87%B1%20%F0%9D%90%8D%F0%9D%90%9E%F0%9D%90%AD%F0%9D%90%A1%F0%9D%90%9E%F0%9D%90%AB%F0%9D%90%A5%F0%9D%90%9A%F0%9D%90%A7%F0%9D%90%9D%F0%9D%90%AC"
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal"
        )

        val configStr = ConfigInjector.injectConfig(mockContext, rawUri, settings)
        println("Generated Configuration:")
        println(configStr)
    }

    @Test
    fun testAiBypassConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir")))

        val rawUri = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.nl.netlume.ir:29757?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=t_9lyts8KkYowHc3eDr22L7DuzRUnjRnodNhd1lspAE&sid=462333e748f7577e#test"
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "ai_bypass",
            warpPrivateKey = "privatekeybase64",
            warpPublicKey = "publickeybase64",
            warpIpAddress = "172.16.0.2/32",
            warpClientId = "6hHy",
            warpDetourMode = "direct",
            warpPort = "4500"
        )

        val configStr = ConfigInjector.injectConfig(mockContext, rawUri, settings)
        println("Generated AI Bypass Configuration:")
        println(configStr)

        val json = org.json.JSONObject(configStr)
        val endpoints = json.getJSONArray("endpoints")
        assert(endpoints.length() == 1)
        val endpoint = endpoints.getJSONObject(0)
        assert(endpoint.getString("type") == "wireguard")
        assert(endpoint.getString("tag") == "warp-out")
        assert(endpoint.getString("address") == "172.16.0.2/32")
        assert(endpoint.getString("private_key") == "privatekeybase64")
        assert(endpoint.getString("detour") == "direct")

        val peers = endpoint.getJSONArray("peers")
        assert(peers.length() == 1)
        val peer = peers.getJSONObject(0)
        val peerAddress = peer.getString("address")
        assert(peerAddress == "162.159.192.1" || peerAddress.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")))
        assert(peer.getInt("port") == 4500)
        assert(peer.getString("reserved") == "6hHy")

        val outbounds = json.getJSONArray("outbounds")
        for (i in 0 until outbounds.length()) {
            val out = outbounds.getJSONObject(i)
            assert(out.getString("tag") != "warp-out")
        }
    }

    @Test
    fun testWarpRegistrationResponse() {
        runBlocking {
            val creds = com.hambalapps.expressivebox.vpn.registerWarpAccount()
            if (creds != null) {
                val file = java.io.File("warp_response.json")
                file.writeText("PrivateKey: ${creds.privateKey}\nPublicKey: ${creds.publicKey}\nIpAddress: ${creds.ipAddress}\nClientId: ${creds.clientId}\n")
            } else {
                val file = java.io.File("warp_response.json")
                file.writeText("FAILED")
            }
        }
    }
}
