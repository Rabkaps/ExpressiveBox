package com.hambalapps.expressivebox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.hambalapps.expressivebox.Config

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val IS_ADVANCED_MODE = booleanPreferencesKey("is_advanced_mode")
        val BYPASS_IRAN = booleanPreferencesKey("bypass_iran")
        val SECURE_DNS = stringPreferencesKey("secure_dns")
        val TUN_STACK = stringPreferencesKey("tun_stack")
        val ENABLE_FRAGMENT = booleanPreferencesKey("enable_fragment")
        val FRAGMENT_LENGTH = stringPreferencesKey("fragment_length")
        val FRAGMENT_INTERVAL = stringPreferencesKey("fragment_interval")
        val ENABLE_MUX = booleanPreferencesKey("enable_mux")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
        val SUBSCRIPTION_SERVERS = stringPreferencesKey("subscription_servers")
        val SUBSCRIPTION_LIST = stringPreferencesKey("subscription_list")
        val ACTIVE_SUB_ID = stringPreferencesKey("active_sub_id")
        val SHOW_LIVE_NOTIFICATION = booleanPreferencesKey("show_live_notification")
        val SPLIT_TUNNELING_ENABLED = booleanPreferencesKey("split_tunneling_enabled")
        val SPLIT_TUNNELING_MODE = stringPreferencesKey("split_tunneling_mode")
        val SPLIT_TUNNELING_APPS = stringSetPreferencesKey("split_tunneling_apps")
        val MANUAL_SERVERS = stringPreferencesKey("manual_servers")
        val SPECIAL_THEME = stringPreferencesKey("special_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val AUTO_UPDATE_SUBS = booleanPreferencesKey("auto_update_subs")
        val AUTO_UPDATE_INTERVAL = stringPreferencesKey("auto_update_interval")
        val LAST_SUBS_UPDATE_TIME = longPreferencesKey("last_subs_update_time")
        val AUTO_CONNECT_SUBS = stringSetPreferencesKey("auto_connect_subs")
        val SHOW_LOGS_TAB = booleanPreferencesKey("show_logs_tab")
        val VPN_MODE = stringPreferencesKey("vpn_mode")
        val WARP_PRIVATE_KEY = stringPreferencesKey("warp_private_key")
        val WARP_PUBLIC_KEY = stringPreferencesKey("warp_public_key")
        val WARP_IP_ADDRESS = stringPreferencesKey("warp_ip_address")
        val WARP_CLIENT_ID = stringPreferencesKey("warp_client_id")
        val VPN_MODE_TUNNEL_GAMES = booleanPreferencesKey("vpn_mode_tunnel_games")
        val DELAY_TEST_URL = stringPreferencesKey("delay_test_url")
        val WARP_DETOUR_MODE = stringPreferencesKey("warp_detour_mode")
        val WARP_PORT = stringPreferencesKey("warp_port")
        
        private val defaultThemeKey = if (Config.IS_SPECIAL) "cherry_blossom" else "dynamic"

        val defaultSettings = UserSettings(
            isAdvancedMode = false,
            bypassIran = true,
            secureDns = "https://1.1.1.1/dns-query",
            tunStack = "mixed",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = false,
            activeProfile = "",
            subscriptionUrl = "",
            subscriptionServers = "",
            subscriptionList = "",
            activeSubId = "",
            showLiveNotification = false,
            splitTunnelingEnabled = false,
            splitTunnelingMode = "bypass",
            splitTunnelingApps = emptySet(),
            manualServers = "",
            specialTheme = defaultThemeKey,
            themeMode = "system",
            cardStyle = "glass",
            bypassLan = true,
            autoUpdateSubs = true,
            autoUpdateInterval = "daily",
            lastSubsUpdateTime = 0L,
            autoConnectSubs = emptySet(),
            showLogsTab = true,
            vpnMode = "standard",
            warpPrivateKey = "",
            warpPublicKey = "",
            warpIpAddress = "",
            warpClientId = "",
            vpnModeTunnelGames = false,
            delayTestUrl = "http://cp.cloudflare.com/generate_204",
            warpDetourMode = "proxy",
            warpPort = "2408",
            deserializedSubscriptions = emptyList()
        )
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val listStr = prefs[SUBSCRIPTION_LIST] ?: ""
        val manualStr = prefs[MANUAL_SERVERS] ?: (if (Config.IS_SPECIAL) Config.DEFAULT_PROFILE else "")
        
        // Parse subscriptions in the background thread (flow mapping runs on the DataStore IO dispatcher)
        val deserialized = deserializeSubscriptions(listStr).toMutableList()
        if (manualStr.isNotEmpty()) {
            deserialized.add(Subscription(
                id = "manual",
                name = "Manual / Custom Configs",
                url = "local://manual",
                servers = manualStr
            ))
        }

        UserSettings(
            isAdvancedMode = prefs[IS_ADVANCED_MODE] ?: false,
            bypassIran = prefs[BYPASS_IRAN] ?: true,
            secureDns = prefs[SECURE_DNS] ?: "https://1.1.1.1/dns-query",
            tunStack = prefs[TUN_STACK] ?: "mixed",
            enableFragment = prefs[ENABLE_FRAGMENT] ?: false,
            fragmentLength = prefs[FRAGMENT_LENGTH] ?: "10-20",
            fragmentInterval = prefs[FRAGMENT_INTERVAL] ?: "10-20",
            enableMux = prefs[ENABLE_MUX] ?: false,
            activeProfile = prefs[ACTIVE_PROFILE] ?: (if (Config.IS_SPECIAL) Config.DEFAULT_PROFILE else ""),
            subscriptionUrl = prefs[SUBSCRIPTION_URL] ?: "",
            subscriptionServers = prefs[SUBSCRIPTION_SERVERS] ?: "",
            subscriptionList = listStr,
            activeSubId = prefs[ACTIVE_SUB_ID] ?: (if (Config.IS_SPECIAL && Config.DEFAULT_PROFILE.isNotEmpty()) "manual" else ""),
            showLiveNotification = prefs[SHOW_LIVE_NOTIFICATION] ?: false,
            splitTunnelingEnabled = prefs[SPLIT_TUNNELING_ENABLED] ?: false,
            splitTunnelingMode = prefs[SPLIT_TUNNELING_MODE] ?: "bypass",
            splitTunnelingApps = prefs[SPLIT_TUNNELING_APPS] ?: emptySet(),
            manualServers = manualStr,
            specialTheme = prefs[SPECIAL_THEME] ?: defaultThemeKey,
            themeMode = prefs[THEME_MODE] ?: "system",
            cardStyle = prefs[CARD_STYLE] ?: "glass",
            bypassLan = prefs[BYPASS_LAN] ?: true,
            autoUpdateSubs = prefs[AUTO_UPDATE_SUBS] ?: true,
            autoUpdateInterval = prefs[AUTO_UPDATE_INTERVAL] ?: "daily",
            lastSubsUpdateTime = prefs[LAST_SUBS_UPDATE_TIME] ?: 0L,
            autoConnectSubs = prefs[AUTO_CONNECT_SUBS] ?: emptySet(),
            showLogsTab = prefs[SHOW_LOGS_TAB] ?: true,
            vpnMode = prefs[VPN_MODE] ?: "standard",
            warpPrivateKey = prefs[WARP_PRIVATE_KEY] ?: "",
            warpPublicKey = prefs[WARP_PUBLIC_KEY] ?: "",
            warpIpAddress = prefs[WARP_IP_ADDRESS] ?: "",
            warpClientId = prefs[WARP_CLIENT_ID] ?: "",
            vpnModeTunnelGames = prefs[VPN_MODE_TUNNEL_GAMES] ?: false,
            delayTestUrl = prefs[DELAY_TEST_URL] ?: "http://cp.cloudflare.com/generate_204",
            warpDetourMode = prefs[WARP_DETOUR_MODE] ?: "proxy",
            warpPort = prefs[WARP_PORT] ?: "2408",
            deserializedSubscriptions = deserialized
        )
    }.distinctUntilChanged()

    val isAdvancedMode: Flow<Boolean> = context.dataStore.data.map { it[IS_ADVANCED_MODE] ?: false }.distinctUntilChanged()
    val bypassIran: Flow<Boolean> = context.dataStore.data.map { it[BYPASS_IRAN] ?: true }.distinctUntilChanged()
    val secureDns: Flow<String> = context.dataStore.data.map { it[SECURE_DNS] ?: "https://1.1.1.1/dns-query" }.distinctUntilChanged()
    val tunStack: Flow<String> = context.dataStore.data.map { it[TUN_STACK] ?: "mixed" }.distinctUntilChanged()
    val enableFragment: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_FRAGMENT] ?: false }.distinctUntilChanged()
    val fragmentLength: Flow<String> = context.dataStore.data.map { it[FRAGMENT_LENGTH] ?: "10-20" }.distinctUntilChanged()
    val fragmentInterval: Flow<String> = context.dataStore.data.map { it[FRAGMENT_INTERVAL] ?: "10-20" }.distinctUntilChanged()
    val enableMux: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_MUX] ?: false }.distinctUntilChanged()
    val activeProfile: Flow<String> = context.dataStore.data.map { it[ACTIVE_PROFILE] ?: "" }.distinctUntilChanged()
    val subscriptionUrl: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_URL] ?: "" }.distinctUntilChanged()
    val subscriptionServers: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_SERVERS] ?: "" }.distinctUntilChanged()
    val subscriptionList: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_LIST] ?: "" }.distinctUntilChanged()
    val activeSubId: Flow<String> = context.dataStore.data.map { it[ACTIVE_SUB_ID] ?: "" }.distinctUntilChanged()
    val showLiveNotification: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LIVE_NOTIFICATION] ?: false }.distinctUntilChanged()
    val splitTunnelingEnabled: Flow<Boolean> = context.dataStore.data.map { it[SPLIT_TUNNELING_ENABLED] ?: false }.distinctUntilChanged()
    val splitTunnelingMode: Flow<String> = context.dataStore.data.map { it[SPLIT_TUNNELING_MODE] ?: "bypass" }.distinctUntilChanged()
    val splitTunnelingApps: Flow<Set<String>> = context.dataStore.data.map { it[SPLIT_TUNNELING_APPS] ?: emptySet() }.distinctUntilChanged()
    val manualServers: Flow<String> = context.dataStore.data.map { it[MANUAL_SERVERS] ?: "" }.distinctUntilChanged()
    val specialTheme: Flow<String> = context.dataStore.data.map { it[SPECIAL_THEME] ?: defaultThemeKey }.distinctUntilChanged()
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }.distinctUntilChanged()
    val cardStyle: Flow<String> = context.dataStore.data.map { it[CARD_STYLE] ?: "glass" }.distinctUntilChanged()
    val bypassLan: Flow<Boolean> = context.dataStore.data.map { it[BYPASS_LAN] ?: true }.distinctUntilChanged()
    val autoUpdateSubs: Flow<Boolean> = context.dataStore.data.map { it[AUTO_UPDATE_SUBS] ?: true }.distinctUntilChanged()
    val autoUpdateInterval: Flow<String> = context.dataStore.data.map { it[AUTO_UPDATE_INTERVAL] ?: "daily" }.distinctUntilChanged()
    val lastSubsUpdateTime: Flow<Long> = context.dataStore.data.map { it[LAST_SUBS_UPDATE_TIME] ?: 0L }.distinctUntilChanged()
    val vpnMode: Flow<String> = context.dataStore.data.map { it[VPN_MODE] ?: "standard" }.distinctUntilChanged()
    val warpPrivateKey: Flow<String> = context.dataStore.data.map { it[WARP_PRIVATE_KEY] ?: "" }.distinctUntilChanged()
    val warpPublicKey: Flow<String> = context.dataStore.data.map { it[WARP_PUBLIC_KEY] ?: "" }.distinctUntilChanged()
    val warpIpAddress: Flow<String> = context.dataStore.data.map { it[WARP_IP_ADDRESS] ?: "" }.distinctUntilChanged()
    val warpClientId: Flow<String> = context.dataStore.data.map { it[WARP_CLIENT_ID] ?: "" }.distinctUntilChanged()
    val vpnModeTunnelGames: Flow<Boolean> = context.dataStore.data.map { it[VPN_MODE_TUNNEL_GAMES] ?: false }.distinctUntilChanged()
    val delayTestUrl: Flow<String> = context.dataStore.data.map { it[DELAY_TEST_URL] ?: "http://cp.cloudflare.com/generate_204" }.distinctUntilChanged()
    val warpDetourMode: Flow<String> = context.dataStore.data.map { it[WARP_DETOUR_MODE] ?: "proxy" }.distinctUntilChanged()
    val warpPort: Flow<String> = context.dataStore.data.map { it[WARP_PORT] ?: "2408" }.distinctUntilChanged()

    suspend fun setAdvancedMode(value: Boolean) { context.dataStore.edit { it[IS_ADVANCED_MODE] = value } }
    suspend fun setBypassIran(value: Boolean) { context.dataStore.edit { it[BYPASS_IRAN] = value } }
    suspend fun setSecureDns(value: String) { context.dataStore.edit { it[SECURE_DNS] = value } }
    suspend fun setTunStack(value: String) { context.dataStore.edit { it[TUN_STACK] = value } }
    suspend fun setEnableFragment(value: Boolean) { context.dataStore.edit { it[ENABLE_FRAGMENT] = value } }
    suspend fun setFragmentLength(value: String) { context.dataStore.edit { it[FRAGMENT_LENGTH] = value } }
    suspend fun setFragmentInterval(value: String) { context.dataStore.edit { it[FRAGMENT_INTERVAL] = value } }
    suspend fun setEnableMux(value: Boolean) { context.dataStore.edit { it[ENABLE_MUX] = value } }
    suspend fun setActiveProfile(value: String) { context.dataStore.edit { it[ACTIVE_PROFILE] = value } }
    suspend fun setSubscriptionUrl(value: String) { context.dataStore.edit { it[SUBSCRIPTION_URL] = value } }
    suspend fun setSubscriptionServers(value: String) { context.dataStore.edit { it[SUBSCRIPTION_SERVERS] = value } }
    suspend fun setSubscriptionList(value: String) { context.dataStore.edit { it[SUBSCRIPTION_LIST] = value } }
    suspend fun setActiveSubId(value: String) { context.dataStore.edit { it[ACTIVE_SUB_ID] = value } }
    suspend fun setShowLiveNotification(value: Boolean) { context.dataStore.edit { it[SHOW_LIVE_NOTIFICATION] = value } }
    suspend fun setSplitTunnelingEnabled(value: Boolean) { context.dataStore.edit { it[SPLIT_TUNNELING_ENABLED] = value } }
    suspend fun setSplitTunnelingMode(value: String) { context.dataStore.edit { it[SPLIT_TUNNELING_MODE] = value } }
    suspend fun setSplitTunnelingApps(value: Set<String>) { context.dataStore.edit { it[SPLIT_TUNNELING_APPS] = value } }
    suspend fun setManualServers(value: String) { context.dataStore.edit { it[MANUAL_SERVERS] = value } }
    suspend fun setSpecialTheme(value: String) { context.dataStore.edit { it[SPECIAL_THEME] = value } }
    suspend fun setThemeMode(value: String) { context.dataStore.edit { it[THEME_MODE] = value } }
    suspend fun setCardStyle(value: String) { context.dataStore.edit { it[CARD_STYLE] = value } }
    suspend fun setBypassLan(value: Boolean) { context.dataStore.edit { it[BYPASS_LAN] = value } }
    suspend fun setAutoUpdateSubs(value: Boolean) { context.dataStore.edit { it[AUTO_UPDATE_SUBS] = value } }
    suspend fun setAutoUpdateInterval(value: String) { context.dataStore.edit { it[AUTO_UPDATE_INTERVAL] = value } }
    suspend fun setLastSubsUpdateTime(value: Long) { context.dataStore.edit { it[LAST_SUBS_UPDATE_TIME] = value } }
    suspend fun setShowLogsTab(value: Boolean) { context.dataStore.edit { it[SHOW_LOGS_TAB] = value } }
    suspend fun setVpnMode(value: String) { context.dataStore.edit { it[VPN_MODE] = value } }
    suspend fun setWarpCredentials(privateKey: String, publicKey: String, ipAddress: String, clientId: String) {
        context.dataStore.edit { prefs ->
            prefs[WARP_PRIVATE_KEY] = privateKey
            prefs[WARP_PUBLIC_KEY] = publicKey
            prefs[WARP_IP_ADDRESS] = ipAddress
            prefs[WARP_CLIENT_ID] = clientId
        }
    }
    suspend fun setVpnModeTunnelGames(value: Boolean) { context.dataStore.edit { it[VPN_MODE_TUNNEL_GAMES] = value } }
    suspend fun setDelayTestUrl(value: String) { context.dataStore.edit { it[DELAY_TEST_URL] = value } }
    suspend fun setWarpDetourMode(value: String) { context.dataStore.edit { it[WARP_DETOUR_MODE] = value } }
    suspend fun setWarpPort(value: String) { context.dataStore.edit { it[WARP_PORT] = value } }

    val autoConnectSubs: Flow<Set<String>> = context.dataStore.data.map { it[AUTO_CONNECT_SUBS] ?: emptySet() }.distinctUntilChanged()

    suspend fun toggleAutoConnectSub(subId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[AUTO_CONNECT_SUBS] ?: emptySet()
            if (current.contains(subId)) {
                prefs[AUTO_CONNECT_SUBS] = current - subId
            } else {
                prefs[AUTO_CONNECT_SUBS] = current + subId
            }
        }
    }
}

