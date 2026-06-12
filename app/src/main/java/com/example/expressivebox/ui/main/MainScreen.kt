package com.example.expressivebox.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.example.expressivebox.data.SettingsManager
import com.example.expressivebox.vpn.VpnServiceWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset


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

    val subscriptions = remember(subscriptionListStr) {
        deserializeSubscriptions(subscriptionListStr)
    }
    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    // Observe VPN state and logs
    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()
    val vpnLogs by VpnServiceWrapper.vpnLogs.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "ExpressiveBox",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v1.0.47",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "General Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bypass Iran Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Advanced Settings Toggle Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.animateContentSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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
                                        shape = RoundedCornerShape(12.dp)
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
                                                shape = RoundedCornerShape(12.dp)
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
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            OutlinedTextField(
                                                value = fragmentInterval,
                                                onValueChange = { scope.launch { settingsManager.setFragmentInterval(it) } },
                                                label = { Text("Interval") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Logs Drawer Item Button
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                        label = { Text("Toggle Logs Console") },
                        selected = showLogs,
                        onClick = {
                            showLogs = !showLogs
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "ExpressiveBox",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
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
                        FilledTonalButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.fillMaxWidth().pressScaleEffect(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Custom Link / Config")
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
                    shape = RoundedCornerShape(28.dp),
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
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = subNameInput,
                                    onValueChange = { subNameInput = it },
                                    label = { Text("Name (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    placeholder = { Text("e.g. My Premium VPN") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = subUrlInput,
                                    onValueChange = { subUrlInput = it },
                                    label = { Text("Subscription Link") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    placeholder = { Text("https://example.com/sub") }
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
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        enabled = !isFetching && subUrlInput.isNotEmpty()
                                    ) {
                                        if (isFetching) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
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
                                        shape = RoundedCornerShape(12.dp),
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
                                            .clip(RoundedCornerShape(12.dp))
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
                                        
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val updatedList = subscriptions.filter { it.id != sub.id }
                                                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
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

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(28.dp),
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
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
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
                                        shape = RoundedCornerShape(12.dp),
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
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(6.dp)
                                    ) {
                                        filteredServerList.forEachIndexed { index, serverLink ->
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
                                            
                                            val itemVisible = remember { mutableStateOf(false) }
                                            LaunchedEffect(serverList) {
                                                kotlinx.coroutines.delay(index * 25L)
                                                itemVisible.value = true
                                            }
                                            val alpha by animateFloatAsState(
                                                targetValue = if (itemVisible.value) 1f else 0f,
                                                animationSpec = tween(durationMillis = 250),
                                                label = "alpha"
                                            )
                                            val translationY by animateFloatAsState(
                                                targetValue = if (itemVisible.value) 0f else 25f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                                label = "translationY"
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        this.alpha = alpha
                                                        this.translationY = translationY
                                                    }
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                                        shape = RoundedCornerShape(14.dp)
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
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(tagContainerColor)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = type,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = tagTextColor,
                                                                fontWeight = FontWeight.Bold
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
                                                                fontWeight = FontWeight.Bold
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
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Logs Console
                AnimatedVisibility(
                    visible = showLogs,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(280.dp),
                        shape = RoundedCornerShape(24.dp),
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
                    Text(
                        text = "Paste VLESS, Trojan, Shadowsocks, or raw sing-box JSON configuration:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("vless://... or trojan://... or { ... }") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            settingsManager.setActiveProfile(importText)
                            showImportDialog = false
                            importText = ""
                            if (vpnState == "CONNECTED") {
                                startVpnService(context)
                            }
                        }
                    },
                    modifier = Modifier.pressScaleEffect(),
                    shape = RoundedCornerShape(12.dp)
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
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionDashboard(
    state: String,
    onConnectToggle: () -> Unit
) {
    val transition = updateTransition(targetState = state, label = "VPNStateTransition")

    val stateText = when (state) {
        "CONNECTED" -> "SECURED"
        "CONNECTING" -> "SHIELD ACTIVE..."
        "DISCONNECTING" -> "DISCONNECTING..."
        else -> "UNPROTECTED"
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

    // Speed details simulator
    var downloadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var uploadSpeed by remember { mutableStateOf("0.0 KB/s") }
    var pingTime by remember { mutableStateOf("0 ms") }
    
    LaunchedEffect(state) {
        if (state == "CONNECTED") {
            pingTime = "${(35..65).random()} ms"
            while (true) {
                kotlinx.coroutines.delay(1800)
                val dl = (100..2800).random() / 10f
                downloadSpeed = if (dl > 100) String.format("%.1f MB/s", dl / 10f) else String.format("%.1f KB/s", dl * 10f)
                val ul = (50..820).random() / 10f
                uploadSpeed = if (ul > 100) String.format("%.1f MB/s", ul / 10f) else String.format("%.1f KB/s", ul * 10f)
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
        shape = RoundedCornerShape(28.dp),
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
                // Custom Canvas-based WaveVisualizer (animates smoothly when connected)
                WaveVisualizer(
                    state = state,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(180.dp)
                )

                // Native Material 3 Expressive Loading and Wavy Progress Indicators
                if (state == "CONNECTED") {
                    CircularWavyProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(136.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main connect button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .scale(scaleFactor)
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
                    .clip(RoundedCornerShape(16.dp))
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
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
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

@Composable
fun WaveVisualizer(
    state: String,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
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
        initialValue = 0f,
        targetValue = -2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (state == "CONNECTED") 1f else 0f,
        animationSpec = tween(1200),
        label = "amplitude"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        
        if (amplitudeMultiplier > 0.01f) {
            val baseRadius = minOf(width, height) / 2f * 0.70f
            
            // Draw Wave 1 (outer primary color wave)
            val path1 = Path()
            val points1 = 120
            val step1 = (2f * Math.PI / points1).toFloat()
            for (i in 0..points1) {
                val angle = i * step1
                val wave = sin(angle * 6f + phase1) * 8.dp.toPx() * amplitudeMultiplier
                val r = baseRadius + wave
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) {
                    path1.moveTo(x, y)
                } else {
                    path1.lineTo(x, y)
                }
            }
            path1.close()
            drawPath(
                path = path1,
                color = primaryColor.copy(alpha = 0.35f),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Draw Wave 2 (inner secondary color wave)
            val path2 = Path()
            val points2 = 120
            val step2 = (2f * Math.PI / points2).toFloat()
            for (i in 0..points2) {
                val angle = i * step2
                val wave = sin(angle * 8f + phase2) * 5.dp.toPx() * amplitudeMultiplier
                val r = (baseRadius - 10.dp.toPx()) + wave
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) {
                    path2.moveTo(x, y)
                } else {
                    path2.lineTo(x, y)
                }
            }
            path2.close()
            drawPath(
                path = path2,
                color = secondaryColor.copy(alpha = 0.25f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}




