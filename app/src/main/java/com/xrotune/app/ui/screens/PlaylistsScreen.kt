package com.xrotune.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xrotune.app.R
import com.xrotune.app.data.local.PlaylistSummary
import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.ui.components.AddToPlaylistDialog
import com.xrotune.app.ui.components.EdgeSwipeBackContainer
import com.xrotune.app.ui.components.IosIconButton
import com.xrotune.app.ui.components.IosPillButton
import com.xrotune.app.ui.components.IosTextField
import com.xrotune.app.ui.components.ObsidianGlassCard
import com.xrotune.app.ui.components.ProgressiveArtworkImage
import com.xrotune.app.ui.components.SongRowCard
import com.xrotune.app.ui.components.StaggeredEnterItem
import com.xrotune.app.ui.components.TrackOptionsSheet
import com.xrotune.app.ui.components.iosClickable
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.MintSignal
import com.xrotune.app.ui.theme.ObsidianBg
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.ui.viewmodel.MusicViewModel

private sealed class LibraryDestination {
    data object Overview : LibraryDestination()
    data object LikedSongs : LibraryDestination()
    data object DownloadedSongs : LibraryDestination()
    data class CustomPlaylist(val playlistId: Long, val name: String) : LibraryDestination()
}

@Composable
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit
) {
    val playlists by viewModel.playlistSummaries.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()

    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.videoId }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.videoId }.toSet() }

    var destination by remember { mutableStateOf<LibraryDestination>(LibraryDestination.Overview) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<PlaylistSummary?>(null) }
    var renameText by remember { mutableStateOf("") }

    var optionsTrack by remember { mutableStateOf<SearchResultItem?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<SearchResultItem?>(null) }

    val pendingPlaylist = viewModel.pendingOpenPlaylist
    LaunchedEffect(pendingPlaylist) {
        if (pendingPlaylist != null) {
            destination = LibraryDestination.CustomPlaylist(pendingPlaylist.first, pendingPlaylist.second)
            viewModel.consumePendingOpenPlaylist()
        }
    }

    if (destination != LibraryDestination.Overview) {
        BackHandler { destination = LibraryDestination.Overview }
    }

    when (val dest = destination) {
        is LibraryDestination.Overview -> {
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
                val chunkedPlaylists = remember(playlists, gridColumns) {
                    playlists.chunked(gridColumns)
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 1160.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Library",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${playlists.size} local playlists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IosPillButton(
                                onClick = { showCreateDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shadowElevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("create_playlist_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Smart Collections
                    item {
                        StaggeredEnterItem(index = 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SmartCollectionCard(
                                    title = "Liked Songs",
                                    count = favoriteSongs.size,
                                    icon = Icons.Default.Favorite,
                                    accent = CoralPulse,
                                    onClick = { destination = LibraryDestination.LikedSongs },
                                    modifier = Modifier.weight(1f)
                                )
                                SmartCollectionCard(
                                    title = "Offline Audio",
                                    count = downloadedSongs.size,
                                    icon = Icons.Default.DownloadDone,
                                    accent = MintSignal,
                                    onClick = { destination = LibraryDestination.DownloadedSongs },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Local Playlists",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (playlists.isEmpty()) {
                        item {
                            StaggeredEnterItem(index = 1) {
                                ObsidianGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    shadowElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.empty_playlists_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.empty_playlists_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IosPillButton(
                                            onClick = { showCreateDialog = true },
                                            containerColor = TintedSurfaceHigh,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text("Create First Playlist", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = chunkedPlaylists,
                            key = { _, row -> row.joinToString("_") { it.id.toString() } }
                        ) { rowIdx, rowItems ->
                            StaggeredEnterItem(index = rowIdx + 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { pl ->
                                        key(pl.id) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                PlaylistGlassCard(
                                                    playlist = pl,
                                                    onOpen = {
                                                        destination = LibraryDestination.CustomPlaylist(pl.id, pl.name)
                                                    },
                                                    onRename = {
                                                        renameTarget = pl
                                                        renameText = pl.name
                                                    },
                                                    onDelete = { viewModel.deletePlaylist(pl.id) }
                                                )
                                            }
                                        }
                                    }
                                    repeat(gridColumns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        is LibraryDestination.LikedSongs -> {
            CollectionDetailView(
                title = "Liked Songs",
                songs = favoriteSongs,
                viewModel = viewModel,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onBack = { destination = LibraryDestination.Overview },
                onBrowseSongs = onNavigateToSearch,
                onMoreClick = { optionsTrack = it }
            )
        }

        is LibraryDestination.DownloadedSongs -> {
            CollectionDetailView(
                title = "Offline Audio",
                songs = downloadedSongs,
                viewModel = viewModel,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onBack = { destination = LibraryDestination.Overview },
                onBrowseSongs = onNavigateToSearch,
                onMoreClick = { optionsTrack = it }
            )
        }

        is LibraryDestination.CustomPlaylist -> {
            val playlistSongs by remember(dest.playlistId) {
                viewModel.getSongsForPlaylist(dest.playlistId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            CollectionDetailView(
                title = dest.name,
                songs = playlistSongs,
                viewModel = viewModel,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onBack = { destination = LibraryDestination.Overview },
                onBrowseSongs = onNavigateToSearch,
                onMoreClick = { optionsTrack = it }
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = TintedSurface,
            title = { Text("Create Local Playlist") },
            text = {
                IosTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = "Playlist Name",
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                IosPillButton(
                    onClick = { showCreateDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 0.dp
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = TintedSurface,
            title = { Text("Rename Playlist") },
            text = {
                IosTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = "Playlist Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renamePlaylist(target.id, renameText)
                            renameTarget = null
                        }
                    },
                    enabled = renameText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                IosPillButton(
                    onClick = { renameTarget = null },
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
        val currentDest = destination
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
            },
            onRemoveFromPlaylist = if (currentDest is LibraryDestination.CustomPlaylist) {
                { viewModel.removeSongFromPlaylist(currentDest.playlistId, vid) }
            } else {
                null
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

@Composable
private fun PlaylistGlassCard(
    playlist: PlaylistSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ObsidianGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ObsidianBg,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
                if (!playlist.coverThumbnail.isNullOrBlank()) {
                    ProgressiveArtworkImage(
                        videoId = null,
                        rawThumbnail = playlist.coverThumbnail,
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IosIconButton(onClick = onRename, size = 40.dp) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IosIconButton(onClick = onDelete, size = 40.dp) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SmartCollectionCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier.iosClickable(scaleDown = 0.965f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollectionDetailView(
    title: String,
    songs: List<SongEntity>,
    viewModel: MusicViewModel,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    onBack: () -> Unit,
    onBrowseSongs: () -> Unit,
    onMoreClick: (SearchResultItem) -> Unit
) {
    val trackItems = remember(songs) { songs.distinctBy { it.videoId }.map { it.toSearchResultItem() } }

    EdgeSwipeBackContainer(onBack = onBack) {
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
            val chunkedTracks = remember(trackItems, gridColumns) { trackItems.chunked(gridColumns) }

            Column(
                modifier = Modifier
                    .widthIn(max = 1160.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                ObsidianGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${songs.size} tracks • Swipe from left edge to return",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (trackItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosPillButton(
                                    onClick = {
                                        viewModel.playTrack(trackItems.first(), trackItems, openPlayerSheet = true)
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shadowElevation = 6.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play All", fontWeight = FontWeight.SemiBold)
                                }
                                IosPillButton(
                                    onClick = {
                                        val shuffled = trackItems.shuffled()
                                        viewModel.playTrack(shuffled.first(), shuffled, openPlayerSheet = true)
                                    },
                                    containerColor = TintedSurfaceHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shadowElevation = 4.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shuffle", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (trackItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ObsidianGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No tracks here yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Discover songs in Search and add them to this collection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IosPillButton(
                                    onClick = onBrowseSongs,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("Find Songs", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = chunkedTracks,
                            key = { rowIdx, row -> "${rowIdx}_" + row.joinToString("_") { it.videoId ?: it.hashCode().toString() } }
                        ) { rowIdx, rowTracks ->
                            StaggeredEnterItem(index = rowIdx) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowTracks.forEach { song ->
                                        val vid = song.videoId ?: ""
                                        key(vid) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                SongRowCard(
                                                    song = song,
                                                    isPlayingNow = viewModel.playerManager.currentSong?.videoId == vid,
                                                    isFavorite = vid in favoriteIds,
                                                    isDownloaded = vid in downloadedIds,
                                                    downloadProgress = viewModel.downloadProgress[vid],
                                                    onPlayClick = {
                                                        viewModel.playTrack(song, trackItems, openPlayerSheet = true)
                                                    },
                                                    onMoreClick = { onMoreClick(song) }
                                                )
                                            }
                                        }
                                    }
                                    repeat(gridColumns - rowTracks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
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
