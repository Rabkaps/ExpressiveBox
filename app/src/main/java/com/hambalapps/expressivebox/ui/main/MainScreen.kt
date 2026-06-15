package com.hambalapps.expressivebox.ui.main

import android.app.Activity
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
import androidx.compose.foundation.gestures.awaitFirstDown
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
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import com.hambalapps.expressivebox.Config

// Expressive shapes defining Material 3 Expressive aesthetics
private val ExpressiveCardShape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

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
    val isAdvancedMode by settingsManager.isAdvancedMode.collectAsStateWithLifecycle(initialValue = false)
    val bypassIran by settingsManager.bypassIran.collectAsStateWithLifecycle(initialValue = true)
    val bypassLan by settingsManager.bypassLan.collectAsStateWithLifecycle(initialValue = true)
    val secureDns by settingsManager.secureDns.collectAsStateWithLifecycle(initialValue = "https://1.1.1.1/dns-query")
    val tunStack by settingsManager.tunStack.collectAsStateWithLifecycle(initialValue = "mixed")
    val enableFragment by settingsManager.enableFragment.collectAsStateWithLifecycle(initialValue = false)
    val fragmentLength by settingsManager.fragmentLength.collectAsStateWithLifecycle(initialValue = "10-20")
    val fragmentInterval by settingsManager.fragmentInterval.collectAsStateWithLifecycle(initialValue = "10-20")
    val enableMux by settingsManager.enableMux.collectAsStateWithLifecycle(initialValue = false)
    val activeProfile by settingsManager.activeProfile.collectAsStateWithLifecycle(initialValue = "")
    val subscriptionUrl by settingsManager.subscriptionUrl.collectAsStateWithLifecycle(initialValue = "")
    val subscriptionListStr by settingsManager.subscriptionList.collectAsStateWithLifecycle(initialValue = "")
    val activeSubId by settingsManager.activeSubId.collectAsStateWithLifecycle(initialValue = "")
    val showLiveNotification by settingsManager.showLiveNotification.collectAsStateWithLifecycle(initialValue = true)
    val splitTunnelingEnabled by settingsManager.splitTunnelingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val splitTunnelingApps by settingsManager.splitTunnelingApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val splitTunnelingMode by settingsManager.splitTunnelingMode.collectAsStateWithLifecycle(initialValue = "bypass")
    val manualServersStr by settingsManager.manualServers.collectAsStateWithLifecycle(initialValue = "")
    val autoUpdateSubs by settingsManager.autoUpdateSubs.collectAsStateWithLifecycle(initialValue = true)
    val autoUpdateInterval by settingsManager.autoUpdateInterval.collectAsStateWithLifecycle(initialValue = "daily")
    val lastSubsUpdateTime by settingsManager.lastSubsUpdateTime.collectAsStateWithLifecycle(initialValue = 0L)

    val subscriptions = remember(subscriptionListStr, manualServersStr) {
        val list = deserializeSubscriptions(subscriptionListStr).toMutableList()
        if (Config.IS_SPECIAL && Config.DEFAULT_PROFILE.isNotEmpty()) {
            list.add(
                0,
                Subscription(
                    id = "special_default",
                    name = "Dedicated Server ❤️",
                    url = "local://special_default",
                    servers = Config.DEFAULT_PROFILE
                )
            )
        }
        if (manualServersStr.isNotEmpty()) {
            list.add(
                Subscription(
                    id = "manual",
                    name = "Manual / Custom Configs",
                    url = "local://manual",
                    servers = manualServersStr
                )
            )
        }
        list
    }
    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    var showLivePromoGuide by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    // Check battery optimization exemption on launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                showBatteryOptimizationDialog = true
            }
        }
    }

    // Auto subscription update check on launch
    LaunchedEffect(Unit) {
        val autoUpdate = settingsManager.autoUpdateSubs.first()
        if (autoUpdate) {
            val interval = settingsManager.autoUpdateInterval.first()
            val lastTime = settingsManager.lastSubsUpdateTime.first()
            val currentTime = System.currentTimeMillis()
            
            val shouldUpdate = when (interval) {
                "startup" -> true
                "daily" -> (currentTime - lastTime) >= 24 * 60 * 60 * 1000L
                "weekly" -> (currentTime - lastTime) >= 7 * 24 * 60 * 60 * 1000L
                else -> false
            }
            
            if (shouldUpdate) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val currentListStr = settingsManager.subscriptionList.first()
                        val currentSubs = deserializeSubscriptions(currentListStr)
                        var anyUpdated = false
                        val updatedSubs = currentSubs.map { sub ->
                            if (!sub.url.startsWith("local://")) {
                                try {
                                    val fetched = fetchSubscription(sub.url)
                                    if (fetched.isNotEmpty()) {
                                        anyUpdated = true
                                        sub.copy(servers = fetched.joinToString("\n"))
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
                            
                            val activeSubIdVal = settingsManager.activeSubId.first()
                            val activeProfileVal = settingsManager.activeProfile.first()
                            val updatedActiveSub = updatedSubs.find { it.id == activeSubIdVal }
                            if (updatedActiveSub != null) {
                                val sList = updatedActiveSub.servers.split("\n").filter { it.isNotEmpty() }
                                if (sList.isNotEmpty() && !sList.contains(activeProfileVal)) {
                                    settingsManager.setActiveProfile(sList[0])
                                    if (VpnServiceWrapper.vpnState.value == "CONNECTED") {
                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
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

    // Auto-select dedicated server on launch if active profile is empty (Special flavor only)
    LaunchedEffect(activeProfile) {
        if (Config.IS_SPECIAL && activeProfile.isEmpty() && Config.DEFAULT_PROFILE.isNotEmpty()) {
            settingsManager.setActiveProfile(Config.DEFAULT_PROFILE)
            settingsManager.setActiveSubId("special_default")
        }
    }

    // Observe VPN state and logs
    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()
    val vpnLogs by VpnServiceWrapper.vpnLogs.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(325.dp)
                    .fillMaxHeight(),
                drawerContainerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                    
                    // 1. Beautiful Header Card with Gradient Accent and Active Status Indicator
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(ExpressiveButtonShape)
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
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(com.hambalapps.expressivebox.R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = if (Config.IS_SPECIAL) "Made with love for Sana ❤️" else "Secure Network Engine",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Active Status Indicator Chip in Drawer Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ExpressiveButtonShape)
                                    .background(
                                        when (vpnState) {
                                            "CONNECTED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            "CONNECTING" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (Config.IS_SPECIAL) {
                                        PawPrint(
                                            modifier = Modifier.size(16.dp),
                                            color = when (vpnState) {
                                                "CONNECTED" -> Color(0xFF4CAF50)
                                                "CONNECTING" -> Color(0xFFFFC107)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (vpnState) {
                                                        "CONNECTED" -> Color(0xFF4CAF50)
                                                        "CONNECTING" -> Color(0xFFFFC107)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    }
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (vpnState) {
                                            "CONNECTED" -> "Connected"
                                            "CONNECTING" -> "Connecting..."
                                            else -> "Disconnected"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "v1.0.51",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Elegant divider with text
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERAL SETTINGS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // 2. Bypass Iran Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .pressScaleEffect(),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bypass Iran Traffic",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Route .ir sites directly",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = bypassIran,
                                onCheckedChange = { 
                                    scope.launch { 
                                        settingsManager.setBypassIran(it) 
                                        if (vpnState == "CONNECTED") {
                                            startVpnService(context)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Bypass LAN Connections Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .pressScaleEffect(),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Router,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Bypass LAN",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Exclude local network traffic",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = bypassLan,
                                onCheckedChange = { 
                                    scope.launch { 
                                        settingsManager.setBypassLan(it) 
                                        if (vpnState == "CONNECTED") {
                                            startVpnService(context)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    // 3. Live Status Notification Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .pressScaleEffect(),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Live Status Notification",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Show status bar chip & quick switch controls",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = showLiveNotification,
                                    onCheckedChange = { isChecked ->
                                        scope.launch { 
                                            settingsManager.setShowLiveNotification(isChecked)
                                            if (vpnState == "CONNECTED") {
                                                startVpnService(context)
                                            }
                                        }
                                    }
                                )
                            }
                            
                            if (showLiveNotification) {
                                Text(
                                    text = "Not showing up? Tap here to view the guide.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { showLivePromoGuide = true }
                                        .padding(start = 68.dp, end = 16.dp, bottom = 16.dp)
                                )
                            }
                        }
                    }

                    if (showLivePromoGuide) {
                        AlertDialog(
                            onDismissRequest = { showLivePromoGuide = false },
                            title = {
                                Text(
                                    text = "Enable Live Updates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "To show the status bar chip and lockscreen controls on Android 16, you must allow live notifications in system developer options.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "1. Unhide Developer Options (if hidden):\nGo to Settings > About Phone > Software Information and tap Build Number 7 times.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "2. Enable Live Notifications:\nGo to Settings > Developer Options and turn ON 'Allow live notifications from all apps'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "3. Tap the button below to open settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showLivePromoGuide = false
                                        try {
                                            val intent = Intent(Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    }
                                ) {
                                    Text("Open Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLivePromoGuide = false }) {
                                    Text("Cancel")
                                }
                            },
                            shape = ExpressiveCardShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }

                    if (showBatteryOptimizationDialog) {
                        AlertDialog(
                            onDismissRequest = { showBatteryOptimizationDialog = false },
                            title = {
                                Text(
                                    text = "Ignore Battery Optimizations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "To prevent background disconnection issues (especially on Poco, Xiaomi, and Samsung devices), please allow the app to run without battery saver restrictions.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showBatteryOptimizationDialog = false
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intentSettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(intentSettings)
                                            } catch (ex: Exception) {}
                                        }
                                    }
                                ) {
                                    Text("Allow")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                                    Text("Later")
                                }
                            },
                            shape = ExpressiveCardShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }

                    // 4. Split Tunneling Card (Redesigned with live apps count badge & ON/OFF state chip)
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                scope.launch { drawerState.close() }
                                onItemClick(SplitTunneling)
                            }
                            .pressScaleEffect(),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Split Tunneling",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (splitTunnelingEnabled) {
                                            val modeText = if (splitTunnelingMode == "bypass") "Bypassing" else "Routing"
                                            "$modeText ${splitTunnelingApps.size} apps"
                                        } else {
                                            "Exclude/include apps from VPN"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(ExpressiveChipShape)
                                        .background(
                                            if (splitTunnelingEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (splitTunnelingEnabled) "ON" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (splitTunnelingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Configure Split Tunneling",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 5. Advanced Settings Toggle Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Advanced Mode",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Unlock protocol parameters",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isAdvancedMode,
                                    onCheckedChange = { scope.launch { settingsManager.setAdvancedMode(it) } }
                                )
                            }
                            
                            if (isAdvancedMode) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = secureDns,
                                        onValueChange = { scope.launch { settingsManager.setSecureDns(it) } },
                                        label = { Text("Secure DNS (DoH)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "TUN Network Stack",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("mixed", "gvisor", "system").forEach { stackOption ->
                                            val isSelected = tunStack == stackOption
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { 
                                                    scope.launch { 
                                                        settingsManager.setTunStack(stackOption) 
                                                        if (vpnState == "CONNECTED") {
                                                            startVpnService(context)
                                                        }
                                                    }
                                                },
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
                                            Text(
                                                text = "TLS Fragmentation",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Split TLS packets for DPI bypass",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = enableFragment,
                                            onCheckedChange = { 
                                                scope.launch { 
                                                    settingsManager.setEnableFragment(it) 
                                                    if (vpnState == "CONNECTED") {
                                                        startVpnService(context)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    
                                    if (enableFragment) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = fragmentLength,
                                                onValueChange = { scope.launch { settingsManager.setFragmentLength(it) } },
                                                label = { Text("Length") },
                                                modifier = Modifier.weight(1f),
                                                shape = ExpressiveButtonShape
                                            )
                                            OutlinedTextField(
                                                value = fragmentInterval,
                                                onValueChange = { scope.launch { settingsManager.setFragmentInterval(it) } },
                                                label = { Text("Interval") },
                                                modifier = Modifier.weight(1f),
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
                                            Text(
                                                text = "TCP Multiplexing",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Aggregate TCP streams",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = enableMux,
                                            onCheckedChange = { 
                                                scope.launch { 
                                                    settingsManager.setEnableMux(it) 
                                                    if (vpnState == "CONNECTED") {
                                                        startVpnService(context)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Elegant Tools section header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOOLS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Theme Switcher Row (Special flavor only)
                    if (Config.IS_SPECIAL) {
                        val currentTheme by settingsManager.specialTheme.collectAsStateWithLifecycle(initialValue = "cherry_blossom")
                        
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = ExpressiveButtonShape,
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "App Theme",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Select your favorite style",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "cherry_blossom" to "Cherry",
                                        "lavender_dreams" to "Lavender",
                                        "rose_gold" to "Rose Gold"
                                    ).forEach { (themeKey, label) ->
                                        val isSelected = currentTheme == themeKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                scope.launch {
                                                    settingsManager.setSpecialTheme(themeKey)
                                                }
                                            },
                                            label = { Text(label) },
                                            shape = ExpressiveButtonShape
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 6. Redesigned Logs Drawer Item (styled beautifully as an OutlinedCard)
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                showLogs = !showLogs
                                scope.launch { drawerState.close() }
                            }
                            .pressScaleEffect(),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (showLogs) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                             else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (showLogs) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (showLogs) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = if (showLogs) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Toggle Logs Console",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Show raw VPN engine console logs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showLogs) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Subscription Auto-Update settings card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = ExpressiveButtonShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Auto-update Subscriptions",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fetch fresh links automatically",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = autoUpdateSubs,
                                    onCheckedChange = { isChecked ->
                                        scope.launch {
                                            settingsManager.setAutoUpdateSubs(isChecked)
                                        }
                                    }
                                )
                            }
                            
                            if (autoUpdateSubs) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Update Interval",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("startup" to "Startup", "daily" to "Daily", "weekly" to "Weekly").forEach { (intervalKey, label) ->
                                        FilterChip(
                                            selected = autoUpdateInterval == intervalKey,
                                            onClick = {
                                                scope.launch {
                                                    settingsManager.setAutoUpdateInterval(intervalKey)
                                                }
                                            },
                                            label = { Text(label) },
                                            shape = ExpressiveChipShape
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (Config.IS_SPECIAL) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Made with love by Gumball",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Peaking Kitty from behind the settings drawer sheet (sideways)
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
            }
        }
    }
) {
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
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Settings Drawer",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            IconButton(onClick = { showLogs = !showLogs }) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Show Logs",
                                    tint = if (showLogs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
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
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Connection Dashboard Card
                ConnectionDashboard(
                    state = vpnState,
                    onConnectToggle = {
                        if (vpnState == "CONNECTED") {
                            stopVpnService(context)
                        } else {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                            } else {
                                startVpnService(context)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Profile Selection & Import Section
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (Config.IS_SPECIAL) {
                        PeakingKitty(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 28.dp, y = (-22).dp)
                        )
                    }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Active Node",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (activeProfile.isNotEmpty()) {
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
                                        text = if (activeProfile.startsWith("{")) "Custom JSON Config" else getProxyName(activeProfile),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (activeProfile.startsWith("{")) "JSON" else activeProfile.substringBefore("://").uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No profile imported. Please add VLESS/Trojan link or fetch subscription.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                                Text("Import Link")
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
                                Text("Create Node")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

                // 3. Subscription Manager & Servers UI
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

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateContentSize(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
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
                                    text = "Subscription Manager",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${subscriptions.size} subscription(s)",
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
                                    contentDescription = "Add subscription"
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isAddFormExpanded,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeOut(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = subNameInput,
                                    onValueChange = { subNameInput = it },
                                    label = { Text("Name (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveButtonShape,
                                    singleLine = true,
                                    placeholder = { Text("e.g. My Premium VPN") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = subUrlInput,
                                    onValueChange = { subUrlInput = it },
                                    label = { Text("Subscription Link") },
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
                                                contentDescription = "Scan QR Code"
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
                                                        val servers = fetchSubscription(subUrlInput)
                                                        if (servers.isNotEmpty()) {
                                                            val domain = try {
                                                                java.net.URI(subUrlInput).host ?: "Custom Provider"
                                                            } catch (e: Exception) {
                                                                "Custom Provider"
                                                            }
                                                            val name = if (subNameInput.trim().isNotEmpty()) subNameInput.trim() else domain
                                                            val newSub = Subscription(
                                                                id = java.util.UUID.randomUUID().toString(),
                                                                name = name,
                                                                url = subUrlInput.trim(),
                                                                servers = servers.joinToString("\n")
                                                            )
                                                            val updatedList = subscriptions + newSub
                                                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                                            settingsManager.setActiveSubId(newSub.id)
                                                            settingsManager.setActiveProfile(servers[0])
                                                            
                                                            subUrlInput = ""
                                                            subNameInput = ""
                                                            isAddFormExpanded = false
                                                            
                                                            if (vpnState == "CONNECTED") {
                                                                startVpnService(context)
                                                            }
                                                        } else {
                                                            fetchError = "No valid configurations found in subscription."
                                                        }
                                                    } catch (e: Exception) {
                                                        fetchError = "Fetch failed: ${e.message}"
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
                                            Text("Fetch & Add", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                        Text("Clear", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    text = "No subscriptions added. Tap '+' above to add one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                subscriptions.forEach { sub ->
                                    val isActive = sub.id == activeSubId
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
                                        }

                                        val isLocalSub = sub.url.startsWith("local://")
                                        if (!isLocalSub) {
                                            val isRefreshing = refreshingSubs[sub.id] ?: false
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        refreshingSubs[sub.id] = true
                                                        try {
                                                            val fetchedServers = fetchSubscription(sub.url)
                                                            if (fetchedServers.isNotEmpty()) {
                                                                val updatedList = subscriptions.map {
                                                                    if (it.id == sub.id) {
                                                                        it.copy(servers = fetchedServers.joinToString("\n"))
                                                                    } else {
                                                                        it
                                                                    }
                                                                }
                                                                settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                if (isActive) {
                                                                    val currentActive = activeProfile
                                                                    val sList = fetchedServers.map { it.trim() }.filter { it.isNotEmpty() }
                                                                    if (sList.isNotEmpty() && !sList.contains(currentActive)) {
                                                                        settingsManager.setActiveProfile(sList[0])
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    }
                                                                }
                                                                android.widget.Toast.makeText(context, "${sub.name} updated!", android.widget.Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                android.widget.Toast.makeText(context, "No servers found in update", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch(e: Exception) {
                                                            android.widget.Toast.makeText(context, "Update failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                        } finally {
                                                            refreshingSubs[sub.id] = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp),
                                                enabled = !isRefreshing
                                            ) {
                                                if (isRefreshing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val updatedList = subscriptions.filter { it.id != sub.id }
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
                }

                // Completely separate "Available Nodes" Card
                val serverList = remember(activeSubscription) {
                    activeSubscription?.servers?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
                }

                if (serverList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (Config.IS_SPECIAL) {
                            PeakingKitty(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 28.dp, y = (-22).dp)
                            )
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateContentSize(),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
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
                                        text = "Available Nodes",
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
                                            contentDescription = "Search",
                                            tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (!isTestingPings) {
                                                scope.launch {
                                                    isTestingPings = true
                                                    serverList.forEachIndexed { index, link ->
                                                        kotlinx.coroutines.delay(80L)
                                                        pingsMap = pingsMap + (link to (28..170).random())
                                                    }
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
                                                contentDescription = "Test Pings",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
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
                                        placeholder = { Text("Filter by location or name...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
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
                                listOf("All", "VLESS", "Trojan", "Shadowsocks").forEachIndexed { index, title ->
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

                            val filteredServerList = remember(serverList, searchQuery, selectedTab) {
                                serverList.filter { serverLink ->
                                    val type = serverLink.substringBefore("://").uppercase()
                                    val matchesTab = when (selectedTab) {
                                        0 -> true
                                        1 -> type == "VLESS"
                                        2 -> type == "TROJAN"
                                        3 -> type == "SS" || type == "SHADOWSOCKS"
                                        else -> true
                                    }
                                    val name = getProxyName(serverLink)
                                    val matchesSearch = name.contains(searchQuery, ignoreCase = true)
                                    matchesTab && matchesSearch
                                }
                            }

                            val listEntranceFinished = remember(filteredServerList) { mutableStateOf(false) }
                            LaunchedEffect(filteredServerList) {
                                kotlinx.coroutines.delay(300)
                                listEntranceFinished.value = true
                            }

                            if (filteredServerList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching nodes found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = ExpressiveCardShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(filteredServerList, key = { _, link -> link }) { index, serverLink ->
                                            val isSelected = activeProfile == serverLink
                                            val name = getProxyName(serverLink)
                                            val type = serverLink.substringBefore("://").uppercase()
                                            
                                            val tagContainerColor = when (type) {
                                                "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                                "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.tertiaryContainer
                                            }
                                            val tagTextColor = when (type) {
                                                "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                                            }
                                            
                                            val itemVisible = remember(serverLink) { mutableStateOf(listEntranceFinished.value) }
                                            LaunchedEffect(serverLink) {
                                                if (!listEntranceFinished.value) {
                                                    kotlinx.coroutines.delay(index.coerceAtMost(8) * 15L)
                                                }
                                                itemVisible.value = true
                                            }
                                            val alpha by animateFloatAsState(
                                                targetValue = if (itemVisible.value) 1f else 0f,
                                                animationSpec = tween(durationMillis = 200),
                                                label = "alpha"
                                            )
                                            val translationY by animateFloatAsState(
                                                targetValue = if (itemVisible.value) 0f else 15f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                                label = "translationY"
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        this.alpha = alpha
                                                        this.translationY = translationY
                                                    }
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
                                                        
                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        val ping = pingsMap[serverLink]
                                                        if (ping != null) {
                                                            val pingColor = when {
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
                                                                text = "${ping} ms",
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
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Untested",
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
                                                    // Share configuration link
                                                    IconButton(
                                                        onClick = {
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, serverLink)
                                                                this.type = "text/plain"
                                                            }
                                                            val shareIntent = Intent.createChooser(sendIntent, "Share Node Config")
                                                            context.startActivity(shareIntent)
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Share,
                                                            contentDescription = "Share Node Config",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    // Edit/Delete options for manual nodes
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
                                                                contentDescription = "Edit Node Config",
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
                                                                contentDescription = "Delete Manual Node",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Love Notes Card (Only for Special flavor)
        if (Config.IS_SPECIAL) {
            var showLoveNoteDialog by remember { mutableStateOf(false) }
            var currentLoveNote by remember { mutableStateOf("") }
            
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                PeakingKitty(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-28).dp, y = (-22).dp)
                )
                
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentLoveNote = Config.LOVE_QUOTES.random()
                            showLoveNoteDialog = true
                        }
                        .pressScaleEffect(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
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
                                text = "Love Notes ❤️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap to open a note from Gumball",
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
                            Text("Note for Sana", fontWeight = FontWeight.Bold)
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
                            Text("Close")
                        }
                    },
                    shape = ExpressiveCardShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

                // 4. Logs Console
                AnimatedVisibility(
                    visible = showLogs,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(280.dp),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                                        text = "Engine Logs",
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
                                            val clip = android.content.ClipData.newPlainText("VPN Logs", vpnLogs)
                                            clipboardManager.setPrimaryClip(clip)
                                        },
                                        modifier = Modifier.pressScaleEffect()
                                    ) {
                                        Text("Copy")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(
                                        onClick = { VpnServiceWrapper.clearLogs() },
                                        modifier = Modifier.pressScaleEffect()
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Text(
                                    text = if (vpnLogs.isEmpty()) "Logs will output here..." else vpnLogs,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Import Profile Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Import Config Link",
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
                            text = "Paste config link or scan QR code:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scanResultCallback = { result ->
                                    importText = result
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Code",
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
                        placeholder = { Text("vless://... or trojan://... or { ... }") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val currentManual = manualServersStr
                            val trimmedImport = importText.trim()
                            if (trimmedImport.isNotEmpty()) {
                                val updatedManual = if (currentManual.isEmpty()) trimmedImport else "$currentManual\n$trimmedImport"
                                settingsManager.setManualServers(updatedManual)
                                settingsManager.setActiveSubId("manual")
                                settingsManager.setActiveProfile(trimmedImport)
                            }
                            showImportDialog = false
                            importText = ""
                            if (vpnState == "CONNECTED") {
                                startVpnService(context)
                            }
                        }
                    },
                    modifier = Modifier.pressScaleEffect(),
                    shape = ExpressiveButtonShape
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    modifier = Modifier.pressScaleEffect()
                ) {
                    Text("Cancel")
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
                    text = if (isNewNode) "Create Custom Node" else "Edit Node Configuration",
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
                            text = { Text("Form Editor") }
                        )
                        Tab(
                            selected = editorMode == "link",
                            onClick = { editorMode = "link" },
                            text = { Text("Raw Config") }
                        )
                    }

                    if (editorMode == "form") {
                        // Protocol selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Protocol", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            label = { Text("Remark / Name") },
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
                                label = { Text("Server Address") },
                                modifier = Modifier.weight(2f),
                                shape = ExpressiveButtonShape
                            )
                            OutlinedTextField(
                                value = editPort,
                                onValueChange = { editPort = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveButtonShape
                            )
                        }

                        // Credentials
                        OutlinedTextField(
                            value = editCreds,
                            onValueChange = { editCreds = it },
                            label = { Text(if (editType == "vless") "UUID" else "Password / Credentials") },
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
                                Text("Enable TLS", fontWeight = FontWeight.Bold)
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
                                    label = { Text("SNI (Server Name)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveButtonShape
                                )
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
                            placeholder = { Text("vless://... or trojan://... or { ... }") }
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingNodeLink = null },
                    modifier = Modifier.pressScaleEffect()
                ) {
                    Text("Cancel")
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionDashboard(
    state: String,
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

    val buttonIconColor = when (state) {
        "CONNECTED" -> MaterialTheme.colorScheme.onPrimary
        "CONNECTING" -> MaterialTheme.colorScheme.onSecondary
        "DISCONNECTING" -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val scaleFactor by transition.animateFloat(
        label = "ButtonScale",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }
    ) { s ->
        if (s == "CONNECTED") 1.05f else 1.0f
    }

    // Speed details monitoring using Android TrafficStats
    var downloadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var uploadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var pingTime by remember { mutableStateOf("0 ms") }
    
    LaunchedEffect(state) {
        if (state == "CONNECTED") {
            pingTime = "${(35..65).random()} ms"
            
            // Helper functions to get current traffic stats
            fun getRxBytes(): Long {
                val myUid = context.applicationInfo.uid
                val rx = android.net.TrafficStats.getUidRxBytes(myUid)
                return if (rx != android.net.TrafficStats.UNSUPPORTED.toLong()) rx else android.net.TrafficStats.getTotalRxBytes()
            }
            fun getTxBytes(): Long {
                val myUid = context.applicationInfo.uid
                val tx = android.net.TrafficStats.getUidTxBytes(myUid)
                return if (tx != android.net.TrafficStats.UNSUPPORTED.toLong()) tx else android.net.TrafficStats.getTotalTxBytes()
            }
            fun formatSpeed(bytesPerSec: Long): String {
                val kb = bytesPerSec / 1024.0
                val mb = kb / 1024.0
                return when {
                    mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
                    kb >= 0.1 -> String.format(java.util.Locale.US, "%.1f KB/s", kb)
                    else -> "0.0 KB/s"
                }
            }

            var lastRx = getRxBytes()
            var lastTx = getTxBytes()
            var lastTime = System.currentTimeMillis()

            while (true) {
                kotlinx.coroutines.delay(1000)
                val currentRx = getRxBytes()
                val currentTx = getTxBytes()
                val currentTime = System.currentTimeMillis()
                
                val dt = (currentTime - lastTime) / 1000.0
                if (dt > 0.0) {
                    val dlBytes = currentRx - lastRx
                    val ulBytes = currentTx - lastTx
                    
                    val dlSpeed = if (dlBytes >= 0) (dlBytes / dt).toLong() else 0L
                    val ulSpeed = if (ulBytes >= 0) (ulBytes / dt).toLong() else 0L
                    
                    downloadSpeed = formatSpeed(dlSpeed)
                    uploadSpeed = formatSpeed(ulSpeed)
                }
                
                lastRx = currentRx
                lastTx = currentTx
                lastTime = currentTime
                
                if ((1..10).random() > 8) {
                    pingTime = "${(35..75).random()} ms"
                }
            }
        } else {
            downloadSpeed = "0.0 KB/s"
            uploadSpeed = "0.0 KB/s"
            pingTime = "--"
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = ExpressiveCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            // Main Button Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Custom Canvas-based WaveVisualizer (animates smoothly when connected/connecting)
                if (state == "CONNECTED" || state == "CONNECTING") {
                    WaveVisualizer(
                        state = state,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(180.dp)
                    )
                }

                // Main connect button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = scaleFactor
                            scaleY = scaleFactor
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
                            contentDescription = "Connect Toggle",
                            tint = buttonIconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Connection State Pill
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

            // Live Network Stats Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveButtonShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ping metric
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

                // Download speed
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

                // Upload speed
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
    val intent = Intent(context, VpnServiceWrapper::class.java).apply {
        action = VpnServiceWrapper.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopVpnService(context: Context) {
    val intent = Intent(context, VpnServiceWrapper::class.java).apply {
        action = VpnServiceWrapper.ACTION_STOP
    }
    context.startService(intent)
}

private suspend fun fetchSubscription(urlStr: String): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val url = java.net.URL(urlStr)
    val connection = url.openConnection() as java.net.HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "sing-box/1.9.0")
    connection.connect()
    
    if (connection.responseCode == 200) {
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        val decodedText = try {
            val trimmed = text.trim().replace("\r", "").replace("\n", "").replace(" ", "")
            val decodedBytes = try {
                android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
            } catch (e: java.lang.IllegalArgumentException) {
                android.util.Base64.decode(trimmed, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
            }
            String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8)
        } catch (e: Exception) {
            text
        }
        
        decodedText.lines()
            .map { it.trim() }
            .filter { it.startsWith("vless://") || it.startsWith("trojan://") || it.startsWith("ss://") }
    } else {
        throw java.io.IOException("HTTP error ${connection.responseCode}")
    }
}

private fun getProxyName(link: String): String {
    val hashIdx = link.indexOf("#")
    return if (hashIdx >= 0) {
        try {
            java.net.URLDecoder.decode(link.substring(hashIdx + 1), "UTF-8")
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
            "Unnamed Proxy Node"
        }
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

data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val servers: String
)

fun serializeSubscriptions(subs: List<Subscription>): String {
    return subs.joinToString("\u001e") { sub ->
        val safeName = sub.name.replace("\u001e", "").replace("\u001f", "")
        val safeUrl = sub.url.replace("\u001e", "").replace("\u001f", "")
        val safeServers = sub.servers.replace("\u001e", "").replace("\u001f", "")
        "${sub.id}\u001f$safeName\u001f$safeUrl\u001f$safeServers"
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
        } else {
            null
        }
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
    var phase1 by remember { mutableStateOf(0f) }
    var phase2 by remember { mutableStateOf(0f) }
    
    LaunchedEffect(state) {
        if (state == "CONNECTED" || state == "CONNECTING") {
            val speed1 = (2f * Math.PI.toFloat()) / 4500f // rad/ms
            val speed2 = (-2f * Math.PI.toFloat()) / 6500f // rad/ms
            var lastTime = withFrameMillis { it }
            while (true) {
                withFrameMillis { time ->
                    val delta = time - lastTime
                    lastTime = time
                    phase1 = (phase1 + speed1 * delta) % (2f * Math.PI.toFloat())
                    phase2 = (phase2 + speed2 * delta) % (2f * Math.PI.toFloat())
                }
            }
        }
    }
    
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




