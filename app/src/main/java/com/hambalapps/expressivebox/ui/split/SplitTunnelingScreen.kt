package com.hambalapps.expressivebox.ui.split

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.hambalapps.expressivebox.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val name: String,
    val packageName: String,
    val appInfo: ApplicationInfo,
    val isSystem: Boolean,
    val icon: Drawable?
)

@Composable
fun DrawableImage(drawable: Drawable, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitTunnelingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val isEnabled by settingsManager.splitTunnelingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val mode by settingsManager.splitTunnelingMode.collectAsStateWithLifecycle(initialValue = "bypass")
    val selectedApps by settingsManager.splitTunnelingApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var appsList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load apps asynchronously
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val mapped = installed.filter { app ->
                app.packageName != context.packageName
            }.map { app ->
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || 
                               (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val icon = try {
                    app.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                AppItem(
                    name = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    appInfo = app,
                    isSystem = isSystem,
                    icon = icon
                )
            }.sortedBy { it.name.lowercase() }
            
            withContext(Dispatchers.Main) {
                appsList = mapped
                isLoading = false
            }
        }
    }

    val filteredApps = remember(appsList, searchQuery, showSystemApps) {
        appsList.filter { app ->
            (showSystemApps || !app.isSystem) &&
            (searchQuery.isEmpty() || 
             app.name.contains(searchQuery, ignoreCase = true) || 
             app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.split_tunneling), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Enable switch card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            text = stringResource(R.string.enable_split),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.split_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                settingsManager.setSplitTunnelingEnabled(checked)
                                if (vpnState == "CONNECTED") {
                                    // Restart service to apply new rules
                                    val serviceIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                        action = VpnServiceWrapper.ACTION_START
                                    }
                                    context.startService(serviceIntent)
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 2. Mode Selector Segmented Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bypassSelected = mode == "bypass"
                        FilterChip(
                            selected = bypassSelected,
                            onClick = {
                                scope.launch {
                                    settingsManager.setSplitTunnelingMode("bypass")
                                    if (vpnState == "CONNECTED") {
                                        val serviceIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                            action = VpnServiceWrapper.ACTION_START
                                        }
                                        context.startService(serviceIntent)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.bypass_apps), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = !bypassSelected,
                            onClick = {
                                scope.launch {
                                    settingsManager.setSplitTunnelingMode("only_route")
                                    if (vpnState == "CONNECTED") {
                                        val serviceIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                            action = VpnServiceWrapper.ACTION_START
                                        }
                                        context.startService(serviceIntent)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.route_apps), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 3. Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_apps)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // 3.5 Show system apps toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.show_system),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = showSystemApps,
                            onCheckedChange = { showSystemApps = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Apps List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isChecked = selectedApps.contains(app.packageName)
                        val pm = context.packageManager

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    if (!isEnabled) return@clickable
                                    val newSet = selectedApps.toMutableSet()
                                    if (isChecked) newSet.remove(app.packageName) else newSet.add(app.packageName)
                                    scope.launch {
                                        settingsManager.setSplitTunnelingApps(newSet)
                                        if (vpnState == "CONNECTED") {
                                            val serviceIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                action = VpnServiceWrapper.ACTION_START
                                            }
                                            context.startService(serviceIntent)
                                        }
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                DrawableImage(
                                    drawable = app.icon,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = isChecked,
                                enabled = isEnabled,
                                onCheckedChange = { checked ->
                                    val newSet = selectedApps.toMutableSet()
                                    if (checked) newSet.add(app.packageName) else newSet.remove(app.packageName)
                                    scope.launch {
                                        settingsManager.setSplitTunnelingApps(newSet)
                                        if (vpnState == "CONNECTED") {
                                            val serviceIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                action = VpnServiceWrapper.ACTION_START
                                            }
                                            context.startService(serviceIntent)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
