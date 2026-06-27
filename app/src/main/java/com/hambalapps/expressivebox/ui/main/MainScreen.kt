package com.hambalapps.expressivebox.ui.main

import android.app.Activity
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.app.NotificationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.res.stringResource
import com.hambalapps.expressivebox.R
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.data.UserSettings
import com.hambalapps.expressivebox.data.Subscription
import com.hambalapps.expressivebox.data.serializeSubscriptions
import com.hambalapps.expressivebox.data.deserializeSubscriptions
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import com.hambalapps.expressivebox.vpn.measurePingDelay
import com.hambalapps.expressivebox.vpn.getHostAndPortFromLink
import com.hambalapps.expressivebox.vpn.tryBase64Decode
import com.hambalapps.expressivebox.vpn.ProxyNameResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.hambalapps.expressivebox.SplitTunneling
import com.hambalapps.expressivebox.ui.qr.QrScannerScreen
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.hambalapps.expressivebox.Config
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer

// Expressive shapes defining Material 3 Expressive aesthetics
private val ExpressiveCardShape = RoundedCornerShape(24.dp)
private val ExpressiveButtonShape = RoundedCornerShape(16.dp)
private val ExpressiveChipShape = RoundedCornerShape(12.dp)

private fun compositeColor(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha
    return Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // Observe settings from DataStore
    val settings by settingsManager.settings.collectAsStateWithLifecycle(initialValue = SettingsManager.defaultSettings)
    val cardStyle = settings.cardStyle
    val isAdvancedMode = settings.isAdvancedMode
    val bypassIran = settings.bypassIran
    val bypassLan = settings.bypassLan
    val secureDns = settings.secureDns
    val tunStack = settings.tunStack
    val enableFragment = settings.enableFragment
    val fragmentLength = settings.fragmentLength
    val fragmentInterval = settings.fragmentInterval
    val enableMux = settings.enableMux
    val activeProfile = settings.activeProfile
    val subscriptionUrl = settings.subscriptionUrl
    val subscriptionListStr = settings.subscriptionList
    val activeSubId = settings.activeSubId
    val showLiveNotification = settings.showLiveNotification
    val splitTunnelingEnabled = settings.splitTunnelingEnabled
    val splitTunnelingApps = settings.splitTunnelingApps
    val splitTunnelingMode = settings.splitTunnelingMode
    val manualServersStr = settings.manualServers
    val autoUpdateSubs = settings.autoUpdateSubs
    val autoUpdateInterval = settings.autoUpdateInterval
    val lastSubsUpdateTime = settings.lastSubsUpdateTime
    val autoConnectSubs = settings.autoConnectSubs
    val showLogsTab = settings.showLogsTab
    val vpnMode = settings.vpnMode
    val warpPrivateKey = settings.warpPrivateKey
    val warpPublicKey = settings.warpPublicKey
    val warpIpAddress = settings.warpIpAddress
    val warpClientId = settings.warpClientId
    val vpnModeTunnelGames = settings.vpnModeTunnelGames
    val delayTestUrl = settings.delayTestUrl
    val warpDetourMode = settings.warpDetourMode
    val warpPort = settings.warpPort

    val subscriptions = settings.deserializedSubscriptions
    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    val serverList = remember(activeSubscription) {
        activeSubscription?.servers?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    var showLivePromoGuide by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    // Check battery optimization exemption on launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if (!isIgnoring) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showBatteryOptimizationDialog = true
                    }
                }
            }
        }
    }

    // Auto subscription update check on launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val currentSettings = settingsManager.settings.first()
            val autoUpdate = currentSettings.autoUpdateSubs
            if (autoUpdate) {
                val interval = currentSettings.autoUpdateInterval
                val lastTime = currentSettings.lastSubsUpdateTime
                val currentTime = System.currentTimeMillis()
                
                val shouldUpdate = when (interval) {
                    "startup" -> true
                    "daily" -> (currentTime - lastTime) >= 24 * 60 * 60 * 1000L
                    "weekly" -> (currentTime - lastTime) >= 7 * 24 * 60 * 60 * 1000L
                    else -> false
                }
                
                if (shouldUpdate) {
                    try {
                        val currentListStr = currentSettings.subscriptionList
                        val currentSubs = deserializeSubscriptions(currentListStr)
                        var anyUpdated = false
                        val updatedSubs = currentSubs.map { sub ->
                            if (!sub.url.startsWith("local://")) {
                                try {
                                    val result = fetchSubscription(sub.url)
                                    if (result.servers.isNotEmpty()) {
                                        anyUpdated = true
                                        sub.copy(
                                            servers = result.servers.joinToString("\n"),
                                            upload = result.upload,
                                            download = result.download,
                                            total = result.total,
                                            expire = result.expire
                                        )
                                    } else {
                                        sub
                                    }
                                } catch (e: Exception) {
                                    sub
                                }
                            } else {
                                sub
                            }
                        }

                        if (anyUpdated) {
                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedSubs.filter { !it.url.startsWith("local://") }))
                            
                            val activeSubIdVal = currentSettings.activeSubId
                            val activeProfileVal = currentSettings.activeProfile
                            val updatedActiveSub = updatedSubs.find { it.id == activeSubIdVal }
                            if (updatedActiveSub != null) {
                                val sList = updatedActiveSub.servers.split("\n").filter { it.isNotEmpty() }
                                if (sList.isNotEmpty() && !sList.contains(activeProfileVal)) {
                                    settingsManager.setActiveProfile(sList[0])
                                    if (VpnServiceWrapper.vpnState.value == "CONNECTED") {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            startVpnService(context)
                                        }
                                    }
                                }
                            }
                        }
                        settingsManager.setLastSubsUpdateTime(currentTime)
                    } catch (e: Exception) {
                        // Silently handle error
                    }
                }
            }
        }
    }

    // Dedicated server auto-select removed

    // Observe VPN state and logs
    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()
    var appVersion by remember { mutableStateOf("v1.6.9") }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val version = "v${pInfo.versionName ?: "1.6.9"}"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    appVersion = version
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }
    
    // Tabs list ordered as: 1. Servers, 2. Home (Main), 3. Settings, 4. Logs (if enabled)
    val tabs = remember(showLogsTab, context) {
        listOfNotNull(
            Triple(1, context.getString(R.string.tab_servers), Icons.Default.Dns),
            Triple(0, context.getString(R.string.tab_home), Icons.Default.Home),
            Triple(3, context.getString(R.string.tab_settings), Icons.Default.Settings),
            if (showLogsTab) Triple(2, context.getString(R.string.tab_logs), Icons.Default.Terminal) else null
        )
    }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })
    var isRegisteringWarp by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var subUrlInput by remember { mutableStateOf("") }
    var subNameInput by remember { mutableStateOf("") }
    var isAddFormExpanded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var pingsMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var isTestingPings by remember { mutableStateOf(false) }

    val filteredServerList = remember(serverList, searchQuery, selectedTab) {
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
                val name = ProxyNameResolver.getProxyName(serverLink, context)
                if (name.contains(searchQuery, ignoreCase = true)) {
                    ServerItem(
                        link = serverLink,
                        name = name,
                        type = type,
                        transport = getTransportType(serverLink)
                    )
                } else null
            } else null
        }
    }

    var showLoveNoteDialog by remember { mutableStateOf(false) }
    var qrCodeToShare by remember { mutableStateOf<Pair<String, String>?>(null) }
    var currentLoveNote by remember { mutableStateOf("") }
    var scanResultCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var editingNodeLink by remember { mutableStateOf<String?>(null) }
    var editLinkInput by remember { mutableStateOf("") }
    var editorMode by remember { mutableStateOf("form") } // "form" or "link"
    var editType by remember { mutableStateOf("vless") }
    var editRemark by remember { mutableStateOf("") }
    var editServer by remember { mutableStateOf("") }
    var editPort by remember { mutableStateOf("443") }
    var editCreds by remember { mutableStateOf("") }
    var editTls by remember { mutableStateOf(false) }
    var editSni by remember { mutableStateOf("") }
    var editShowAdvanced by remember { mutableStateOf(false) }
    var editTransportType by remember { mutableStateOf("tcp") }
    var editTransportPath by remember { mutableStateOf("") }
    var editTransportHost by remember { mutableStateOf("") }
    var editTransportServiceName by remember { mutableStateOf("") }
    var editTransportSeed by remember { mutableStateOf("") }
    var editTransportHeaderType by remember { mutableStateOf("none") }
    var isNodesExpanded by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val refreshingSubs = remember { mutableStateMapOf<String, Boolean>() }

    // Launcher for VPN system permission dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(context)
        }
    }

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val cardBackground = if (isDark) Color.Black else Color(0xFFF7F9FB)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val cardBorderBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, outlineVariant) {
        if (cardStyle == "solid" || cardStyle == "vibrant") {
            SolidColor(outlineVariant)
        } else {
            val colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.60f else 0.18f),
                secondaryColor.copy(alpha = if (isDark) 0.40f else 0.06f)
            )
            Brush.linearGradient(colors = colors)
        }
    }

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, surfaceContainerHigh, primaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerHigh)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.55f),
                    secondaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    val secondaryCardBrush = remember(isDark, cardStyle, secondaryColor, tertiaryColor, surfaceContainer, secondaryContainer, primaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainer)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            val colors = if (isDark) {
                listOf(
                    secondaryColor.copy(alpha = 0.55f),
                    tertiaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    secondaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    val tertiaryCardBrush = remember(isDark, cardStyle, tertiaryColor, primaryColor, surfaceContainerLow, tertiaryContainer, primaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerLow)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            val colors = if (isDark) {
                listOf(
                    tertiaryColor.copy(alpha = 0.55f),
                    primaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    tertiaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    // Active Card background animation (alive and pulsing flow)
    val infiniteTransition = rememberInfiniteTransition(label = "ActiveCardTransition")
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowOffset"
    )

    val activeCardBackgroundBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, tertiaryColor, primaryContainer, flowOffset) {
        if (cardStyle == "solid") {
            SolidColor(primaryContainer)
        } else if (cardStyle == "vibrant") {
            Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.68f),
                    secondaryColor.copy(alpha = 0.50f),
                    tertiaryColor.copy(alpha = 0.30f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.25f),
                    secondaryColor.copy(alpha = 0.15f),
                    Color.White
                )
            }
            Brush.linearGradient(
                colors = colors,
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = pagerState.currentPage == 0,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(ExpressiveCardShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = context.getString(com.hambalapps.expressivebox.R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = if (Config.IS_SPECIAL) stringResource(R.string.app_name_special) else stringResource(R.string.app_name_standard),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (Config.IS_SPECIAL) "Developed with love by Gumball for Sana. Featuring a custom reactive visualizer, Monet adaptive themes, and stable key signing." 
                               else stringResource(R.string.app_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (Config.IS_SPECIAL) {
                        PawPrint(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sana ❤️ Gumball",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.secure_network_engine),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (Config.IS_SPECIAL) {
                PeakingKitty(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = (-80).dp)
                        .graphicsLayer {
                            rotationZ = -90f
                        }
                )
            }
            Scaffold(
            topBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    context.getString(com.hambalapps.expressivebox.R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (Config.IS_SPECIAL) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PawPrint(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, dragAmount ->
                                            if (dragAmount > 10f) {
                                                scope.launch { drawerState.open() }
                                            }
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.open_settings_drawer),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            if (showLogsTab) {
                                val targetLogPageIdx = remember(tabs) { tabs.indexOfFirst { it.first == 2 } }
                                if (targetLogPageIdx >= 0) {
                                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(targetLogPageIdx) } }) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = stringResource(R.string.show_logs),
                                            tint = if (pagerState.targetPage == targetLogPageIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    )
                    if (Config.IS_SPECIAL) {
                        PeakingKitty(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-18).dp)
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    tabs.forEach { (tabId, label, icon) ->
                        val pageIdx = tabs.indexOfFirst { it.first == tabId }
                        NavigationBarItem(
                            selected = if (pageIdx >= 0) pagerState.targetPage == pageIdx else false,
                            onClick = { 
                                if (pageIdx >= 0) {
                                    scope.launch { pagerState.animateScrollToPage(pageIdx) }
                                } 
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        ) { innerPadding ->

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val tabId = if (pageIndex < tabs.size) tabs[pageIndex].first else 0
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                val scale = 0.92f + (1f - 0.92f) * (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f))
                                val alpha = 0.5f + (1f - 0.5f) * (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f))
                                this.scaleX = scale
                                this.scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        when (tabId) {
                    0 -> { // Home Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            ConnectionDashboard(
                                state = vpnState,
                                cardStyle = cardStyle,
                                isDark = isDark,
                                delayTestUrl = delayTestUrl,
                                onConnectToggle = {
                                    if (vpnState == "CONNECTED") {
                                        stopVpnService(context)
                                    } else {
                                        if (activeProfile.trim().isEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.notif_no_node),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val intent = VpnService.prepare(context)
                                            if (intent != null) {
                                                vpnPermissionLauncher.launch(intent)
                                            } else {
                                                startVpnService(context)
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // VPN Mode Selector Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                                    .border(
                                        width = 1.dp,
                                        brush = cardBorderBrush,
                                        shape = ExpressiveCardShape
                                    ),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = stringResource(R.string.vpn_mode_title),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val modes = listOf(
                                            Triple("standard", R.string.mode_standard_title, R.string.tag_stable),
                                            Triple("gaming", R.string.mode_gaming_title, R.string.tag_experimental),
                                            Triple("ai_bypass", R.string.mode_ai_bypass_title, R.string.tag_experimental)
                                        )
                                        modes.forEach { (modeKey, titleResId, tagResId) ->
                                            val isSelected = vpnMode == modeKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(ExpressiveChipShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        shape = ExpressiveChipShape
                                                    )
                                                    .clickable {
                                                        if (modeKey == "ai_bypass") {
                                                            if (warpPrivateKey.isEmpty() || warpClientId.isEmpty()) {
                                                                isRegisteringWarp = true
                                                                scope.launch {
                                                                    val creds = com.hambalapps.expressivebox.vpn.registerWarpAccount()
                                                                    isRegisteringWarp = false
                                                                    if (creds != null) {
                                                                        settingsManager.setWarpCredentials(
                                                                            creds.privateKey,
                                                                            creds.publicKey,
                                                                            creds.ipAddress,
                                                                            creds.clientId
                                                                        )
                                                                        settingsManager.setVpnMode("ai_bypass")
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    } else {
                                                                        android.widget.Toast.makeText(
                                                                            context,
                                                                            context.getString(R.string.warp_failed),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                }
                                                            } else {
                                                                scope.launch {
                                                                    settingsManager.setVpnMode("ai_bypass")
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            scope.launch {
                                                                settingsManager.setVpnMode(modeKey)
                                                                if (vpnState == "CONNECTED") {
                                                                    startVpnService(context)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .pressScaleEffect()
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = stringResource(titleResId),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = stringResource(tagResId),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f),
                                                        fontWeight = FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = vpnMode == "gaming",
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(R.string.tunnel_games_title),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.tunnel_games_desc),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Switch(
                                                    checked = vpnModeTunnelGames,
                                                    onCheckedChange = { checked ->
                                                        scope.launch {
                                                            settingsManager.setVpnModeTunnelGames(checked)
                                                            if (vpnState == "CONNECTED") {
                                                                startVpnService(context)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = vpnMode == "ai_bypass",
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // 1. WARP Detour Selection
                                            Text(
                                                text = stringResource(R.string.warp_detour_title),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.warp_detour_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val detourOptions = listOf(
                                                    "proxy" to "Proxy",
                                                    "direct" to "Direct"
                                                )
                                                detourOptions.forEach { (optionKey, optionName) ->
                                                    val isSelected = warpDetourMode == optionKey
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(ExpressiveChipShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                                shape = ExpressiveChipShape
                                                            )
                                                            .clickable {
                                                                scope.launch {
                                                                    settingsManager.setWarpDetourMode(optionKey)
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                            .pressScaleEffect()
                                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = optionName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(20.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // 2. WARP Port Selection
                                            Text(
                                                text = stringResource(R.string.warp_port_title),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.warp_port_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val portOptions = listOf("2408", "500", "1701", "4500")
                                                portOptions.forEach { portStr ->
                                                    val isSelected = warpPort == portStr
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(ExpressiveChipShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                                shape = ExpressiveChipShape
                                                            )
                                                            .clickable {
                                                                scope.launch {
                                                                    settingsManager.setWarpPort(portStr)
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                            .pressScaleEffect()
                                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = portStr,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isRegisteringWarp) {
                                AlertDialog(
                                    onDismissRequest = {},
                                    confirmButton = {},
                                    title = { Text(stringResource(R.string.registering_warp)) },
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    },
                                    shape = ExpressiveCardShape,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (Config.IS_SPECIAL) {
                                    PeakingKitty(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = 28.dp, y = (-22).dp)
                                    )
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = if (vpnState == "CONNECTED" || vpnState == "CONNECTING") activeCardBackgroundBrush else primaryCardBrush,
                                            shape = ExpressiveCardShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = cardBorderBrush,
                                            shape = ExpressiveCardShape
                                        )
                                        .clickable { scope.launch { pagerState.animateScrollToPage(0) } }
                                        .pressScaleEffect(),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = stringResource(R.string.active_node),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        AnimatedContent(
                                            targetState = activeProfile,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                                                 scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                                .togetherWith(fadeOut(animationSpec = tween(90)))
                                            },
                                            label = "ActiveProfileContent"
                                        ) { targetProfile ->
                                            if (targetProfile.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(ExpressiveButtonShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                            shape = ExpressiveButtonShape
                                                        )
                                                        .padding(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Active",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (targetProfile.startsWith("{")) stringResource(R.string.custom_json) else ProxyNameResolver.getProxyName(targetProfile, context),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (targetProfile.startsWith("{")) "JSON" else targetProfile.substringBefore("://").uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = stringResource(R.string.no_profile_active),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (Config.IS_SPECIAL) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    PeakingKitty(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-28).dp, y = (-22).dp)
                                    )
                                    
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                    val quoteBorderBrush = remember(isDark, primaryColor, tertiaryColor) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = if (isDark) 0.60f else 0.35f),
                                                tertiaryColor.copy(alpha = if (isDark) 0.35f else 0.15f)
                                            )
                                        )
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                            .border(
                                                width = 1.dp,
                                                brush = quoteBorderBrush,
                                                shape = ExpressiveCardShape
                                            )
                                            .clickable {
                                                currentLoveNote = Config.LOVE_QUOTES.random()
                                                showLoveNoteDialog = true
                                            }
                                            .pressScaleEffect(),
                                        shape = ExpressiveCardShape,
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Transparent
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = null,
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.love_notes),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = stringResource(R.string.love_notes_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                if (showLoveNoteDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showLoveNoteDialog = false },
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = null,
                                                    tint = Color.Red
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.note_for_sana), fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = currentLoveNote,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showLoveNoteDialog = false }) {
                                                Text(stringResource(R.string.cancel))
                                            }
                                        },
                                        shape = ExpressiveCardShape,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                    1 -> { // Servers Tab
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val screenWidthDp = configuration.screenWidthDp
                        val useDropdownMenu = !isLandscape || screenWidthDp < 600

                        val subscriptionManagerCard: @Composable (Modifier, Modifier) -> Unit = { modifier, listModifier ->
                            Card(
                                modifier = modifier
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(
                                        width = 1.dp,
                                        brush = cardBorderBrush,
                                        shape = ExpressiveCardShape
                                    )
                                    .animateContentSize(),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.sub_manager),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.sub_count, subscriptions.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FilledTonalIconToggleButton(
                                            checked = isAddFormExpanded,
                                            onCheckedChange = { isAddFormExpanded = it },
                                            modifier = Modifier.pressScaleEffect()
                                        ) {
                                            Icon(
                                                imageVector = if (isAddFormExpanded) Icons.Default.Close else Icons.Default.Add,
                                                contentDescription = stringResource(R.string.add_sub)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isAddFormExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = subNameInput,
                                                onValueChange = { subNameInput = it },
                                                label = { Text(stringResource(R.string.sub_name_label)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = ExpressiveButtonShape,
                                                singleLine = true,
                                                placeholder = { Text(stringResource(R.string.sub_name_placeholder)) }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = subUrlInput,
                                                onValueChange = { subUrlInput = it },
                                                label = { Text(stringResource(R.string.sub_link_label)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = ExpressiveButtonShape,
                                                singleLine = true,
                                                placeholder = { Text("https://example.com/sub") },
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            scanResultCallback = { result ->
                                                                subUrlInput = result
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.QrCodeScanner,
                                                            contentDescription = stringResource(R.string.scan_qr_code)
                                                        )
                                                    }
                                                }
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (subUrlInput.isNotEmpty()) {
                                                            scope.launch {
                                                                isFetching = true
                                                                fetchError = null
                                                                try {
                                                                    val result = fetchSubscription(subUrlInput)
                                                                    if (result.servers.isNotEmpty()) {
                                                                        val domain = try {
                                                                            java.net.URI(subUrlInput).host ?: context.getString(R.string.custom_provider)
                                                                        } catch (e: Exception) {
                                                                            context.getString(R.string.custom_provider)
                                                                        }
                                                                        val name = if (subNameInput.trim().isNotEmpty()) subNameInput.trim() else domain
                                                                        val newSub = Subscription(
                                                                            id = java.util.UUID.randomUUID().toString(),
                                                                            name = name,
                                                                            url = subUrlInput.trim(),
                                                                            servers = result.servers.joinToString("\n"),
                                                                            upload = result.upload,
                                                                            download = result.download,
                                                                            total = result.total,
                                                                            expire = result.expire
                                                                        )
                                                                        val updatedList = subscriptions + newSub
                                                                        settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                                                        settingsManager.setActiveSubId(newSub.id)
                                                                        settingsManager.setActiveProfile(result.servers[0])
                                                                        
                                                                        subUrlInput = ""
                                                                        subNameInput = ""
                                                                        isAddFormExpanded = false
                                                                        
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    } else {
                                                                        fetchError = context.getString(R.string.no_valid_configs)
                                                                    }
                                                                } catch (e: Exception) {
                                                                    fetchError = context.getString(R.string.fetch_failed, e.message ?: "")
                                                                } finally {
                                                                    isFetching = false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1.5f).pressScaleEffect(),
                                                    shape = ExpressiveButtonShape,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                    enabled = !isFetching && subUrlInput.isNotEmpty()
                                                ) {
                                                    if (isFetching) {
                                                        LoadingIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(stringResource(R.string.fetch_and_add), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                                
                                                OutlinedButton(
                                                    onClick = {
                                                        subUrlInput = ""
                                                        subNameInput = ""
                                                        fetchError = null
                                                    },
                                                    modifier = Modifier.weight(1f).pressScaleEffect(),
                                                    shape = ExpressiveButtonShape,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    enabled = !isFetching
                                                ) {
                                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(stringResource(R.string.clear), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            
                                            fetchError?.let { err ->
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }

                                    if (subscriptions.isEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_subs_added),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = listModifier
                                        ) {
                                            subscriptions.forEach { sub ->
                                                val isActive = sub.id == activeSubId
                                                var menuExpanded by remember { mutableStateOf(false) }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(ExpressiveButtonShape)
                                                        .background(
                                                            if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            scope.launch {
                                                                settingsManager.setActiveSubId(sub.id)
                                                                val servers = sub.servers.split("\n").filter { it.isNotEmpty() }
                                                                if (servers.isNotEmpty()) {
                                                                    settingsManager.setActiveProfile(servers[0])
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isActive) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isActive) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Active",
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.RssFeed,
                                                                contentDescription = "Sub",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = sub.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val domain = try {
                                                            java.net.URI(sub.url).host ?: sub.url
                                                        } catch (e: Exception) {
                                                            sub.url
                                                        }
                                                        Text(
                                                            text = domain,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        if (sub.total != null && sub.total > 0) {
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            val up = sub.upload ?: 0L
                                                            val down = sub.download ?: 0L
                                                            val used = up + down
                                                            val total = sub.total
                                                            val pct = (used.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                androidx.compose.material3.LinearProgressIndicator(
                                                                    progress = pct.toFloat(),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(4.dp)
                                                                        .clip(CircleShape),
                                                                    color = if (pct > 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                                Text(
                                                                    text = "${(pct * 100).toInt()}%",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "${formatBytes(used)} / ${formatBytes(total)}" + 
                                                                    if (sub.expire != null && sub.expire > 0) " • Exp: ${formatExpiry(sub.expire)}" else "",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        } else if (sub.expire != null && sub.expire > 0) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Expires: ${formatExpiry(sub.expire)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    val isAutoConnectEnabled = autoConnectSubs.contains(sub.id)
                                                    val isLocalSub = sub.url.startsWith("local://")
                                                    val isRefreshing = refreshingSubs[sub.id] ?: false

                                                    if (useDropdownMenu) {
                                                        Box {
                                                            IconButton(
                                                                onClick = { menuExpanded = true },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.MoreVert,
                                                                    contentDescription = "Subscription options",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            
                                                            DropdownMenu(
                                                                expanded = menuExpanded,
                                                                onDismissRequest = { menuExpanded = false },
                                                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                            ) {
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Text(
                                                                            text = if (isAutoConnectEnabled) "Auto Connect (Enabled)" else "Auto Connect",
                                                                            style = MaterialTheme.typography.bodyMedium
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        scope.launch {
                                                                            settingsManager.toggleAutoConnectSub(sub.id)
                                                                        }
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Bolt,
                                                                            contentDescription = null,
                                                                            tint = if (isAutoConnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                )
                                                                if (!isLocalSub) {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Refresh", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            scope.launch {
                                                                                refreshingSubs[sub.id] = true
                                                                                try {
                                                                                    val result = fetchSubscription(sub.url)
                                                                                    if (result.servers.isNotEmpty()) {
                                                                                        val updatedList = subscriptions.map {
                                                                                            if (it.id == sub.id) {
                                                                                                it.copy(
                                                                                                    servers = result.servers.joinToString("\n"),
                                                                                                    upload = result.upload,
                                                                                                    download = result.download,
                                                                                                    total = result.total,
                                                                                                    expire = result.expire
                                                                                                )
                                                                                            } else {
                                                                                                it
                                                                                            }
                                                                                        }
                                                                                        settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                                        if (isActive) {
                                                                                            val currentActive = activeProfile
                                                                                            val sList = result.servers.map { it.trim() }.filter { it.isNotEmpty() }
                                                                                            if (sList.isNotEmpty() && !sList.contains(currentActive)) {
                                                                                                settingsManager.setActiveProfile(sList[0])
                                                                                                if (vpnState == "CONNECTED") {
                                                                                                    startVpnService(context)
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_updated, sub.name), android.widget.Toast.LENGTH_SHORT).show()
                                                                                    } else {
                                                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_no_servers), android.widget.Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                } catch(e: Exception) {
                                                                                    android.widget.Toast.makeText(context, context.getString(R.string.toast_update_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                                                                                } finally {
                                                                                    refreshingSubs[sub.id] = false
                                                                                }
                                                                            }
                                                                        },
                                                                        leadingIcon = {
                                                                            if (isRefreshing) {
                                                                                LoadingIndicator(
                                                                                    modifier = Modifier.size(18.dp),
                                                                                    color = MaterialTheme.colorScheme.primary
                                                                                )
                                                                            } else {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Refresh,
                                                                                    contentDescription = null,
                                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                    modifier = Modifier.size(20.dp)
                                                                                )
                                                                            }
                                                                        },
                                                                        enabled = !isRefreshing
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = { Text("Share Link", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            val sendIntent = Intent().apply {
                                                                                action = Intent.ACTION_SEND
                                                                                putExtra(Intent.EXTRA_TEXT, sub.url)
                                                                                this.type = "text/plain"
                                                                            }
                                                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                                                            context.startActivity(shareIntent)
                                                                        },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Share,
                                                                                contentDescription = null,
                                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = { Text("Share QR Code", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            qrCodeToShare = Pair(sub.name, sub.url)
                                                                        },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                imageVector = Icons.Default.QrCode,
                                                                                contentDescription = null,
                                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                                DropdownMenuItem(
                                                                    text = { Text("Delete", style = MaterialTheme.typography.bodyMedium) },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        scope.launch {
                                                                            val updatedList = subscriptions.filter { it != sub }
                                                                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                            if (isActive) {
                                                                                val nextActive = updatedList.firstOrNull()
                                                                                if (nextActive != null) {
                                                                                    settingsManager.setActiveSubId(nextActive.id)
                                                                                    val nextServers = nextActive.servers.split("\n").filter { it.isNotEmpty() }
                                                                                    if (nextServers.isNotEmpty()) {
                                                                                        settingsManager.setActiveProfile(nextServers[0])
                                                                                    }
                                                                                } else {
                                                                                    settingsManager.setActiveSubId("")
                                                                                }
                                                                                if (vpnState == "CONNECTED") {
                                                                                    startVpnService(context)
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Delete,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    settingsManager.toggleAutoConnectSub(sub.id)
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Bolt,
                                                                contentDescription = "Auto Connect Toggle",
                                                                tint = if (isAutoConnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        if (!isLocalSub) {
                                                            IconButton(
                                                                onClick = {
                                                                    scope.launch {
                                                                        refreshingSubs[sub.id] = true
                                                                        try {
                                                                            val result = fetchSubscription(sub.url)
                                                                            if (result.servers.isNotEmpty()) {
                                                                                val updatedList = subscriptions.map {
                                                                                    if (it.id == sub.id) {
                                                                                        it.copy(
                                                                                            servers = result.servers.joinToString("\n"),
                                                                                            upload = result.upload,
                                                                                            download = result.download,
                                                                                            total = result.total,
                                                                                            expire = result.expire
                                                                                        )
                                                                                    } else {
                                                                                        it
                                                                                    }
                                                                                }
                                                                                settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                                if (isActive) {
                                                                                    val currentActive = activeProfile
                                                                                    val sList = result.servers.map { it.trim() }.filter { it.isNotEmpty() }
                                                                                    if (sList.isNotEmpty() && !sList.contains(currentActive)) {
                                                                                        settingsManager.setActiveProfile(sList[0])
                                                                                        if (vpnState == "CONNECTED") {
                                                                                            startVpnService(context)
                                                                                        }
                                                                                    }
                                                                                }
                                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_updated, sub.name), android.widget.Toast.LENGTH_SHORT).show()
                                                                            } else {
                                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_no_servers), android.widget.Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        } catch(e: Exception) {
                                                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_update_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                                                                        } finally {
                                                                            refreshingSubs[sub.id] = false
                                                                        }
                                                                    }
                                                                },
                                                                modifier = Modifier.size(36.dp),
                                                                enabled = !isRefreshing
                                                            ) {
                                                                if (isRefreshing) {
                                                                    LoadingIndicator(
                                                                        modifier = Modifier.size(18.dp),
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Refresh,
                                                                        contentDescription = stringResource(R.string.refresh_label),
                                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    val sendIntent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, sub.url)
                                                                        this.type = "text/plain"
                                                                    }
                                                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                                                    context.startActivity(shareIntent)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Share,
                                                                    contentDescription = "Share",
                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    qrCodeToShare = Pair(sub.name, sub.url)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.QrCode,
                                                                    contentDescription = "QR Share",
                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    val updatedList = subscriptions.filter { it != sub }
                                                                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                    if (isActive) {
                                                                        val nextActive = updatedList.firstOrNull()
                                                                        if (nextActive != null) {
                                                                            settingsManager.setActiveSubId(nextActive.id)
                                                                            val nextServers = nextActive.servers.split("\n").filter { it.isNotEmpty() }
                                                                            if (nextServers.isNotEmpty()) {
                                                                                settingsManager.setActiveProfile(nextServers[0])
                                                                            }
                                                                        } else {
                                                                            settingsManager.setActiveSubId("")
                                                                        }
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { showImportDialog = true },
                                            modifier = Modifier.weight(1f).pressScaleEffect(),
                                            shape = ExpressiveButtonShape
                                        ) {
                                            Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.import_str))
                                        }
                                        Button(
                                            onClick = {
                                                editingNodeLink = "new_node"
                                                editType = "vless"
                                                editRemark = ""
                                                editServer = ""
                                                editPort = "443"
                                                editCreds = ""
                                                editTls = false
                                                editSni = ""
                                                editLinkInput = ""
                                                editorMode = "form"
                                            },
                                            modifier = Modifier.weight(1f).pressScaleEffect(),
                                            shape = ExpressiveButtonShape
                                        ) {
                                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.create_str))
                                        }
                                    }
                                }
                            }
                        }

                        val availableNodesCard: @Composable (Modifier, Modifier) -> Unit = { modifier, listModifier ->
                            Box(modifier = modifier) {
                                if (Config.IS_SPECIAL) {
                                    PeakingKitty(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = 28.dp, y = (-22).dp)
                                    )
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                        .border(
                                            width = 1.dp,
                                            brush = cardBorderBrush,
                                            shape = ExpressiveCardShape
                                        )
                                        .animateContentSize(),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Dns,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.available_nodes),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { 
                                                        isSearchVisible = !isSearchVisible
                                                        if (!isSearchVisible) searchQuery = ""
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                                                        contentDescription = stringResource(R.string.search),
                                                        tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (!isTestingPings) {
                                                            scope.launch {
                                                                isTestingPings = true
                                                                val jobs = serverList.map { link ->
                                                                    scope.async(kotlinx.coroutines.Dispatchers.IO) {
                                                                        val hostPort = getHostAndPortFromLink(link)
                                                                        val ping = if (hostPort != null) {
                                                                            measurePingDelay(hostPort.first, hostPort.second)
                                                                        } else {
                                                                            -1
                                                                        }
                                                                        link to ping
                                                                    }
                                                                }
                                                                val results = jobs.awaitAll()
                                                                pingsMap = pingsMap + results.toMap()
                                                                isTestingPings = false
                                                            }
                                                        }
                                                    },
                                                    enabled = !isTestingPings
                                                ) {
                                                    if (isTestingPings) {
                                                        LoadingIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Speed,
                                                            contentDescription = stringResource(R.string.test_pings),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { isNodesExpanded = true },
                                                    modifier = Modifier.pressScaleEffect()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fullscreen,
                                                        contentDescription = "Expand Card",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isSearchVisible,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = searchQuery,
                                                    onValueChange = { searchQuery = it },
                                                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape,
                                                    singleLine = true,
                                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                                    trailingIcon = {
                                                        if (searchQuery.isNotEmpty()) {
                                                            IconButton(onClick = { searchQuery = "" }) {
                                                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        ScrollableTabRow(
                                            selectedTabIndex = selectedTab,
                                            edgePadding = 0.dp,
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            indicator = { tabPositions ->
                                                if (selectedTab < tabPositions.size) {
                                                    TabRowDefaults.SecondaryIndicator(
                                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            divider = {}
                                        ) {
                                            listOf(stringResource(R.string.tab_all), "VLESS", "Trojan", "Shadowsocks", "VMess", "Hysteria", "TUIC").forEachIndexed { index, title ->
                                                Tab(
                                                    selected = selectedTab == index,
                                                    onClick = { selectedTab = index },
                                                    text = { 
                                                        Text(
                                                            text = title, 
                                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 13.sp
                                                        ) 
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (filteredServerList.isEmpty()) {
                                            Box(
                                                modifier = listModifier,
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.no_matching_nodes),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = listModifier,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                itemsIndexed(filteredServerList, key = { _, item -> item.link }) { index, serverItem ->
                                                    val serverLink = serverItem.link
                                                    val isSelected = activeProfile == serverLink
                                                    val name = serverItem.name
                                                    val type = serverItem.type
                                                    val transport = serverItem.transport
                                                    
                                                    val tagContainerColor = when (type) {
                                                        "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                                        "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                                        "VMESS" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.errorContainer
                                                        "TUIC" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                    val tagTextColor = when (type) {
                                                        "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        "VMESS" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.onErrorContainer
                                                        "TUIC" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .animateItem()
                                                            .fillMaxWidth()
                                                            .clip(ExpressiveButtonShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                                else Color.Transparent
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                                                shape = ExpressiveButtonShape
                                                            )
                                                            .clickable {
                                                                scope.launch {
                                                                    settingsManager.setActiveProfile(serverLink)
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Hub,
                                                                contentDescription = null,
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(ExpressiveChipShape)
                                                                        .background(tagContainerColor)
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = type,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = tagTextColor,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                }
                                                                
                                                                if (transport.isNotEmpty()) {
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(ExpressiveChipShape)
                                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = transport,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            fontWeight = FontWeight.Bold,
                                                                            maxLines = 1,
                                                                            softWrap = false
                                                                        )
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.width(8.dp))
 
                                                                val ping = pingsMap[serverLink]
                                                                if (ping != null) {
                                                                    val isTimeout = ping < 0
                                                                    val pingColor = when {
                                                                        isTimeout -> Color(0xFFF44336)
                                                                        ping < 60 -> Color(0xFF4CAF50)
                                                                        ping < 120 -> Color(0xFFFFB300)
                                                                        else -> Color(0xFFF44336)
                                                                    }
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(6.dp)
                                                                            .clip(CircleShape)
                                                                            .background(pingColor)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = if (isTimeout) "Timeout" else "${ping} ms",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = pingColor,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                } else {
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = stringResource(R.string.untested),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    val sendIntent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, serverLink)
                                                                        this.type = "text/plain"
                                                                    }
                                                                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_config))
                                                                    context.startActivity(shareIntent)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Share,
                                                                    contentDescription = stringResource(R.string.share_config),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            
                                                            IconButton(
                                                                onClick = {
                                                                    qrCodeToShare = Pair(name, serverLink)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.QrCode,
                                                                    contentDescription = "QR Share",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }

                                                            if (activeSubId == "manual") {
                                                                IconButton(
                                                                    onClick = {
                                                                        editingNodeLink = serverLink
                                                                        editLinkInput = serverLink
                                                                    },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Edit,
                                                                        contentDescription = stringResource(R.string.edit_config),
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = {
                                                                        scope.launch {
                                                                            val updatedManualList = serverList.filter { it != serverLink }
                                                                            val updatedManualStr = updatedManualList.joinToString("\n")
                                                                            settingsManager.setManualServers(updatedManualStr)
                                                                            if (isSelected) {
                                                                                val nextActive = updatedManualList.firstOrNull() ?: ""
                                                                                settingsManager.setActiveProfile(nextActive)
                                                                                if (vpnState == "CONNECTED" && nextActive.isNotEmpty()) {
                                                                                    startVpnService(context)
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = stringResource(R.string.delete_config),
                                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                        modifier = Modifier.size(18.dp)
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
                        }

                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                subscriptionManagerCard(
                                    Modifier.weight(1.1f).fillMaxHeight(),
                                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                                )
                                availableNodesCard(
                                    Modifier.weight(1f).fillMaxHeight(),
                                    Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                subscriptionManagerCard(
                                    Modifier.fillMaxWidth(),
                                    Modifier.fillMaxWidth().heightIn(max = 140.dp).verticalScroll(rememberScrollState())
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                availableNodesCard(
                                    Modifier.fillMaxWidth().weight(1f),
                                    Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                        }
                    }                    2 -> { // Logs Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val isLogsTabActive = tabs[pagerState.currentPage].first == 2
                            LogsConsole(
                                isActive = isLogsTabActive,
                                context = context,
                                cardStyle = cardStyle,
                                isDark = isDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    3 -> { // Settings Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.conn_settings),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.bypass_iran), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.bypass_iran_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = bypassIran, onCheckedChange = { scope.launch { settingsManager.setBypassIran(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.bypass_lan), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.bypass_lan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = bypassLan, onCheckedChange = { scope.launch { settingsManager.setBypassLan(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.live_stats), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.live_stats_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = showLiveNotification, onCheckedChange = { scope.launch { settingsManager.setShowLiveNotification(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.show_logs_tab), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.show_logs_tab_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = showLogsTab, onCheckedChange = { scope.launch { settingsManager.setShowLogsTab(it) } })
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.delay_test_url_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.delay_test_url_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "http://cp.cloudflare.com/generate_204" to "Cloudflare",
                                                "http://www.google.com/generate_204" to "Google",
                                                "http://www.gstatic.com/generate_204" to "GStatic",
                                                "http://play.googleapis.com/generate_204" to "Google Play"
                                            ).forEach { (urlVal, label) ->
                                                FilterChip(
                                                    selected = delayTestUrl == urlVal,
                                                    onClick = { scope.launch { settingsManager.setDelayTestUrl(urlVal) } },
                                                    label = { Text(label) },
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }
                                    }

                                    if (showLiveNotification) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.guide_tap_here),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable { showLivePromoGuide = true }
                                                .padding(start = 36.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                                    .clickable { onItemClick(SplitTunneling) }
                                    .pressScaleEffect(),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(stringResource(R.string.split_tunneling), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = if (splitTunnelingEnabled) {
                                                    val modeText = if (splitTunnelingMode == "bypass") stringResource(R.string.bypass_apps) else stringResource(R.string.route_apps)
                                                    "$modeText: ${splitTunnelingApps.size}"
                                                } else stringResource(R.string.split_tunneling_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ExpressiveChipShape)
                                                .background(if (splitTunnelingEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (splitTunnelingEnabled) stringResource(R.string.state_on) else stringResource(R.string.state_off),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (splitTunnelingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = tertiaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.auto_update), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.auto_update_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = autoUpdateSubs, onCheckedChange = { scope.launch { settingsManager.setAutoUpdateSubs(it) } })
                                    }
                                    AnimatedVisibility(
                                         visible = autoUpdateSubs,
                                         enter = expandVertically() + fadeIn(),
                                         exit = shrinkVertically() + fadeOut()
                                     ) {
                                         Column {
                                             Spacer(modifier = Modifier.height(12.dp))
                                             HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                             Spacer(modifier = Modifier.height(8.dp))
                                             Text(stringResource(R.string.interval), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                             Spacer(modifier = Modifier.height(6.dp))
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                                             ) {
                                                 listOf("startup" to stringResource(R.string.startup), "daily" to stringResource(R.string.daily), "weekly" to stringResource(R.string.weekly)).forEach { (intervalKey, label) ->
                                                     FilterChip(
                                                         selected = autoUpdateInterval == intervalKey,
                                                         onClick = { scope.launch { settingsManager.setAutoUpdateInterval(intervalKey) } },
                                                         label = { Text(label) },
                                                         shape = ExpressiveChipShape
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.animateContentSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.adv_mode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.adv_mode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(checked = isAdvancedMode, onCheckedChange = { scope.launch { settingsManager.setAdvancedMode(it) } })
                                    }
                                    AnimatedVisibility(
                                         visible = isAdvancedMode,
                                         enter = expandVertically() + fadeIn(),
                                         exit = shrinkVertically() + fadeOut()
                                     ) {
                                         Column {
                                             HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                             Column(modifier = Modifier.padding(16.dp)) {
                                                 OutlinedTextField(
                                                     value = secureDns,
                                                     onValueChange = { scope.launch { settingsManager.setSecureDns(it) } },
                                                     label = { Text(stringResource(R.string.secure_dns_doh)) },
                                                     modifier = Modifier.fillMaxWidth(),
                                                     shape = ExpressiveButtonShape
                                                 )
                                                 
                                                 Spacer(modifier = Modifier.height(12.dp))
                                                 
                                                 Text(stringResource(R.string.tun_network_stack), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                 ) {
                                                     listOf("mixed", "gvisor", "system").forEach { stackOption ->
                                                         FilterChip(
                                                             selected = tunStack == stackOption,
                                                             onClick = { scope.launch { settingsManager.setTunStack(stackOption); if (vpnState == "CONNECTED") startVpnService(context) } },
                                                             label = { Text(stackOption) },
                                                             shape = ExpressiveButtonShape
                                                         )
                                                     }
                                                 }
                                                 
                                                 Spacer(modifier = Modifier.height(12.dp))
                                                 
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.SpaceBetween,
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Column(modifier = Modifier.weight(1f)) {
                                                         Text(stringResource(R.string.tls_fragmentation), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                         Text(stringResource(R.string.tls_fragmentation_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                     }
                                                     Switch(checked = enableFragment, onCheckedChange = { scope.launch { settingsManager.setEnableFragment(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                                 }
                                                 
                                                 AnimatedVisibility(
                                                     visible = enableFragment,
                                                     enter = expandVertically() + fadeIn(),
                                                     exit = shrinkVertically() + fadeOut()
                                                 ) {
                                                     Column {
                                                         Spacer(modifier = Modifier.height(8.dp))
                                                         Row(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                         ) {
                                                             OutlinedTextField(
                                                                 value = fragmentLength,
                                                                 onValueChange = { scope.launch { settingsManager.setFragmentLength(it) } },
                                                                 label = { Text(stringResource(R.string.length)) },
                                                                 modifier = Modifier.weight(1f),
                                                                 shape = ExpressiveButtonShape
                                                             )
                                                             OutlinedTextField(
                                                                 value = fragmentInterval,
                                                                 onValueChange = { scope.launch { settingsManager.setFragmentInterval(it) } },
                                                                 label = { Text(stringResource(R.string.interval)) },
                                                                 modifier = Modifier.weight(1f),
                                                                 shape = ExpressiveButtonShape
                                                             )
                                                         }
                                                     }
                                                 }
                                                 
                                                 Spacer(modifier = Modifier.height(12.dp))
                                                 
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.SpaceBetween,
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Column(modifier = Modifier.weight(1f)) {
                                                         Text(stringResource(R.string.tcp_multiplexing), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                         Text(stringResource(R.string.tcp_multiplexing_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                     }
                                                     Switch(checked = enableMux, onCheckedChange = { scope.launch { settingsManager.setEnableMux(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                                 }
                                             }
                                         }
                                     }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (true) {
                                val defaultThemeKey = if (Config.IS_SPECIAL) "cherry_blossom" else "dynamic"
                                val currentTheme by settingsManager.specialTheme.collectAsStateWithLifecycle(initialValue = defaultThemeKey)
                                val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle(initialValue = "system")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                        .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Row 1: Dark Mode Toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = when (themeMode) {
                                                        "light" -> Icons.Default.LightMode
                                                        "dark" -> Icons.Default.DarkMode
                                                        else -> Icons.Default.SettingsSuggest
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(stringResource(R.string.dark_mode_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.dark_mode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "system" to R.string.theme_mode_system,
                                                "light" to R.string.theme_mode_light,
                                                "dark" to R.string.theme_mode_dark
                                            ).forEach { (modeKey, stringId) ->
                                                FilterChip(
                                                    selected = themeMode == modeKey,
                                                    onClick = { scope.launch { settingsManager.setThemeMode(modeKey) } },
                                                    label = { Text(stringResource(stringId)) },
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Row 2: Theme Palette Selector
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.app_theme), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.select_style), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "dynamic" to R.string.theme_dynamic,
                                                "cherry_blossom" to R.string.theme_cherry,
                                                "lavender_dreams" to R.string.theme_lavender,
                                                "rose_gold" to R.string.theme_rosegold,
                                                "midnight_blue" to R.string.theme_midnight,
                                                "forest_green" to R.string.theme_forest,
                                                "sunset_orange" to R.string.theme_sunset,
                                                "ocean_teal" to R.string.theme_teal,
                                                "royal_amethyst" to R.string.theme_amethyst,
                                                "nordic_slate" to R.string.theme_slate
                                            ).filter { (themeKey, _) ->
                                                Config.IS_SPECIAL || (themeKey != "cherry_blossom" && themeKey != "lavender_dreams" && themeKey != "rose_gold")
                                            }.forEach { (themeKey, stringId) ->
                                                FilterChip(
                                                    selected = currentTheme == themeKey,
                                                    onClick = { scope.launch { settingsManager.setSpecialTheme(themeKey) } },
                                                    label = { Text(stringResource(stringId)) },
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Row 3: Card Style Selector
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(stringResource(R.string.style_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.style_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "glass" to R.string.style_glass,
                                                "solid" to R.string.style_solid,
                                                "vibrant" to R.string.style_vibrant
                                            ).forEach { (styleKey, stringId) ->
                                                FilterChip(
                                                    selected = cardStyle == styleKey,
                                                    onClick = { scope.launch { settingsManager.setCardStyle(styleKey) } },
                                                    label = { Text(stringResource(stringId)) },
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Expressive Box $appVersion",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (Config.IS_SPECIAL) "Made with ❤️ for Sana" else stringResource(R.string.app_short_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
                }
                }
            }
        }
    }

    var isImportFetching by remember { mutableStateOf(false) }

    // Import Profile Dialog
    if (showImportDialog) {
        val isImportingSubscription = remember(importText) {
            val trimmed = importText.trim()
            (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            !trimmed.startsWith("vless://") &&
            !trimmed.startsWith("trojan://") &&
            !trimmed.startsWith("ss://") &&
            !trimmed.startsWith("socks5://") &&
            !trimmed.startsWith("socks://") &&
            !trimmed.startsWith("vmess://") &&
            !trimmed.startsWith("hysteria2://") &&
            !trimmed.startsWith("hy2://") &&
            !trimmed.startsWith("tuic://")
        }

        AlertDialog(
            onDismissRequest = { if (!isImportFetching) showImportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.import_config_link),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.paste_config_link),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scanResultCallback = { result ->
                                    importText = result
                                }
                            },
                            enabled = !isImportFetching
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_qr_code),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = ExpressiveButtonShape,
                        placeholder = { Text("vless://... or vmess://... or hysteria2://... or tuic://...") },
                        enabled = !isImportFetching
                    )
                    if (isImportingSubscription) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                        val detectionText = if (isRtl) {
                            "لینک اشتراک شناسایی شد! با زدن درون‌ریزی، کانفیگ‌های آن دریافت و به مدیریت اشتراک‌ها اضافه می‌شوند."
                        } else {
                            "Subscription link detected! Clicking import will fetch its configs and add it to your Subscription Manager."
                        }
                        Text(
                            text = detectionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val trimmedImport = importText.trim()
                            if (trimmedImport.isNotEmpty()) {
                                if (isImportingSubscription) {
                                    isImportFetching = true
                                    try {
                                        val result = fetchSubscription(trimmedImport)
                                        if (result.servers.isNotEmpty()) {
                                            val domain = try {
                                                java.net.URI(trimmedImport).host ?: context.getString(R.string.custom_provider)
                                            } catch (e: Exception) {
                                                context.getString(R.string.custom_provider)
                                            }
                                            val newSub = Subscription(
                                                id = java.util.UUID.randomUUID().toString(),
                                                name = domain,
                                                url = trimmedImport,
                                                servers = result.servers.joinToString("\n"),
                                                upload = result.upload,
                                                download = result.download,
                                                total = result.total,
                                                expire = result.expire
                                            )
                                            val updatedList = subscriptions + newSub
                                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                            settingsManager.setActiveSubId(newSub.id)
                                            settingsManager.setActiveProfile(result.servers[0])
                                            
                                            if (vpnState == "CONNECTED") {
                                                startVpnService(context)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ExpressiveBox", "Failed to fetch subscription on import: ${e.message}")
                                    } finally {
                                        isImportFetching = false
                                    }
                                } else {
                                    val currentManual = manualServersStr
                                    val updatedManual = if (currentManual.isEmpty()) trimmedImport else "$currentManual\n$trimmedImport"
                                    settingsManager.setManualServers(updatedManual)
                                    settingsManager.setActiveSubId("manual")
                                    settingsManager.setActiveProfile(trimmedImport)
                                    if (vpnState == "CONNECTED") {
                                        startVpnService(context)
                                    }
                                }
                            }
                            showImportDialog = false
                            importText = ""
                        }
                    },
                    modifier = Modifier.pressScaleEffect(),
                    shape = ExpressiveButtonShape,
                    enabled = !isImportFetching && importText.trim().isNotEmpty()
                ) {
                    if (isImportFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.import_str))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    modifier = Modifier.pressScaleEffect(),
                    enabled = !isImportFetching
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = ExpressiveCardShape
        )
    }

    // Observe/Parse link when editor opens
    LaunchedEffect(editingNodeLink) {
        val link = editingNodeLink
        if (link != null) {
            if (link == "new_node") {
                editType = "vless"
                editRemark = "New Node"
                editServer = ""
                editPort = "443"
                editCreds = ""
                editTls = false
                editSni = ""
                editLinkInput = ""
                editorMode = "form"
                editShowAdvanced = false
                editTransportType = "tcp"
                editTransportPath = ""
                editTransportHost = ""
                editTransportServiceName = ""
                editTransportSeed = ""
                editTransportHeaderType = "none"
            } else if (link.startsWith("{")) {
                editorMode = "link"
                editLinkInput = link
            } else {
                try {
                    val trimmed = link.trim()
                    val fragmentIdx = trimmed.indexOf("#")
                    editRemark = if (fragmentIdx >= 0) {
                        try { java.net.URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8") } catch(e: Exception) { trimmed.substring(fragmentIdx + 1) }
                    } else { "" }
                    
                    val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
                    val schemeIdx = rest.indexOf("://")
                    val scheme = if (schemeIdx >= 0) rest.substring(0, schemeIdx).lowercase() else "vless"
                    editType = scheme
                    
                    val content = if (schemeIdx >= 0) rest.substring(schemeIdx + 3) else rest
                    val queryIdx = content.indexOf("?")
                    val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
                    val queryPart = if (queryIdx >= 0) content.substring(queryIdx + 1) else ""
                    
                    val atIdx = mainPart.indexOf("@")
                    editCreds = if (atIdx >= 0) mainPart.substring(0, atIdx) else ""
                    val serverPart = if (atIdx >= 0) mainPart.substring(atIdx + 1) else mainPart
                    
                    val colonIdx = serverPart.lastIndexOf(":")
                    editServer = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
                    editPort = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
                    
                    val queryParams = mutableMapOf<String, String>()
                    if (queryPart.isNotEmpty()) {
                        val pairs = queryPart.split("&")
                        for (pair in pairs) {
                            val idx = pair.indexOf("=")
                            if (idx > 0) {
                                val k = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                                val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                                queryParams[k] = v
                            }
                        }
                    }
                    val security = queryParams["security"]?.lowercase()
                    editTls = security == "tls" || security == "reality" || queryParams["tls"] == "true" || queryParams["tls"] == "1" || scheme == "https"
                    editSni = queryParams["sni"] ?: ""
                    
                    val type = queryParams["type"] ?: "tcp"
                    editTransportType = type
                    editTransportPath = queryParams["path"] ?: ""
                    editTransportHost = queryParams["host"] ?: ""
                    editTransportServiceName = queryParams["serviceName"] ?: queryParams["service_name"] ?: ""
                    editTransportSeed = queryParams["seed"] ?: ""
                    editTransportHeaderType = queryParams["headerType"] ?: queryParams["header_type"] ?: queryParams["header"] ?: "none"
                    editShowAdvanced = type != "tcp" && type.isNotEmpty()
                    
                    editLinkInput = link
                    editorMode = "form"
                } catch(e: Exception) {
                    editorMode = "link"
                    editLinkInput = link
                }
            }
        }
    }

    // Edit/Create Node Dialog
    if (editingNodeLink != null) {
        val isNewNode = editingNodeLink == "new_node"
        AlertDialog(
            onDismissRequest = { editingNodeLink = null },
            title = {
                Text(
                    text = if (isNewNode) stringResource(R.string.create_custom_node) else stringResource(R.string.edit_node_config),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = if (editorMode == "form") 0 else 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = editorMode == "form",
                            onClick = { editorMode = "form" },
                            text = { Text(stringResource(R.string.form_editor)) }
                        )
                        Tab(
                            selected = editorMode == "link",
                            onClick = { editorMode = "link" },
                            text = { Text(stringResource(R.string.raw_config)) }
                        )
                    }

                    if (editorMode == "form") {
                        // Protocol selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.protocol), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("vless", "trojan", "ss").forEach { proto ->
                                    val label = if (proto == "ss") "Shadowsocks" else proto.uppercase()
                                    FilterChip(
                                        selected = editType == proto,
                                        onClick = { 
                                            editType = proto
                                            if (proto == "ss") editTls = false
                                        },
                                        label = { Text(label) },
                                        shape = ExpressiveChipShape
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("socks5", "http", "https").forEach { proto ->
                                    FilterChip(
                                        selected = editType == proto,
                                        onClick = { 
                                            editType = proto 
                                            if (proto == "https") editTls = true
                                        },
                                        label = { Text(proto.uppercase()) },
                                        shape = ExpressiveChipShape
                                    )
                                }
                            }
                        }

                        // Remark / Name
                        OutlinedTextField(
                            value = editRemark,
                            onValueChange = { editRemark = it },
                            label = { Text(stringResource(R.string.remark_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        // Server & Port
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editServer,
                                onValueChange = { editServer = it },
                                label = { Text(stringResource(R.string.server_address)) },
                                modifier = Modifier.weight(2f),
                                shape = ExpressiveButtonShape
                            )
                            OutlinedTextField(
                                value = editPort,
                                onValueChange = { editPort = it },
                                label = { Text(stringResource(R.string.port)) },
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveButtonShape
                            )
                        }

                        // Credentials
                        OutlinedTextField(
                            value = editCreds,
                            onValueChange = { editCreds = it },
                            label = { Text(if (editType == "vless") stringResource(R.string.uuid) else stringResource(R.string.password_credentials)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        // TLS & SNI options
                        val showTlsOption = editType == "vless" || editType == "trojan" || editType == "https"
                        if (showTlsOption) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.enable_tls), fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = editTls || editType == "https",
                                    onCheckedChange = { if (editType != "https") editTls = it },
                                    enabled = editType != "https"
                                )
                            }
                            
                            if (editTls || editType == "https") {
                                OutlinedTextField(
                                    value = editSni,
                                    onValueChange = { editSni = it },
                                    label = { Text(stringResource(R.string.sni_server_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveButtonShape
                                )
                            }
                        }

                        // Advanced Settings Toggle
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced Transport Settings",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Switch(
                                checked = editShowAdvanced,
                                onCheckedChange = { editShowAdvanced = it }
                            )
                        }

                        if (editShowAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Transport Type selector
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Transport Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("tcp", "ws", "grpc").forEach { trans ->
                                            FilterChip(
                                                selected = editTransportType == trans,
                                                onClick = { editTransportType = trans },
                                                label = { Text(trans.uppercase()) },
                                                shape = ExpressiveChipShape
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("httpupgrade", "mkcp", "xhttp").forEach { trans ->
                                            val label = if (trans == "mkcp") "mKCP" else trans
                                            FilterChip(
                                                selected = editTransportType == trans,
                                                onClick = { editTransportType = trans },
                                                label = { Text(label) },
                                                shape = ExpressiveChipShape
                                            )
                                        }
                                    }
                                }

                                if (editTransportType == "ws" || editTransportType == "httpupgrade" || editTransportType == "xhttp") {
                                    OutlinedTextField(
                                        value = editTransportPath,
                                        onValueChange = { editTransportPath = it },
                                        label = { Text("Path") },
                                        placeholder = { Text("/") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editTransportHost,
                                        onValueChange = { editTransportHost = it },
                                        label = { Text("Host") },
                                        placeholder = { Text("example.com") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                } else if (editTransportType == "grpc") {
                                    OutlinedTextField(
                                        value = editTransportServiceName,
                                        onValueChange = { editTransportServiceName = it },
                                        label = { Text("Service Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                } else if (editTransportType == "kcp" || editTransportType == "mkcp") {
                                    OutlinedTextField(
                                        value = editTransportSeed,
                                        onValueChange = { editTransportSeed = it },
                                        label = { Text("KCP Seed") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Header Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("none", "srtp", "utp").forEach { hType ->
                                                FilterChip(
                                                    selected = editTransportHeaderType == hType,
                                                    onClick = { editTransportHeaderType = hType },
                                                    label = { Text(hType) },
                                                    shape = ExpressiveChipShape
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("wechat-video", "dtls", "wireguard").forEach { hType ->
                                                FilterChip(
                                                    selected = editTransportHeaderType == hType,
                                                    onClick = { editTransportHeaderType = hType },
                                                    label = { Text(hType) },
                                                    shape = ExpressiveChipShape
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Raw text area
                        OutlinedTextField(
                            value = editLinkInput,
                            onValueChange = { editLinkInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = ExpressiveButtonShape,
                            placeholder = { Text("vless://... or vmess://... or hysteria2://... or tuic://...") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val originalLink = editingNodeLink
                        if (originalLink != null) {
                            val finalLink = if (editorMode == "link") {
                                editLinkInput.trim()
                            } else {
                                try {
                                    val finalUserInfo = editCreds.trim()
                                    val finalServer = editServer.trim()
                                    val finalPort = editPort.trim().toIntOrNull() ?: 443
                                    val finalRemark = editRemark.trim()
                                    val queryList = mutableListOf<String>()
                                    if (editType == "vless" || editType == "trojan") {
                                        if (editTls) {
                                            queryList.add("security=tls")
                                            if (editSni.isNotEmpty()) queryList.add("sni=${java.net.URLEncoder.encode(editSni.trim(), "UTF-8")}")
                                        } else {
                                            queryList.add("security=none")
                                        }
                                    } else if (editType == "https") {
                                        if (editSni.isNotEmpty()) queryList.add("sni=${java.net.URLEncoder.encode(editSni.trim(), "UTF-8")}")
                                    }

                                    if (editShowAdvanced && editTransportType != "tcp") {
                                        queryList.add("type=$editTransportType")
                                        if (editTransportType == "ws" || editTransportType == "httpupgrade" || editTransportType == "xhttp") {
                                            if (editTransportPath.isNotEmpty()) {
                                                queryList.add("path=${java.net.URLEncoder.encode(editTransportPath.trim(), "UTF-8")}")
                                            }
                                            if (editTransportHost.isNotEmpty()) {
                                                queryList.add("host=${java.net.URLEncoder.encode(editTransportHost.trim(), "UTF-8")}")
                                            }
                                        } else if (editTransportType == "grpc") {
                                            if (editTransportServiceName.isNotEmpty()) {
                                                queryList.add("serviceName=${java.net.URLEncoder.encode(editTransportServiceName.trim(), "UTF-8")}")
                                            }
                                        } else if (editTransportType == "kcp" || editTransportType == "mkcp") {
                                            if (editTransportSeed.isNotEmpty()) {
                                                queryList.add("seed=${java.net.URLEncoder.encode(editTransportSeed.trim(), "UTF-8")}")
                                            }
                                            if (editTransportHeaderType.isNotEmpty()) {
                                                queryList.add("headerType=${java.net.URLEncoder.encode(editTransportHeaderType.trim(), "UTF-8")}")
                                            }
                                        }
                                    }

                                    val queryStr = if (queryList.isNotEmpty()) "?" + queryList.joinToString("&") else ""
                                    val remarkStr = if (finalRemark.isNotEmpty()) "#" + java.net.URLEncoder.encode(finalRemark, "UTF-8") else ""
                                    val protocolScheme = editType
                                    "$protocolScheme://$finalUserInfo@$finalServer:$finalPort$queryStr$remarkStr"
                                } catch (e: Exception) {
                                    ""
                                }
                            }

                            if (finalLink.isNotEmpty()) {
                                scope.launch {
                                    if (isNewNode) {
                                        val currentManual = manualServersStr
                                        val updatedManual = if (currentManual.isEmpty()) finalLink else "$currentManual\n$finalLink"
                                        settingsManager.setManualServers(updatedManual)
                                        settingsManager.setActiveSubId("manual")
                                        settingsManager.setActiveProfile(finalLink)
                                    } else {
                                        val currentManualList = manualServersStr.split("\n").filter { it.isNotEmpty() }
                                        val updatedManualList = currentManualList.map {
                                            if (it == originalLink) finalLink else it
                                        }
                                        settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                                        if (activeProfile == originalLink) {
                                            settingsManager.setActiveProfile(finalLink)
                                        }
                                    }
                                    if (vpnState == "CONNECTED") {
                                        startVpnService(context)
                                    }
                                    editingNodeLink = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.pressScaleEffect(),
                    shape = ExpressiveButtonShape
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingNodeLink = null },
                    modifier = Modifier.pressScaleEffect()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = ExpressiveCardShape
        )
    }

    val currentCallback = scanResultCallback
    if (currentCallback != null) {
        QrScannerScreen(
            onScanSuccess = { result ->
                currentCallback(result)
                scanResultCallback = null
            },
            onClose = {
                scanResultCallback = null
            }
        )
    }

    val currentQrShare = qrCodeToShare
    if (currentQrShare != null) {
        QrCodeShareDialog(
            title = currentQrShare.first,
            content = currentQrShare.second,
            onDismiss = { qrCodeToShare = null }
        )
    }

    AnimatedVisibility(
        visible = isNodesExpanded,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isNodesExpanded = false },
                        modifier = Modifier.pressScaleEffect()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.available_nodes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) searchQuery = ""
                        },
                        modifier = Modifier.pressScaleEffect()
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!isTestingPings) {
                                scope.launch {
                                    isTestingPings = true
                                    val jobs = serverList.map { link ->
                                        scope.async(Dispatchers.IO) {
                                            val hostPort = getHostAndPortFromLink(link)
                                            val ping = if (hostPort != null) {
                                                measurePingDelay(hostPort.first, hostPort.second)
                                            } else {
                                                -1
                                            }
                                            link to ping
                                        }
                                    }
                                    val results = jobs.awaitAll()
                                    pingsMap = pingsMap + results.toMap()
                                    isTestingPings = false
                                }
                            }
                        },
                        enabled = !isTestingPings,
                        modifier = Modifier.pressScaleEffect()
                    ) {
                        if (isTestingPings) {
                            LoadingIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = stringResource(R.string.test_pings),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = ExpressiveButtonShape,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (subscriptions.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subscriptions) { sub ->
                            val isSelected = activeSubId == sub.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    scope.launch {
                                        settingsManager.setActiveSubId(sub.id)
                                    }
                                },
                                label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                shape = ExpressiveChipShape
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {}
                ) {
                    listOf(stringResource(R.string.tab_all), "VLESS", "Trojan", "Shadowsocks", "VMess", "Hysteria", "TUIC").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredServerList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_matching_nodes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredServerList, key = { _, item -> item.link }) { index, serverItem ->
                            val serverLink = serverItem.link
                            val isSelected = activeProfile == serverLink
                            val name = serverItem.name
                            val type = serverItem.type
                            val transport = serverItem.transport

                            val tagContainerColor = when (type) {
                                "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                "VMESS" -> MaterialTheme.colorScheme.tertiaryContainer
                                "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.errorContainer
                                "TUIC" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val tagTextColor = when (type) {
                                "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "VMESS" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.onErrorContainer
                                "TUIC" -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(ExpressiveButtonShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = ExpressiveButtonShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            settingsManager.setActiveProfile(serverLink)
                                            if (vpnState == "CONNECTED") {
                                                startVpnService(context)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ExpressiveChipShape)
                                                .background(tagContainerColor)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tagTextColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(ExpressiveChipShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = transport,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        val ping = pingsMap[serverLink]
                                        if (ping != null) {
                                            val isTimeout = ping < 0
                                            val pingColor = when {
                                                isTimeout -> Color(0xFFF44336)
                                                ping < 60 -> Color(0xFF4CAF50)
                                                ping < 120 -> Color(0xFFFFB300)
                                                else -> Color(0xFFF44336)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(pingColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isTimeout) "Timeout" else "${ping} ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = pingColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.untested),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, serverLink)
                                                this.type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_config))
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = stringResource(R.string.share_config),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            qrCodeToShare = Pair(name, serverLink)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "QR Share",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (activeSubId == "manual") {
                                        IconButton(
                                            onClick = {
                                                editingNodeLink = serverLink
                                                editLinkInput = serverLink
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.edit_config),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val updatedManualList = serverList.filter { it != serverLink }
                                                    val updatedManualStr = updatedManualList.joinToString("\n")
                                                    settingsManager.setManualServers(updatedManualStr)
                                                    if (isSelected) {
                                                        val nextActive = updatedManualList.firstOrNull() ?: ""
                                                        settingsManager.setActiveProfile(nextActive)
                                                        if (vpnState == "CONNECTED" && nextActive.isNotEmpty()) {
                                                            startVpnService(context)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete_config),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
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
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionDashboard(
    state: String,
    cardStyle: String,
    isDark: Boolean,
    delayTestUrl: String,
    onConnectToggle: () -> Unit
) {
    val context = LocalContext.current
    val transition = updateTransition(targetState = state, label = "VPNStateTransition")

    val stateText = if (Config.IS_SPECIAL) {
        when (state) {
            "CONNECTED" -> "Meow"
            "CONNECTING" -> "CONNECTING TO YOUR HEART... 💓"
            "DISCONNECTING" -> "DISCONNECTING... 💔"
            else -> "OFFLINE, BUT THINKING OF YOU 💔"
        }
    } else {
        when (state) {
            "CONNECTED" -> "SECURED"
            "CONNECTING" -> "SHIELD ACTIVE..."
            "DISCONNECTING" -> "DISCONNECTING..."
            else -> "UNPROTECTED"
        }
    }

    val containerColor = when (state) {
        "CONNECTED" -> MaterialTheme.colorScheme.primaryContainer
        "CONNECTING" -> MaterialTheme.colorScheme.secondaryContainer
        "DISCONNECTING" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (state) {
        "CONNECTED" -> MaterialTheme.colorScheme.onPrimaryContainer
        "CONNECTING" -> MaterialTheme.colorScheme.onSecondaryContainer
        "DISCONNECTING" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val buttonColor by transition.animateColor(label = "ButtonColor") { s ->
        when (s) {
            "CONNECTED" -> MaterialTheme.colorScheme.primary
            "CONNECTING" -> MaterialTheme.colorScheme.secondary
            "DISCONNECTING" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

    val buttonIconColor by transition.animateColor(label = "ButtonIconColor") { s ->
        when (s) {
            "CONNECTED" -> MaterialTheme.colorScheme.onPrimary
            "CONNECTING" -> MaterialTheme.colorScheme.onSecondary
            "DISCONNECTING" -> MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val pulseInfiniteTransition = rememberInfiniteTransition(label = "ConnectingPulse")
    val pulseScale by pulseInfiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val scaleFactor by transition.animateFloat(
        label = "ButtonScale",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }
    ) { s ->
        if (s == "CONNECTED") 1.06f else 1.0f
    }

    val finalScale = if (state == "CONNECTING") pulseScale else scaleFactor

    var downloadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var uploadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var pingTime by remember { mutableStateOf("0 ms") }
    
    LaunchedEffect(state, delayTestUrl) {
        if (state == "CONNECTED") {
            launch(Dispatchers.IO) {
                while (true) {
                    val startTime = System.currentTimeMillis()
                    var connection: java.net.HttpURLConnection? = null
                    val ping = try {
                        val url = java.net.URL(delayTestUrl)
                        connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.requestMethod = "GET"
                        connection.useCaches = false
                        connection.instanceFollowRedirects = false
                        val responseCode = connection.responseCode
                        val elapsed = System.currentTimeMillis() - startTime
                        "${elapsed} ms"
                    } catch (e: Exception) {
                        "Timeout"
                    } finally {
                        connection?.disconnect()
                    }
                    
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        pingTime = ping
                    }
                    kotlinx.coroutines.delay(10000)
                }
            }
            
            var lastRx = android.net.TrafficStats.getTotalRxBytes()
            var lastTx = android.net.TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()

            while (true) {
                kotlinx.coroutines.delay(1000)
                val currentRx = android.net.TrafficStats.getTotalRxBytes()
                val currentTx = android.net.TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()
                
                val dt = (currentTime - lastTime) / 1000.0
                if (dt > 0.0) {
                    val dlBytes = currentRx - lastRx
                    val ulBytes = currentTx - lastTx
                    
                    val formatSpeed = { bytesPerSec: Long ->
                        val kb = bytesPerSec / 1024.0
                        val mb = kb / 1024.0
                        when {
                            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
                            kb >= 0.1 -> String.format(java.util.Locale.US, "%.1f KB/s", kb)
                            else -> "0.0 KB/s"
                        }
                    }
                    downloadSpeed = formatSpeed(if (dlBytes >= 0) (dlBytes / dt).toLong() else 0L)
                    uploadSpeed = formatSpeed(if (ulBytes >= 0) (ulBytes / dt).toLong() else 0L)
                }
                lastRx = currentRx
                lastTx = currentTx
                lastTime = currentTime
            }
        } else {
            downloadSpeed = "0.0 KB/s"
            uploadSpeed = "0.0 KB/s"
            pingTime = "--"
        }
    }

    val cardBackground = if (isDark) Color.Black else Color(0xFFF7F9FB)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val cardBorderBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, outlineVariant) {
        if (cardStyle == "solid" || cardStyle == "vibrant") {
            SolidColor(outlineVariant)
        } else {
            val colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.60f else 0.18f),
                secondaryColor.copy(alpha = if (isDark) 0.40f else 0.06f)
            )
            Brush.linearGradient(colors = colors)
        }
    }

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, surfaceContainerHigh, primaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerHigh)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.55f),
                    secondaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ActiveCardDashboardTransition")
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowOffset"
    )

    val activeCardBackgroundBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, tertiaryColor, primaryContainer, flowOffset) {
        if (cardStyle == "solid") {
            SolidColor(primaryContainer)
        } else if (cardStyle == "vibrant") {
            Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.68f),
                    secondaryColor.copy(alpha = 0.50f),
                    tertiaryColor.copy(alpha = 0.30f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.25f),
                    secondaryColor.copy(alpha = 0.15f),
                    Color.White
                )
            }
            Brush.linearGradient(
                colors = colors,
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                brush = if (state == "CONNECTED" || state == "CONNECTING") activeCardBackgroundBrush else primaryCardBrush,
                shape = ExpressiveCardShape
            )
            .border(
                width = 1.dp,
                brush = cardBorderBrush,
                shape = ExpressiveCardShape
            ),
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                if (state == "CONNECTED" || state == "CONNECTING") {
                    WaveVisualizer(
                        state = state,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(180.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = finalScale
                            scaleY = finalScale
                        }
                        .pressScaleEffect()
                        .clip(CircleShape)
                        .background(buttonColor)
                        .clickable { onConnectToggle() }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                ) {
                    if (state == "CONNECTING") {
                        LoadingIndicator(
                            modifier = Modifier.size(56.dp),
                            color = buttonIconColor
                        )
                    } else {
                        Icon(
                            imageVector = if (state == "CONNECTED") Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.connect_toggle),
                            tint = buttonIconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(containerColor)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (Config.IS_SPECIAL) {
                        PawPrint(
                            modifier = Modifier.size(16.dp),
                            color = if (state == "CONNECTED") MaterialTheme.colorScheme.primary 
                                    else if (state == "CONNECTING") MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state == "CONNECTED") MaterialTheme.colorScheme.primary 
                                    else if (state == "CONNECTING") MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stateText,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveButtonShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pingTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DOWNLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = downloadSpeed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UPLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = uploadSpeed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun startVpnService(context: Context) {
    val settingsManager = SettingsManager(context.applicationContext)
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        val currentSettings = settingsManager.settings.first()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            val intent = Intent(context, VpnServiceWrapper::class.java).apply {
                action = VpnServiceWrapper.ACTION_START
                putExtra("active_profile", currentSettings.activeProfile)
                putExtra("show_live_notification", currentSettings.showLiveNotification)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

private fun stopVpnService(context: Context) {
    val intent = Intent(context, VpnServiceWrapper::class.java).apply {
        action = VpnServiceWrapper.ACTION_STOP
    }
    context.startService(intent)
}

private data class FetchResult(
    val servers: List<String>,
    val upload: Long? = null,
    val download: Long? = null,
    val total: Long? = null,
    val expire: Long? = null
)

private data class SubscriptionUserInfo(
    val upload: Long?,
    val download: Long?,
    val total: Long?,
    val expire: Long?
)

private fun parseSubscriptionUserInfo(header: String?): SubscriptionUserInfo? {
    if (header == null) return null
    var upload: Long? = null
    var download: Long? = null
    var total: Long? = null
    var expire: Long? = null
    header.split(Regex("[;,]")).forEach { part ->
        val pair = if (part.contains("=")) part.split("=") else part.split(":")
        if (pair.size == 2) {
            val key = pair[0].trim().lowercase()
            val value = pair[1].trim().toLongOrNull()
            when (key) {
                "upload" -> upload = value
                "download" -> download = value
                "total" -> total = value
                "expire" -> expire = value
            }
        }
    }
    return SubscriptionUserInfo(upload, download, total, expire)
}

private suspend fun fetchSubscription(urlStr: String): FetchResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url = java.net.URL(urlStr)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "sing-box/1.9.0")
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val rawData = connection.inputStream.bufferedReader().use { it.readText() }
            val decoded = tryBase64Decode(rawData) ?: rawData
            val servers = decoded.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            var userInfoHeader: String? = null
            for ((key, values) in connection.headerFields) {
                if (key != null && (key.equals("subscription-userinfo", ignoreCase = true) || key.equals("x-user-info", ignoreCase = true))) {
                    userInfoHeader = values.firstOrNull()
                    break
                }
            }
            val parsedInfo = parseSubscriptionUserInfo(userInfoHeader)
            FetchResult(
                servers = servers,
                upload = parsedInfo?.upload,
                download = parsedInfo?.download,
                total = parsedInfo?.total,
                expire = parsedInfo?.expire
            )
        } else {
            FetchResult(emptyList())
        }
    } catch (e: Exception) {
        FetchResult(emptyList())
    }
}

@Composable
fun Modifier.pressScaleEffect(): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

// ServerItem helper class for precomputed server properties to optimize scroll performance
data class ServerItem(
    val link: String,
    val name: String,
    val type: String,
    val transport: String
)

fun getTransportType(link: String): String {
    val scheme = link.substringBefore("://").lowercase()
    if (scheme == "vmess") {
        val base64Part = link.substringAfter("vmess://")
        val decoded = tryBase64Decode(base64Part)
        if (decoded != null && decoded.startsWith("{")) {
            try {
                val json = org.json.JSONObject(decoded)
                val net = json.optString("net").lowercase()
                if (net.isNotEmpty()) {
                    return when (net) {
                        "tcp" -> "TCP"
                        "ws" -> "WebSocket"
                        "h2" -> "HTTP/2"
                        "http" -> "HTTP"
                        "grpc" -> "gRPC"
                        "httpupgrade" -> "HTTPUpgrade"
                        "kcp" -> "mKCP"
                        "mkcp" -> "mKCP"
                        "quic" -> "QUIC"
                        else -> net.uppercase()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    } else {
        try {
            val uri = java.net.URI(link)
            val query = uri.rawQuery
            if (query != null) {
                val params = query.split("&").associate {
                    val parts = it.split("=")
                    val key = parts[0].lowercase()
                    val value = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else ""
                    key to value
                }
                
                val type = params["type"]?.lowercase()
                if (type != null && type.isNotEmpty()) {
                    return when (type) {
                        "tcp" -> {
                            val headerType = params["headerType"] ?: params["header_type"]
                            if (headerType == "http") "HTTP" else "TCP"
                        }
                        "ws" -> "WebSocket"
                        "grpc" -> "gRPC"
                        "httpupgrade" -> "HTTPUpgrade"
                        "xhttp" -> "xHTTP"
                        "kcp" -> "mKCP"
                        "mkcp" -> "mKCP"
                        "quic" -> "QUIC"
                        else -> type.uppercase()
                    }
                }
                val plugin = params["plugin"]
                if (plugin != null && plugin.isNotEmpty()) {
                    return plugin.substringBefore(";").uppercase()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    return when (scheme) {
        "hysteria", "hysteria2", "hy2" -> "Hysteria"
        "tuic" -> "TUIC"
        "ssh" -> "SSH"
        else -> "TCP"
    }
}

private class WaveCache(points: Int) {
    val cosAngle = FloatArray(points + 1)
    val sinAngle = FloatArray(points + 1)
    
    val cos2a = FloatArray(points + 1)
    val sin2a = FloatArray(points + 1)
    
    val cos3a = FloatArray(points + 1)
    val sin3a = FloatArray(points + 1)
    
    val cos4a = FloatArray(points + 1)
    val sin4a = FloatArray(points + 1)
    
    val cos5a = FloatArray(points + 1)
    val sin5a = FloatArray(points + 1)
    
    val cos6a = FloatArray(points + 1)
    val sin6a = FloatArray(points + 1)
    
    val cos7a = FloatArray(points + 1)
    val sin7a = FloatArray(points + 1)
    
    val cos11a = FloatArray(points + 1)
    val sin11a = FloatArray(points + 1)
    
    init {
        val step = (2f * Math.PI / points).toFloat()
        for (i in 0..points) {
            val angle = i * step
            cosAngle[i] = kotlin.math.cos(angle)
            sinAngle[i] = kotlin.math.sin(angle)
            
            cos2a[i] = kotlin.math.cos(angle * 2f)
            sin2a[i] = kotlin.math.sin(angle * 2f)
            
            cos3a[i] = kotlin.math.cos(angle * 3f)
            sin3a[i] = kotlin.math.sin(angle * 3f)
            
            cos4a[i] = kotlin.math.cos(angle * 4f)
            sin4a[i] = kotlin.math.sin(angle * 4f)
            
            cos5a[i] = kotlin.math.cos(angle * 5f)
            sin5a[i] = kotlin.math.sin(angle * 5f)
            
            cos6a[i] = kotlin.math.cos(angle * 6f)
            sin6a[i] = kotlin.math.sin(angle * 6f)
            
            cos7a[i] = kotlin.math.cos(angle * 7f)
            sin7a[i] = kotlin.math.sin(angle * 7f)
            
            cos11a[i] = kotlin.math.cos(angle * 11f)
            sin11a[i] = kotlin.math.sin(angle * 11f)
        }
    }
}

@Composable
fun WaveVisualizer(
    state: String,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveVisualizerTransition")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 2f * Math.PI.toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (state == "CONNECTED" || state == "CONNECTING") 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "amplitude"
    )
    
    val scaleFactor by animateFloatAsState(
        targetValue = if (state == "CONNECTED" || state == "CONNECTING") 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val baseRadius1Px = remember(density) { density.run { 61.dp.toPx() } }
    val baseRadius2Px = remember(density) { density.run { 73.dp.toPx() } }
    val baseRadius3Px = remember(density) { density.run { 85.dp.toPx() } }
    
    val stroke1Px = remember(density) { density.run { 3.5.dp.toPx() } }
    val stroke2Px = remember(density) { density.run { 2.dp.toPx() } }
    val stroke3Px = remember(density) { density.run { 1.5.dp.toPx() } }
    
    val amp1Px = remember(density) { density.run { 6.dp.toPx() } }
    val amp2Px = remember(density) { density.run { 9.dp.toPx() } }
    val amp3Px = remember(density) { density.run { 7.dp.toPx() } }
    
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    
    val waveCache = remember { WaveCache(80) }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        
        if (amplitudeMultiplier > 0.01f) {
            val cosP1 = kotlin.math.cos(phase1)
            val sinP1 = kotlin.math.sin(phase1)
            val cosP2 = kotlin.math.cos(phase2)
            val sinP2 = kotlin.math.sin(phase2)
            
            val breathing1 = 1f + 0.08f * sinP1
            val breathing2 = 1f + 0.12f * cosP2
            val breathing3 = 1f + 0.15f * sinP1
            
            // Draw Wave 1 (Main Circle)
            path1.reset()
            val points1 = 80
            for (i in 0..points1) {
                val s4 = waveCache.sin4a[i] * cosP1 + waveCache.cos4a[i] * sinP1
                val c6 = waveCache.cos6a[i] * cosP2 + waveCache.sin6a[i] * sinP2
                val s2 = waveCache.sin2a[i] * cosP1 + waveCache.cos2a[i] * sinP1
                
                val waveOffset = s4 * 0.5f + c6 * 0.3f + s2 * 0.2f
                val wave = waveOffset * amp1Px * amplitudeMultiplier * breathing1
                val r = (baseRadius1Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path1.moveTo(x, y)
                } else {
                    path1.lineTo(x, y)
                }
            }
            path1.close()
            drawPath(
                path = path1,
                color = primaryColor.copy(alpha = 0.85f * amplitudeMultiplier),
                style = Stroke(width = stroke1Px)
            )
            
            // Draw Wave 2 (Middle Stripe)
            path2.reset()
            val points2 = 80
            for (i in 0..points2) {
                val s5 = waveCache.sin5a[i] * cosP2 - waveCache.cos5a[i] * sinP2
                val c3 = waveCache.cos3a[i] * cosP1 - waveCache.sin3a[i] * sinP1
                val s7 = waveCache.sin7a[i] * cosP2 - waveCache.cos7a[i] * sinP2
                
                val waveOffset = s5 * 0.6f + c3 * 0.3f + s7 * 0.1f
                val wave = waveOffset * amp2Px * amplitudeMultiplier * breathing2
                val r = (baseRadius2Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path2.moveTo(x, y)
                } else {
                    path2.lineTo(x, y)
                }
            }
            path2.close()
            drawPath(
                path = path2,
                color = secondaryColor.copy(alpha = 0.5f * amplitudeMultiplier),
                style = Stroke(width = stroke2Px)
            )
            
            // Draw Wave 3 (Outer Stripe)
            path3.reset()
            val points3 = 80
            for (i in 0..points3) {
                val c3 = waveCache.cos3a[i] * cosP1 - waveCache.sin3a[i] * sinP1
                val s7 = waveCache.sin7a[i] * cosP2 - waveCache.cos7a[i] * sinP2
                val c11 = waveCache.cos11a[i] * cosP1 - waveCache.sin11a[i] * sinP1
                
                val waveOffset = c3 * 0.5f + s7 * 0.3f + c11 * 0.2f
                val wave = waveOffset * amp3Px * amplitudeMultiplier * breathing3
                val r = (baseRadius3Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path3.moveTo(x, y)
                } else {
                    path3.lineTo(x, y)
                }
            }
            path3.close()
            drawPath(
                path = path3,
                color = primaryColor.copy(alpha = 0.3f * amplitudeMultiplier),
                style = Stroke(width = stroke3Px)
            )
        }
    }
}

@Composable
fun PeakingKitty(
    modifier: Modifier = Modifier,
    catColor: Color = Color(0xFFFFE5B4), // Peach/Cream cat
    earInnerColor: Color = Color(0xFFFFB7C5) // Pink inner ear
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.height(36.dp).width(50.dp)) {
        val width = size.width
        val height = size.height

        // Draw Cat Head peaking from bottom (y = height)
        val headRadius = width * 0.4f
        val headCenterX = width / 2f
        val headCenterY = height

        // Draw left ear
        val leftEarPath = Path().apply {
            moveTo(headCenterX - headRadius * 0.8f, headCenterY - headRadius * 0.5f)
            lineTo(headCenterX - headRadius * 1.1f, headCenterY - headRadius * 1.3f)
            lineTo(headCenterX - headRadius * 0.2f, headCenterY - headRadius * 0.9f)
            close()
        }
        drawPath(leftEarPath, catColor)
        drawPath(leftEarPath, outlineColor, style = Stroke(width = 2.dp.toPx()))

        // Inner left ear
        val leftEarInnerPath = Path().apply {
            moveTo(headCenterX - headRadius * 0.75f, headCenterY - headRadius * 0.55f)
            lineTo(headCenterX - headRadius * 0.95f, headCenterY - headRadius * 1.1f)
            lineTo(headCenterX - headRadius * 0.35f, headCenterY - headRadius * 0.8f)
            close()
        }
        drawPath(leftEarInnerPath, earInnerColor)

        // Draw right ear
        val rightEarPath = Path().apply {
            moveTo(headCenterX + headRadius * 0.8f, headCenterY - headRadius * 0.5f)
            lineTo(headCenterX + headRadius * 1.1f, headCenterY - headRadius * 1.3f)
            lineTo(headCenterX + headRadius * 0.2f, headCenterY - headRadius * 0.9f)
            close()
        }
        drawPath(rightEarPath, catColor)
        drawPath(rightEarPath, outlineColor, style = Stroke(width = 2.dp.toPx()))

        // Inner right ear
        val rightEarInnerPath = Path().apply {
            moveTo(headCenterX + headRadius * 0.75f, headCenterY - headRadius * 0.55f)
            lineTo(headCenterX + headRadius * 0.95f, headCenterY - headRadius * 1.1f)
            lineTo(headCenterX + headRadius * 0.35f, headCenterY - headRadius * 0.8f)
            close()
        }
        drawPath(rightEarInnerPath, earInnerColor)

        // Draw head circle
        drawArc(
            color = catColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(headCenterX - headRadius, headCenterY - headRadius),
            size = androidx.compose.ui.geometry.Size(headRadius * 2, headRadius * 2)
        )
        // Head outline (only the top curve)
        drawArc(
            color = outlineColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(headCenterX - headRadius, headCenterY - headRadius),
            size = androidx.compose.ui.geometry.Size(headRadius * 2, headRadius * 2),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Eyes (two little black circles)
        val eyeRadius = 3.dp.toPx()
        val leftEyeX = headCenterX - headRadius * 0.35f
        val rightEyeX = headCenterX + headRadius * 0.35f
        val eyeY = headCenterY - headRadius * 0.45f
        drawCircle(color = Color.Black, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
        drawCircle(color = Color.Black, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
        // Small eye highlight (white)
        drawCircle(color = Color.White, radius = eyeRadius * 0.3f, center = Offset(leftEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))
        drawCircle(color = Color.White, radius = eyeRadius * 0.3f, center = Offset(rightEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))

        // Draw Blush (two pink circles under eyes)
        drawCircle(color = earInnerColor.copy(alpha = 0.6f), radius = eyeRadius * 1.5f, center = Offset(leftEyeX - 2.dp.toPx(), eyeY + 4.dp.toPx()))
        drawCircle(color = earInnerColor.copy(alpha = 0.6f), radius = eyeRadius * 1.5f, center = Offset(rightEyeX + 2.dp.toPx(), eyeY + 4.dp.toPx()))

        // Draw Nose (small pink triangle)
        val nosePath = Path().apply {
            moveTo(headCenterX - 2.dp.toPx(), headCenterY - headRadius * 0.3f)
            lineTo(headCenterX + 2.dp.toPx(), headCenterY - headRadius * 0.3f)
            lineTo(headCenterX, headCenterY - headRadius * 0.23f)
            close()
        }
        drawPath(nosePath, earInnerColor)

        // Draw Mouth (two small curves w)
        val mouthY = headCenterY - headRadius * 0.2f
        val mouthPath = Path().apply {
            moveTo(headCenterX - 4.dp.toPx(), mouthY)
            quadraticTo(headCenterX - 2.dp.toPx(), mouthY + 2.dp.toPx(), headCenterX, mouthY)
            quadraticTo(headCenterX + 2.dp.toPx(), mouthY + 2.dp.toPx(), headCenterX + 4.dp.toPx(), mouthY)
        }
        drawPath(mouthPath, outlineColor, style = Stroke(width = 1.5.dp.toPx()))

        // Draw Whiskers (two lines on each side)
        drawLine(outlineColor, Offset(headCenterX - headRadius * 0.6f, headCenterY - headRadius * 0.25f), Offset(headCenterX - headRadius * 1.1f, headCenterY - headRadius * 0.3f), strokeWidth = 1.5.dp.toPx())
        drawLine(outlineColor, Offset(headCenterX - headRadius * 0.6f, headCenterY - headRadius * 0.15f), Offset(headCenterX - headRadius * 1.1f, headCenterY - headRadius * 0.15f), strokeWidth = 1.5.dp.toPx())

        drawLine(outlineColor, Offset(headCenterX + headRadius * 0.6f, headCenterY - headRadius * 0.25f), Offset(headCenterX + headRadius * 1.1f, headCenterY - headRadius * 0.3f), strokeWidth = 1.5.dp.toPx())
        drawLine(outlineColor, Offset(headCenterX + headRadius * 0.6f, headCenterY - headRadius * 0.15f), Offset(headCenterX + headRadius * 1.1f, headCenterY - headRadius * 0.15f), strokeWidth = 1.5.dp.toPx())

        // Draw Paws resting on the line (which is y = height)
        val pawWidth = 8.dp.toPx()
        val pawHeight = 6.dp.toPx()
        // Paw 1: left
        drawRoundRect(
            color = catColor,
            topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Paw 2: right
        drawRoundRect(
            color = catColor,
            topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun PawPrint(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Main pad (large oval/circle)
        val padRadius = width * 0.28f
        val padCenterX = width / 2f
        val padCenterY = height * 0.6f
        drawCircle(color = color, radius = padRadius, center = Offset(padCenterX, padCenterY))
        
        // 4 toes
        val toeRadius = width * 0.12f
        // Leftmost toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX - padRadius * 1.2f, padCenterY - padRadius * 0.8f))
        // Middle left toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX - padRadius * 0.4f, padCenterY - padRadius * 1.5f))
        // Middle right toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX + padRadius * 0.4f, padCenterY - padRadius * 1.5f))
        // Rightmost toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX + padRadius * 1.2f, padCenterY - padRadius * 0.8f))
    }
}

@Composable
private fun LogsConsole(
    isActive: Boolean,
    context: Context,
    cardStyle: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val rawVpnLogs by VpnServiceWrapper.vpnLogs.collectAsStateWithLifecycle()
    val vpnLogs = if (isActive) rawVpnLogs else ""
    val logLines = remember(vpnLogs) {
        if (vpnLogs.isEmpty()) {
            emptyList()
        } else {
            vpnLogs.split("\n")
        }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    val filteredLogLines = remember(logLines, selectedFilter) {
        if (selectedFilter == "ALL") {
            logLines
        } else {
            logLines.filter { line ->
                when (selectedFilter) {
                    "INFO" -> line.contains("INFO", ignoreCase = true)
                    "WARN" -> line.contains("WARN", ignoreCase = true)
                    "ERROR" -> line.contains("ERROR", ignoreCase = true) || line.contains("FATAL", ignoreCase = true)
                    "DEBUG" -> line.contains("DEBUG", ignoreCase = true)
                    else -> true
                }
            }
        }
    }
    
    val cardBackground = if (isDark) Color.Black else Color(0xFFF7F9FB)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val cardBorderBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, outlineVariant) {
        if (cardStyle == "solid" || cardStyle == "vibrant") {
            SolidColor(outlineVariant)
        } else {
            val colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.60f else 0.18f),
                secondaryColor.copy(alpha = if (isDark) 0.40f else 0.06f)
            )
            Brush.linearGradient(colors = colors)
        }
    }

    val tertiaryCardBrush = remember(isDark, cardStyle, tertiaryColor, primaryColor, surfaceContainerLow, tertiaryContainer, primaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerLow)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            val colors = if (isDark) {
                listOf(
                    tertiaryColor.copy(alpha = 0.55f),
                    primaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    tertiaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(280.dp)
            .background(brush = tertiaryCardBrush, shape = ExpressiveCardShape)
            .border(
                width = 1.dp,
                brush = cardBorderBrush,
                shape = ExpressiveCardShape
            ),
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.engine_logs),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    TextButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val logsToCopy = filteredLogLines.joinToString("\n")
                            val clip = android.content.ClipData.newPlainText("VPN Logs", logsToCopy)
                            clipboardManager.setPrimaryClip(clip)
                        },
                        modifier = Modifier.pressScaleEffect()
                    ) {
                        Text(stringResource(R.string.copy))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { VpnServiceWrapper.clearLogs() },
                        modifier = Modifier.pressScaleEffect()
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))

            // Log level filter chips
            val filterLevels = listOf("ALL", "INFO", "WARN", "ERROR", "DEBUG")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterLevels.forEach { level ->
                    val isSelected = selectedFilter == level
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    val chipText = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    val chipBorder = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg,
                        border = chipBorder,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedFilter = level }
                    ) {
                        Text(
                            text = level,
                            color = chipText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            val listState = rememberLazyListState()
            LaunchedEffect(filteredLogLines.size) {
                if (filteredLogLines.isNotEmpty()) {
                    listState.scrollToItem(filteredLogLines.size - 1)
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (filteredLogLines.isEmpty()) {
                    item {
                        Text(
                            text = if (logLines.isEmpty()) stringResource(R.string.logs_placeholder) else "No logs match this filter",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    itemsIndexed(filteredLogLines) { index, line ->
                        Text(
                            text = line,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

fun generateQrCode(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun QrCodeShareDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(content) {
        generateQrCode(content, 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = "Failed to generate QR Code",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, content)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            ) {
                Text(stringResource(R.string.share_config))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel)) // or R.string.close or cancel
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatExpiry(expirySecs: Long): String {
    if (expirySecs <= 0) return ""
    val ms = expirySecs * 1000L
    val date = java.util.Date(ms)
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return format.format(date)
}

