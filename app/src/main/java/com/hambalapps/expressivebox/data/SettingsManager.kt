package com.hambalapps.expressivebox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val AUTO_UPDATE_SUBS = booleanPreferencesKey("auto_update_subs")
        val AUTO_UPDATE_INTERVAL = stringPreferencesKey("auto_update_interval")
        val LAST_SUBS_UPDATE_TIME = longPreferencesKey("last_subs_update_time")
        
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
            showLiveNotification = true,
            splitTunnelingEnabled = false,
            splitTunnelingMode = "bypass",
            splitTunnelingApps = emptySet(),
            manualServers = "",
            specialTheme = "cherry_blossom",
            bypassLan = true,
            autoUpdateSubs = true,
            autoUpdateInterval = "daily",
            lastSubsUpdateTime = 0L
        )
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            isAdvancedMode = prefs[IS_ADVANCED_MODE] ?: false,
            bypassIran = prefs[BYPASS_IRAN] ?: true,
            secureDns = prefs[SECURE_DNS] ?: "https://1.1.1.1/dns-query",
            tunStack = prefs[TUN_STACK] ?: "mixed",
            enableFragment = prefs[ENABLE_FRAGMENT] ?: false,
            fragmentLength = prefs[FRAGMENT_LENGTH] ?: "10-20",
            fragmentInterval = prefs[FRAGMENT_INTERVAL] ?: "10-20",
            enableMux = prefs[ENABLE_MUX] ?: false,
            activeProfile = prefs[ACTIVE_PROFILE] ?: "",
            subscriptionUrl = prefs[SUBSCRIPTION_URL] ?: "",
            subscriptionServers = prefs[SUBSCRIPTION_SERVERS] ?: "",
            subscriptionList = prefs[SUBSCRIPTION_LIST] ?: "",
            activeSubId = prefs[ACTIVE_SUB_ID] ?: "",
            showLiveNotification = prefs[SHOW_LIVE_NOTIFICATION] ?: true,
            splitTunnelingEnabled = prefs[SPLIT_TUNNELING_ENABLED] ?: false,
            splitTunnelingMode = prefs[SPLIT_TUNNELING_MODE] ?: "bypass",
            splitTunnelingApps = prefs[SPLIT_TUNNELING_APPS] ?: emptySet(),
            manualServers = prefs[MANUAL_SERVERS] ?: "",
            specialTheme = prefs[SPECIAL_THEME] ?: "cherry_blossom",
            bypassLan = prefs[BYPASS_LAN] ?: true,
            autoUpdateSubs = prefs[AUTO_UPDATE_SUBS] ?: true,
            autoUpdateInterval = prefs[AUTO_UPDATE_INTERVAL] ?: "daily",
            lastSubsUpdateTime = prefs[LAST_SUBS_UPDATE_TIME] ?: 0L
        )
    }

    val isAdvancedMode: Flow<Boolean> = context.dataStore.data.map { it[IS_ADVANCED_MODE] ?: false }
    val bypassIran: Flow<Boolean> = context.dataStore.data.map { it[BYPASS_IRAN] ?: true }
    val secureDns: Flow<String> = context.dataStore.data.map { it[SECURE_DNS] ?: "https://1.1.1.1/dns-query" }
    val tunStack: Flow<String> = context.dataStore.data.map { it[TUN_STACK] ?: "mixed" }
    val enableFragment: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_FRAGMENT] ?: false }
    val fragmentLength: Flow<String> = context.dataStore.data.map { it[FRAGMENT_LENGTH] ?: "10-20" }
    val fragmentInterval: Flow<String> = context.dataStore.data.map { it[FRAGMENT_INTERVAL] ?: "10-20" }
    val enableMux: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_MUX] ?: false }
    val activeProfile: Flow<String> = context.dataStore.data.map { it[ACTIVE_PROFILE] ?: "" }
    val subscriptionUrl: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_URL] ?: "" }
    val subscriptionServers: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_SERVERS] ?: "" }
    val subscriptionList: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_LIST] ?: "" }
    val activeSubId: Flow<String> = context.dataStore.data.map { it[ACTIVE_SUB_ID] ?: "" }
    val showLiveNotification: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LIVE_NOTIFICATION] ?: true }
    val splitTunnelingEnabled: Flow<Boolean> = context.dataStore.data.map { it[SPLIT_TUNNELING_ENABLED] ?: false }
    val splitTunnelingMode: Flow<String> = context.dataStore.data.map { it[SPLIT_TUNNELING_MODE] ?: "bypass" }
    val splitTunnelingApps: Flow<Set<String>> = context.dataStore.data.map { it[SPLIT_TUNNELING_APPS] ?: emptySet() }
    val manualServers: Flow<String> = context.dataStore.data.map { it[MANUAL_SERVERS] ?: "" }
    val specialTheme: Flow<String> = context.dataStore.data.map { it[SPECIAL_THEME] ?: "cherry_blossom" }
    val bypassLan: Flow<Boolean> = context.dataStore.data.map { it[BYPASS_LAN] ?: true }
    val autoUpdateSubs: Flow<Boolean> = context.dataStore.data.map { it[AUTO_UPDATE_SUBS] ?: true }
    val autoUpdateInterval: Flow<String> = context.dataStore.data.map { it[AUTO_UPDATE_INTERVAL] ?: "daily" }
    val lastSubsUpdateTime: Flow<Long> = context.dataStore.data.map { it[LAST_SUBS_UPDATE_TIME] ?: 0L }

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
    suspend fun setBypassLan(value: Boolean) { context.dataStore.edit { it[BYPASS_LAN] = value } }
    suspend fun setAutoUpdateSubs(value: Boolean) { context.dataStore.edit { it[AUTO_UPDATE_SUBS] = value } }
    suspend fun setAutoUpdateInterval(value: String) { context.dataStore.edit { it[AUTO_UPDATE_INTERVAL] = value } }
    suspend fun setLastSubsUpdateTime(value: Long) { context.dataStore.edit { it[LAST_SUBS_UPDATE_TIME] = value } }
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
    val bypassLan: Boolean,
    val autoUpdateSubs: Boolean,
    val autoUpdateInterval: String,
    val lastSubsUpdateTime: Long
)
