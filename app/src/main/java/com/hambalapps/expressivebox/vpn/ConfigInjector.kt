package com.hambalapps.expressivebox.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class InjectorSettings(
    val bypassIran: Boolean,
    val secureDns: String,
    val tunStack: String,
    val enableFragment: Boolean,
    val fragmentLength: String,
    val fragmentInterval: String,
    val enableMux: Boolean,
    val bypassLan: Boolean,
    val vpnMode: String = "standard",
    val warpPrivateKey: String = "",
    val warpPublicKey: String = "",
    val warpIpAddress: String = "",
    val vpnModeTunnelGames: Boolean = false
)

object ConfigInjector {

    private val gamingDomains = listOf(
        "pubgmobile.com", "pubg.com", "riotgames.com", "playvalorant.com", "leagueoflegends.com",
        "activision.com", "callofduty.com", "epicgames.com", "ea.com", "origin.com",
        "supercell.com", "clashofclans.com", "steampowered.com", "steamcommunity.com",
        "fortnite.com", "sony.com", "playstation.com", "playstation.net", "xbox.com", "xboxlive.com",
        "garena.com", "roblox.com", "blizzard.com", "battle.net", "ubisoft.com", "apexlegends.com",
        "levelinfinite.com", "steamstatic.com", "moonton.com", "mobilelegends.com"
    )

    private val aiBypassDomains = listOf(
        "gemini.google.com", "generativelanguage.googleapis.com", "ai.google.dev", "makersuite.google.com",
        "openai.com", "chatgpt.com", "chat.openai.com", "oaistatic.com", "oaiusercontent.com",
        "anthropic.com", "claude.ai",
        "netflix.com", "netflix.net", "nflximg.net", "nflxvideo.net", "nflxso.net", "nflxext.com"
    )

    fun injectConfig(context: Context, rawProfile: String, settings: InjectorSettings): String {
        try {
            val trimmedProfile = rawProfile.trim()
            val configJson = if (trimmedProfile.startsWith("{")) {
                JSONObject(rawProfile)
            } else if (trimmedProfile.startsWith("vless://") ||
                trimmedProfile.startsWith("trojan://") ||
                trimmedProfile.startsWith("ss://") ||
                trimmedProfile.startsWith("socks5://") ||
                trimmedProfile.startsWith("socks://") ||
                trimmedProfile.startsWith("http://") ||
                trimmedProfile.startsWith("https://") ||
                trimmedProfile.startsWith("vmess://") ||
                trimmedProfile.startsWith("hysteria2://") ||
                trimmedProfile.startsWith("hy2://") ||
                trimmedProfile.startsWith("tuic://")) {
                buildConfigFromUri(rawProfile, settings)
            } else {
                // Return default empty configuration skeleton
                buildDefaultSkeleton(settings)
            }

            // Override log configuration to output to vpn.log
            val logFile = java.io.File(context.cacheDir, "vpn.log")
            try {
                if (logFile.exists()) logFile.delete()
            } catch (e: Exception) {}
            val logObj = configJson.optJSONObject("log") ?: JSONObject().also { configJson.put("log", it) }
            logObj.put("level", "info")
            logObj.put("output", logFile.absolutePath)
            logObj.put("timestamp", true)

            // Sanitize invalid port fields in outbounds and inbounds
            sanitizePortFields(configJson)

            // 1. Pre-resolve proxy server domains to raw IPs to bypass DNS hijacking
            preResolveProxyServers(context, configJson, settings)

            // 2. Inject or update inbounds (TUN interface)
            injectTunInbound(configJson, settings)

            // 3. Inject or update DNS (Split DNS rules)
            injectDns(context, configJson, settings)

            // 4. Inject or update Routing Rules (Iran bypass)
            injectRouting(context, configJson, settings)

            // 5. Inject direct/block outbounds
            injectOutbounds(configJson, settings)

            return configJson.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return buildDefaultSkeleton(settings).toString(2)
        }
    }

