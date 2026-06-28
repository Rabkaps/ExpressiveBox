package com.hambalapps.expressivebox.desktop.vpn

import com.hambalapps.expressivebox.desktop.data.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigInjector {

    fun injectConfig(
        rawProfile: String,
        settings: UserSettings,
        geoipPath: String,
        geositePath: String,
        logPath: String
    ): String {
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
                buildDefaultSkeleton(settings)
            }

            // Override log configuration to output to logPath
            val logObj = configJson.optJSONObject("log") ?: JSONObject().also { configJson.put("log", it) }
            logObj.put("level", "info")
            logObj.put("output", logPath)
            logObj.put("timestamp", true)

            // Sanitize invalid port fields in outbounds and inbounds
            sanitizePortFields(configJson)

            // 1. Inject or update inbounds (mixed local proxy at 127.0.0.1:2080)
            injectMixedInbound(configJson, settings)

            // 2. Inject or update DNS (Split DNS rules)
            injectDns(configJson, settings)

            // 3. Inject or update Routing Rules (Iran bypass)
            injectRouting(configJson, settings, geoipPath, geositePath)

            // 4. Inject direct/block outbounds
            injectOutbounds(configJson, settings)

            // 5. Add Clash API for traffic stats
            injectClashApi(configJson)

            return configJson.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return buildDefaultSkeleton(settings).toString(2)
        }
    }

    private fun injectMixedInbound(config: JSONObject, settings: UserSettings) {
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        val newInbounds = JSONArray()
        
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            val type = inbound.optString("type")
            if (type != "mixed" && type != "tun") {
                newInbounds.put(inbound)
            }
        }

        val mixedInbound = JSONObject().apply {
            put("type", "mixed")
            put("tag", "mixed-in")
            put("listen", "127.0.0.1")
            put("listen_port", 2080)
        }
        newInbounds.put(mixedInbound)

        if (settings.enableTun) {
            val tunInbound = JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                put("interface_name", "sing-box-tun")
                put("address", JSONArray(listOf("172.19.0.1/30", "fdfe:dcba:9876::1/126")))
                put("auto_route", true)
                put("strict_route", true)
                put("stack", settings.tunStack.ifEmpty { "mixed" })
                put("platform", JSONObject().apply {
                    put("http_proxy", JSONObject().apply {
                        put("enabled", false)
                    })
                })
            }
            newInbounds.put(tunInbound)
        }

        config.put("inbounds", newInbounds)
    }

    private fun injectClashApi(config: JSONObject) {
        val experimental = config.optJSONObject("experimental") ?: JSONObject().also { config.put("experimental", it) }
        val clashApi = JSONObject().apply {
            put("external_controller", "127.0.0.1:9090")
            put("secret", "")
        }
        experimental.put("clash_api", clashApi)
    }

    private fun getSystemDnsServers(): List<String> {
        val dnsList = mutableListOf<String>()
        try {
            val clazz = Class.forName("sun.net.dns.ResolverConfiguration")
            val openMethod = clazz.getMethod("open")
            val instance = openMethod.invoke(null)
            val nameserversMethod = clazz.getMethod("nameservers")
            val ns = nameserversMethod.invoke(instance) as List<*>
            ns.forEach {
                val dnsHost = it.toString().trim()
                if (dnsHost.isNotEmpty() && !dnsHost.contains(":")) {
                    dnsList.add(dnsHost)
                }
            }
        } catch (e: Exception) {
            // Silently ignore
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
        } else if (trimmed.startsWith("tls://")) {
            serverObj.put("type", "tls")
            serverObj.put("server", trimmed.substringAfter("tls://"))
        } else if (trimmed.startsWith("quic://")) {
            serverObj.put("type", "quic")
            serverObj.put("server", trimmed.substringAfter("quic://"))
        } else {
            serverObj.put("type", "udp")
            serverObj.put("server", trimmed)
        }
        return serverObj
    }

    private fun injectDns(config: JSONObject, settings: UserSettings) {
        val dns = JSONObject()
        val servers = JSONArray()

        // 1. Secure DNS Server (routes via the proxy)
        val secureServer = createDnsServer("dns-secure", settings.secureDns, "proxy")
        servers.put(secureServer)

        // 2. Local Bypass DNS Server for Iran domains (runs directly, detouring proxy)
        val systemDnsList = getSystemDnsServers()
        var directDnsAddr = "178.22.122.100" // Default Shecan/Local DNS
        
        for (dnsIp in systemDnsList) {
            if (dnsIp != "8.8.8.8" && dnsIp != "8.8.4.4" && dnsIp != "1.1.1.1" && dnsIp != "1.0.0.1" && dnsIp != "9.9.9.9") {
                directDnsAddr = dnsIp
                break
            }
        }

        val directServer = createDnsServer("dns-direct", directDnsAddr, null)
        servers.put(directServer)

        // 3. Clean Bootstrap DNS Server for resolving proxy/DNS hostnames reliably
        val bootstrapDnsAddr = "https://8.8.8.8/dns-query"
        val bootstrapServer = createDnsServer("dns-bootstrap", bootstrapDnsAddr, null)
        servers.put(bootstrapServer)

        dns.put("servers", servers)

        val rules = JSONArray()

        // Inject bootstrap rules for proxy server domain and secure DNS DoH domain
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
            val irGeositeRule = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geosite-ir")))
                put("server", "dns-direct")
            }
            rules.put(irGeositeRule)

            val irSuffixRule = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("server", "dns-direct")
            }
            rules.put(irSuffixRule)
        }

        dns.put("rules", rules)
        config.put("dns", dns)
    }

    private fun injectRouting(config: JSONObject, settings: UserSettings, geoipPath: String, geositePath: String) {
        val route = config.optJSONObject("route") ?: JSONObject().also { config.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

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
        }
        newRules.put(sniffRule)

        // Add standard DNS routing rule
        val dnsRule = JSONObject().apply {
            put("protocol", "dns")
            put("action", "hijack-dns")
        }
        newRules.put(dnsRule)

        // Block Private DNS (DoT)
        val blockDotRule = JSONObject().apply {
            put("port", JSONArray(listOf(853)))
            put("outbound", "block")
        }
        newRules.put(blockDotRule)

        // Route private/local IP networks directly
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

        val systemDnsList = getSystemDnsServers()
        var directDnsAddr = "178.22.122.100"
        for (dnsIp in systemDnsList) {
            if (dnsIp != "8.8.8.8" && dnsIp != "8.8.4.4" && dnsIp != "1.1.1.1" && dnsIp != "1.0.0.1" && dnsIp != "9.9.9.9") {
                directDnsAddr = dnsIp
                break
            }
        }
        if (directDnsAddr.isNotEmpty() && isIpAddress(directDnsAddr)) {
            directIps.add(directDnsAddr)
        }
        val bootstrapDnsAddr = "8.8.8.8"
        if (bootstrapDnsAddr.isNotEmpty() && isIpAddress(bootstrapDnsAddr)) {
            directIps.add(bootstrapDnsAddr)
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
            // Inject or update local rule sets declaration
            val ruleSetArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "geoip-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geoipPath)
                })
                put(JSONObject().apply {
                    put("tag", "geosite-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geositePath)
                })
            }
            route.put("rule_set", ruleSetArray)

            // Add Iran Bypass Geosite Rule via rule_set
            val irGeosite = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geosite-ir")))
                put("outbound", "direct")
            }
            newRules.put(irGeosite)

            // Add Iran Bypass GeoIP Rule via rule_set
            val irGeoip = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geoip-ir")))
                put("outbound", "direct")
            }
            newRules.put(irGeoip)

            // Add Iran .ir Suffix Rule
            val irSuffix = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("outbound", "direct")
            }
            newRules.put(irSuffix)
        }

        route.put("rules", newRules)
        route.put("default_domain_resolver", "dns-bootstrap")
        route.put("auto_detect_interface", true)
    }

    private fun injectOutbounds(config: JSONObject, settings: UserSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: JSONArray().also { config.put("outbounds", it) }
        val cleanOutbounds = JSONArray()
        var hasDirect = false
        var hasBlock = false

        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val type = out.optString("type")
            val tag = out.optString("tag")
            if (type == "dns" || tag == "dns-out") {
                continue
            }
            if (tag == "direct") hasDirect = true
            if (tag == "block") hasBlock = true

            if (tag == "proxy" && settings.enableFragment) {
                injectFragmentToOutbound(out, settings)
            }
            if (tag == "proxy" && settings.enableMux) {
                val mux = JSONObject().apply {
                    put("enabled", true)
                    put("protocol", "smux")
                    put("max_connections", 4)
                    put("min_streams", 4)
                }
                out.put("multiplex", mux)
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
        config.put("outbounds", cleanOutbounds)
    }

    private fun injectFragmentToOutbound(outbound: JSONObject, settings: UserSettings) {
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

    private fun buildDefaultSkeleton(settings: UserSettings): JSONObject {
        return JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "info")
                put("timestamp", true)
            })
            put("outbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "proxy")
                })
            })
        }
    }

    private fun buildConfigFromUri(uriStr: String, settings: UserSettings): JSONObject {
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

                val hasTls = security == "tls" || isReality || queryParams["tls"] == "true" || queryParams["tls"] == "1"
                if (hasTls) {
                    val tls = JSONObject()
                    tls.put("enabled", true)
                    
                    val sni = queryParams["sni"] ?: queryParams["host"]
                    if (sni != null && sni.isNotEmpty()) {
                        tls.put("server_name", sni)
                    }

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

                injectTransport(outbound, queryParams)
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

                injectTransport(outbound, queryParams)
            } else if (scheme == "ss") {
                outbound.put("type", "shadowsocks")
                if (userInfo.isEmpty()) {
                    val decoded = String(java.util.Base64.getUrlDecoder().decode(mainPart), StandardCharsets.UTF_8)
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
                    val decodedCreds = if (userInfo.contains(":")) {
                        userInfo
                    } else {
                        tryBase64Decode(userInfo) ?: userInfo
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
                    val portNum = when (portVal) {
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
                    val h = vmessJson.optString("host")
                    val path = vmessJson.optString("path")
                    val tlsVal = vmessJson.optString("tls").lowercase()
                    val sni = vmessJson.optString("sni")

                    outbound.put("type", "vmess")
                    outbound.put("server", add)
                    outbound.put("server_port", portNum)
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
                        } else if (h.isNotEmpty() && net != "tcp") {
                            tls.put("server_name", h)
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

                        if (net == "ws") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                val headers = JSONObject()
                                headers.put("Host", h)
                                transport.put("headers", headers)
                            }
                        } else if (net == "grpc") {
                            transport.put("service_name", path)
                        } else if (net == "httpupgrade" || net == "http" || net == "h2") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                transport.put("host", h)
                                val headers = JSONObject()
                                headers.put("Host", h)
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
                    injectTransport(outbound, queryParams)
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
            } else if (scheme == "tuic") {
                outbound.put("type", "tuic")
                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("uuid", parts[0])
                    outbound.put("password", parts[1])
                } else {
                    outbound.put("uuid", userInfo)
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
                outbound.put("tls", tls)

                val congestionControl = queryParams["congestion_control"] ?: queryParams["congestionControl"] ?: "bbr"
                outbound.put("congestion_control", congestionControl)
                
                val udpRelayMode = queryParams["udp_relay_mode"] ?: queryParams["udpRelayMode"] ?: "native"
                outbound.put("udp_relay_mode", udpRelayMode)
            }

            outbound.put("tag", "proxy")
            outbounds.put(0, outbound)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return config
    }

    private fun injectTransport(outbound: JSONObject, queryParams: Map<String, String>) {
        var type = queryParams["type"]?.lowercase()
        val headerType = queryParams["headerType"]?.lowercase() ?: queryParams["header_type"]?.lowercase()
        val security = queryParams["security"]?.lowercase()
        val isReality = security == "reality"
        if ((type == null || type == "tcp") && headerType == "http" && !isReality) {
            type = "http"
        }
        if (type == null) return
        if (type == "ws" || type == "grpc" || type == "httpupgrade" || type == "kcp" || type == "mkcp" || type == "http") {
            val transport = JSONObject()
            transport.put("type", if (type == "mkcp") "kcp" else type)

            if (type == "ws") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { host ->
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
            } else if (type == "grpc") {
                queryParams["serviceName"]?.let { transport.put("service_name", it) }
            } else if (type == "httpupgrade") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { transport.put("host", it) }
            } else if (type == "http") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { transport.put("host", it) }
            }
            outbound.put("transport", transport)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (query.isEmpty()) return params
        try {
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                val key = if (idx > 0) URLDecoder.decode(pair.substring(0, idx).replace("+", "%2B"), "UTF-8") else pair
                val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1).replace("+", "%2B"), "UTF-8") else ""
                params[key] = value
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    fun tryBase64Decode(src: String): String? {
        val clean = src.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        return try {
            val bytes = java.util.Base64.getDecoder().decode(clean)
            String(bytes, java.nio.charset.StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val bytes = java.util.Base64.getUrlDecoder().decode(clean)
                String(bytes, java.nio.charset.StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                try {
                    val padded = when (clean.length % 4) {
                        2 -> "$clean=="
                        3 -> "$clean="
                        else -> clean
                    }
                    val bytes = java.util.Base64.getDecoder().decode(padded)
                    String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                } catch (e3: Exception) {
                    try {
                        val padded = when (clean.length % 4) {
                            2 -> "$clean=="
                            3 -> "$clean="
                            else -> clean
                        }
                        val bytes = java.util.Base64.getUrlDecoder().decode(padded)
                        String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    } catch (e4: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun sanitizePortFields(config: JSONObject) {
        val inbounds = config.optJSONArray("inbounds")
        if (inbounds != null) {
            for (i in 0 until inbounds.length()) {
                val inbound = inbounds.optJSONObject(i) ?: continue
                sanitizePortInObject(inbound, "listen_port")
            }
        }

        val outbounds = config.optJSONArray("outbounds")
        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                sanitizePortInObject(outbound, "server_port")
            }
        }
    }

    private fun sanitizePortInObject(obj: JSONObject, portField: String) {
        if (obj.has(portField)) {
            val value = obj.opt(portField)
            if (value is String) {
                val parsed = value.toIntOrNull()
                if (parsed != null) {
                    obj.put(portField, parsed)
                } else {
                    obj.remove(portField)
                }
            }
        }
    }

    private fun getProxyServerHosts(config: JSONObject): List<String> {
        val hosts = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return hosts
        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            if (out.optString("tag") == "proxy") {
                val server = out.optString("server")
                if (server.isNotEmpty()) {
                    hosts.add(server)
                }
            }
        }
        return hosts
    }

    private fun extractHostFromUrl(urlString: String): String? {
        return try {
            val cleaned = urlString.trim()
            val withoutProtocol = cleaned.substringAfter("://")
            val hostPortPath = withoutProtocol.substringBefore("/")
            hostPortPath.substringBefore(":")
        } catch (e: Exception) {
            null
        }
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.isEmpty()) return false
        val parts = host.split(".")
        if (parts.size == 4) {
            return parts.all { it.toIntOrNull() in 0..255 }
        }
        return host.contains(":")
    }
}
