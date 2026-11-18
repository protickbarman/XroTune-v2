package com.xrotune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.xrotune.app.ui.components.ObsidianGlassCard
import com.xrotune.app.ui.components.ProgressiveArtworkImage
import com.xrotune.app.ui.components.SoftPulseLoadingIndicator
import com.xrotune.app.ui.components.SoftTrackLoadingSkeleton
import com.xrotune.app.ui.components.SongRowCard
import com.xrotune.app.ui.components.StaggeredEnterItem
import com.xrotune.app.ui.components.TrackOptionsSheet
import com.xrotune.app.ui.components.iosClickable
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.viewmodel.CollectionTarget
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun SearchScreen(viewModel: MusicViewModel) {
    val focusManager = LocalFocusManager.current
    val playlists by viewModel.playlistSummaries.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()

    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.videoId }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.videoId }.toSet() }

    var optionsTrack by remember { mutableStateOf<SearchResultItem?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<SearchResultItem?>(null) }

    val looksLikeLink = remember(viewModel.searchQuery) {
        viewModel.repository.looksLikeMediaUrlOrId(viewModel.searchQuery)
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

        Column(
            modifier = Modifier
                .widthIn(max = 1160.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Discover & Search",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            IosTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = stringResource(R.string.search_placeholder),
                leadingIcon = {
                    Icon(
                        imageVector = if (looksLikeLink) Icons.Default.Link else Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (looksLikeLink) {
                            IosPillButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.submitSearchOrUrl()
                                },
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                shadowElevation = 0.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Open", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IosIconButton(onClick = { viewModel.onSearchQueryChanged("") }, size = 40.dp) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.submitSearchOrUrl()
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            val filterOptions = listOf(
                "songs" to "Songs",
                "albums" to "Albums",
                "artists" to "Artists",
                "playlists" to "Playlists",
                "videos" to "Music Videos",
                "yt_video" to "YouTube"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { (type, label) ->
                    IosSurfaceChip(
                        selected = viewModel.searchType == type,
                        onClick = { viewModel.setSearchFilterType(type) },
                        label = label,
                        modifier = Modifier.testTag("search_filter_$type")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                viewModel.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 4.dp)
                    ) {
                        SoftTrackLoadingSkeleton(count = 6)
                    }
                }
                viewModel.searchError != null && viewModel.searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Could not load search results",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.searchError ?: "Please check your connection and try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IosPillButton(
                                onClick = { viewModel.retrySearch() },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                viewModel.searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No results found. Try another keyword or paste a video link.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    val dedupedResults = remember(viewModel.searchResults) {
                        viewModel.searchResults.distinctBy { item ->
                            val id = item.videoId?.trim()?.takeIf { it.isNotBlank() }
                                ?: item.browseId?.trim()?.takeIf { it.isNotBlank() }
                                ?: item.channelId?.trim()?.takeIf { it.isNotBlank() }
                                ?: item.playlistId?.trim()?.takeIf { it.isNotBlank() }
                                ?: "${item.title.orEmpty()}_${item.channel.orEmpty()}"
                            "${item.kind ?: "video"}:$id"
                        }
                    }
                    val playableVideos = remember(dedupedResults) {
                        dedupedResults
                            .filter { (it.kind == null || it.kind == "video") && !it.videoId.isNullOrBlank() }
                            .distinctBy { it.videoId!!.trim() }
                    }
                    val chunkedResults = remember(dedupedResults, gridColumns) {
                        dedupedResults.chunked(gridColumns)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = chunkedResults,
                            key = { rowIdx, row ->
                                "${rowIdx}_" + row.joinToString("_") {
                                    it.videoId ?: it.browseId ?: it.channelId ?: it.playlistId ?: it.hashCode().toString()
                                }
                            }
                        ) { rowIdx, rowItems ->
                            StaggeredEnterItem(index = rowIdx) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEachIndexed { colIdx, item ->
                                        val itemKey = item.videoId ?: item.browseId ?: item.channelId ?: item.playlistId ?: "idx_${rowIdx}_$colIdx"
                                        key(itemKey) {
                                            when {
                                                viewModel.searchType == "albums" ||
                                                    (item.kind == "playlist" && item.browseId?.startsWith("MPREb") == true) -> {
                                                    AlbumOrPlaylistCard(
                                                        item = item,
                                                        badgeLabel = "ALBUM",
                                                        isAlbum = true,
                                                        onClick = {
                                                            val bid = item.browseId ?: item.playlistId
                                                            if (!bid.isNullOrBlank()) {
                                                                viewModel.openCollection(
                                                                    CollectionTarget.Album(bid, item.title ?: "Album")
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                item.kind == "channel" || viewModel.searchType == "artists" -> {
                                                    ChannelResultCard(
                                                        item = item,
                                                        onClick = {
                                                            val cid = item.channelId ?: item.browseId
                                                            if (!cid.isNullOrBlank()) {
                                                                viewModel.openCollection(
                                                                    CollectionTarget.Artist(cid, item.title ?: "Artist")
                                                                )
                                                            } else {
                                                                val term = item.title ?: item.handle ?: ""
                                                                if (term.isNotBlank()) {
                                                                    viewModel.searchByKeyword(term, type = "songs")
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                item.kind == "playlist" -> {
                                                    AlbumOrPlaylistCard(
                                                        item = item,
                                                        badgeLabel = "PLAYLIST",
                                                        isAlbum = false,
                                                        onClick = {
                                                            val pid = item.playlistId ?: item.browseId
                                                            if (!pid.isNullOrBlank()) {
                                                                viewModel.openCollection(
                                                                    CollectionTarget.RemotePlaylist(pid, item.title ?: "Playlist")
                                                                )
                                                            } else {
                                                                val term = item.title ?: ""
                                                                if (term.isNotBlank()) {
                                                                    viewModel.searchByKeyword(term, type = "songs")
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                else -> {
                                                    val vid = item.videoId ?: ""
                                                    SongRowCard(
                                                        song = item,
                                                        isPlayingNow = viewModel.playerManager.currentSong?.videoId == vid,
                                                        isFavorite = vid in favoriteIds,
                                                        isDownloaded = vid in downloadedIds,
                                                        downloadProgress = viewModel.downloadProgress[vid],
                                                        onPlayClick = {
                                                            viewModel.playTrack(item, playableVideos, openPlayerSheet = true)
                                                        },
                                                        onMoreClick = { optionsTrack = item },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(gridColumns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (viewModel.continuationToken != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IosPillButton(
                                        onClick = { viewModel.loadMoreSearchResults() },
                                        enabled = !viewModel.isLoadingMore,
                                        containerColor = TintedSurfaceHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("search_load_more")
                                    ) {
                                        if (viewModel.isLoadingMore) {
                                            SoftPulseLoadingIndicator(size = 18.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Loading...")
                                        } else {
                                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Load More")
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

@Composable
private fun ChannelResultCard(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = listOfNotNull(
                    item.channel?.takeIf { it.isNotBlank() },
                    item.handle?.takeIf { it.isNotBlank() },
                    item.subscriberCountText?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "ARTIST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AlbumOrPlaylistCard(
    item: SearchResultItem,
    badgeLabel: String,
    isAlbum: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .iosClickable(scaleDown = 0.975f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAlbum) Icons.Default.Album else Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                ProgressiveArtworkImage(
                    videoId = item.videoId,
                    rawThumbnail = item.thumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = listOfNotNull(
                    item.channel?.takeIf { it.isNotBlank() },
                    item.videoCountText?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