data class UserSettings(
    val isAdvancedMode: Boolean,
    val bypassIran: Boolean,
    val secureDns: String,
    val tunStack: String,
    val enableFragment: Boolean,
    val fragmentLength: String,
    val fragmentInterval: String,
    val enableMux: Boolean,
    val activeProfile: String,
    val subscriptionUrl: String,
    val subscriptionServers: String,
    val subscriptionList: String,
    val activeSubId: String,
    val showLiveNotification: Boolean,
    val splitTunnelingEnabled: Boolean,
    val splitTunnelingMode: String,
    val splitTunnelingApps: Set<String>,
    val manualServers: String,
    val specialTheme: String,
    val themeMode: String,
    val cardStyle: String,
    val bypassLan: Boolean,
    val autoUpdateSubs: Boolean,
    val autoUpdateInterval: String,
    val lastSubsUpdateTime: Long,
    val autoConnectSubs: Set<String>,
    val showLogsTab: Boolean,
    val vpnMode: String,
    val warpPrivateKey: String,
    val warpPublicKey: String,
    val warpIpAddress: String,
    val warpClientId: String,
    val vpnModeTunnelGames: Boolean,
    val delayTestUrl: String,
    val warpDetourMode: String,
    val warpPort: String,
    val deserializedSubscriptions: List<Subscription>
)

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val servers: String,
    val upload: Long? = null,
    val download: Long? = null,
    val total: Long? = null,
    val expire: Long? = null
)

