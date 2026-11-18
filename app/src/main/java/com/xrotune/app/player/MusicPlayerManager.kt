package com.xrotune.app.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xrotune.app.data.local.MusicDatabase
import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.data.preferences.UserPreferencesRepository
import com.xrotune.app.data.repository.MusicRepository
import com.xrotune.app.util.AppLogger
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

enum class RepeatMode {
    OFF, ALL, ONE
}

object MusicPlaybackCoordinator {

    private var appContext: Context? = null
    private var repository: MusicRepository? = null
    private var prefsRepo: UserPreferencesRepository? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var resolveJob: Job? = null
    private val playRequestSequence = AtomicLong(0L)

    private var serviceRef: PlaybackService? = null
    private var exoPlayer: ExoPlayer? = null

    private var pendingStreamStart: PendingStream? = null

    private data class PendingStream(
        val song: SearchResultItem,
        val streamUrl: String,
        val isOffline: Boolean,
        val formatLabel: String?,
        val bitrate: Long?,
        val requestId: Long
    )

    var currentSong by mutableStateOf<SearchResultItem?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    var isOfflinePlayback by mutableStateOf(false)
        private set

    var currentStreamBitrate by mutableStateOf<Long?>(null)
        private set

    var currentStreamFormat by mutableStateOf<String?>(null)
        private set

    var playbackError by mutableStateOf<String?>(null)
        private set

    var currentPosition by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set

    var queue by mutableStateOf<List<SearchResultItem>>(emptyList())
        private set

    var currentIndex by mutableIntStateOf(-1)
        private set