    private fun injectTunInbound(config: JSONObject, settings: InjectorSettings) {
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        
        // Remove existing TUN inbounds if any
        val newInbounds = JSONArray()
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") != "tun") {
                newInbounds.put(inbound)
            }
        }

        val tunInbound = JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("interface_name", "tun0")
            put("stack", if (settings.vpnMode == "gaming") "system" else (settings.run { if (tunStack.isEmpty()) "mixed" else tunStack }))
            put("mtu", if (settings.vpnMode == "gaming") 1350 else 9000)
            put("auto_route", true)
            put("strict_route", true)
            put("address", JSONArray(listOf("172.19.0.1/30")))
        }
        newInbounds.put(tunInbound)
        config.put("inbounds", newInbounds)
    }

    private fun getSystemDnsServers(context: Context): List<String> {
        val dnsList = mutableListOf<String>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            try {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val lp = cm.getLinkProperties(activeNetwork)
                    lp?.dnsServers?.forEach { dnsAddr ->
                        val dnsHost = dnsAddr.hostAddress
                        if (dnsHost != null) {
                            val cleanHost = dnsHost.substringBefore("%")
                            if (cleanHost.isNotEmpty() && !cleanHost.contains(":")) {
                                dnsList.add(cleanHost)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpressiveBox", "Failed to get system DNS: ${e.message}")
            }
        }
        return dnsList
    }

    private fun createDnsServer(tag: String, address: String, detour: String?): JSONObject {
        val serverObj = JSONObject()
        serverObj.put("tag", tag)
        if (detour != null) {
            serverObj.put("detour", detour)
        }

        val trimmed = address.trim()
        if (trimmed.startsWith("https://")) {
            serverObj.put("type", "https")
            val hostPart = trimmed.substringAfter("https://").substringBefore("/")
            serverObj.put("server", hostPart)
            val path = "/" + trimmed.substringAfter("https://").substringAfter("/", "")
            if (path.length > 1) {
                serverObj.put("path", path)
            }
            if (hostPart == "10.202.10.10") {
                val tls = JSONObject().apply {
                    put("enabled", true)
                    put("server_name", "radar.game")
                    put("insecure", true)
                }
                serverObj.put("tls", tls)
            } else if (hostPart == "185.51.200.2" || hostPart == "178.22.122.100") {
                val tls = JSONObject().apply {
                    put("enabled", true)
                    put("server_name", "shecan.ir")
                    put("insecure", true)
                }
                serverObj.put("tls", tls)
            }
        } else if (trimmed.startsWith("tls://")) {
            serverObj.put("type", "tls")
            serverObj.put("server", trimmed.substringAfter("tls://"))
        } else if (trimmed.startsWith("tcp://")) {
            serverObj.put("type", "tcp")
            serverObj.put("server", trimmed.substringAfter("tcp://"))
        } else if (trimmed.startsWith("quic://")) {
            serverObj.put("type", "quic")
            serverObj.put("server", trimmed.substringAfter("quic://"))
        } else {
            serverObj.put("type", "udp")
            serverObj.put("server", trimmed)
        }
        return serverObj
    }

    private fun injectDns(context: Context, config: JSONObject, settings: InjectorSettings) {
        val dns = JSONObject()
        dns.put("reverse_mapping", true)
        val servers = JSONArray()

        // 1. Secure DNS Server (routes via the proxy)
        val secureServer = createDnsServer("dns-secure", settings.secureDns, "proxy")

        // 2. Local Bypass DNS Server for Iran domains (runs directly, detouring proxy)
        val systemDnsList = getSystemDnsServers(context)
        var directDnsAddr = "178.22.122.100" // Default Shecan/Local DNS
        
        for (dnsIp in systemDnsList) {
            // Filter out well-known hijacked public DNS servers in Iran
            if (dnsIp != "8.8.8.8" && dnsIp != "8.8.4.4" && dnsIp != "1.1.1.1" && dnsIp != "1.0.0.1" && dnsIp != "9.9.9.9") {
                directDnsAddr = dnsIp
                break
            }
        }

        android.util.Log.i("ExpressiveBox", "Direct DNS set to: $directDnsAddr (from system DNS: $systemDnsList)")

        val directServer = createDnsServer("dns-direct", directDnsAddr, null)

        // 3. Clean Bootstrap DNS Server for resolving proxy/DNS hostnames reliably (without carrier hijacking)
        val bootstrapDnsAddr = if (settings.bypassIran) "https://178.22.122.100/dns-query" else "https://1.1.1.1/dns-query"
        val bootstrapServer = createDnsServer("dns-bootstrap", bootstrapDnsAddr, null)

        if (settings.vpnMode == "gaming" && !settings.vpnModeTunnelGames) {
            val radarServer = createDnsServer("dns-radar", "tcp://10.202.10.10", null)
            val shecanServer = createDnsServer("dns-shecan", "tcp://185.51.200.2", null)
            servers.put(secureServer)
            servers.put(radarServer)
            servers.put(shecanServer)
            servers.put(directServer)
            servers.put(bootstrapServer)
        } else {
            servers.put(secureServer)
            servers.put(directServer)
            servers.put(bootstrapServer)
        }

        dns.put("servers", servers)

        val rules = JSONArray()

        // Inject bootstrap rules for proxy server domain and secure DNS DoH domain to route directly
        val proxyHosts = getProxyServerHosts(config)
        val secureDnsHost = extractHostFromUrl(settings.secureDns)
        val directDomains = mutableListOf<String>()

        for (host in proxyHosts) {
            if (host.isNotEmpty() && !isIpAddress(host)) {
                directDomains.add(host)
            }
        }
        if (secureDnsHost != null && secureDnsHost.isNotEmpty() && !isIpAddress(secureDnsHost)) {
            directDomains.add(secureDnsHost)
        }

        if (directDomains.isNotEmpty()) {
            val bootstrapRule = JSONObject().apply {
                put("domain", JSONArray(directDomains))
                put("server", "dns-bootstrap")
            }
            rules.put(bootstrapRule)
        }
        
        if (settings.bypassIran) {
            val geositeFile = java.io.File(context.filesDir, "geosite-ir.srs")
            if (geositeFile.exists()) {
                // Rule: Route Iranian geosite to local DNS via rule_set
                val irGeositeRule = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geosite-ir")))
                    put("server", "dns-direct")
                }
                rules.put(irGeositeRule)
            }

            // Rule: Route .ir domains to local DNS
            val irSuffixRule = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("server", "dns-direct")
            }
            rules.put(irSuffixRule)
        }

        if (settings.vpnMode == "gaming") {
            val gameDnsRule = JSONObject().apply {
                put("domain_suffix", JSONArray(gamingDomains))
                put("server", if (settings.vpnModeTunnelGames) "dns-secure" else "dns-radar")
            }
            rules.put(gameDnsRule)
        }

        dns.put("rules", rules)
        config.put("dns", dns)
    }

    private fun injectRouting(context: Context, config: JSONObject, settings: InjectorSettings) {
        val route = config.optJSONObject("route") ?: JSONObject().also { config.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

        // Remove existing DNS routing and Iran rules to refresh them
        val newRules = JSONArray()
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            val protocol = r.optString("protocol")
            val geosite = r.optJSONArray("geosite")
            val geoip = r.optJSONArray("geoip")
            val suffix = r.optJSONArray("domain_suffix")
            val ruleSetName = r.optString("rule_set")
            val ruleSetArrayVal = r.optJSONArray("rule_set")

            val isIranRule = (geosite != null && geosite.toString().contains("ir")) ||
                             (geoip != null && geoip.toString().contains("ir")) ||
                             (suffix != null && suffix.toString().contains(".ir")) ||
                             (ruleSetName != null && ruleSetName.contains("ir")) ||
                             (ruleSetArrayVal != null && ruleSetArrayVal.toString().contains("ir"))
            
            if (protocol != "dns" && !isIranRule) {
                newRules.put(r)
            }
        }

        // Add sniffing rule at the beginning
        val sniffRule = JSONObject().apply {
            put("action", "sniff")
            if (settings.vpnMode == "gaming") {
                put("sniffer", JSONArray(listOf("http", "tls")))
                put("network", "tcp")
            } else {
                put("sniffer", JSONArray(listOf("http", "tls", "quic", "dns", "stun")))
            }
        }
        newRules.put(sniffRule)

        // Add standard DNS routing rule (required for internal DNS hijacking)
        val dnsRule = JSONObject().apply {
            put("protocol", "dns")
            put("action", "hijack-dns")
        }
        newRules.put(dnsRule)

        // Block Private DNS (DoT) on port 853 to force fallback to hijacked standard DNS
        val blockDotRule = JSONObject().apply {
            put("port", JSONArray(listOf(853)))
            put("action", "reject")
            put("method", "default")
        }
        newRules.put(blockDotRule)

        // Route private/local IP networks directly if bypassLan is enabled
        val localIps = mutableListOf<String>().apply {
            add("127.0.0.0/8")
            add("::1/128")
            if (settings.bypassLan) {
                addAll(listOf(
                    "10.0.0.0/8",
                    "172.16.0.0/12",
                    "192.168.0.0/16",
                    "169.254.0.0/16",
                    "fc00::/7",
                    "fe80::/10"
                ))
            }
        }
        val privateIpsRule = JSONObject().apply {
            put("ip_cidr", JSONArray(localIps))
            put("outbound", "direct")
        }
        newRules.put(privateIpsRule)

        // Route proxy and secure DNS domains/IPs directly
        val proxyHosts = getProxyServerHosts(config)
        val secureDnsHost = extractHostFromUrl(settings.secureDns)
        val directDomains = mutableListOf<String>()
        val directIps = mutableListOf<String>()

        // Add direct DNS server IP to directIps to ensure it bypasses the VPN tunnel
        val systemDnsList = getSystemDnsServers(context)
        var directDnsAddr = "178.22.122.100" // Default Shecan/Local DNS
        for (dnsIp in systemDnsList) {
            if (dnsIp != "8.8.8.8" && dnsIp != "8.8.4.4" && dnsIp != "1.1.1.1" && dnsIp != "1.0.0.1" && dnsIp != "9.9.9.9") {
                directDnsAddr = dnsIp
                break
            }
        }
        if (directDnsAddr.isNotEmpty() && isIpAddress(directDnsAddr)) {
            directIps.add(directDnsAddr)
        }
        val bootstrapDnsAddr = if (settings.bypassIran) "178.22.122.100" else "1.1.1.1"
        if (bootstrapDnsAddr.isNotEmpty() && isIpAddress(bootstrapDnsAddr)) {
            directIps.add(bootstrapDnsAddr)
        }

        if (settings.vpnMode == "gaming") {
            listOf("10.202.10.10", "10.202.10.11", "185.51.200.2", "178.22.122.100").forEach { ip ->
                if (!directIps.contains(ip)) {
                    directIps.add(ip)
                }
            }
        }

        for (host in proxyHosts) {
            if (host.isNotEmpty()) {
                if (isIpAddress(host)) {
                    directIps.add(host)
                } else {
                    directDomains.add(host)
                }
            }
        }

        if (directDomains.isNotEmpty()) {
            val bypassBypassRule = JSONObject().apply {
                put("domain", JSONArray(directDomains))
                put("outbound", "direct")
            }
            newRules.put(bypassBypassRule)
        }

        if (directIps.isNotEmpty()) {
            val bypassIpsRule = JSONObject().apply {
                put("ip_cidr", JSONArray(directIps))
                put("outbound", "direct")
            }
            newRules.put(bypassIpsRule)
        }

        if (settings.bypassIran) {
            val geositeFile = java.io.File(context.filesDir, "geosite-ir.srs")
            val geoipFile = java.io.File(context.filesDir, "geoip-ir.srs")

            // Inject or update local rule sets declaration
            val ruleSetArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "geoip-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geoipFile.absolutePath)
                })
                put(JSONObject().apply {
                    put("tag", "geosite-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geositeFile.absolutePath)
                })
            }
            route.put("rule_set", ruleSetArray)

            if (geositeFile.exists()) {
                // Add Iran Bypass Geosite Rule via rule_set
                val irGeosite = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geosite-ir")))
                    put("outbound", "direct")
                }
                newRules.put(irGeosite)
            }

            if (geoipFile.exists()) {
                // Add Iran Bypass GeoIP Rule via rule_set
                val irGeoip = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geoip-ir")))
                    put("outbound", "direct")
                }
                newRules.put(irGeoip)
            }

            // Add Iran .ir Suffix Rule
            val irSuffix = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("outbound", "direct")
            }
            newRules.put(irSuffix)
        }

        if (settings.vpnMode == "gaming") {
            val gameRouteRule = JSONObject().apply {
                put("domain_suffix", JSONArray(gamingDomains))
                put("outbound", if (settings.vpnModeTunnelGames) "proxy" else "direct")
            }
            newRules.put(gameRouteRule)
        } else if (settings.vpnMode == "ai_bypass" && settings.warpPrivateKey.isNotEmpty()) {
            val aiRouteRule = JSONObject().apply {
                put("domain_suffix", JSONArray(aiBypassDomains))
                put("outbound", "warp-out")
            }
            newRules.put(aiRouteRule)
        }

        route.put("rules", newRules)
        route.put("auto_detect_interface", true)
        route.put("override_android_vpn", true)
    }

    private fun injectOutbounds(config: JSONObject, settings: InjectorSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: JSONArray().also { config.put("outbounds", it) }

        val cleanOutbounds = JSONArray()
        var hasDirect = false
        var hasBlock = false

        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val type = out.optString("type")
            val tag = out.optString("tag")
            if (type == "dns" || tag == "dns-out") {
                continue // Remove deprecated DNS outbounds
            }
            if (tag == "direct") hasDirect = true
            if (tag == "block") hasBlock = true

            // Inject fragmentation into proxy outbound if enabled
            if (tag == "proxy" && settings.enableFragment) {
                injectFragmentToOutbound(out, settings)
            }
            // Inject multiplexing if enabled (disabled in gaming mode, and for Reality configs)
            val tls = out.optJSONObject("tls")
            val isReality = tls?.has("reality") ?: false
            if (tag == "proxy" && settings.enableMux && settings.vpnMode != "gaming" && !isReality) {
                val mux = JSONObject().apply {
                    put("enabled", true)
                    put("protocol", "smux")
                    put("max_connections", 4)
                    put("min_streams", 4)
                }
                out.put("multiplex", mux)
            } else if (tag == "proxy" && (settings.vpnMode == "gaming" || isReality)) {
                out.remove("multiplex")
            }
            cleanOutbounds.put(out)
        }

        if (!hasDirect) {
            cleanOutbounds.put(JSONObject().apply {
                put("type", "direct")
                put("tag", "direct")
            })
        }
        if (!hasBlock) {
            cleanOutbounds.put(JSONObject().apply {
                put("type", "block")
                put("tag", "block")
            })
        }

        // Inject Cloudflare WARP WireGuard outbound for AI Bypass Mode
        if (settings.vpnMode == "ai_bypass" && settings.warpPrivateKey.isNotEmpty()) {
            val warpOutbound = JSONObject().apply {
                put("type", "wireguard")
                put("tag", "warp-out")
                put("server", "engage.cloudflareclient.com")
                put("server_port", 2408)
                
                val localAddresses = JSONArray().apply {
                    put(settings.warpIpAddress.ifEmpty { "172.16.0.2/32" })
                    put("fd00::5/128")
                }
                put("local_address", localAddresses)
                put("private_key", settings.warpPrivateKey)
                put("peer_public_key", "bmXOC+F1fxEMDXGggWMuGcIy77Dd1KAD4kURmMyd378=")
                // Chain it over the active proxy outbound!
                put("dialer", "proxy")
            }
            cleanOutbounds.put(warpOutbound)
        }

        config.put("outbounds", cleanOutbounds)
    }

    private fun injectFragmentToOutbound(outbound: JSONObject, settings: InjectorSettings) {
        val tls = outbound.optJSONObject("tls") ?: JSONObject().also { outbound.put("tls", it) }
        tls.put("enabled", true)
        tls.put("fragment", true)
        tls.put("record_fragment", true)
        
        val interval = settings.fragmentInterval.trim()
        val delayStr = if (interval.isEmpty()) {
            "20ms"
        } else if (interval.endsWith("ms")) {
            interval
        } else {
            "${interval}ms"
        }
        tls.put("fragment_fallback_delay", delayStr)
    }

    private fun buildDefaultSkeleton(settings: InjectorSettings): JSONObject {
        return JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "info")
                put("timestamp", true)
            })
            put("outbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "proxy") // Fallback direct outbound labeled as proxy
                })
            })
        }
    }

    private fun buildConfigFromUri(uriStr: String, settings: InjectorSettings): JSONObject {
        val config = buildDefaultSkeleton(settings)
        val outbounds = config.getJSONArray("outbounds")

        try {
            val trimmed = uriStr.trim()
            val fragmentIdx = trimmed.indexOf("#")
            val name = if (fragmentIdx >= 0) {
                URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8")
            } else {
                "proxy"
            }
            
            val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
            val schemeIdx = rest.indexOf("://")
            if (schemeIdx < 0) return config
            val scheme = rest.substring(0, schemeIdx).lowercase()
            
            val content = rest.substring(schemeIdx + 3)
            val queryIdx = content.indexOf("?")
            val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
            val queryPart = if (queryIdx >= 0) content.substring(queryIdx + 1) else ""
            
            val atIdx = mainPart.indexOf("@")
            val userInfo = if (atIdx >= 0) mainPart.substring(0, atIdx) else ""
            val serverPart = if (atIdx >= 0) mainPart.substring(atIdx + 1) else mainPart
            
            val colonIdx = serverPart.lastIndexOf(":")
            val host = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
            val portStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
            val port = portStr.toIntOrNull() ?: 443
            
            val queryParams = parseQueryParams(queryPart)
            val tag = "proxy"
            val outbound = JSONObject()
            outbound.put("tag", tag)

            if (scheme == "vless") {
                outbound.put("type", "vless")
                outbound.put("uuid", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)
                outbound.put("packet_encoding", "xudp")

                val security = queryParams["security"]?.lowercase()
                val isReality = security == "reality"

                // Flow control (only allowed for standard TCP transport in sing-box)
                // headerType=http is a legacy obfuscation that Reality ignores,
                // so we skip flow injection when headerType=http to match original behavior.
                val type = queryParams["type"]
                val headerType = queryParams["headerType"] ?: queryParams["header_type"]
                val isStandardTcp = (type == null || type.equals("tcp", ignoreCase = true)) && headerType != "http"
                if (isStandardTcp) {
                    val flow = queryParams["flow"]
                    if (flow != null && flow.isNotEmpty() && flow != "none") {
                        outbound.put("flow", flow)
                    }
                }

                // TLS
                val hasTls = security == "tls" || isReality || queryParams["tls"] == "true" || queryParams["tls"] == "1"
                if (hasTls) {
                    val tls = JSONObject()
                    tls.put("enabled", true)
                    
                    val sni = queryParams["sni"] ?: queryParams["host"]
                    if (sni != null && sni.isNotEmpty()) {
                        tls.put("server_name", sni)
                    }

                    // Enable uTLS if security is reality or fingerprint is specified
                    if (isReality || queryParams.containsKey("fp")) {
                        val utls = JSONObject()
                        utls.put("enabled", true)
                        val fingerprint = queryParams["fp"] ?: "chrome"
                        utls.put("fingerprint", fingerprint)
                        tls.put("utls", utls)
                    }

                    if (isReality) {
                        val reality = JSONObject()
                        reality.put("enabled", true)
                        queryParams["pbk"]?.let { reality.put("public_key", it) }
                        queryParams["sid"]?.let { reality.put("short_id", it) }
                        tls.put("reality", reality)
                    }
                    outbound.put("tls", tls)
                }

                // Transport
                injectTransport(outbound, queryParams, host)
            } else if (scheme == "trojan") {
                outbound.put("type", "trojan")
                outbound.put("password", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)
                queryParams["sni"]?.let { tls.put("server_name", it) }

                if (queryParams.containsKey("fp")) {
                    val utls = JSONObject()
                    utls.put("enabled", true)
                    val fingerprint = queryParams["fp"] ?: "chrome"
                    utls.put("fingerprint", fingerprint)
                    tls.put("utls", utls)
                }
                outbound.put("tls", tls)

                // Transport
                injectTransport(outbound, queryParams, host)
            } else if (scheme == "ss") {
                outbound.put("type", "shadowsocks")
                // Shadowsocks format: ss://base64(method:password)@host:port or ss://base64(method:password@host:port)
                if (userInfo.isEmpty()) {
                    // Modern format base64
                    val decoded = String(Base64.decode(mainPart, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8)
                    if (decoded.contains("@")) {
                        val parts = decoded.split("@")
                        val creds = parts[0].split(":")
                        outbound.put("method", creds[0])
                        outbound.put("password", creds[1])
                        
                        val serverParts = parts[1].split(":")
                        outbound.put("server", serverParts[0])
                        outbound.put("server_port", serverParts[1].toInt())
                    }
                } else {
                    // Classic format
                    val decodedCreds = if (userInfo.contains(":")) {
                        userInfo
                    } else {
                        String(Base64.decode(userInfo, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8)
                    }
                    val creds = decodedCreds.split(":")
                    outbound.put("method", creds[0])
                    outbound.put("password", creds[1])
                    outbound.put("server", host)
                    outbound.put("server_port", port)
                }
            } else if (scheme == "socks" || scheme == "socks5") {
                outbound.put("type", "socks")
                outbound.put("server", host)
                outbound.put("server_port", port)
                if (userInfo.isNotEmpty()) {
                    val creds = userInfo.split(":")
                    outbound.put("username", creds[0])
                    if (creds.size > 1) {
                        outbound.put("password", creds[1])
                    }
                }
            } else if (scheme == "http" || scheme == "https") {
                outbound.put("type", "http")
                outbound.put("server", host)
                outbound.put("server_port", port)
                if (userInfo.isNotEmpty()) {
                    val creds = userInfo.split(":")
                    outbound.put("username", creds[0])
                    if (creds.size > 1) {
                        outbound.put("password", creds[1])
                    }
                }
                if (scheme == "https") {
                    val tls = JSONObject().apply {
                        put("enabled", true)
                        queryParams["sni"]?.let { put("server_name", it) } ?: put("server_name", host)
                    }
                    outbound.put("tls", tls)
                }
            } else if (scheme == "vmess") {
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
                    val id = vmessJson.optString("id")
                    val aidVal = vmessJson.opt("aid")
                    val aid = when (aidVal) {
                        is Number -> aidVal.toInt()
                        is String -> aidVal.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val scy = vmessJson.optString("scy", "auto")
                    val net = vmessJson.optString("net").lowercase()
                    val host = vmessJson.optString("host")
                    val path = vmessJson.optString("path")
                    val tlsVal = vmessJson.optString("tls").lowercase()
                    val sni = vmessJson.optString("sni")

                    outbound.put("type", "vmess")
                    outbound.put("server", add)
                    outbound.put("server_port", port)
                    outbound.put("uuid", id)
                    outbound.put("security", if (scy.isEmpty()) "auto" else scy)
                    outbound.put("alter_id", aid)
                    outbound.put("packet_encoding", "xudp")

                    val hasTls = tlsVal == "tls" || tlsVal == "true" || tlsVal == "1" || sni.isNotEmpty()
                    if (hasTls) {
                        val tls = JSONObject()
                        tls.put("enabled", true)
                        if (sni.isNotEmpty()) {
                            tls.put("server_name", sni)
                        } else if (host.isNotEmpty() && net != "tcp") {
                            tls.put("server_name", host)
                        }

                        val utls = JSONObject()
                        utls.put("enabled", true)
                        utls.put("fingerprint", "chrome")
                        tls.put("utls", utls)

                        val alpnVal = vmessJson.optString("alpn")
                        if (alpnVal.isNotEmpty()) {
                            val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (alpnList.isNotEmpty()) {
                                tls.put("alpn", JSONArray(alpnList))
                            }
                        }
                        outbound.put("tls", tls)
                    }

                    if (net == "ws" || net == "grpc" || net == "httpupgrade" || net == "kcp" || net == "mkcp" || net == "h2" || net == "http") {
                        val transport = JSONObject()
                        val transType = if (net == "h2") "http" else net
                        transport.put("type", transType)

                        val fallbackHost = if (host.isNotEmpty()) host else add
                        if (net == "ws") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (fallbackHost.isNotEmpty()) {
                                val headers = JSONObject()
                                headers.put("Host", fallbackHost)
                                transport.put("headers", headers)
                            }
                        } else if (net == "grpc") {
                            transport.put("service_name", path)
                        } else if (net == "httpupgrade" || net == "http" || net == "h2") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (fallbackHost.isNotEmpty()) {
                                if (net == "http" || net == "h2") {
                                    val hostArray = JSONArray()
                                    hostArray.put(fallbackHost)
                                    transport.put("host", hostArray)
                                } else {
                                    transport.put("host", fallbackHost)
                                }
                                val headers = JSONObject()
                                headers.put("Host", fallbackHost)
                                transport.put("headers", headers)
                            }
                        }
                        outbound.put("transport", transport)
                    }
                } else {
                    outbound.put("type", "vmess")
                    outbound.put("uuid", userInfo)
                    outbound.put("server", host)
                    outbound.put("server_port", port)
                    outbound.put("security", queryParams["scy"] ?: "auto")
                    outbound.put("alter_id", queryParams["aid"]?.toIntOrNull() ?: 0)
                    outbound.put("packet_encoding", "xudp")

                    val security = queryParams["security"]?.lowercase()
                    val hasTls = security == "tls" || queryParams["tls"] == "true" || queryParams["tls"] == "1"
                    if (hasTls) {
                        val tls = JSONObject()
                        tls.put("enabled", true)
                        queryParams["sni"]?.let { tls.put("server_name", it) }
                        val utls = JSONObject().apply {
                            put("enabled", true)
                            put("fingerprint", queryParams["fp"] ?: "chrome")
                        }
                        tls.put("utls", utls)
                        outbound.put("tls", tls)
                    }
                    injectTransport(outbound, queryParams, host)
                }
            } else if (scheme == "hysteria2" || scheme == "hy2") {
                outbound.put("type", "hysteria2")
                outbound.put("password", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)

                val sni = queryParams["sni"] ?: queryParams["peer"] ?: host
                if (sni.isNotEmpty()) {
                    tls.put("server_name", sni)
                }

                val insecure = queryParams["insecure"] == "1" || queryParams["insecure"] == "true" || queryParams["allowInsecure"] == "1" || queryParams["allowInsecure"] == "true"
                tls.put("insecure", insecure)

                val alpnVal = queryParams["alpn"]
                if (alpnVal != null && alpnVal.isNotEmpty()) {
                    val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tls.put("alpn", JSONArray(alpnList))
                }

                val pinSha256 = queryParams["pinSHA256"] ?: queryParams["pin_sha256"]
                if (pinSha256 != null && pinSha256.isNotEmpty()) {
                    tls.put("pin_sha256", JSONArray(listOf(pinSha256)))
                }

                outbound.put("tls", tls)

                val upStr = queryParams["up"] ?: queryParams["up_mbps"]
                if (upStr != null && upStr.isNotEmpty()) {
                    val upClean = upStr.filter { it.isDigit() }.toIntOrNull()
                    if (upClean != null) {
                        outbound.put("up_mbps", upClean)
                    }
                }
                val downStr = queryParams["down"] ?: queryParams["down_mbps"]
                if (downStr != null && downStr.isNotEmpty()) {
                    val downClean = downStr.filter { it.isDigit() }.toIntOrNull()
                    if (downClean != null) {
                        outbound.put("down_mbps", downClean)
                    }
                }

                val obfsType = queryParams["obfs"] ?: queryParams["obfs.type"] ?: queryParams["obfs_type"]
                val obfsPassword = queryParams["obfs-password"] ?: queryParams["obfs.password"] ?: queryParams["obfs_password"]
                if (obfsType != null && obfsType.isNotEmpty() && obfsType != "none") {
                    val obfsObj = JSONObject()
                    obfsObj.put("type", obfsType)
                    if (obfsPassword != null && obfsPassword.isNotEmpty()) {
                        obfsObj.put("password", obfsPassword)
                    }
                    outbound.put("obfs", obfsObj)
                }
            } else if (scheme == "tuic") {
                outbound.put("type", "tuic")

                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("uuid", parts[0])
                    outbound.put("password", parts[1])
                } else {
                    outbound.put("uuid", userInfo)
                    queryParams["pass"]?.let { outbound.put("password", it) }
                    queryParams["password"]?.let { outbound.put("password", it) }
                }

                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)

                val sni = queryParams["sni"] ?: queryParams["peer"] ?: host
                if (sni.isNotEmpty()) {
                    tls.put("server_name", sni)
                }

                val insecure = queryParams["insecure"] == "1" || queryParams["insecure"] == "true" || queryParams["allowInsecure"] == "1" || queryParams["allowInsecure"] == "true"
                tls.put("insecure", insecure)

                val alpnVal = queryParams["alpn"]
                if (alpnVal != null && alpnVal.isNotEmpty()) {
                    val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tls.put("alpn", JSONArray(alpnList))
                }

                val pinSha256 = queryParams["pinSHA256"] ?: queryParams["pin_sha256"]
                if (pinSha256 != null && pinSha256.isNotEmpty()) {
                    tls.put("pin_sha256", JSONArray(listOf(pinSha256)))
                }

                outbound.put("tls", tls)

                val cc = queryParams["congestion_control"] ?: queryParams["congestionControl"] ?: queryParams["cc"]
                if (cc != null && cc.isNotEmpty()) {
                    outbound.put("congestion_control", cc)
                }

                val urm = queryParams["udp_relay_mode"] ?: queryParams["udpRelayMode"]
                if (urm != null && urm.isNotEmpty()) {
                    outbound.put("udp_relay_mode", urm)
                }

                val zr = queryParams["zero_rtt_handshake"] ?: queryParams["zeroRttHandshake"] ?: queryParams["zero_rtt"] ?: queryParams["zeroRtt"]
                if (zr != null && zr.isNotEmpty()) {
                    val zrBool = zr == "1" || zr == "true"
                    outbound.put("zero_rtt_handshake", zrBool)
                }

                val hb = queryParams["heartbeat"] ?: queryParams["heartbeat_interval"]
                if (hb != null && hb.isNotEmpty()) {
                    val hbStr = if (hb.endsWith("s") || hb.endsWith("ms")) hb else "${hb}s"
                    outbound.put("heartbeat", hbStr)
                }
            }

            // Replace the fallback direct outbound in index 0
            outbounds.put(0, outbound)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return config
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isEmpty()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx).replace("+", "%2B"), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1).replace("+", "%2B"), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun sanitizePortFields(config: JSONObject) {
        // 1. Sanitize outbounds
        val outbounds = config.optJSONArray("outbounds")
        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                if (outbound.has("port")) {
                    val portVal = outbound.get("port")
                    outbound.remove("port")
                    if (!outbound.has("server_port") || outbound.optInt("server_port") == 0) {
                        outbound.put("server_port", portVal)
                    }
                }
            }
        }

        // 2. Sanitize inbounds
        val inbounds = config.optJSONArray("inbounds")
        if (inbounds != null) {
            for (i in 0 until inbounds.length()) {
                val inbound = inbounds.optJSONObject(i) ?: continue
                if (inbound.has("port")) {
                    val portVal = inbound.get("port")
                    inbound.remove("port")
                    if (!inbound.has("listen_port") || inbound.optInt("listen_port") == 0) {
                        inbound.put("listen_port", portVal)
                    }
                }
            }
        }
    }

    private fun injectTransport(outbound: JSONObject, queryParams: Map<String, String>, defaultHost: String) {
        var type = queryParams["type"]
        val headerType = queryParams["headerType"] ?: queryParams["header_type"]
        
        // Map type=tcp & headerType=http to httpupgrade transport, and type=h2 to http transport
        if ((type == null || type == "tcp") && headerType == "http") {
            type = "httpupgrade"
        } else if (type == "h2") {
            type = "http"
        }
        
        if (type == null) return
        
        if (type == "ws" || type == "grpc" || type == "httpupgrade" || type == "xhttp" || type == "kcp" || type == "mkcp" || type == "http") {
            val transport = JSONObject()
            if (type == "kcp" || type == "mkcp") {
                transport.put("type", "kcp")
            } else {
                transport.put("type", type)
            }
            if (type == "ws") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
                val edVal = queryParams["ed"]?.toIntOrNull()
                if (edVal != null) {
                    transport.put("max_early_data", edVal)
                    transport.put("early_data_header_name", "Sec-Raw-Websocket-Protocol")
                }
            } else if (type == "grpc") {
                val serviceName = queryParams["serviceName"] ?: queryParams["service_name"] ?: ""
                transport.put("service_name", serviceName)
            } else if (type == "httpupgrade") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    transport.put("host", host)
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
            } else if (type == "http") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    val hostArray = JSONArray()
                    hostArray.put(host)
                    transport.put("host", hostArray)
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
                val method = queryParams["method"] ?: "GET"
                transport.put("method", method)
            } else if (type == "kcp" || type == "mkcp") {
                val seed = queryParams["seed"]
                if (seed != null && seed.isNotEmpty()) {
                    transport.put("seed", seed)
                }
                val hType = queryParams["headerType"] ?: queryParams["header_type"] ?: queryParams["header"]
                if (hType != null && hType.isNotEmpty()) {
                    transport.put("header_type", hType)
                    val headerObj = JSONObject()
                    headerObj.put("type", hType)
                    transport.put("header", headerObj)
                }
            } else if (type == "xhttp") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    transport.put("host", host)
                }
                val mode = queryParams["mode"] ?: "stream-one"
                transport.put("mode", mode)
                
                val extraStr = queryParams["extra"]
                if (extraStr != null && extraStr.isNotEmpty()) {
                    try {
                        val extraObj = JSONObject(extraStr)
                        val keys = extraObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            transport.put(key, extraObj.get(key))
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
            outbound.put("transport", transport)
        }
    }

    private fun getProxyServerHosts(config: JSONObject): List<String> {
        val hosts = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return hosts
        val proxyTypes = setOf("vless", "trojan", "shadowsocks", "vmess", "shadowsocksr", "tuic", "hysteria", "hysteria2", "socks", "http")
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val type = outbound.optString("type")
            if (proxyTypes.contains(type)) {
                val server = outbound.optString("server")
                if (server.isNotEmpty()) {
                    hosts.add(server)
                }
            }
        }
        return hosts
    }

    private fun extractHostFromUrl(urlStr: String): String? {
        return try {
            val uri = URI(urlStr)
            uri.host ?: urlStr.substringAfter("://").substringBefore("/")
        } catch (e: Exception) {
            null
        }
    }

    private fun isIpAddress(host: String): Boolean {
        val ipv4Pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$"
        val ipv6Pattern = "^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$"
        return host.matches(ipv4Pattern.toRegex()) || host.matches(ipv6Pattern.toRegex())
    }

    private fun preResolveProxyServers(context: Context, config: JSONObject, settings: InjectorSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: return
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val tag = outbound.optString("tag")
            if (tag == "proxy") {
                val server = outbound.optString("server")
                if (server.isNotEmpty() && !isIpAddress(server)) {
                    android.util.Log.i("ExpressiveBox", "Pre-resolving proxy server domain: $server")
                    val resolvedIp = resolveDomainWithFallbacks(context, server, settings)
                    if (resolvedIp != null) {
                        android.util.Log.i("ExpressiveBox", "Proxy server $server successfully pre-resolved to IP: $resolvedIp")
                        
                        // If outbound has TLS enabled, ensure SNI (server_name) is set to original hostname
                        val tls = outbound.optJSONObject("tls")
                        if (tls != null) {
                            if (tls.optBoolean("enabled", false) && !tls.has("server_name")) {
                                tls.put("server_name", server)
                            }
                        }
                        
                        // Preserve original domain hostname in transport host / Host headers if pre-resolved
                        val transport = outbound.optJSONObject("transport")
                        if (transport != null) {
                            val transType = transport.optString("type")
                            if (transType == "ws") {
                                var headers = transport.optJSONObject("headers")
                                if (headers == null) {
                                    headers = JSONObject()
                                    transport.put("headers", headers)
                                }
                                if (!headers.has("Host")) {
                                    headers.put("Host", server)
                                }
                            } else if (transType == "httpupgrade" || transType == "http") {
                                if (!transport.has("host")) {
                                    if (transType == "http") {
                                        val hostArray = JSONArray()
                                        hostArray.put(server)
                                        transport.put("host", hostArray)
                                    } else {
                                        transport.put("host", server)
                                    }
                                }
                                var headers = transport.optJSONObject("headers")
                                if (headers == null) {
                                    headers = JSONObject()
                                    transport.put("headers", headers)
                                }
                                if (!headers.has("Host")) {
                                    headers.put("Host", server)
                                }
                            } else if (transType == "xhttp") {
                                if (!transport.has("host")) {
                                    transport.put("host", server)
                                }
                            }
                        }
                        
                        // Overwrite server domain with the resolved IP
                        outbound.put("server", resolvedIp)
                    } else {
                        android.util.Log.w("ExpressiveBox", "Failed to pre-resolve proxy server: $server. Falling back to default routing.")
                    }
                }
            }
        }
    }

    private fun resolveDomainWithFallbacks(context: Context, domain: String, settings: InjectorSettings): String? {
        val dnsServers = mutableListOf<String>()
        
        if (settings.bypassIran) {
            // For Iran: prioritize clean domestic resolvers that bypass censorship/sanctions, then Cloudflare
            listOf("185.51.200.2", "178.22.122.100", "10.202.10.10", "1.1.1.1", "8.8.8.8").forEach { ip ->
                if (!dnsServers.contains(ip)) {
                    dnsServers.add(ip)
                }
            }
        } else {
            // Outside Iran: prioritize Cloudflare, Google, then Shecan
            listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "178.22.122.100").forEach { ip ->
                if (!dnsServers.contains(ip)) {
                    dnsServers.add(ip)
                }
            }
        }

        // Add system DNS at the end as a fallback
        val systemDns = getSystemDnsServers(context)
        systemDns.forEach { ip ->
            if (!dnsServers.contains(ip)) {
                dnsServers.add(ip)
            }
        }

        for (dnsServer in dnsServers) {
            val ip = resolveDomainDirectly(domain, dnsServer)
            if (ip != null) {
                return ip
            }
        }

        // Final fallback: try system DNS (may be hijacked, but better than nothing)
        try {
            val addresses = java.net.InetAddress.getAllByName(domain)
            for (addr in addresses) {
                val ip = addr.hostAddress
                if (ip != null && isPublicIp(ip)) {
                    return ip
                }
            }
        } catch (e: Exception) {}

        return null
    }

    private fun resolveDomainDirectly(domain: String, dnsServerIp: String, timeoutMs: Int = 2000): String? {
        // Clean dnsServerIp from protocol prefixes if present (e.g. tcp://)
        val cleanDnsIp = dnsServerIp.substringAfter("tcp://").substringAfter("udp://")
        
        // Try TCP DNS query first to bypass UDP DNS hijacking in Iran
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(cleanDnsIp, 53), timeoutMs)
            socket.soTimeout = timeoutMs

            val baos = java.io.ByteArrayOutputStream()
            val dos = java.io.DataOutputStream(baos)

            // DNS Header
            dos.writeShort(0x1234) // Transaction ID
            dos.writeShort(0x0100) // Flags: Standard Query
            dos.writeShort(1)      // Questions
            dos.writeShort(0)      // Answer RRs
            dos.writeShort(0)      // Authority RRs
            dos.writeShort(0)      // Additional RRs

            // Question: Domain Name
            val parts = domain.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0) // End of domain

            dos.writeShort(1) // Type A
            dos.writeShort(1) // Class IN

            val queryData = baos.toByteArray()
            
            // For DNS over TCP, prefix the payload with its 2-byte length!
            val outStream = socket.getOutputStream()
            val dataOut = java.io.DataOutputStream(outStream)
            dataOut.writeShort(queryData.size)
            dataOut.write(queryData)
            dataOut.flush()

            val inStream = socket.getInputStream()
            val dataIn = java.io.DataInputStream(inStream)
            
            // Read 2-byte response length header
            val responseLength = dataIn.readUnsignedShort()
            val response = ByteArray(responseLength)
            dataIn.readFully(response)
            socket.close()

            // Parse Response
            val responseStream = java.io.DataInputStream(java.io.ByteArrayInputStream(response))
            val txId = responseStream.readUnsignedShort()
            val flags = responseStream.readUnsignedShort()
            val questions = responseStream.readUnsignedShort()
            val answers = responseStream.readUnsignedShort()
            val authority = responseStream.readUnsignedShort()
            val additional = responseStream.readUnsignedShort()

            // Skip Question Section
            for (q in 0 until questions) {
                var len = responseStream.readByte().toInt()
                while (len > 0) {
                    responseStream.skipBytes(len)
                    len = responseStream.readByte().toInt()
                }
                responseStream.skipBytes(4)
            }

            // Parse Answers
            for (a in 0 until answers) {
                var b = responseStream.readByte().toInt() and 0xFF
                while (b > 0) {
                    if ((b and 0xC0) == 0xC0) {
                        responseStream.readByte()
                        break
                    } else {
                        responseStream.skipBytes(b)
                        b = responseStream.readByte().toInt() and 0xFF
                    }
                }

                val type = responseStream.readUnsignedShort()
                val clazz = responseStream.readUnsignedShort()
                val ttl = responseStream.readInt()
                val dataLength = responseStream.readUnsignedShort()

                if (type == 1 && dataLength == 4) { // Type A (IPv4)
                    val ipBytes = ByteArray(4)
                    responseStream.readFully(ipBytes)
                    val ip = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
                    if (isPublicIp(ip)) {
                        return ip
                    }
                } else {
                    responseStream.skipBytes(dataLength)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpressiveBox", "TCP DNS query to $cleanDnsIp failed: ${e.message}. Falling back to UDP.")
        }

        // Fallback to UDP if TCP fails
        return resolveDomainDirectlyUDP(domain, cleanDnsIp, timeoutMs)
    }

    private fun resolveDomainDirectlyUDP(domain: String, dnsServerIp: String, timeoutMs: Int = 2000): String? {
        try {
            val socket = java.net.DatagramSocket()
            socket.soTimeout = timeoutMs
            val address = java.net.InetAddress.getByName(dnsServerIp)

            val baos = java.io.ByteArrayOutputStream()
            val dos = java.io.DataOutputStream(baos)

            // DNS Header
            dos.writeShort(0x1234) // Transaction ID
            dos.writeShort(0x0100) // Flags: Standard Query
            dos.writeShort(1)      // Questions
            dos.writeShort(0)      // Answer RRs
            dos.writeShort(0)      // Authority RRs
            dos.writeShort(0)      // Additional RRs

            // Question: Domain Name
            val parts = domain.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0) // End of domain

            dos.writeShort(1) // Type A
            dos.writeShort(1) // Class IN

            val queryData = baos.toByteArray()
            val packet = java.net.DatagramPacket(queryData, queryData.size, address, 53)
            socket.send(packet)

            val buffer = ByteArray(512)
            val responsePacket = java.net.DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            socket.close()

            // Parse Response
            val response = responsePacket.data
            val length = responsePacket.length
            if (length < 12) return null

            val responseStream = java.io.DataInputStream(java.io.ByteArrayInputStream(response, 0, length))
            val txId = responseStream.readUnsignedShort()
            val flags = responseStream.readUnsignedShort()
            val questions = responseStream.readUnsignedShort()
            val answers = responseStream.readUnsignedShort()
            val authority = responseStream.readUnsignedShort()
            val additional = responseStream.readUnsignedShort()

            // Skip Question Section
            for (q in 0 until questions) {
                // Skip domain labels
                var len = responseStream.readByte().toInt()
                while (len > 0) {
                    responseStream.skipBytes(len)
                    len = responseStream.readByte().toInt()
                }
                responseStream.skipBytes(4) // Skip Type and Class
            }

            // Parse Answers
            for (a in 0 until answers) {
                // Name: skip compressed name pointer or labels
                var b = responseStream.readByte().toInt() and 0xFF
                while (b > 0) {
                    if ((b and 0xC0) == 0xC0) {
                        // Pointer: skip the second byte of the pointer and end
                        responseStream.readByte()
                        break
                    } else {
                        responseStream.skipBytes(b)
                        b = responseStream.readByte().toInt() and 0xFF
                    }
                }

                val type = responseStream.readUnsignedShort()
                val clazz = responseStream.readUnsignedShort()
                val ttl = responseStream.readInt()
                val dataLength = responseStream.readUnsignedShort()

                if (type == 1 && dataLength == 4) { // Type A (IPv4)
                    val ipBytes = ByteArray(4)
                    responseStream.readFully(ipBytes)
                    val ip = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
                    if (isPublicIp(ip)) {
                        return ip
                    }
                } else {
                    responseStream.skipBytes(dataLength)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpressiveBox", "UDP DNS query to $dnsServerIp failed: ${e.message}")
        }
        return null
    }

    private fun isPublicIp(ip: String): Boolean {
        if (!isIpAddress(ip)) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        try {
            val p0 = parts[0].toInt()
            val p1 = parts[1].toInt()
            if (p0 == 127) return false
            if (p0 == 10) return false
            if (p0 == 172 && p1 in 16..31) return false
            if (p0 == 192 && p1 == 168) return false
            if (p0 == 169 && p1 == 254) return false
            if (p0 == 0 || p0 >= 224) return false
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
