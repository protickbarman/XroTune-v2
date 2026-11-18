package com.xrotune.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xrotune.app.data.local.PlaylistSummary
import com.xrotune.app.data.model.FormatItem
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.formatBitrate
import com.xrotune.app.ui.util.formatByteSize
import com.xrotune.app.ui.util.formatDurationSeconds
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun SongRowCard(
    song: SearchResultItem,
    isPlayingNow: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onPlayClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPlayingNow) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
    } else {
        TintedSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onPlayClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = if (isPlayingNow) 10.dp else 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                ProgressiveArtworkImage(
                    videoId = song.videoId,
                    rawThumbnail = song.thumbnail,
                    contentDescription = "Track Cover",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlayingNow) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.58f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniEqualizerBars(isPlaying = true)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "Unknown Track",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isPlayingNow) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.channel ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val durationText = song.lengthText
                        ?: song.lengthSeconds?.takeIf { it > 0 }?.let { formatDurationSeconds(it) }
                    if (!durationText.isNullOrBlank()) {
                        Text(
                            text = " • $durationText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isDownloaded || isFavorite || downloadProgress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isDownloaded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Offline Available",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Liked",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (downloadProgress != null) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            IosIconButton(
                onClick = onMoreClick,
                size = 42.dp
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Track Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    var showInlineCreate by remember { mutableStateOf(playlists.isEmpty()) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TintedSurface,
        title = {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (playlists.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .iosClickable(scaleDown = 0.97f) { onSelectPlaylist(pl.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = TintedSurfaceHigh,
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.songCount} tracks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                if (showInlineCreate) {
                    IosTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "New playlist name",
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_playlist_name_input")
                    )
                } else {
                    IosPillButton(
                        onClick = { showInlineCreate = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = TintedSurfaceHigh,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Playlist")
                    }
                }
            }
        },
        confirmButton = {
            if (showInlineCreate) {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateAndAdd(newName.trim())
                        }
                    },
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Create & Add")
                }
            }
        },
        dismissButton = {
            IosPillButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 0.dp
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    song: SearchResultItem,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInspectFormats: () -> Unit,
    onDownloadToggle: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TintedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!song.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title ?: "Unknown Track",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.channel ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            SheetActionItem(
                icon = Icons.Default.PlayArrow,
                label = "Play Now",
                onClick = { onDismiss(); onPlayNow() }
            )
            SheetActionItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (isFavorite) "Remove from Liked Songs" else "Add to Liked Songs",
                iconTint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                onClick = { onDismiss(); onToggleFavorite() }
            )
            SheetActionItem(
                icon = Icons.Default.PlaylistAdd,
                label = "Add to Playlist",
                onClick = { onDismiss(); onAddToPlaylist() }
            )
            SheetActionItem(
                icon = Icons.Default.Tune,
                label = "Audio & Quality Options",
                onClick = { onDismiss(); onInspectFormats() }
            )
            SheetActionItem(
                icon = if (isDownloaded) Icons.Default.DeleteOutline else Icons.Default.Download,
                label = if (isDownloaded) "Remove Offline Download" else "Download Audio for Offline Playback",
                iconTint = if (isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                onClick = { onDismiss(); onDownloadToggle() }
            )
            if (onRemoveFromPlaylist != null) {
                SheetActionItem(
                    icon = Icons.Default.DeleteOutline,
                    label = "Remove from This Playlist",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { onDismiss(); onRemoveFromPlaylist() }
                )
            }
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = TintedSurfaceHigh.copy(alpha = 0.72f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatInspectorSheet(
    viewModel: MusicViewModel,
    onAddToPlaylist: (SearchResultItem) -> Unit
) {
    if (!viewModel.showInfoSheet) return
    var selectedKind by remember { mutableStateOf("audio") }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeFormatInspector() },
        containerColor = TintedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Available Audio & Formats",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            when {
                viewModel.isLoadingInfo -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftPulseLoadingIndicator(size = 32.dp)
                    }
                }
                viewModel.infoError != null -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = viewModel.infoError ?: "Failed to load track info",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                viewModel.inspectedInfo != null -> {
                    val info = viewModel.inspectedInfo!!
                    val meta = info.video
                    val trackItem = SearchResultItem(
                        kind = "video",
                        videoId = meta?.videoId,
                        title = meta?.title ?: "Untitled Track",
                        channel = meta?.author ?: "Unknown Artist",
                        lengthSeconds = meta?.lengthSeconds,
                        thumbnail = meta?.videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
                    )

                    Column {
                        Text(
                            text = meta?.title ?: "Unknown Title",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(meta?.author ?: "Unknown")
                                meta?.lengthSeconds?.let { append(" • ${formatDurationSeconds(it)}") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosPillButton(
                            onClick = {
                                viewModel.closeFormatInspector()
                                viewModel.playTrack(trackItem, openPlayerSheet = true)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 8.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stream Best", fontWeight = FontWeight.SemiBold)
                        }
                        IosPillButton(
                            onClick = { onAddToPlaylist(trackItem) },
                            containerColor = TintedSurfaceHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("audio" to "Audio", "muxed" to "Muxed", "video" to "Video").forEach { (kind, label) ->
                            IosSurfaceChip(
                                selected = selectedKind == kind,
                                onClick = { selectedKind = kind },
                                label = label
                            )
                        }
                    }

                    val formats = info.formats.orEmpty().filter { it.kind == selectedKind }
                    if (formats.isEmpty()) {
                        Text(
                            text = "No $selectedKind tracks available for this item.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(formats) { fmt ->
                                FormatItemRow(
                                    format = fmt,
                                    onPlayItag = {
                                        viewModel.closeFormatInspector()
                                        viewModel.playTrack(trackItem, explicitItag = fmt.itag, openPlayerSheet = true)
                                    },
                                    onDownloadItag = {
                                        viewModel.downloadTrackOffline(trackItem, explicitItag = fmt.itag)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatItemRow(
    format: FormatItem,
    onPlayItag: () -> Unit,
    onDownloadItag: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TintedSurfaceHigh,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append((format.container ?: "Audio").uppercase())
                        format.codec?.let { append(" ($it)") }
                        format.quality?.let { append(" • $it") }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        formatBitrate(format.bitrate).takeIf { it.isNotBlank() },
                        formatByteSize(format.contentLength)
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (format.kind == "audio" || format.kind == "muxed") {
                IosIconButton(onClick = onPlayItag, size = 40.dp) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream Track",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IosIconButton(onClick = onDownloadItag, size = 40.dp) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Track",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

private data class QualityPresetOption(
    val tier: String,
    val title: String,
    val subtitle: String,
    val format: FormatItem?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualityPickerSheet(viewModel: MusicViewModel) {
    val song = viewModel.pendingDownloadSong ?: return
    val settings by viewModel.userSettings.collectAsState()
    val formats = viewModel.downloadQualityFormats
    val isLoading = viewModel.isLoadingDownloadQualities

    var selectedTier by remember(song.videoId, settings.preferredDownloadQuality) {
        mutableStateOf(settings.preferredDownloadQuality)
    }
    var selectedFormatItag by remember(song.videoId, formats) {
        mutableStateOf(
            viewModel.repository.selectAudioFormatForTier(
                formats = formats,
                qualityTier = settings.preferredDownloadQuality,
                preferredContainer = settings.preferredContainer
            )?.itag
        )
    }
    var askEveryTime by remember(song.videoId, settings.askDownloadQuality) {
        mutableStateOf(settings.askDownloadQuality)
    }

    LaunchedEffect(formats, settings.preferredDownloadQuality) {
        if (formats.isNotEmpty() && selectedFormatItag == null) {
            selectedFormatItag = viewModel.repository.selectAudioFormatForTier(
                formats = formats,
                qualityTier = selectedTier,
                preferredContainer = settings.preferredContainer
            )?.itag
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissDownloadQualityPicker() },
        containerColor = TintedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!song.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Download Quality",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = song.title ?: "Unknown Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Your chosen quality is saved in Settings and your local .xrotune profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SoftPulseLoadingIndicator(size = 28.dp)
                        Text(
                            text = "Checking available audio qualities…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val options: List<QualityPresetOption> = remember(formats, settings.preferredContainer) {
                    if (formats.isNotEmpty()) {
                        val highFmt = viewModel.repository.selectAudioFormatForTier(formats, "high", settings.preferredContainer)
                        val medFmt = viewModel.repository.selectAudioFormatForTier(formats, "medium", settings.preferredContainer)
                        val lowFmt = viewModel.repository.selectAudioFormatForTier(formats, "low", settings.preferredContainer)

                        val tierItems = mutableListOf<QualityPresetOption>()
                        highFmt?.let { fmt ->
                            val details = listOfNotNull(
                                (fmt.container ?: "audio").uppercase().let { if (it == "MP4") "M4A" else it },
                                fmt.codec,
                                formatBitrate(fmt.bitrate).takeIf { it.isNotBlank() },
                                formatByteSize(fmt.contentLength)
                            ).joinToString(" • ")
                            tierItems.add(QualityPresetOption("high", "High Quality (Best)", details, fmt))
                        }
                        if (medFmt != null && medFmt.itag != highFmt?.itag) {
                            val details = listOfNotNull(
                                (medFmt.container ?: "audio").uppercase().let { if (it == "MP4") "M4A" else it },
                                medFmt.codec,
                                formatBitrate(medFmt.bitrate).takeIf { it.isNotBlank() },
                                formatByteSize(medFmt.contentLength)
                            ).joinToString(" • ")
                            tierItems.add(QualityPresetOption("medium", "Medium Quality (Balanced)", details, medFmt))
                        }
                        if (lowFmt != null && lowFmt.itag != medFmt?.itag && lowFmt.itag != highFmt?.itag) {
                            val details = listOfNotNull(
                                (lowFmt.container ?: "audio").uppercase().let { if (it == "MP4") "M4A" else it },
                                lowFmt.codec,
                                formatBitrate(lowFmt.bitrate).takeIf { it.isNotBlank() },
                                formatByteSize(lowFmt.contentLength)
                            ).joinToString(" • ")
                            tierItems.add(QualityPresetOption("low", "Data Saver (Low)", details, lowFmt))
                        }
                        tierItems
                    } else {
                        listOf(
                            QualityPresetOption("high", "High Quality (Best)", "Highest available audio bitrate", null),
                            QualityPresetOption("medium", "Medium Quality (Balanced)", "Balanced size & audio clarity (~70 kbps)", null),
                            QualityPresetOption("low", "Data Saver (Low)", "Smallest offline storage footprint", null)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { opt ->
                        val isSelected = selectedTier == opt.tier &&
                            (opt.format == null || selectedFormatItag == null || selectedFormatItag == opt.format.itag)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosClickable(scaleDown = 0.975f) {
                                    selectedTier = opt.tier
                                    selectedFormatItag = opt.format?.itag
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                TintedSurfaceHigh
                            },
                            shadowElevation = if (isSelected) 8.dp else 2.dp,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTier = opt.tier
                                        selectedFormatItag = opt.format?.itag
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = opt.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ask quality on every download",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = askEveryTime,
                        onCheckedChange = { askEveryTime = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IosPillButton(
                        onClick = { viewModel.dismissDownloadQualityPicker() },
                        containerColor = TintedSurfaceHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    IosPillButton(
                        onClick = {
                            val chosenFormat = formats.firstOrNull { it.itag == selectedFormatItag }
                                ?: options.firstOrNull { it.tier == selectedTier }?.format
                            viewModel.confirmDownloadWithQuality(
                                song = song,
                                selectedFormat = chosenFormat,
                                qualityTier = selectedTier,
                                askEveryTime = askEveryTime
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_download_quality")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
