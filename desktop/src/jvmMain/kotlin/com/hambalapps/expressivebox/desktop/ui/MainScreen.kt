package com.hambalapps.expressivebox.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.expressivebox.desktop.data.SettingsManager
import com.hambalapps.expressivebox.desktop.data.Subscription
import com.hambalapps.expressivebox.desktop.data.UserSettings
import com.hambalapps.expressivebox.desktop.data.serializeSubscriptions
import com.hambalapps.expressivebox.desktop.vpn.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLDecoder

private val ExpressiveCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

object DesktopStrings {
    private val fa = mapOf(
        "app_name" to "ExpressiveBox",
        "connected" to "متصل شد",
        "connecting" to "در حال اتصال...",
        "disconnected" to "قطع شد",
        "disconnecting" to "در حال قطع اتصال...",
        "active_node" to "سرور فعال",
        "dashboard" to "داشبورد",
        "profiles" to "پروفایل‌ها",
        "add_config" to "افزودن پیکربندی",
        "logs" to "لاگ‌ها",
        "settings" to "تنظیمات",
        "bypass_iran" to "دور زدن سایت‌های ایرانی (Direct Iran)",
        "bypass_lan" to "دور زدن شبکه محلی (Bypass LAN)",
        "secure_dns" to "دی‌ان‌اس امن (Secure DNS)",
        "theme" to "تم رنگی برنامه",
        "dark_mode" to "حالت تاریک",
        "advanced_mode" to "تنظیمات پیشرفته (Fragmentation & Mux)",
        "import_profile" to "وارد کردن لینک سرور (پکیج)",
        "import_sub" to "وارد کردن لینک اشتراک",
        "add_node" to "ساخت سرور دستی",
        "protocol" to "پروتکل",
        "remark" to "نام مستعار (Remark)",
        "server" to "آدرس سرور (IP/Host)",
        "port" to "پورت",
        "uuid" to "شناسه کاربری (UUID/Password)",
        "sni" to "نام سرور امنیتی (SNI)",
        "tls" to "فعال‌سازی TLS / Reality",
        "save" to "ذخیره سرور",
        "delete" to "حذف",
        "ping_all" to "تست تاخیر همه سرورها",
        "auto_connect" to "اتصال خودکار به بهترین پینگ",
        "log_level" to "سطح نمایش لاگ",
        "clear_logs" to "پاک کردن لاگ‌ها",
        "active_sub" to "اشتراک فعال",
        "paste_links" to "پیست کردن لینک‌های پیکربندی (vless, vmess, trojan, ss)",
        "import_btn" to "وارد کردن",
        "sub_url" to "آدرس URL اشتراک",
        "sub_name" to "نام اشتراک",
        "sub_import_btn" to "افزودن اشتراک",
        "download" to "دانلود",
        "upload" to "آپلود",
        "theme_dynamic" to "سیستم (ویندوز)",
        "theme_cherry" to "شکوفه گیلاس",
        "theme_lavender" to "اسطوخودوس",
        "theme_rose" to "رز گلد",
        "theme_midnight" to "آبی نیمه‌شب",
        "theme_forest" to "سبز جنگلی",
        "theme_sunset" to "غروب آفتاب",
        "theme_teal" to "آبی اقیانوسی",
        "theme_amethyst" to "آمیتیس سلطنتی",
        "theme_slate" to "سنگ لوح",
        "lang_name" to "زبان / Language",
        "enable_mux" to "فعال‌سازی Multiplexing (smux)",
        "enable_frag" to "فعال‌سازی Fragmentation",
        "frag_len" to "بازه طول پکت (Packet Length)",
        "frag_int" to "بازه زمانی تاخیر (Interval ms)"
    )

