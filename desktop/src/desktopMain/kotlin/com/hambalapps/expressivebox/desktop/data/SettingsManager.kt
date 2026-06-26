package com.hambalapps.expressivebox.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val servers: String
)

@Serializable
data class UserSettings(
    val isAdvancedMode: Boolean = false,
    val bypassIran: Boolean = true,
    val secureDns: String = "https://1.1.1.1/dns-query",
    val tunStack: String = "mixed",
    val enableFragment: Boolean = false,
    val fragmentLength: String = "10-20",
    val fragmentInterval: String = "10-20",
    val enableMux: Boolean = false,
    val activeProfile: String = "",
    val subscriptionUrl: String = "",
    val subscriptionServers: String = "",
    val subscriptionList: String = "",
    val activeSubId: String = "",
    val showLiveNotification: Boolean = false,
    val splitTunnelingEnabled: Boolean = false,
    val splitTunnelingMode: String = "bypass",
    val splitTunnelingApps: Set<String> = emptySet(),
    val manualServers: String = "",
    val specialTheme: String = "dynamic",
    val themeMode: String = "system",
    val cardStyle: String = "glass",
    val bypassLan: Boolean = true,
    val autoUpdateSubs: Boolean = true,
    val autoUpdateInterval: String = "daily",
    val lastSubsUpdateTime: Long = 0L,
    val autoConnectSubs: Set<String> = emptySet(),
    val isFarsi: Boolean = false,
    val enableTun: Boolean = false,
    val delayTestUrl: String = "http://cp.cloudflare.com/generate_204",
    val warpDetourMode: String = "proxy",
    val warpPort: String = "2408"
) {
    val deserializedSubscriptions: List<Subscription>
        get() {
            val list = deserializeSubscriptions(subscriptionList).toMutableList()
            if (manualServers.isNotEmpty()) {
                list.add(Subscription(
                    id = "manual",
                    name = "Manual / Custom Configs",
                    url = "local://manual",
                    servers = manualServers
                ))
            }
            return list
        }
}

fun deserializeSubscriptions(data: String): List<Subscription> {
    if (data.isEmpty()) return emptyList()
    return data.split("\u001e").mapNotNull { record ->
        val fields = record.split("\u001f")
        if (fields.size >= 4) {
            Subscription(
                id = fields[0],
                name = fields[1],
                url = fields[2],
                servers = fields[3]
            )
        } else null
    }
}

fun serializeSubscriptions(subs: List<Subscription>): String {
    return subs.joinToString("\u001e") { sub ->
        val safeName = sub.name.replace("\u001e", "").replace("\u001f", "")
        val safeUrl = sub.url.replace("\u001e", "").replace("\u001f", "")
        val safeServers = sub.servers.replace("\u001e", "").replace("\u001f", "")
        "${sub.id}\u001f$safeName\u001f$safeUrl\u001f$safeServers"
    }
}

class SettingsManager {
    val settings: StateFlow<UserSettings> get() = Companion.settings

    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        private val settingsFile = File(System.getProperty("user.home"), ".expressivebox_settings.json")
        
        private val _settings = MutableStateFlow(loadSettings())
        val settings: StateFlow<UserSettings> = _settings.asStateFlow()

        private fun loadSettings(): UserSettings {
            return try {
                if (settingsFile.exists()) {
                    json.decodeFromString<UserSettings>(settingsFile.readText())
                } else {
                    UserSettings()
                }
            } catch (e: Exception) {
                UserSettings()
            }
        }

        private fun saveSettings(newSettings: UserSettings) {
            try {
                settingsFile.parentFile?.mkdirs()
                settingsFile.writeText(json.encodeToString(UserSettings.serializer(), newSettings))
                _settings.value = newSettings
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val currentSettings: UserSettings get() = _settings.value

    fun setAdvancedMode(value: Boolean) { saveSettings(currentSettings.copy(isAdvancedMode = value)) }
    fun setBypassIran(value: Boolean) { saveSettings(currentSettings.copy(bypassIran = value)) }
    fun setSecureDns(value: String) { saveSettings(currentSettings.copy(secureDns = value)) }
    fun setTunStack(value: String) { saveSettings(currentSettings.copy(tunStack = value)) }
    fun setEnableFragment(value: Boolean) { saveSettings(currentSettings.copy(enableFragment = value)) }
    fun setFragmentLength(value: String) { saveSettings(currentSettings.copy(fragmentLength = value)) }
    fun setFragmentInterval(value: String) { saveSettings(currentSettings.copy(fragmentInterval = value)) }
    fun setEnableMux(value: Boolean) { saveSettings(currentSettings.copy(enableMux = value)) }
    fun setActiveProfile(value: String) { saveSettings(currentSettings.copy(activeProfile = value)) }
    fun setSubscriptionUrl(value: String) { saveSettings(currentSettings.copy(subscriptionUrl = value)) }
    fun setSubscriptionServers(value: String) { saveSettings(currentSettings.copy(subscriptionServers = value)) }
    fun setSubscriptionList(value: String) { saveSettings(currentSettings.copy(subscriptionList = value)) }
    fun setActiveSubId(value: String) { saveSettings(currentSettings.copy(activeSubId = value)) }
    fun setShowLiveNotification(value: Boolean) { saveSettings(currentSettings.copy(showLiveNotification = value)) }
    fun setSplitTunnelingEnabled(value: Boolean) { saveSettings(currentSettings.copy(splitTunnelingEnabled = value)) }
    fun setSplitTunnelingMode(value: String) { saveSettings(currentSettings.copy(splitTunnelingMode = value)) }
    fun setSplitTunnelingApps(value: Set<String>) { saveSettings(currentSettings.copy(splitTunnelingApps = value)) }
    fun setManualServers(value: String) { saveSettings(currentSettings.copy(manualServers = value)) }
    fun setSpecialTheme(value: String) { saveSettings(currentSettings.copy(specialTheme = value)) }
    fun setThemeMode(value: String) { saveSettings(currentSettings.copy(themeMode = value)) }
    fun setCardStyle(value: String) { saveSettings(currentSettings.copy(cardStyle = value)) }
    fun setBypassLan(value: Boolean) { saveSettings(currentSettings.copy(bypassLan = value)) }
    fun setAutoUpdateSubs(value: Boolean) { saveSettings(currentSettings.copy(autoUpdateSubs = value)) }
    fun setAutoUpdateInterval(value: String) { saveSettings(currentSettings.copy(autoUpdateInterval = value)) }
    fun setLastSubsUpdateTime(value: Long) { saveSettings(currentSettings.copy(lastSubsUpdateTime = value)) }
    fun setIsFarsi(value: Boolean) { saveSettings(currentSettings.copy(isFarsi = value)) }
    fun setEnableTun(value: Boolean) { saveSettings(currentSettings.copy(enableTun = value)) }
    fun setDelayTestUrl(value: String) { saveSettings(currentSettings.copy(delayTestUrl = value)) }
    fun setWarpDetourMode(value: String) { saveSettings(currentSettings.copy(warpDetourMode = value)) }
    fun setWarpPort(value: String) { saveSettings(currentSettings.copy(warpPort = value)) }

    fun toggleAutoConnectSub(subId: String) {
        val current = currentSettings.autoConnectSubs
        val updated = if (current.contains(subId)) current - subId else current + subId
        saveSettings(currentSettings.copy(autoConnectSubs = updated))
    }
}
