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
}