    private val en = mapOf(
        "app_name" to "ExpressiveBox",
        "connected" to "Connected",
        "connecting" to "Connecting...",
        "disconnected" to "Disconnected",
        "disconnecting" to "Disconnecting...",
        "active_node" to "Active Node",
        "dashboard" to "Dashboard",
        "profiles" to "Profiles",
        "add_config" to "Add Config",
        "logs" to "Logs",
        "settings" to "Settings",
        "bypass_iran" to "Bypass Iran Domains (Direct)",
        "bypass_lan" to "Bypass Local Area Network (LAN)",
        "secure_dns" to "Secure DNS Server",
        "theme" to "Application Theme Color",
        "dark_mode" to "Dark Mode",
        "advanced_mode" to "Advanced Mode (Fragmentation & Mux)",
        "import_profile" to "Import Profile Link",
        "import_sub" to "Import Subscription",
        "add_node" to "Add Manual Node",
        "protocol" to "Protocol",
        "remark" to "Remark / Name",
        "server" to "Server (IP/Host)",
        "port" to "Port",
        "uuid" to "Credentials (UUID/Password)",
        "sni" to "Server Name (SNI)",
        "tls" to "Enable TLS / Reality",
        "save" to "Save Config",
        "delete" to "Delete",
        "ping_all" to "Test All Server Pings",
        "auto_connect" to "Auto-Connect to Best Ping",
        "log_level" to "Log Level",
        "clear_logs" to "Clear Logs",
        "active_sub" to "Active Subscription",
        "paste_links" to "Paste raw connection links (vless, vmess, trojan, ss, hy2, tuic)",
        "import_btn" to "Import Configs",
        "sub_url" to "Subscription URL",
        "sub_name" to "Subscription Name",
        "sub_import_btn" to "Import Subscription",
        "download" to "Download",
        "upload" to "Upload",
        "theme_dynamic" to "System (Windows Accent)",
        "theme_cherry" to "Cherry Blossom",
        "theme_lavender" to "Lavender Dreams",
        "theme_rose" to "Rose Gold",
        "theme_midnight" to "Midnight Blue",
        "theme_forest" to "Forest Green",
        "theme_sunset" to "Sunset Orange",
        "theme_teal" to "Ocean Teal",
        "theme_amethyst" to "Royal Amethyst",
        "theme_slate" to "Nordic Slate",
        "lang_name" to "Language / زبان",
        "enable_mux" to "Enable Multiplexing (smux)",
        "enable_frag" to "Enable Fragmentation",
        "frag_len" to "Packet Length Range",
        "frag_int" to "Delay Interval Range (ms)"
    )

    fun get(key: String, isFarsi: Boolean): String {
        val map = if (isFarsi) fa else en
        return map[key] ?: key
    }
}

data class ServerItem(
    val link: String,
    val name: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }
    val settings by settingsManager.settings.collectAsState()

    val isFarsi = settings.isFarsi
    val layoutDirection = if (isFarsi) LayoutDirection.Rtl else LayoutDirection.Ltr

    var currentScreen by remember { mutableStateOf("dashboard") }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar Navigation
                Sidebar(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                    settings = settings,
                    settingsManager = settingsManager
                )

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                // Main Content Window
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(settings, settingsManager)
                        "profiles" -> ProfilesScreen(settings, settingsManager)
                        "add_config" -> AddConfigScreen(settings, settingsManager)
                        "logs" -> LogsScreen(settings)
                        "settings" -> SettingsScreen(settings, settingsManager)
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    settings: UserSettings,
    settingsManager: SettingsManager
) {
    val isFarsi = settings.isFarsi
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ExpressiveButtonShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnLock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = DesktopStrings.get("app_name", isFarsi),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Navigation Items
            val navItems = listOf(
                Triple("dashboard", Icons.Filled.Dashboard, "dashboard"),
                Triple("profiles", Icons.Filled.List, "profiles"),
                Triple("add_config", Icons.Filled.Add, "add_config"),
                Triple("logs", Icons.Filled.Terminal, "logs"),
                Triple("settings", Icons.Filled.Settings, "settings")
            )

            navItems.forEach { (screen, icon, stringKey) ->
                val selected = currentScreen == screen
                val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val tc = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(ExpressiveButtonShape)
                        .background(bg)
                        .clickable { onScreenChange(screen) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tc)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = DesktopStrings.get(stringKey, isFarsi),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tc
                    )
                }
            }
        }

        // Language Select Button at the bottom
        Button(
            onClick = { settingsManager.setIsFarsi(!isFarsi) },
            shape = ExpressiveButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isFarsi) "English" else "فارسی", fontSize = 13.sp)
        }
    }
}

