package com.xrotune.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xrotune.app.R
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.player.RepeatMode
import com.xrotune.app.ui.components.AdaptiveFullArtworkFrame
import com.xrotune.app.ui.components.AddToPlaylistDialog
import com.xrotune.app.ui.components.AnimatedPlaybackAura
import com.xrotune.app.ui.components.IosIconButton
import com.xrotune.app.ui.components.IosPillButton
import com.xrotune.app.ui.components.IosSurfaceChip
import com.xrotune.app.ui.components.MiniEqualizerBars
import com.xrotune.app.ui.components.ObsidianGlassCard
import com.xrotune.app.ui.components.ProgressiveArtworkImage
import com.xrotune.app.ui.components.SoftPulseLoadingIndicator
import com.xrotune.app.ui.components.SwipeToMinimizeContainer
import com.xrotune.app.ui.components.iosClickable
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.ObsidianBg
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.formatBitrate
import com.xrotune.app.ui.util.formatCompactCount
import com.xrotune.app.ui.util.formatDurationMs
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler { viewModel.showNowPlayingSheet = false }

    val currentSong = viewModel.playerManager.currentSong ?: return
    val isPlaying = viewModel.playerManager.isPlaying
    val isBuffering = viewModel.playerManager.isBuffering
    val currentPosition = viewModel.playerManager.currentPosition
    val duration = viewModel.playerManager.duration
    val queue = viewModel.playerManager.queue
    val currentIndex = viewModel.playerManager.currentIndex
    val isShuffle = viewModel.playerManager.isShuffleEnabled
    val repeatMode = viewModel.playerManager.repeatMode
    val playbackError = viewModel.playerManager.playbackError

    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlistSummaries.collectAsStateWithLifecycle()

    val videoId = currentSong.videoId ?: ""
    val isFavorite = remember(favoriteSongs, videoId) { favoriteSongs.any { it.videoId == videoId } }
    val isDownloaded = remember(downloadedSongs, videoId) { downloadedSongs.any { it.videoId == videoId } }
    val downloadProgress = viewModel.downloadProgress[videoId]

    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    var activeTab by remember { mutableStateOf("player") } // player, lyrics, queue
    var showAddToPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        sliderPosition = null
        if (videoId.isNotBlank()) {
            viewModel.loadLyricsAndDetailsForCurrentSong(videoId)
        }
    }

    SwipeToMinimizeContainer(
        onMinimize = { viewModel.showNowPlayingSheet = false },
        baseCornerRadius = 28.dp,
        modifier = modifier
    ) { handleDragModifier ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = ObsidianBg,
            shadowElevation = 24.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedPlaybackAura(isPlaying = isPlaying)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NowPlayingPopupTopBar(
                        isPlaying = isPlaying,
                        isOffline = viewModel.playerManager.isOfflinePlayback,
                        streamFormat = viewModel.playerManager.currentStreamFormat,
                        streamBitrate = viewModel.playerManager.currentStreamBitrate,
                        onMinimize = { viewModel.showNowPlayingSheet = false },
                        onInspectFormats = {
                            if (videoId.isNotBlank()) viewModel.openFormatInspector(videoId)
                        },
                        modifier = handleDragModifier
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Popup Mode Bar (Player / Lyrics / Queue & Radio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        IosSurfaceChip(
                            selected = activeTab == "player",
                            onClick = { activeTab = "player" },
                            label = "Player"
                        )
                        IosSurfaceChip(
                            selected = activeTab == "lyrics",
                            onClick = {
                                activeTab = "lyrics"
                                if (videoId.isNotBlank()) {
                                    viewModel.loadLyricsAndDetailsForCurrentSong(videoId)
                                }
                            },
                            label = "Lyrics"
                        )
                        IosSurfaceChip(
                            selected = activeTab == "queue",
                            onClick = { activeTab = "queue" },
                            label = "Queue (${queue.size})"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeTab) {
                            "lyrics" -> {
                                LyricsPopupTab(
                                    viewModel = viewModel,
                                    videoId = videoId,
                                    currentSong = currentSong
                                )
                            }
                            "queue" -> {
                                QueueAndRadioPopupTab(
                                    viewModel = viewModel,
                                    videoId = videoId,
                                    queue = queue,
                                    currentIndex = currentIndex,
                                    isPlaying = isPlaying
                                )
                            }
                            else -> {
                                val isWideLandscape = maxWidth >= 560.dp && maxWidth > maxHeight
                                val landscapeArtMaxHeight = (maxHeight * 0.85f).coerceAtLeast(180.dp)

                                if (isWideLandscape) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(22.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.46f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AdaptiveFullArtworkFrame(
                                                videoId = videoId,
                                                rawThumbnail = currentSong.thumbnail,
                                                isPlaying = isPlaying,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 160.dp, max = landscapeArtMaxHeight)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(0.54f)
                                                .fillMaxHeight()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            NowPlayingControlsPanel(
                                                currentSong = currentSong,
                                                queue = queue,
                                                playbackError = playbackError,
                                                isFavorite = isFavorite,
                                                isDownloaded = isDownloaded,
                                                downloadProgress = downloadProgress,
                                                currentPosition = currentPosition,
                                                duration = duration,
                                                sliderPosition = sliderPosition,
                                                onSliderChange = { sliderPosition = it },
                                                onSliderFinish = {
                                                    sliderPosition?.let {
                                                        viewModel.playerManager.seekTo(it.toLong())
                                                        sliderPosition = null
                                                    }
                                                },
                                                isPlaying = isPlaying,
                                                isBuffering = isBuffering,
                                                isShuffle = isShuffle,
                                                repeatMode = repeatMode,
                                                onRetry = { viewModel.playTrack(currentSong, queue) },
                                                onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                                                onToggleShuffle = { viewModel.playerManager.toggleShuffle() },
                                                onPrevious = { viewModel.playerManager.playPrevious() },
                                                onPlayPause = { viewModel.playerManager.togglePlayPause() },
                                                onNext = { viewModel.playerManager.playNext() },
                                                onCycleRepeat = { viewModel.playerManager.cycleRepeatMode() },
                                                onAddToPlaylist = { showAddToPlaylist = true },
                                                onDownloadToggle = {
                                                    if (isDownloaded) viewModel.removeOfflineDownload(videoId)
                                                    else viewModel.requestDownloadTrack(currentSong)
                                                },
                                                onOpenLyrics = { activeTab = "lyrics" },
                                                onOpenQueue = { activeTab = "queue" }
                                            )
                                        }
                                    }
                                } else {
                                    val maxArtHeight = (maxHeight * 0.38f).coerceIn(155.dp, 280.dp)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AdaptiveFullArtworkFrame(
                                                videoId = videoId,
                                                rawThumbnail = currentSong.thumbnail,
                                                isPlaying = isPlaying,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 150.dp, max = maxArtHeight)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        NowPlayingControlsPanel(
                                            currentSong = currentSong,
                                            queue = queue,
                                            playbackError = playbackError,
                                            isFavorite = isFavorite,
                                            isDownloaded = isDownloaded,
                                            downloadProgress = downloadProgress,
                                            currentPosition = currentPosition,
                                            duration = duration,
                                            sliderPosition = sliderPosition,
                                            onSliderChange = { sliderPosition = it },
                                            onSliderFinish = {
                                                sliderPosition?.let {
                                                    viewModel.playerManager.seekTo(it.toLong())
                                                    sliderPosition = null
                                                }
                                            },
                                            isPlaying = isPlaying,
                                            isBuffering = isBuffering,
                                            isShuffle = isShuffle,
                                            repeatMode = repeatMode,
                                            onRetry = { viewModel.playTrack(currentSong, queue) },
                                            onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                                            onToggleShuffle = { viewModel.playerManager.toggleShuffle() },
                                            onPrevious = { viewModel.playerManager.playPrevious() },
                                            onPlayPause = { viewModel.playerManager.togglePlayPause() },
                                            onNext = { viewModel.playerManager.playNext() },
                                            onCycleRepeat = { viewModel.playerManager.cycleRepeatMode() },
                                            onAddToPlaylist = { showAddToPlaylist = true },
                                            onDownloadToggle = {
                                                if (isDownloaded) viewModel.removeOfflineDownload(videoId)
                                                else viewModel.requestDownloadTrack(currentSong)
                                            },
                                            onOpenLyrics = { activeTab = "lyrics" },
                                            onOpenQueue = { activeTab = "queue" }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylist = false },
            onSelectPlaylist = { plId ->
                viewModel.addSongToPlaylist(plId, currentSong)
                showAddToPlaylist = false
            },
            onCreateAndAdd = { newName ->
                viewModel.createPlaylistAndAddSong(newName, currentSong)
                showAddToPlaylist = false
            }
        )
    }
}

@Composable
private fun NowPlayingPopupTopBar(
    isPlaying: Boolean,
    isOffline: Boolean,
    streamFormat: String?,
    streamBitrate: Long?,
    onMinimize: () -> Unit,
    onInspectFormats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(18.dp)
                .iosClickable(scaleDown = 0.92f, role = Role.Button, onClick = onMinimize)
                .semantics { contentDescription = "Swipe or tap to minimize player" }
                .testTag("now_playing_drag_handle"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IosIconButton(
                onClick = onInspectFormats,
                containerColor = TintedSurface,
                size = 42.dp,
                modifier = Modifier.testTag("now_playing_inspect_formats")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Audio Formats",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = TintedSurface,
                shadowElevation = 6.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniEqualizerBars(
                        isPlaying = isPlaying,
                        color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.now_playing).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        val streamModeLabel = buildString {
                            append(if (isOffline) "OFFLINE" else "STREAM")
                            streamFormat?.let { append(" • $it") }
                            val br = formatBitrate(streamBitrate)
                            if (br.isNotBlank()) append(" • $br")
                        }
                        Text(
                            text = streamModeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOffline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Visible Cross ('X') button to minimize popup back to Mini Player
            IosIconButton(
                onClick = onMinimize,
                containerColor = TintedSurfaceHigh,
                size = 42.dp,
                modifier = Modifier.testTag("now_playing_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Minimize Player",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsPopupTab(
    viewModel: MusicViewModel,
    videoId: String,
    currentSong: SearchResultItem
) {
    val songDetail = viewModel.currentSongDetail

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ObsidianGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title ?: "Unknown Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val metaLine = buildString {
                        append(songDetail?.song?.artist ?: currentSong.channel ?: "Unknown Artist")
                        val views = formatCompactCount(songDetail?.song?.viewCount)
                        if (views.isNotBlank()) append(" • $views plays")
                        songDetail?.formatCount?.let { append(" • $it formats") }
                    }
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IosIconButton(
                    onClick = { viewModel.loadLyricsAndDetailsForCurrentSong(videoId, forceRefresh = true) },
                    containerColor = TintedSurfaceHigh,
                    size = 38.dp
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Lyrics", modifier = Modifier.size(18.dp))
                }
            }
        }

        ObsidianGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            when {
                viewModel.isLoadingLyrics -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SoftPulseLoadingIndicator(size = 32.dp)
                            Text(
                                text = "Fetching lyrics…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                !viewModel.lyricsText.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = viewModel.lyricsText!!,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = viewModel.lyricsError ?: "Lyrics unavailable for this track",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            IosPillButton(
                                onClick = {
                                    viewModel.loadLyricsAndDetailsForCurrentSong(videoId, forceRefresh = true)
                                },
                                containerColor = TintedSurfaceHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueAndRadioPopupTab(
    viewModel: MusicViewModel,
    videoId: String,
    queue: List<SearchResultItem>,
    currentIndex: Int,
    isPlaying: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IosPillButton(
            onClick = { viewModel.loadRadioForTrack(videoId) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isLoadingRadio) {
                SoftPulseLoadingIndicator(size = 18.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading Track Radio…")
            } else {
                Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load YouTube Music Radio Mix (/api/music/radio)", fontWeight = FontWeight.SemiBold)
            }
        }

        if (viewModel.radioError != null) {
            Text(
                text = viewModel.radioError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        val radioQueueItems = remember(viewModel.radioTracks) {
            viewModel.radioTracks
                .filter { !it.videoId.isNullOrBlank() }
                .map { it.toSearchResultItem() }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (radioQueueItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Radio Mix (${radioQueueItems.size} tracks)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                itemsIndexed(
                    items = radioQueueItems,
                    key = { idx, item -> "radio_${item.videoId}_$idx" }
                ) { _, song ->
                    QueueTrackCard(
                        song = song,
                        isSelected = song.videoId == viewModel.playerManager.currentSong?.videoId,
                        isPlaying = isPlaying,
                        onClick = { viewModel.playTrack(song, radioQueueItems) }
                    )
                }
            }

            item {
                Text(
                    text = "Up Next Queue (${queue.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (queue.isEmpty()) {
                item {
                    Text(
                        text = "Queue is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = queue,
                    key = { idx, item -> "${item.videoId ?: "q"}_$idx" }
                ) { index, song ->
                    QueueTrackCard(
                        song = song,
                        isSelected = index == currentIndex,
                        isPlaying = isPlaying,
                        onClick = { viewModel.playerManager.playQueueItem(index, song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTrackCard(
    song: SearchResultItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onClick),
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                ProgressiveArtworkImage(
                    videoId = song.videoId,
                    rawThumbnail = song.thumbnail,
                    contentDescription = song.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniEqualizerBars(isPlaying = isPlaying)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "Unknown Track",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    }
}

@Composable
private fun NowPlayingControlsPanel(
    currentSong: SearchResultItem,
    queue: List<SearchResultItem>,
    playbackError: String?,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    currentPosition: Long,
    duration: Long,
    sliderPosition: Float?,
    onSliderChange: (Float) -> Unit,
    onSliderFinish: () -> Unit,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownloadToggle: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (playbackError != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playbackError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IosPillButton(
                        onClick = onRetry,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.error,
                        shadowElevation = 0.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        ObsidianGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title ?: "Unknown Track",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSong.channel ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IosIconButton(
                        onClick = onToggleFavorite,
                        containerColor = if (isFavorite) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                        } else {
                            TintedSurfaceHigh
                        },
                        size = 42.dp,
                        modifier = Modifier.testTag("now_playing_favorite")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unlike" else "Like",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    val safeMax = maxOf(1f, duration.toFloat())
                    Slider(
                        value = (sliderPosition ?: currentPosition.toFloat()).coerceIn(0f, safeMax),
                        onValueChange = { if (duration > 0L) onSliderChange(it) },
                        onValueChangeFinished = { if (duration > 0L) onSliderFinish() },
                        enabled = duration > 0L,
                        valueRange = 0f..safeMax,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onPrimary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(sliderPosition?.toLong() ?: currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (duration > 0L) formatDurationMs(duration) else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconButton(onClick = onToggleShuffle, size = 46.dp) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IosIconButton(onClick = onPrevious, size = 48.dp) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .size(68.dp)
                    .iosClickable(scaleDown = 0.92f, onClick = onPlayPause)
                    .testTag("now_playing_play_pause")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        SoftPulseLoadingIndicator(
                            size = 26.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            IosIconButton(onClick = onNext, size = 48.dp) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }

            IosIconButton(onClick = onCycleRepeat, size = 46.dp) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat Mode",
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ObsidianGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosPillButton(
                    onClick = onAddToPlaylist,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }

                IosPillButton(
                    onClick = onDownloadToggle,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (isDownloaded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDownloaded) "Offline" else "Get", style = MaterialTheme.typography.labelMedium)
                    }
                }

                IosPillButton(
                    onClick = onOpenLyrics,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lyrics", style = MaterialTheme.typography.labelMedium)
                }

                IosPillButton(
                    onClick = onOpenQueue,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("now_playing_queue_button")
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${queue.size}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
