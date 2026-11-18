package com.xrotune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xrotune.app.R
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.ui.components.AddToPlaylistDialog
import com.xrotune.app.ui.components.IosIconButton
import com.xrotune.app.ui.components.IosPillButton
import com.xrotune.app.ui.components.IosSurfaceChip
import com.xrotune.app.ui.components.IosTextField
import com.xrotune.app.ui.components.MusicShelfItemCard
import com.xrotune.app.ui.components.ObsidianGlassCard
import com.xrotune.app.ui.components.ProgressiveArtworkImage
import com.xrotune.app.ui.components.SoftTrackLoadingSkeleton
import com.xrotune.app.ui.components.SongRowCard
import com.xrotune.app.ui.components.StaggeredEnterItem
import com.xrotune.app.ui.components.TrackOptionsSheet
import com.xrotune.app.ui.components.iosClickable
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    val homeResults = viewModel.homeResults
    val savedSongs by viewModel.savedSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlistSummaries.collectAsStateWithLifecycle()

    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.videoId }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.videoId }.toSet() }

    var optionsTrack by remember { mutableStateOf<SearchResultItem?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<SearchResultItem?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }

    val moodGenres = remember {
        listOf(
            "trending music" to "Trending",
            "bangla song" to "Bangla Hits",
            "lofi chill beats" to "Lofi Chill",
            "acoustic hits" to "Acoustic",
            "synthwave music" to "Synthwave"
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val gridColumns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }
        val videoItems = remember(homeResults) {
            homeResults
                .filter { (it.kind == null || it.kind == "video") && !it.videoId.isNullOrBlank() }
                .distinctBy { it.videoId!!.trim() }
                .take(12)
        }
        val chunkedVideos = remember(videoItems, gridColumns) { videoItems.chunked(gridColumns) }
        val activeShelves = remember(viewModel.homeFeedMode, viewModel.musicHomeShelves, viewModel.musicTrendingShelves) {
            if (viewModel.homeFeedMode == "trending") viewModel.musicTrendingShelves else viewModel.musicHomeShelves
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1160.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                            shadowElevation = 10.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Music Studio & Offline Player",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IosIconButton(
                        onClick = { viewModel.refreshHomeFeed() },
                        containerColor = TintedSurfaceHigh,
                        size = 44.dp,
                        modifier = Modifier.testTag("home_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Feed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                StaggeredEnterItem(index = 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = TintedSurface,
                        shadowElevation = 12.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
                                            TintedSurface
                                        )
                                    )
                                )
                                .padding(22.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Listen Live or Offline",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Browse Music Home, Trending charts, albums, artists, lyrics, and radio mixes with instant offline saving.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    IosPillButton(
                                        onClick = onNavigateToSearch,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.testTag("home_search_cta")
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Search Music", fontWeight = FontWeight.SemiBold)
                                    }
                                    IosPillButton(
                                        onClick = { showLinkDialog = true },
                                        containerColor = TintedSurfaceHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.testTag("home_paste_link_cta")
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Link / ID", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Music Home vs Trending Shelves (/api/music/home & /api/music/trending)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YouTube Music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IosSurfaceChip(
                                selected = viewModel.homeFeedMode == "home",
                                onClick = { viewModel.setHomeMode("home") },
                                label = "Home"
                            )
                            IosSurfaceChip(
                                selected = viewModel.homeFeedMode == "trending",
                                onClick = { viewModel.setHomeMode("trending") },
                                label = "Trending"
                            )
                        }
                    }

                    if (viewModel.isLoadingMusicShelves && activeShelves.isEmpty()) {
                        SoftTrackLoadingSkeleton(count = 2)
                    } else if (activeShelves.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            activeShelves.take(6).forEach { shelf ->
                                val shelfItems = shelf.items.orEmpty()
                                if (shelfItems.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = shelf.title ?: "Featured",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(
                                                items = shelfItems,
                                                key = { item ->
                                                    item.videoId ?: item.browseId ?: item.playlistId ?: "${item.title}_${item.subtitle}"
                                                }
                                            ) { item ->
                                                MusicShelfItemCard(
                                                    item = item,
                                                    onClick = { viewModel.openMusicItem(item, shelfItems) }
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

            // Local Playlists Quick Shelf
            item {
                StaggeredEnterItem(index = 1) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Local Playlists",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IosPillButton(
                                onClick = onNavigateToPlaylists,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                shadowElevation = 0.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Manage", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                ObsidianGlassCard(
                                    modifier = Modifier
                                        .width(164.dp)
                                        .iosClickable(scaleDown = 0.965f) {
                                            viewModel.requestOpenPlaylist(pl.id, pl.name)
                                            onNavigateToPlaylists()
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(92.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.background),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            if (!pl.coverThumbnail.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = pl.coverThumbnail,
                                                    contentDescription = pl.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
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
                                }
                            }

                            item {
                                ObsidianGlassCard(
                                    modifier = Modifier
                                        .width(148.dp)
                                        .height(152.dp)
                                        .iosClickable(scaleDown = 0.965f) { onNavigateToPlaylists() },
                                    shape = RoundedCornerShape(18.dp),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Create Playlist",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "New Playlist",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (savedSongs.isNotEmpty()) {
                item {
                    StaggeredEnterItem(index = 2) {
                        Column {
                            Text(
                                text = "Recently Played",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val recentItems = remember(savedSongs) {
                                savedSongs.distinctBy { it.videoId }.take(10).map { it.toSearchResultItem() }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(recentItems, key = { it.videoId ?: it.hashCode() }) { song ->
                                    ObsidianGlassCard(
                                        modifier = Modifier
                                            .width(166.dp)
                                            .iosClickable(scaleDown = 0.965f) {
                                                viewModel.playTrack(song, recentItems, openPlayerSheet = true)
                                            },
                                        shape = RoundedCornerShape(18.dp),
                                        shadowElevation = 6.dp
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(94.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.background)
                                            ) {
                                                ProgressiveArtworkImage(
                                                    videoId = song.videoId,
                                                    rawThumbnail = song.thumbnail,
                                                    contentDescription = song.title,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(listOf(ElectricViolet, CoralPulse))
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = song.title ?: "Unknown",
                                                style = MaterialTheme.typography.titleSmall,
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
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Discover Mixes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moodGenres) { (query, label) ->
                            IosSurfaceChip(
                                selected = viewModel.homeFeedQuery == query,
                                onClick = { viewModel.refreshHomeFeed(query) },
                                label = label
                            )
                        }
                    }
                }
            }

            when {
                viewModel.isLoadingHome && videoItems.isEmpty() -> {
                    item {
                        SoftTrackLoadingSkeleton(count = 5)
                    }
                }
                viewModel.homeError != null && videoItems.isEmpty() -> {
                    item {
                        ObsidianGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Unable to load music feed",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = viewModel.homeError ?: "Check your internet connection and try again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IosPillButton(
                                    onClick = { viewModel.refreshHomeFeed() },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("Retry", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                else -> {
                    itemsIndexed(
                        items = chunkedVideos,
                        key = { rowIdx, row -> "${rowIdx}_" + row.joinToString("_") { it.videoId ?: it.hashCode().toString() } }
                    ) { rowIdx, rowSongs ->
                        StaggeredEnterItem(index = rowIdx) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowSongs.forEach { song ->
                                    val vid = song.videoId ?: ""
                                    key(vid) {
                                        SongRowCard(
                                            song = song,
                                            isPlayingNow = viewModel.playerManager.currentSong?.videoId == vid,
                                            isFavorite = vid in favoriteIds,
                                            isDownloaded = vid in downloadedIds,
                                            downloadProgress = viewModel.downloadProgress[vid],
                                            onPlayClick = { viewModel.playTrack(song, videoItems, openPlayerSheet = true) },
                                            onMoreClick = { optionsTrack = song },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                repeat(gridColumns - rowSongs.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            containerColor = TintedSurface,
            title = { Text("Load Video Link or ID") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a bare ID or full media URL (watch?v=, youtu.be/, shorts/).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IosTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        label = "Media URL or ID",
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_link_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (linkInput.isNotBlank()) {
                            val target = linkInput.trim()
                            showLinkDialog = false
                            linkInput = ""
                            viewModel.openFormatInspector(target)
                        }
                    },
                    enabled = linkInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Inspect & Play")
                }
            },
            dismissButton = {
                IosPillButton(
                    onClick = { showLinkDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 0.dp
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    optionsTrack?.let { song ->
        val vid = song.videoId ?: ""
        TrackOptionsSheet(
            song = song,
            isFavorite = vid in favoriteIds,
            isDownloaded = vid in downloadedIds,
            onDismiss = { optionsTrack = null },
            onPlayNow = { viewModel.playTrack(song, openPlayerSheet = true) },
            onToggleFavorite = { viewModel.toggleFavorite(song) },
            onAddToPlaylist = { addToPlaylistTrack = song },
            onInspectFormats = { if (vid.isNotBlank()) viewModel.openFormatInspector(vid) },
            onDownloadToggle = {
                if (vid in downloadedIds) viewModel.removeOfflineDownload(vid)
                else viewModel.requestDownloadTrack(song)
            }
        )
    }

    addToPlaylistTrack?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addToPlaylistTrack = null },
            onSelectPlaylist = { plId ->
                viewModel.addSongToPlaylist(plId, song)
                addToPlaylistTrack = null
            },
            onCreateAndAdd = { newName ->
                viewModel.createPlaylistAndAddSong(newName, song)
                addToPlaylistTrack = null
            }
        )
    }
}
