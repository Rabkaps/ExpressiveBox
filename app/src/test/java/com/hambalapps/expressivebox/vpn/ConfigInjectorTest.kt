package com.hambalapps.expressivebox.vpn

import android.content.Context
import org.junit.Test
import org.mockito.Mockito
import java.io.File

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
}
