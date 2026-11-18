package com.xrotune.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xrotune.app.data.model.MusicItem
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.ui.viewmodel.CollectionTarget
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun MusicShelfItemCard(
    item: MusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArtist = item.kind.equals("artist", ignoreCase = true)
    ObsidianGlassCard(
        modifier = modifier
            .width(158.dp)
            .iosClickable(scaleDown = 0.965f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(if (isArtist) CircleShape else RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.kind?.lowercase()) {
                        "artist" -> Icons.Default.Person
                        "album" -> Icons.Default.Album
                        "playlist" -> Icons.Default.QueueMusic
                        else -> Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(30.dp)
                )
                if (!item.videoId.isNullOrBlank() || !item.thumbnail.isNullOrBlank()) {
                    ProgressiveArtworkImage(
                        videoId = item.videoId,
                        rawThumbnail = item.thumbnail,
                        contentDescription = item.title,
                        contentScale = if (isArtist) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (!item.videoId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ElectricViolet, CoralPulse))),
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
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title ?: "Untitled",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle ?: item.kind?.replaceFirstChar { it.uppercase() } ?: "Music",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicCollectionSheet(
    viewModel: MusicViewModel,
    onRequestTrackOptions: (SearchResultItem) -> Unit
) {
    val target = viewModel.activeCollectionTarget ?: return
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.videoId }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.videoId }.toSet() }

    val artistResp = viewModel.artistDetail
    val header = viewModel.collectionHeader
    val tracks = viewModel.collectionTracks
    val playableTracks = remember(tracks) {
        tracks.filter { !it.videoId.isNullOrBlank() }
            .distinctBy { it.videoId!!.trim() }
            .map { it.toSearchResultItem() }
    }

    val title = when (target) {
        is CollectionTarget.Artist -> artistResp?.artist?.title ?: target.fallbackTitle
        is CollectionTarget.Album -> header?.title ?: target.fallbackTitle
        is CollectionTarget.RemotePlaylist -> header?.title ?: target.fallbackTitle
    }
    val subtitle = when (target) {
        is CollectionTarget.Artist -> artistResp?.artist?.subscribers ?: "Artist Profile"
        is CollectionTarget.Album -> listOfNotNull(header?.subtitle, header?.secondSubtitle).joinToString(" • ")
        is CollectionTarget.RemotePlaylist -> listOfNotNull(header?.subtitle, header?.secondSubtitle).joinToString(" • ")
    }
    val coverUrl = when (target) {
        is CollectionTarget.Artist -> artistResp?.artist?.thumbnail
        else -> header?.thumbnail
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeCollectionSheet() },
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(if (target is CollectionTarget.Artist) CircleShape else RoundedCornerShape(16.dp))
                        .background(TintedSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (target) {
                            is CollectionTarget.Artist -> Icons.Default.Person
                            is CollectionTarget.Album -> Icons.Default.Album
                            is CollectionTarget.RemotePlaylist -> Icons.Default.QueueMusic
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    if (!coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IosIconButton(onClick = { viewModel.closeCollectionSheet() }, size = 40.dp) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (playableTracks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosPillButton(
                        onClick = {
                            viewModel.closeCollectionSheet()
                            viewModel.playTrack(playableTracks.first(), playableTracks, openPlayerSheet = true)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.SemiBold)
                    }
                    IosPillButton(
                        onClick = {
                            val shuffled = playableTracks.shuffled()
                            viewModel.closeCollectionSheet()
                            viewModel.playTrack(shuffled.first(), shuffled, openPlayerSheet = true)
                        },
                        containerColor = TintedSurfaceHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 4.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            when {
                viewModel.isLoadingCollection -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftPulseLoadingIndicator(size = 32.dp)
                    }
                }
                viewModel.collectionError != null -> {
                    ObsidianGlassCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.collectionError ?: "Unable to load collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = playableTracks,
                            key = { it.videoId ?: it.hashCode() }
                        ) { song ->
                            val vid = song.videoId ?: ""
                            SongRowCard(
                                song = song,
                                isPlayingNow = viewModel.playerManager.currentSong?.videoId == vid,
                                isFavorite = vid in favoriteIds,
                                isDownloaded = vid in downloadedIds,
                                downloadProgress = viewModel.downloadProgress[vid],
                                onPlayClick = {
                                    viewModel.closeCollectionSheet()
                                    viewModel.playTrack(song, playableTracks, openPlayerSheet = true)
                                },
                                onMoreClick = { onRequestTrackOptions(song) }
                            )
                        }

                        if (target is CollectionTarget.Artist) {
                            if (viewModel.collectionContinuation == null) {
                                item {
                                    IosPillButton(
                                        onClick = { viewModel.loadAllArtistSongs(target.browseId) },
                                        containerColor = TintedSurfaceHigh,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (viewModel.isLoadingMoreCollection) {
                                            SoftPulseLoadingIndicator(size = 18.dp)
                                        } else {
                                            Text("View Full Artist Song Catalog")
                                        }
                                    }
                                }
                            }
                            val albums = artistResp?.albums.orEmpty() + artistResp?.singles.orEmpty()
                            if (albums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Albums & Singles",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(albums, key = { it.browseId ?: it.title ?: it.hashCode() }) { alb ->
                                            MusicShelfItemCard(
                                                item = alb,
                                                onClick = { viewModel.openMusicItem(alb) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (viewModel.collectionContinuation != null) {
                            item {
                                IosPillButton(
                                    onClick = { viewModel.loadMoreCollectionTracks() },
                                    containerColor = TintedSurfaceHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (viewModel.isLoadingMoreCollection) {
                                        SoftPulseLoadingIndicator(size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading More...")
                                    } else {
                                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Load More Tracks")
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
