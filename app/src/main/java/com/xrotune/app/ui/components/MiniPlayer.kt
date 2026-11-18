package com.xrotune.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xrotune.app.ui.theme.CoralPulse
import com.xrotune.app.ui.theme.ElectricViolet
import com.xrotune.app.ui.theme.TintedSurfaceHigh
import com.xrotune.app.ui.viewmodel.MusicViewModel

@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong = viewModel.playerManager.currentSong ?: return
    val isPlaying = viewModel.playerManager.isPlaying
    val isBuffering = viewModel.playerManager.isBuffering
    val position = viewModel.playerManager.currentPosition
    val duration = viewModel.playerManager.duration
    val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .iosClickable(scaleDown = 0.98f) { viewModel.showNowPlayingSheet = true }
            .testTag("mini_player"),
        shape = RoundedCornerShape(22.dp),
        color = TintedSurfaceHigh.copy(alpha = 0.96f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = CoralPulse,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    ProgressiveArtworkImage(
                        videoId = currentSong.videoId,
                        rawThumbnail = currentSong.thumbnail,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isPlaying) {
                            MiniEqualizerBars(isPlaying = true)
                        }
                        Text(
                            text = currentSong.title ?: "Unknown Track",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(currentSong.channel ?: "Unknown Artist")
                            if (viewModel.playerManager.isOfflinePlayback) {
                                append(" • Offline")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        SoftPulseLoadingIndicator(
                            size = 20.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            ElectricViolet.copy(alpha = 0.85f),
                                            CoralPulse.copy(alpha = 0.78f)
                                        )
                                    )
                                )
                                .iosClickable(scaleDown = 0.92f) {
                                    viewModel.playerManager.togglePlayPause()
                                }
                                .testTag("mini_player_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                IosIconButton(
                    onClick = { viewModel.playerManager.playNext() },
                    size = 40.dp,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IosIconButton(
                    onClick = { viewModel.stopAndDismissPlayer() },
                    size = 40.dp,
                    modifier = Modifier.testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Mini Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
