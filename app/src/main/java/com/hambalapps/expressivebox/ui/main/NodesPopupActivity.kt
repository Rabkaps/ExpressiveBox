package com.hambalapps.expressivebox.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.expressivebox.R
import com.hambalapps.expressivebox.data.SettingsManager
import com.hambalapps.expressivebox.data.deserializeSubscriptions
import com.hambalapps.expressivebox.theme.ExpressiveBoxTheme
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import com.hambalapps.expressivebox.vpn.measurePingDelay
import com.hambalapps.expressivebox.vpn.getHostAndPortFromLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

private val ExpressiveCardShape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

class NodesPopupActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveBoxTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val settingsManager = remember { SettingsManager(context.applicationContext) }
                val settings by settingsManager.settings.collectAsStateWithLifecycle(initialValue = SettingsManager.defaultSettings)
                val activeProfile = settings.activeProfile
                val activeSubId = settings.activeSubId
                val subscriptions = settings.deserializedSubscriptions
                val activeSubscription = remember(subscriptions, activeSubId) {
                    subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
                }
                val serverList = remember(activeSubscription) {
                    activeSubscription?.servers?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
                }

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
                            else -> true
                        }
                        if (matchesTab) {
                            val name = getProxyName(serverLink, context)
                            if (name.contains(searchQuery, ignoreCase = true)) {
                                ServerItem(link = serverLink, name = name, type = type)
                            } else null
                        } else null
                    }
                }

                val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()

                // Translucent dim backdrop click closes popup
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(500.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = ExpressiveCardShape
                            )
                            .clickable(enabled = false) {}, // Prevent clicks inside card from closing
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
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
                                        style = MaterialTheme.typography.titleMedium,
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
                                                contentDescription = stringResource(R.string.test_pings),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                            if (subscriptions.size > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
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
                                Spacer(modifier = Modifier.height(4.dp))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

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
                                listOf(stringResource(R.string.tab_all), "VLESS", "Trojan", "Shadowsocks").forEachIndexed { index, title ->
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

                            Spacer(modifier = Modifier.height(8.dp))

                            if (filteredServerList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(filteredServerList, key = { _, item -> item.link }) { index, serverItem ->
                                        val serverLink = serverItem.link
                                        val isSelected = activeProfile == serverLink
                                        val name = serverItem.name
                                        val type = serverItem.type

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

                                        Row(
                                            modifier = Modifier
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
                                                            val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                                action = VpnServiceWrapper.ACTION_START
                                                            }
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                context.startForegroundService(startIntent)
                                                            } else {
                                                                context.startService(startIntent)
                                                            }
                                                        }
                                                        finish() // Close popup on selection
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
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                        )
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

private fun getProxyName(link: String, context: Context): String {
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
            context.getString(R.string.notif_unnamed)
        }
    }
}