    var isShuffleEnabled by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = exoPlayer
            if (player != null) {
                currentPosition = maxOf(0L, player.currentPosition)
                if (player.duration > 0L) {
                    duration = player.duration
                }
                if (player.isPlaying) {
                    handler.postDelayed(this, 400)
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            if (playing) {
                playbackError = null
                handler.removeCallbacks(progressRunnable)
                handler.post(progressRunnable)
            } else {
                handler.removeCallbacks(progressRunnable)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            isBuffering = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                if (player.duration > 0L) {
                    duration = player.duration
                }
                isBuffering = false
                AppLogger.playback(
                    "Player",
                    "STATE_READY: ${currentSong?.title ?: currentSong?.videoId}",
                    if (isOfflinePlayback) "Mode=OFFLINE" else "Mode=STREAM"
                )
            } else if (playbackState == Player.STATE_ENDED) {
                isBuffering = false
                handleTrackEnded()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val expectedId = currentSong?.videoId?.trim()?.takeIf { it.isNotBlank() } ?: return
            val actualId = mediaItem?.mediaId?.trim()?.takeIf { it.isNotBlank() } ?: return
            if (actualId != expectedId) {
                exoPlayer?.pause()
                exoPlayer?.clearMediaItems()
                reportError("Playback halted: unexpected track ($actualId != $expectedId)")
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            isPlaying = false
            isBuffering = false
            val msg = error.localizedMessage ?: "Audio stream failed (${error.errorCodeName})"
            playbackError = msg
            AppLogger.error("Player", "ExoPlayer error (${error.errorCodeName})", msg)
        }
    }

    fun init(context: Context) {
        if (appContext == null) {
            val app = context.applicationContext
            appContext = app
            val db = MusicDatabase.getDatabase(app)
            repository = MusicRepository(app, db.musicDao())
            prefsRepo = UserPreferencesRepository.getInstance(app)
            scope.launch {
                val settings = prefsRepo?.userSettings?.first()
                if (settings != null) {
                    isShuffleEnabled = settings.shuffleEnabled
                    repeatMode = RepeatMode.entries.getOrElse(settings.repeatMode) { RepeatMode.OFF }
                }
            }
        }
    }

    fun attachService(service: PlaybackService, player: ExoPlayer) {
        serviceRef = service
        exoPlayer?.removeListener(playerListener)
        exoPlayer = player
        player.addListener(playerListener)

        pendingStreamStart?.let { pending ->
            pendingStreamStart = null
            if (pending.requestId == playRequestSequence.get()) {
                applyStreamToPlayer(pending)
            }
        }
    }

    fun detachService(service: PlaybackService) {
        if (serviceRef === service) {
            exoPlayer?.removeListener(playerListener)
            handler.removeCallbacks(progressRunnable)
            exoPlayer = null
            serviceRef = null
        }
    }

    fun setPlaybackModes(shuffle: Boolean, repeat: RepeatMode) {
        isShuffleEnabled = shuffle
        repeatMode = repeat
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        val s = isShuffleEnabled
        val r = repeatMode
        scope.launch {
            prefsRepo?.setShuffleEnabled(s)
            prefsRepo?.setRepeatMode(r.ordinal)
        }
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val s = isShuffleEnabled
        val r = repeatMode
        scope.launch {
            prefsRepo?.setShuffleEnabled(s)
            prefsRepo?.setRepeatMode(r.ordinal)
        }
    }

    fun canSkipNext(): Boolean {
        if (queue.isEmpty()) return false
        return isShuffleEnabled && queue.size > 1 ||
            currentIndex < queue.lastIndex ||
            repeatMode == RepeatMode.ALL
    }

    fun canSkipPrevious(): Boolean {
        if (queue.isEmpty()) return false
        return currentIndex > 0 || repeatMode == RepeatMode.ALL || currentPosition > 4000L
    }

    fun playTrack(
        song: SearchResultItem,
        playlist: List<SearchResultItem>? = null,
        explicitItag: Int? = null
    ) {
        val ctx = appContext ?: return
        val repo = repository ?: return
        val prefs = prefsRepo ?: return
        val videoId = song.videoId?.trim()?.takeIf { it.isNotBlank() } ?: return
        val targetSong = if (song.videoId == videoId) song else song.copy(videoId = videoId)

        AppLogger.playback(
            "Coordinator",
            "playTrack: ${targetSong.title ?: videoId}",
            "id=$videoId explicitItag=$explicitItag"
        )

        ensureServiceStarted(ctx)

        val requestId = playRequestSequence.incrementAndGet()
        resolveJob?.cancel()
        pendingStreamStart = null
        prepareLoading(targetSong, playlist)

        resolveJob = scope.launch {
            val settings = prefs.userSettings.first()
            val resolved = repo.resolveAudioStream(
                videoId = videoId,
                preferredContainer = settings.preferredContainer,
                explicitItag = explicitItag
            )

            ensureActive()
            if (requestId != playRequestSequence.get()) return@launch

            if (resolved == null || !repo.verifyStreamMatchesVideoId(videoId, resolved.streamUrl)) {
                reportError("Unable to verify audio stream for the selected track.")
                return@launch
            }

            val songEntity = SongEntity(
                videoId = videoId,
                title = targetSong.title ?: "Unknown Track",
                author = targetSong.channel ?: "Unknown Artist",
                thumbnail = targetSong.thumbnail,
                durationSeconds = targetSong.lengthSeconds ?: 0,
                preferredItag = resolved.itag,
                container = resolved.container,
                codec = resolved.codec,
                bitrate = resolved.bitrate,
                contentLength = resolved.contentLength,
                audioUrl = resolved.streamUrl,
                lastPlayedAt = System.currentTimeMillis()
            )
            repo.saveSong(songEntity)
            prefs.incrementTracksPlayed()

            ensureActive()
            if (requestId != playRequestSequence.get() || currentSong?.videoId?.trim() != videoId) {
                return@launch
            }

            val fmtLabel = listOfNotNull(
                resolved.codec?.uppercase() ?: resolved.container?.uppercase(),
                resolved.itag?.let { "#$it" }
            ).joinToString(" • ").ifBlank { null }

            startStream(
                song = targetSong,
                streamUrl = resolved.streamUrl,
                isOffline = resolved.isOffline,
                formatLabel = fmtLabel,
                bitrate = resolved.bitrate,
                requestId = requestId
            )
        }
    }

    private fun ensureServiceStarted(context: Context) {
        if (serviceRef == null) {
            val intent = Intent(context, PlaybackService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                runCatching { context.startService(intent) }
            }
        }
    }

    private fun prepareLoading(song: SearchResultItem, newQueue: List<SearchResultItem>?) {
        val targetId = song.videoId?.trim()
        val sanitizedQueue = newQueue
            ?.mapNotNull { item ->
                val id = item.videoId?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (item.videoId == id) item else item.copy(videoId = id)
            }
            ?.distinctBy { it.videoId }

        if (!sanitizedQueue.isNullOrEmpty()) {
            val foundIdx = sanitizedQueue.indexOfFirst { it.videoId == targetId }
            if (foundIdx >= 0) {
                queue = sanitizedQueue
                currentIndex = foundIdx
            } else {
                queue = listOf(song) + sanitizedQueue
                currentIndex = 0
            }
        } else {
            val existingIdx = queue.indexOfFirst { it.videoId == targetId }
            if (existingIdx >= 0) {
                currentIndex = existingIdx
            } else {
                queue = listOf(song)
                currentIndex = 0
            }
        }
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        handler.removeCallbacks(progressRunnable)

        currentSong = song
        isOfflinePlayback = false
        isPlaying = false
        isBuffering = true
        playbackError = null
        currentPosition = 0L
        duration = (song.lengthSeconds ?: 0) * 1000L
    }

    fun verifyPlaybackTarget(
        expectedVideoId: String,
        streamUrl: String,
        isOffline: Boolean
    ): Boolean {
        val cleanExpected = expectedVideoId.trim()
        if (cleanExpected.isBlank()) return false
        if (currentSong?.videoId?.trim() != cleanExpected) return false

        val repoMatch = repository?.verifyStreamMatchesVideoId(cleanExpected, streamUrl)
        if (repoMatch == false) return false

        val parsed = runCatching { Uri.parse(streamUrl) }.getOrNull() ?: return false
        if (isOffline) {
            val fileName = parsed.lastPathSegment?.substringBeforeLast('.')
            if (fileName != cleanExpected) return false
        } else {
            val queryId = parsed.getQueryParameter("v")?.trim()?.takeIf { it.isNotBlank() }
            if (queryId != null && queryId != cleanExpected) return false
        }
        return true
    }

    private fun startStream(
        song: SearchResultItem,
        streamUrl: String,
        isOffline: Boolean,
        formatLabel: String?,
        bitrate: Long?,
        requestId: Long
    ) {
        val expectedId = song.videoId?.trim() ?: return
        if (requestId != playRequestSequence.get() || !verifyPlaybackTarget(expectedId, streamUrl, isOffline)) {
            reportError("Playback verification failed: mismatched track stream.")
            return
        }

        currentSong = song
        isOfflinePlayback = isOffline
        currentStreamFormat = formatLabel
        currentStreamBitrate = bitrate
        playbackError = null
        isBuffering = !isOffline

        val stream = PendingStream(song, streamUrl, isOffline, formatLabel, bitrate, requestId)
        if (exoPlayer != null) {
            applyStreamToPlayer(stream)
        } else {
            pendingStreamStart = stream
        }
    }

    private fun applyStreamToPlayer(stream: PendingStream) {
        val player = exoPlayer ?: return
        val song = stream.song
        val expectedId = song.videoId?.trim()
        if (stream.requestId != playRequestSequence.get() ||
            expectedId.isNullOrBlank() ||
            !verifyPlaybackTarget(expectedId, stream.streamUrl, stream.isOffline)
        ) {
            reportError("Playback blocked: track verification mismatch.")
            return
        }

        AppLogger.playback(
            "Verify",
            "Verified target $expectedId (${if (stream.isOffline) "LOCAL FILE" else "REMOTE STREAM"})",
            stream.formatLabel
        )

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title ?: "Unknown Track")
            .setArtist(song.channel ?: "Unknown Artist")
            .setDisplayTitle(song.title ?: "Unknown Track")
            .setSubtitle(song.channel ?: "Unknown Artist")
            .apply {
                song.thumbnail?.takeIf { it.isNotBlank() }?.let { thumb ->
                    setArtworkUri(Uri.parse(thumb))
                }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(expectedId)
            .setUri(stream.streamUrl)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        handler.post(progressRunnable)
    }

    fun reportError(message: String) {
        isBuffering = false
        isPlaying = false
        playbackError = message
        AppLogger.error("Coordinator", message)
    }

    fun clearError() {
        playbackError = null
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player == null) {
            currentSong?.let { playTrack(it, queue) }
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            if (player.mediaItemCount == 0 && currentSong != null) {
                playTrack(currentSong!!, queue)
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        currentPosition = positionMs
    }

    fun jumpToQueueIndex(index: Int) {
        if (index in queue.indices) {
            currentIndex = index
            playTrack(queue[index], queue)
        }
    }

    fun playQueueItem(index: Int, expectedSong: SearchResultItem) {
        val expectedId = expectedSong.videoId?.trim()?.takeIf { it.isNotBlank() }
        if (expectedId == null) {
            jumpToQueueIndex(index)
            return
        }
        val atIndex = queue.getOrNull(index)
        if (atIndex?.videoId?.trim() == expectedId) {
            currentIndex = index
            playTrack(atIndex, queue)
        } else {
            val matchedIndex = queue.indexOfFirst { it.videoId?.trim() == expectedId }
            if (matchedIndex >= 0) {
                currentIndex = matchedIndex
                playTrack(queue[matchedIndex], queue)
            } else {
                playTrack(expectedSong, queue)
            }
        }
    }

    fun playNext() {
        if (queue.isEmpty()) return
        val nextIndex = if (isShuffleEnabled && queue.size > 1) {
            val candidates = queue.indices.filter { it != currentIndex }
            candidates.random()
        } else if (currentIndex < queue.size - 1) {
            currentIndex + 1
        } else if (repeatMode == RepeatMode.ALL) {
            0
        } else {
            return
        }
        currentIndex = nextIndex
        playTrack(queue[nextIndex], queue)
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        val currentPos = exoPlayer?.currentPosition ?: currentPosition
        if (currentPos > 4000L) {
            seekTo(0L)
            return
        }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else if (repeatMode == RepeatMode.ALL) {
            queue.lastIndex
        } else {
            0
        }
        currentIndex = prevIndex
        playTrack(queue[prevIndex], queue)
    }

    fun stopAndClear() {
        resolveJob?.cancel()
        pendingStreamStart = null
        handler.removeCallbacks(progressRunnable)
        serviceRef?.dismissAndStop()
        currentSong = null
        isPlaying = false
        isBuffering = false
        playbackError = null
        currentPosition = 0L
        duration = 0L
        currentStreamFormat = null
        currentStreamBitrate = null
        isOfflinePlayback = false
        AppLogger.playback("Coordinator", "Playback stopped & cleared")
    }

    private fun handleTrackEnded() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (currentIndex < queue.lastIndex || (isShuffleEnabled && queue.size > 1)) {
                    playNext()
                } else {
                    isPlaying = false
                }
            }
        }
    }
}

class MusicPlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        MusicPlaybackCoordinator.init(appContext)
        connectController()
    }

    private fun connectController() {
        runCatching {
            val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        }
    }

    val currentSong: SearchResultItem?
        get() = MusicPlaybackCoordinator.currentSong

    val isPlaying: Boolean
        get() = MusicPlaybackCoordinator.isPlaying

    val isBuffering: Boolean
        get() = MusicPlaybackCoordinator.isBuffering

    val isOfflinePlayback: Boolean
        get() = MusicPlaybackCoordinator.isOfflinePlayback

    val currentStreamBitrate: Long?
        get() = MusicPlaybackCoordinator.currentStreamBitrate

    val currentStreamFormat: String?
        get() = MusicPlaybackCoordinator.currentStreamFormat

    val playbackError: String?
        get() = MusicPlaybackCoordinator.playbackError

    val currentPosition: Long
        get() = MusicPlaybackCoordinator.currentPosition

    val duration: Long
        get() = MusicPlaybackCoordinator.duration

    val queue: List<SearchResultItem>
        get() = MusicPlaybackCoordinator.queue

    val currentIndex: Int
        get() = MusicPlaybackCoordinator.currentIndex

    val isShuffleEnabled: Boolean
        get() = MusicPlaybackCoordinator.isShuffleEnabled

    val repeatMode: RepeatMode
        get() = MusicPlaybackCoordinator.repeatMode

    fun setPlaybackModes(shuffle: Boolean, repeat: RepeatMode) {
        MusicPlaybackCoordinator.setPlaybackModes(shuffle, repeat)
    }

    fun toggleShuffle() = MusicPlaybackCoordinator.toggleShuffle()

    fun cycleRepeatMode() = MusicPlaybackCoordinator.cycleRepeatMode()

    fun playTrack(
        song: SearchResultItem,
        playlist: List<SearchResultItem>? = null,
        explicitItag: Int? = null
    ) = MusicPlaybackCoordinator.playTrack(song, playlist, explicitItag)

    fun togglePlayPause() = MusicPlaybackCoordinator.togglePlayPause()

    fun seekTo(positionMs: Long) = MusicPlaybackCoordinator.seekTo(positionMs)

    fun jumpToQueueIndex(index: Int) = MusicPlaybackCoordinator.jumpToQueueIndex(index)

    fun playQueueItem(index: Int, song: SearchResultItem) =
        MusicPlaybackCoordinator.playQueueItem(index, song)

    fun playNext() = MusicPlaybackCoordinator.playNext()

    fun playPrevious() = MusicPlaybackCoordinator.playPrevious()

    fun stopAndClear() = MusicPlaybackCoordinator.stopAndClear()

    fun clearError() = MusicPlaybackCoordinator.clearError()

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
