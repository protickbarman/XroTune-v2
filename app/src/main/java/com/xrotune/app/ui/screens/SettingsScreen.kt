package com.xrotune.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xrotune.app.data.local.XroTuneProfileData
import com.xrotune.app.data.model.ApiDocResponse
import com.xrotune.app.data.model.HealthResponse
import com.xrotune.app.data.preferences.UserSettings
import com.xrotune.app.data.repository.MusicRepository
import com.xrotune.app.ui.components.IosIconButton
import com.xrotune.app.ui.components.IosPillButton
import com.xrotune.app.ui.components.IosSurfaceChip
import com.xrotune.app.ui.components.IosTextField
import com.xrotune.app.ui.components.ObsidianGlassCard
import com.xrotune.app.ui.components.SoftPulseLoadingIndicator
import com.xrotune.app.ui.components.StaggeredEnterItem
import com.xrotune.app.ui.components.iosClickable
import com.xrotune.app.ui.theme.AmberGold
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.ErrorCrimson
import com.xrotune.app.ui.theme.MintSignal
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.formatByteSize
import com.xrotune.app.ui.viewmodel.MusicViewModel
import com.xrotune.app.util.AppLogEntry
import com.xrotune.app.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MusicViewModel) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val xroTuneDoc by viewModel.xroTuneDocument.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val appLogs by viewModel.appLogs.collectAsStateWithLifecycle()

    val profile = xroTuneDoc.profile
    val totalDownloadBytes = remember(downloadedSongs) {
        downloadedSongs.sumOf { it.contentLength ?: 0L }
    }
    val xroTuneSizeBytes = remember(xroTuneDoc.updatedAt) {
        viewModel.getXroTuneFileSizeBytes()
    }
    val updatedTimeText = remember(xroTuneDoc.updatedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(xroTuneDoc.updatedAt))
    }

    val avatarGradients = listOf(
        ElectricViolet to CoralPulse,
        CoralPulse to MintSignal,
        MintSignal to ElectricViolet,
        MaterialTheme.colorScheme.primaryContainer to ElectricViolet
    )
    val activeGradient = avatarGradients[profile.avatarColorIndex.coerceIn(0, avatarGradients.lastIndex)]

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var diagnosticsInitialTab by remember { mutableStateOf<String?>(null) } // null, "logs", "docs"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportXroTuneFile(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importXroTuneFile(it) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTwoColumn = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Profile & Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your profile, download quality, API docs telemetry, and preferences are saved locally in .xrotune.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isTwoColumn) {
                item {
                    StaggeredEnterItem(index = 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            UserProfileCard(
                                profile = profile,
                                gradient = activeGradient,
                                onEditClick = { showEditProfileDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            XroTuneDeviceFileCard(
                                xroTuneSizeBytes = xroTuneSizeBytes,
                                updatedTimeText = updatedTimeText,
                                onExport = { exportLauncher.launch(".xrotune") },
                                onImport = { importLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                item {
                    StaggeredEnterItem(index = 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DownloadQualityCard(
                                settings = settings,
                                onSelectQuality = { viewModel.setPreferredDownloadQuality(it) },
                                onToggleAskQuality = { viewModel.setAskDownloadQuality(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            StreamingFormatCard(
                                settings = settings,
                                onSelectContainer = { viewModel.setPreferredContainer(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                item {
                    StaggeredEnterItem(index = 2) {
                        ServerDocsAndLogsCard(
                            health = viewModel.healthInfo,
                            apiDoc = viewModel.apiDoc,
                            isLoadingHealth = viewModel.isLoadingHealth || viewModel.isLoadingApiDoc,
                            isServiceReachable = viewModel.isServiceReachable,
                            logCount = appLogs.size,
                            errorCount = appLogs.count { it.level == LogLevel.ERROR },
                            onRefresh = { viewModel.refreshHealthAndDocs() },
                            onOpenLogs = { diagnosticsInitialTab = "logs" },
                            onOpenDocs = { diagnosticsInitialTab = "docs" },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    StaggeredEnterItem(index = 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OfflineStorageCard(
                                trackCount = downloadedSongs.size,
                                totalBytes = totalDownloadBytes,
                                onClearAll = { viewModel.clearAllDownloads() },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            AboutXroTuneCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            } else {
                item {
                    StaggeredEnterItem(index = 0) {
                        UserProfileCard(
                            profile = profile,
                            gradient = activeGradient,
                            onEditClick = { showEditProfileDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 1) {
                        XroTuneDeviceFileCard(
                            xroTuneSizeBytes = xroTuneSizeBytes,
                            updatedTimeText = updatedTimeText,
                            onExport = { exportLauncher.launch(".xrotune") },
                            onImport = { importLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 2) {
                        DownloadQualityCard(
                            settings = settings,
                            onSelectQuality = { viewModel.setPreferredDownloadQuality(it) },
                            onToggleAskQuality = { viewModel.setAskDownloadQuality(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 3) {
                        StreamingFormatCard(
                            settings = settings,
                            onSelectContainer = { viewModel.setPreferredContainer(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 4) {
                        ServerDocsAndLogsCard(
                            health = viewModel.healthInfo,
                            apiDoc = viewModel.apiDoc,
                            isLoadingHealth = viewModel.isLoadingHealth || viewModel.isLoadingApiDoc,
                            isServiceReachable = viewModel.isServiceReachable,
                            logCount = appLogs.size,
                            errorCount = appLogs.count { it.level == LogLevel.ERROR },
                            onRefresh = { viewModel.refreshHealthAndDocs() },
                            onOpenLogs = { diagnosticsInitialTab = "logs" },
                            onOpenDocs = { diagnosticsInitialTab = "docs" },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 5) {
                        OfflineStorageCard(
                            trackCount = downloadedSongs.size,
                            totalBytes = totalDownloadBytes,
                            onClearAll = { viewModel.clearAllDownloads() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    StaggeredEnterItem(index = 6) {
                        AboutXroTuneCard(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    diagnosticsInitialTab?.let { initialTab ->
        DiagnosticsAndApiDocSheet(
            viewModel = viewModel,
            logs = appLogs,
            initialTab = initialTab,
            onDismiss = { diagnosticsInitialTab = null }
        )
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(profile.displayName) }
        var bioInput by remember { mutableStateOf(profile.bio) }
        var genreInput by remember { mutableStateOf(profile.favoriteGenre) }
        var colorIndex by remember { mutableIntStateOf(profile.avatarColorIndex) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = TintedSurface,
            title = { Text("Edit XroTune Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = "Display Name",
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                    IosTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = "Bio / Status",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    IosTextField(
                        value = genreInput,
                        onValueChange = { genreInput = it },
                        label = "Favorite Genre",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Avatar Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        avatarGradients.forEachIndexed { idx, pair ->
                            val selected = colorIndex == idx
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                shadowElevation = if (selected) 8.dp else 2.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .iosClickable(scaleDown = 0.9f) { colorIndex = idx }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(pair.first, pair.second))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            displayName = nameInput,
                            bio = bioInput,
                            favoriteGenre = genreInput,
                            avatarColorIndex = colorIndex
                        )
                        showEditProfileDialog = false
                    },
                    enabled = nameInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save to .xrotune")
                }
            },
            dismissButton = {
                IosPillButton(
                    onClick = { showEditProfileDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 0.dp
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServerDocsAndLogsCard(
    health: HealthResponse?,
    apiDoc: ApiDocResponse?,
    isLoadingHealth: Boolean,
    isServiceReachable: Boolean,
    logCount: Int,
    errorCount: Int,
    onRefresh: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenDocs: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isServiceReachable) MintSignal else CoralPulse)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Server Health, /api/doc & Live Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = MusicRepository.API_BASE_URL,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IosIconButton(onClick = onRefresh, size = 38.dp) {
                    if (isLoadingHealth) {
                        SoftPulseLoadingIndicator(size = 18.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Server Health & /api/doc"
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TintedSurfaceHigh.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Clients",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = listOfNotNull(health?.client, health?.musicClient)
                                .ifEmpty { listOf(if (isServiceReachable) "Online" else "Offline") }
                                .joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TintedSurfaceHigh.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Endpoints / Logs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${apiDoc?.count ?: apiDoc?.routes?.size ?: 0} routes • $logCount logs" +
                                if (errorCount > 0) " ($errorCount err)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (errorCount > 0) CoralPulse else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IosPillButton(
                    onClick = onOpenLogs,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_live_logs_button")
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Logs", fontWeight = FontWeight.SemiBold)
                }
                IosPillButton(
                    onClick = onOpenDocs,
                    containerColor = TintedSurfaceHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 3.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_api_doc_button")
                ) {
                    Icon(Icons.Default.Api, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View /api/doc", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticsAndApiDocSheet(
    viewModel: MusicViewModel,
    logs: List<AppLogEntry>,
    initialTab: String,
    onDismiss: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var logFilter by remember { mutableStateOf("ALL") } // ALL, API, PLAY, ERR
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val filteredLogs = remember(logs, logFilter) {
        when (logFilter) {
            "API" -> logs.filter { it.level == LogLevel.API }
            "PLAY" -> logs.filter { it.level == LogLevel.PLAYBACK }
            "ERR" -> logs.filter { it.level == LogLevel.ERROR || it.level == LogLevel.WARN }
            else -> logs
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TintedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeTab == "logs") "Live Telemetry & Logs" else "/api/doc Reference",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val h = viewModel.healthInfo
                    Text(
                        text = buildString {
                            append(if (viewModel.isServiceReachable) "Connected" else "Offline/Unreachable")
                            h?.uptimeSeconds?.let { append(" • uptime ${it}s") }
                            h?.host?.let { append(" • $it") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (viewModel.isServiceReachable) MintSignal else CoralPulse
                    )
                }
                IosIconButton(
                    onClick = { viewModel.refreshHealthAndDocs() },
                    size = 40.dp
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IosIconButton(
                    onClick = onDismiss,
                    size = 40.dp
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IosSurfaceChip(
                    selected = activeTab == "logs",
                    onClick = { activeTab = "logs" },
                    label = "Live Logs (${logs.size})"
                )
                IosSurfaceChip(
                    selected = activeTab == "docs",
                    onClick = { activeTab = "docs" },
                    label = "API Routes (${viewModel.apiDoc?.routes?.size ?: 0})"
                )
            }

            if (activeTab == "logs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "API" to "API",
                            "PLAY" to "Playback",
                            "ERR" to "Warnings/Err"
                        ).forEach { (key, title) ->
                            IosSurfaceChip(
                                selected = logFilter == key,
                                onClick = { logFilter = key },
                                label = title
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IosIconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(viewModel.getFormattedLogs()))
                                viewModel.showMessage("Copied ${logs.size} logs to clipboard")
                            },
                            containerColor = TintedSurfaceHigh,
                            size = 38.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Logs",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IosIconButton(
                            onClick = { viewModel.clearLogs() },
                            containerColor = TintedSurfaceHigh,
                            size = 38.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Logs",
                                tint = CoralPulse,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (filteredLogs.isEmpty()) {
                    ObsidianGlassCard(
                        containerColor = TintedSurfaceHigh.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No log entries in this filter yet. Play a track or search to capture API and playback telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { entry ->
                            val badgeColor = when (entry.level) {
                                LogLevel.ERROR -> ErrorCrimson
                                LogLevel.WARN -> AmberGold
                                LogLevel.PLAYBACK -> ElectricViolet
                                LogLevel.API -> MintSignal
                                LogLevel.INFO -> MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TintedSurfaceHigh.copy(alpha = 0.72f),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeColor.copy(alpha = 0.22f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = entry.level.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text(
                                                text = entry.tag,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = timeFormat.format(Date(entry.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = entry.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!entry.detail.isNullOrBlank()) {
                                        Text(
                                            text = entry.detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val doc = viewModel.apiDoc
                when {
                    viewModel.isLoadingApiDoc && doc == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftPulseLoadingIndicator(size = 32.dp)
                        }
                    }
                    viewModel.apiDocError != null && doc == null -> {
                        ObsidianGlassCard(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = viewModel.apiDocError ?: "Unable to fetch /api/doc",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    doc != null -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 440.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!doc.conventions.isNullOrEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = TintedSurfaceHigh.copy(alpha = 0.72f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = doc.name ?: "API Conventions",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            doc.conventions.forEach { conv ->
                                                Text(
                                                    text = "• $conv",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(doc.routes.orEmpty()) { route ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = TintedSurfaceHigh.copy(alpha = 0.8f),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MintSignal.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = route.method ?: "GET",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MintSignal
                                                )
                                            }
                                            Text(
                                                text = route.path ?: "",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            route.group?.let { grp ->
                                                Text(
                                                    text = grp,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (!route.summary.isNullOrBlank()) {
                                            Text(
                                                text = route.summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (!route.params.isNullOrEmpty()) {
                                            val paramSummary = route.params.joinToString(" • ") { p ->
                                                val req = if (p.required == true) "*" else ""
                                                "${p.name}$req"
                                            }
                                            Text(
                                                text = "Params: $paramSummary",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!route.example.isNullOrBlank()) {
                                            Text(
                                                text = route.example,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
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

@Composable
private fun UserProfileCard(
    profile: XroTuneProfileData,
    gradient: Pair<Color, Color>,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.displayName.firstOrNull()?.uppercase() ?: "X",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.favoriteGenre} • ${profile.totalTracksPlayed} plays",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IosIconButton(
                onClick = onEditClick,
                containerColor = TintedSurfaceHigh,
                size = 42.dp,
                modifier = Modifier.testTag("edit_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile"
                )
            }
        }
    }
}

@Composable
private fun XroTuneDeviceFileCard(
    xroTuneSizeBytes: Long,
    updatedTimeText: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ".xrotune Device File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Synced (${formatByteSize(xroTuneSizeBytes)}) • Updated $updatedTimeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintSignal
                    )
                }
            }
            Text(
                text = "Stores your profile data, audio preferences, and download quality settings directly on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IosPillButton(
                    onClick = onExport,
                    containerColor = TintedSurfaceHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 3.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_xrotune_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export")
                }
                IosPillButton(
                    onClick = onImport,
                    containerColor = TintedSurfaceHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 3.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_xrotune_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadQualityCard(
    settings: UserSettings,
    onSelectQuality: (String) -> Unit,
    onToggleAskQuality: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MintSignal)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Default audio quality used for offline downloads and pre-selected in the download prompt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "high" to "High (Best)",
                    "medium" to "Medium",
                    "low" to "Data Saver"
                ).forEach { (key, label) ->
                    IosSurfaceChip(
                        selected = settings.preferredDownloadQuality == key,
                        onClick = { onSelectQuality(key) },
                        label = label
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask quality when tapping Download",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Prompt for bitrate & size before saving offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = settings.askDownloadQuality,
                    onCheckedChange = onToggleAskQuality,
                    modifier = Modifier.testTag("ask_download_quality_switch")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreamingFormatCard(
    settings: UserSettings,
    onSelectContainer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preferred Streaming Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Choose which audio codec container XroTune selects by default when streaming.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("any" to "Auto Best", "mp4" to "M4A / AAC", "webm" to "WebM / Opus").forEach { (key, label) ->
                    IosSurfaceChip(
                        selected = settings.preferredContainer == key,
                        onClick = { onSelectContainer(key) },
                        label = label
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineStorageCard(
    trackCount: Int,
    totalBytes: Long,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = CoralPulse)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Offline Audio Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$trackCount tracks • ${formatByteSize(totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trackCount > 0) {
                IosPillButton(
                    onClick = onClearAll,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.error,
                    shadowElevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AboutXroTuneCard(
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "XroTune",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Version 1.0.0\nStream live music in the background, explore YouTube Music shelves, view lyrics & radio mixes, inspect /api/doc telemetry, and back up your settings in .xrotune.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