@Composable
fun DashboardScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    val vpnState by SingboxManager.vpnState.collectAsState()
    val trafficStats by SingboxManager.trafficStats.collectAsState()

    val scope = rememberCoroutineScope()
    
    val activeProfile = settings.activeProfile
    val displayNodeName = remember(activeProfile) {
        if (activeProfile.isEmpty()) {
            "No Node Selected"
        } else {
            getProxyName(activeProfile)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: connection stats, duration
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = DesktopStrings.get("dashboard", isFarsi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Stats Cards
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DesktopStrings.get("download", isFarsi),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSpeed(trafficStats.second),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DesktopStrings.get("upload", isFarsi),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSpeed(trafficStats.first),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Selected Node Banner
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = DesktopStrings.get("active_node", isFarsi),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayNodeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right Column: large animated connect circle
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // Background flowing wave visualizer
                WaveVisualizer(
                    state = vpnState,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxSize()
                )

                // Large Connect Button Circle
                val infiniteTransition = rememberInfiniteTransition(label = "rotating_progress")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                val buttonScale by animateFloatAsState(
                    targetValue = if (vpnState == "CONNECTED") 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (vpnState == "CONNECTED") {
                                SingboxManager.stop()
                            } else if (vpnState == "DISCONNECTED") {
                                if (activeProfile.isNotEmpty()) {
                                    scope.launch {
                                        SingboxManager.start(activeProfile, settingsManager)
                                    }
                                }
                            }
                        }
                        .background(
                            Brush.sweepGradient(
                                if (vpnState == "CONNECTED") {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                } else {
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.colorScheme.surfaceVariant)
                                }
                            )
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (vpnState == "CONNECTING" || vpnState == "DISCONNECTING") {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(rotation)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (vpnState == "CONNECTED") Icons.Filled.PowerSettingsNew else Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (vpnState == "CONNECTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (vpnState == "CONNECTED") {
                                DesktopStrings.get("connected", isFarsi)
                            } else if (vpnState == "CONNECTING") {
                                DesktopStrings.get("connecting", isFarsi)
                            } else if (vpnState == "DISCONNECTING") {
                                DesktopStrings.get("disconnecting", isFarsi)
                            } else {
                                DesktopStrings.get("disconnected", isFarsi)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vpnState == "CONNECTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilesScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ALL", "VLESS", "TROJAN", "SHADOWSOCKS", "VMESS", "HYSTERIA", "TUIC")

    val subscriptions = settings.deserializedSubscriptions
    val activeSubId = settings.activeSubId

    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    val serverList = remember(activeSubscription) {
        activeSubscription?.servers?.split("\n")?.filter { it.trim().isNotEmpty() } ?: emptyList()
    }

    var pingsMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var isTestingPings by remember { mutableStateOf(false) }

    val filteredServerList = remember(serverList, selectedTab) {
        serverList.mapNotNull { serverLink ->
            val type = serverLink.substringBefore("://").uppercase()
            val matchesTab = when (selectedTab) {
                0 -> true
                1 -> type == "VLESS"
                2 -> type == "TROJAN"
                3 -> type == "SS" || type == "SHADOWSOCKS"
                4 -> type == "VMESS"
                5 -> type == "HYSTERIA" || type == "HYSTERIA2" || type == "HY2"
                6 -> type == "TUIC"
                else -> true
            }
            if (matchesTab) {
                val name = getProxyName(serverLink)
                ServerItem(link = serverLink, name = name, type = type)
            } else null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DesktopStrings.get("profiles", isFarsi),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Subscriptions Dropdown or Tab Row (Simplified for desktop view)
            Row {
                Button(
                    onClick = {
                        if (!isTestingPings && serverList.isNotEmpty()) {
                            isTestingPings = true
                            scope.launch {
                                val jobs = serverList.map { server ->
                                    async {
                                        val hostPort = getHostAndPortFromLink(server)
                                        val delay = if (hostPort != null) {
                                            measurePingDelay(hostPort.first, hostPort.second)
                                        } else {
                                            -1
                                        }
                                        server to delay
                                    }
                                }
                                val results = jobs.awaitAll()
                                pingsMap = results.toMap()
                                isTestingPings = false
                            }
                        }
                    },
                    shape = ExpressiveButtonShape,
                    enabled = !isTestingPings && serverList.isNotEmpty(),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (isTestingPings) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(imageVector = Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = DesktopStrings.get("ping_all", isFarsi), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Protocol Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Selector (Renders active subscription choices)
        if (subscriptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DesktopStrings.get("active_sub", isFarsi) + ": ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subscriptions) { sub ->
                        val selected = sub.id == activeSubId
                        FilterChip(
                            selected = selected,
                            onClick = { scope.launch { settingsManager.setActiveSubId(sub.id) } },
                            label = { Text(text = sub.name) }
                        )
                    }
                }
            }
        }

        // Servers List
        if (filteredServerList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No profiles found in this category.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredServerList) { index, item ->
                    val isSelected = settings.activeProfile == item.link
                    
                    val bg = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    Card(
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    settingsManager.setActiveProfile(item.link)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Protocol Badge
                                Box(
                                    modifier = Modifier
                                        .clip(ExpressiveChipShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Latency Ping tag
                                val ping = pingsMap[item.link]
                                if (ping != null) {
                                    val color = when {
                                        ping < 0 -> MaterialTheme.colorScheme.error
                                        ping < 200 -> Color(0xFF2E7D32)
                                        ping < 450 -> Color(0xFFEF6C00)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    val text = if (ping < 0) "Timeout" else "${ping} ms"
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                // Delete button for manual servers
                                if (activeSubId == "manual") {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val remaining = serverList.toMutableList()
                                                remaining.remove(item.link)
                                                settingsManager.setManualServers(remaining.joinToString("\n"))
                                                if (settings.activeProfile == item.link) {
                                                    settingsManager.setActiveProfile("")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddConfigScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) }
    
    // Form States
    var formRemark by remember { mutableStateOf("") }
    var formProtocol by remember { mutableStateOf("vless") }
    var formServer by remember { mutableStateOf("") }
    var formPort by remember { mutableStateOf("443") }
    var formCreds by remember { mutableStateOf("") }
    var formTls by remember { mutableStateOf(true) }
    var formSni by remember { mutableStateOf("") }

    // Raw import states
    var rawTextImport by remember { mutableStateOf("") }
    
    // Sub Import States
    var subUrlInput by remember { mutableStateOf("") }
    var subNameInput by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = DesktopStrings.get("add_config", isFarsi),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = activeTab, containerColor = Color.Transparent) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text(text = DesktopStrings.get("import_profile", isFarsi), modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text(text = DesktopStrings.get("import_sub", isFarsi), modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text(text = DesktopStrings.get("add_node", isFarsi), modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                0 -> { // Import raw links
                    Column {
                        OutlinedTextField(
                            value = rawTextImport,
                            onValueChange = { rawTextImport = it },
                            placeholder = { Text(text = DesktopStrings.get("paste_links", isFarsi)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = ExpressiveCardShape
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (rawTextImport.trim().isNotEmpty()) {
                                    scope.launch {
                                        val existing = settings.manualServers
                                        val newLinks = rawTextImport.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                        val updated = if (existing.isEmpty()) {
                                            newLinks.joinToString("\n")
                                        } else {
                                            existing + "\n" + newLinks.joinToString("\n")
                                        }
                                        settingsManager.setManualServers(updated)
                                        settingsManager.setActiveSubId("manual")
                                        rawTextImport = ""
                                        activeTab = 0
                                        // Auto select first link
                                        if (newLinks.isNotEmpty() && settings.activeProfile.isEmpty()) {
                                            settingsManager.setActiveProfile(newLinks[0])
                                        }
                                    }
                                }
                            },
                            shape = ExpressiveButtonShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = DesktopStrings.get("import_btn", isFarsi))
                        }
                    }
                }
                1 -> { // Import Sub subscription
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = subNameInput,
                            onValueChange = { subNameInput = it },
                            label = { Text(text = DesktopStrings.get("sub_name", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        OutlinedTextField(
                            value = subUrlInput,
                            onValueChange = { subUrlInput = it },
                            label = { Text(text = DesktopStrings.get("sub_url", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        if (fetchError != null) {
                            Text(text = fetchError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (subUrlInput.trim().isNotEmpty() && subNameInput.trim().isNotEmpty()) {
                                    isFetching = true
                                    fetchError = null
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val url = URL(subUrlInput)
                                            val conn = url.openConnection()
                                            conn.connectTimeout = 6000
                                            conn.readTimeout = 6000
                                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                                            
                                            // Handle base64 decryption
                                            val decoded = tryBase64Decode(text) ?: text
                                            val servers = decoded.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                            
                                            if (servers.isNotEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    val currentSubs = settings.deserializedSubscriptions.toMutableList()
                                                    // Filter out local presets
                                                    val userSubs = currentSubs.filter { !it.url.startsWith("local://") }.toMutableList()
                                                    val subId = System.currentTimeMillis().toString()
                                                    userSubs.add(Subscription(
                                                        id = subId,
                                                        name = subNameInput,
                                                        url = subUrlInput,
                                                        servers = servers.joinToString("\n")
                                                    ))
                                                    settingsManager.setSubscriptionList(serializeSubscriptions(userSubs))
                                                    settingsManager.setActiveSubId(subId)
                                                    
                                                    // Auto select first server
                                                    settingsManager.setActiveProfile(servers[0])

                                                    subNameInput = ""
                                                    subUrlInput = ""
                                                    isFetching = false
                                                    activeTab = 0
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    fetchError = "No servers found in subscription response."
                                                    isFetching = false
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                fetchError = "Failed to fetch: ${e.message}"
                                                isFetching = false
                                            }
                                        }
                                    }
                                }
                            },
                            shape = ExpressiveButtonShape,
                            enabled = !isFetching && subUrlInput.isNotEmpty() && subNameInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(text = DesktopStrings.get("sub_import_btn", isFarsi))
                            }
                        }
                    }
                }
                2 -> { // Form creator
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formRemark,
                            onValueChange = { formRemark = it },
                            label = { Text(text = DesktopStrings.get("remark", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        // Protocol selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("vless", "trojan", "ss", "socks5").forEach { proto ->
                                FilterChip(
                                    selected = formProtocol == proto,
                                    onClick = { formProtocol = proto },
                                    label = { Text(text = proto.uppercase()) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = formServer,
                            onValueChange = { formServer = it },
                            label = { Text(text = DesktopStrings.get("server", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        OutlinedTextField(
                            value = formPort,
                            onValueChange = { formPort = it },
                            label = { Text(text = DesktopStrings.get("port", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        OutlinedTextField(
                            value = formCreds,
                            onValueChange = { formCreds = it },
                            label = { Text(text = DesktopStrings.get("uuid", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        if (formProtocol == "vless" || formProtocol == "trojan") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { formTls = !formTls }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(checked = formTls, onCheckedChange = { formTls = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = DesktopStrings.get("tls", isFarsi), fontSize = 13.sp)
                            }

                            if (formTls) {
                                OutlinedTextField(
                                    value = formSni,
                                    onValueChange = { formSni = it },
                                    label = { Text(text = DesktopStrings.get("sni", isFarsi)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveButtonShape
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (formServer.isNotEmpty() && formPort.isNotEmpty() && formRemark.isNotEmpty()) {
                                    val portNum = formPort.toIntOrNull() ?: 443
                                    val link = when (formProtocol) {
                                        "vless" -> {
                                            val query = mutableListOf<String>()
                                            if (formTls) {
                                                query.add("security=tls")
                                                if (formSni.isNotEmpty()) query.add("sni=$formSni")
                                            } else {
                                                query.add("security=none")
                                            }
                                            val qStr = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                                            "vless://$formCreds@$formServer:$portNum$qStr#${URLDecoder.decode(formRemark, "UTF-8")}"
                                        }
                                        "trojan" -> {
                                            val query = mutableListOf<String>()
                                            if (formTls) {
                                                if (formSni.isNotEmpty()) query.add("sni=$formSni")
                                            }
                                            val qStr = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
                                            "trojan://$formCreds@$formServer:$portNum$qStr#${URLDecoder.decode(formRemark, "UTF-8")}"
                                        }
                                        "ss" -> {
                                            // Format: method:pass in base64
                                            val credsB64 = java.util.Base64.getEncoder().encodeToString("$formCreds".toByteArray())
                                            "ss://$credsB64@$formServer:$portNum#${URLDecoder.decode(formRemark, "UTF-8")}"
                                        }
                                        else -> {
                                            "socks5://$formCreds@$formServer:$portNum#${URLDecoder.decode(formRemark, "UTF-8")}"
                                        }
                                    }

                                    scope.launch {
                                        val existing = settings.manualServers
                                        val updated = if (existing.isEmpty()) link else "$existing\n$link"
                                        settingsManager.setManualServers(updated)
                                        settingsManager.setActiveSubId("manual")
                                        settingsManager.setActiveProfile(link)
                                        
                                        // Reset fields
                                        formRemark = ""
                                        formServer = ""
                                        formPort = "443"
                                        formCreds = ""
                                        formSni = ""
                                        activeTab = 0
                                    }
                                }
                            },
                            shape = ExpressiveButtonShape,
                            enabled = formServer.isNotEmpty() && formPort.isNotEmpty() && formRemark.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = DesktopStrings.get("save", isFarsi))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsScreen(settings: UserSettings) {
    val isFarsi = settings.isFarsi
    val logs by SingboxManager.vpnLogs.collectAsState()

    val logList = remember(logs) { logs.lines().filter { it.isNotEmpty() } }
    val state = rememberScrollState()

    // Auto scroll logs to bottom
    LaunchedEffect(logList.size) {
        state.animateScrollTo(state.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DesktopStrings.get("logs", isFarsi),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = { SingboxManager.clearLogs() },
                shape = ExpressiveButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = DesktopStrings.get("clear_logs", isFarsi), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(ExpressiveCardShape)
                .background(Color(0xFF0C0A0F))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), ExpressiveCardShape)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state)
            ) {
                logList.forEach { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = when {
                            line.contains("WARNING", true) || line.contains("WARN", true) -> Color(0xFFEF6C00)
                            line.contains("ERROR", true) || line.contains("ERR", true) || line.contains("FATAL", true) -> Color(0xFFD32F2F)
                            line.contains("CONNECTED", true) -> Color(0xFF2E7D32)
                            else -> Color(0xFFE0E0E0)
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = DesktopStrings.get("settings", isFarsi),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Bypass Iran domains Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsManager.setBypassIran(!settings.bypassIran) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DesktopStrings.get("bypass_iran", isFarsi),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Routes .ir and local Iranian domains directly (detouring VPN) for maximum speed.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.bypassIran, onCheckedChange = { settingsManager.setBypassIran(it) })
            }
        }

        // Bypass LAN domains Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsManager.setBypassLan(!settings.bypassLan) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DesktopStrings.get("bypass_lan", isFarsi),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keeps connections to local network devices (printers, local routers) direct.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.bypassLan, onCheckedChange = { settingsManager.setBypassLan(it) })
            }
        }

        // Secure DNS Settings Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DesktopStrings.get("secure_dns", isFarsi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "https://1.1.1.1/dns-query" to "Cloudflare DoH (Fast & Secure)",
                    "https://8.8.8.8/dns-query" to "Google DoH",
                    "https://78.22.122.100/dns-query" to "Shecan DNS (Iran censorship circumvention DOH)",
                    "udp://1.1.1.1" to "Cloudflare UDP Standard (Fastest)"
                ).forEach { (dnsUrl, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setSecureDns(dnsUrl) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.secureDns == dnsUrl,
                            onClick = { settingsManager.setSecureDns(dnsUrl) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = dnsUrl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Advanced Config Card (Fragment & Mux)
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DesktopStrings.get("advanced_mode", isFarsi),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Advanced DPI bypassing parameters. Useful under extreme ISP restrictions.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.isAdvancedMode, onCheckedChange = { settingsManager.setAdvancedMode(it) })
                }

                if (settings.isAdvancedMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Multiplexing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setEnableMux(!settings.enableMux) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = settings.enableMux, onCheckedChange = { settingsManager.setEnableMux(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = DesktopStrings.get("enable_mux", isFarsi), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fragmentation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setEnableFragment(!settings.enableFragment) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = settings.enableFragment, onCheckedChange = { settingsManager.setEnableFragment(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = DesktopStrings.get("enable_frag", isFarsi), fontSize = 13.sp)
                    }

                    if (settings.enableFragment) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.fragmentLength,
                            onValueChange = { settingsManager.setFragmentLength(it) },
                            label = { Text(text = DesktopStrings.get("frag_len", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.fragmentInterval,
                            onValueChange = { settingsManager.setFragmentInterval(it) },
                            label = { Text(text = DesktopStrings.get("frag_int", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )
                    }
                }
            }
        }

        // Theme Palette Selector Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DesktopStrings.get("theme", isFarsi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                val themes = listOf(
                    "dynamic" to "theme_dynamic",
                    "cherry_blossom" to "theme_cherry",
                    "lavender_dreams" to "theme_lavender",
                    "rose_gold" to "theme_rose",
                    "midnight_blue" to "theme_midnight",
                    "forest_green" to "theme_forest",
                    "sunset_orange" to "theme_sunset",
                    "ocean_teal" to "theme_teal",
                    "royal_amethyst" to "theme_amethyst",
                    "nordic_slate" to "theme_slate"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (themeKey, nameKey) ->
                        val selected = settings.specialTheme == themeKey
                        val tc = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(ExpressiveChipShape)
                                .background(bg)
                                .clickable { settingsManager.setSpecialTheme(themeKey) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = DesktopStrings.get(nameKey, isFarsi),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tc
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dark mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextMode = when (settings.themeMode) {
                                "dark" -> "light"
                                "light" -> "system"
                                else -> "dark"
                            }
                            settingsManager.setThemeMode(nextMode)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = DesktopStrings.get("dark_mode", isFarsi) + " (${settings.themeMode.uppercase()})", fontSize = 13.sp)
                    Switch(
                        checked = settings.themeMode == "dark",
                        onCheckedChange = {
                            settingsManager.setThemeMode(if (it) "dark" else "light")
                        }
                    )
                }
            }
        }
    }
}

// Helper formats
private fun formatSpeed(bytes: Long): String {
    val kb = bytes / 1024f
    return if (kb > 1024) {
        val mb = kb / 1024f
        String.format("%.2f MB/s", mb)
    } else {
        String.format("%.1f KB/s", kb)
    }
}

fun getProxyName(link: String): String {
    val hashIdx = link.indexOf("#")
    return if (hashIdx >= 0) {
        try {
            URLDecoder.decode(link.substring(hashIdx + 1), "UTF-8")
        } catch (e: Exception) {
            link.substring(hashIdx + 1)
        }
    } else {
        try {
            val rest = link.substringAfter("://")
            val host = rest.substringAfter("@").substringBefore(":")
            val scheme = link.substringBefore("://").uppercase()
            "$scheme ($host)"
        } catch (e: Exception) {
            "Unnamed Profile"
        }
    }
}