fun deserializeSubscriptions(data: String): List<Subscription> {
    if (data.isEmpty()) return emptyList()
    return data.split("\u001e").mapNotNull { record ->
        val fields = record.split("\u001f")
        if (fields.size >= 4) {
            Subscription(
                id = fields[0],
                name = fields[1],
                url = fields[2],
                servers = fields[3],
                upload = if (fields.size > 4 && fields[4].isNotEmpty()) fields[4].toLongOrNull() else null,
                download = if (fields.size > 5 && fields[5].isNotEmpty()) fields[5].toLongOrNull() else null,
                total = if (fields.size > 6 && fields[6].isNotEmpty()) fields[6].toLongOrNull() else null,
                expire = if (fields.size > 7 && fields[7].isNotEmpty()) fields[7].toLongOrNull() else null
            )
        } else null
    }
}

fun serializeSubscriptions(subs: List<Subscription>): String {
    return subs.joinToString("\u001e") { sub ->
        val safeName = sub.name.replace("\u001e", "").replace("\u001f", "")
        val safeUrl = sub.url.replace("\u001e", "").replace("\u001f", "")
        val safeServers = sub.servers.replace("\u001e", "").replace("\u001f", "")
        val uploadStr = sub.upload?.toString() ?: ""
        val downloadStr = sub.download?.toString() ?: ""
        val totalStr = sub.total?.toString() ?: ""
        val expireStr = sub.expire?.toString() ?: ""
        "${sub.id}\u001f$safeName\u001f$safeUrl\u001f$safeServers\u001f$uploadStr\u001f$downloadStr\u001f$totalStr\u001f$expireStr"
    }
}
