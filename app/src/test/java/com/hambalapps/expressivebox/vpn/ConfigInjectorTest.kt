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

        val rawUri = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.it.f2gate.ir:48870?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=WNkRjCdLYYm2fGAklDkdpeXrBlf-puKDOL8XJj_mwAk&sid=b8ddd8982a450ec7#%F0%9F%87%AE%F0%9F%87%B9%20%F0%9D%90%88%F0%9D%90%AD%F0%9D%90%9A%F0%9D%90%A5%F0%9D%90%B2%20here%20an%20example"
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
