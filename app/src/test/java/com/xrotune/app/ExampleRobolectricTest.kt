package com.xrotune.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xrotune.app.data.local.MusicDatabase
import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.local.XroTuneDocument
import com.xrotune.app.data.local.XroTuneFileStorage
import com.xrotune.app.data.local.XroTuneProfileData
import com.xrotune.app.data.local.XroTuneSettingsData
import com.xrotune.app.data.model.FormatItem
import com.xrotune.app.data.model.MusicItem
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.data.repository.MusicRepository
import com.xrotune.app.player.MusicPlaybackCoordinator
import com.xrotune.app.player.RepeatMode
import com.xrotune.app.ui.util.buildArtworkCandidates
import com.xrotune.app.ui.util.formatBitrate
import com.xrotune.app.ui.util.formatByteSize
import com.xrotune.app.ui.util.formatCompactCount
import com.xrotune.app.ui.util.formatDurationSeconds
import com.xrotune.app.ui.util.parseDurationTextToSeconds
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.util.AppLogger
import com.xrotune.app.util.LogLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("XroTune", appName)
    }

    @Test
    fun `repository resolves download stream urls, quality tiers, and detects media links`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = MusicRepository(context, db.musicDao())

        val relative = repo.resolveRelativeUrl("/api/download?v=Pu--qQBsnOg&itag=140")
        assertTrue(relative!!.endsWith("/api/download?v=Pu--qQBsnOg&itag=140"))

        val downloadUrl = repo.buildDownloadStreamUrl("Pu--qQBsnOg", 251, dl = true)
        assertTrue(downloadUrl.contains("/api/download?v=Pu--qQBsnOg&itag=251&dl=1"))

        val formats = listOf(
            FormatItem(itag = 249, kind = "audio", container = "webm", codec = "opus", quality = null, bitrate = 50_000L, contentLength = 1_200_000L, proxyUrl = null, directUrl = null, mergeUrl = null),
            FormatItem(itag = 250, kind = "audio", container = "webm", codec = "opus", quality = null, bitrate = 70_000L, contentLength = 1_700_000L, proxyUrl = null, directUrl = null, mergeUrl = null),
            FormatItem(itag = 140, kind = "audio", container = "mp4", codec = "mp4a.40.2", quality = null, bitrate = 129_000L, contentLength = 3_300_000L, proxyUrl = null, directUrl = null, mergeUrl = null)
        )
        assertEquals(140, repo.selectAudioFormatForTier(formats, "high")?.itag)
        assertEquals(250, repo.selectAudioFormatForTier(formats, "medium")?.itag)
        assertEquals(249, repo.selectAudioFormatForTier(formats, "low")?.itag)

        assertTrue(repo.looksLikeMediaUrlOrId("iss_85SWLYA"))
        assertTrue(repo.looksLikeMediaUrlOrId("https://youtu.be/64f_JnZuJPo"))
        assertTrue(repo.looksLikeMediaUrlOrId("https://example.com/watch?v=64f_JnZuJPo"))
        assertFalse(repo.looksLikeMediaUrlOrId("bangla folk song"))
        db.close()
    }

    @Test
    fun `xrotune file storage persists profile and download settings on device`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = XroTuneFileStorage(context)
        val initial = storage.loadOrCreate()
        assertEquals("XroTune", initial.appName)
        assertTrue(storage.storageFile.exists())
        assertEquals(".xrotune", storage.storageFile.name)

        val updated = XroTuneDocument(
            profile = XroTuneProfileData(
                displayName = "Anandho",
                bio = "Late night listener",
                favoriteGenre = "Bangla Indie",
                avatarColorIndex = 2,
                totalTracksPlayed = 15
            ),
            settings = XroTuneSettingsData(
                preferredContainer = "mp4",
                preferredDownloadQuality = "medium",
                askDownloadQuality = true,
                shuffleEnabled = true,
                repeatMode = 1
            )
        )
        storage.saveDocument(updated)

        val reloaded = storage.loadOrCreate()
        assertEquals("Anandho", reloaded.profile.displayName)
        assertEquals("Bangla Indie", reloaded.profile.favoriteGenre)
        assertEquals(15, reloaded.profile.totalTracksPlayed)
        assertEquals("medium", reloaded.settings.preferredDownloadQuality)
        assertEquals("mp4", reloaded.settings.preferredContainer)
        assertTrue(reloaded.settings.askDownloadQuality)
    }

    @Test
    fun `playback coordinator manages modes and clears state on stop`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MusicPlaybackCoordinator.init(context)
        MusicPlaybackCoordinator.setPlaybackModes(shuffle = false, repeat = RepeatMode.OFF)
        MusicPlaybackCoordinator.toggleShuffle()
        assertTrue(MusicPlaybackCoordinator.isShuffleEnabled)

        MusicPlaybackCoordinator.cycleRepeatMode()
        assertEquals(RepeatMode.ALL, MusicPlaybackCoordinator.repeatMode)
        MusicPlaybackCoordinator.cycleRepeatMode()
        assertEquals(RepeatMode.ONE, MusicPlaybackCoordinator.repeatMode)
        MusicPlaybackCoordinator.cycleRepeatMode()
        assertEquals(RepeatMode.OFF, MusicPlaybackCoordinator.repeatMode)

        MusicPlaybackCoordinator.stopAndClear()
        assertNull(MusicPlaybackCoordinator.currentSong)
        assertFalse(MusicPlaybackCoordinator.isPlaying)
        assertFalse(MusicPlaybackCoordinator.canSkipNext())
        assertFalse(MusicPlaybackCoordinator.canSkipPrevious())
    }

    @Test
    fun `format utils and music item conversion produce expected durations, counts, and HD candidates`() {
        assertEquals("5:51", formatDurationSeconds(351))
        assertEquals("1:01:05", formatDurationSeconds(3665))
        assertEquals(225, parseDurationTextToSeconds("3:45"))
        assertEquals(3665, parseDurationTextToSeconds("1:01:05"))
        assertEquals(0, parseDurationTextToSeconds(null))
        assertEquals("128 kbps", formatBitrate(128000L))
        assertEquals("3.5 MB", formatByteSize(3670016L))
        assertEquals("1.2M", formatCompactCount(1_240_000L))
        assertEquals("45.2K", formatCompactCount(45_200L))

        val musicItem = MusicItem(
            kind = "song",
            title = "Midnight City",
            subtitle = "M83 • Hurry Up, We're Dreaming",
            videoId = "dX3k_QDnzHE",
            duration = "4:03",
            durationSeconds = null,
            thumbnail = "https://lh3.googleusercontent.com/sample"
        )
        val converted = musicItem.toSearchResultItem()
        assertEquals("dX3k_QDnzHE", converted.videoId)
        assertEquals("Midnight City", converted.title)
        assertEquals("M83 • Hurry Up, We're Dreaming", converted.channel)
        assertEquals(243, converted.lengthSeconds)

        val candidates = buildArtworkCandidates(
            videoId = "Pu--qQBsnOg",
            rawThumbnail = "https://i.ytimg.com/vi/Pu--qQBsnOg/hqdefault.jpg"
        )
        assertEquals(
            listOf(
                "https://i.ytimg.com/vi/Pu--qQBsnOg/maxresdefault.jpg",
                "https://i.ytimg.com/vi/Pu--qQBsnOg/hq720.jpg",
                "https://i.ytimg.com/vi/Pu--qQBsnOg/sddefault.jpg",
                "https://i.ytimg.com/vi/Pu--qQBsnOg/hqdefault.jpg"
            ),
            candidates
        )
        assertEquals(candidates.distinct().size, candidates.size)
    }

    @Test
    fun `app logger records tagged telemetry and formats entries cleanly`() {
        AppLogger.clear()
        AppLogger.info("Boot", "App started")
        AppLogger.api("HTTP", "GET /api/doc?json=1 -> 200", "42ms")
        AppLogger.playback("Queue", "Playing Midnight City", "videoId=dX3k_QDnzHE")
        AppLogger.error("Stream", "Timeout", "HTTP 504")

        val entries = AppLogger.logs.value
        assertEquals(4, entries.size)
        assertEquals(LogLevel.ERROR, entries.first().level)
        assertEquals("Stream", entries.first().tag)

        val exported = AppLogger.formatAllForExport()
        assertTrue(exported.contains("[API] [HTTP] GET /api/doc?json=1 -> 200 | 42ms"))
        assertTrue(exported.contains("[PLAY] [Queue] Playing Midnight City | videoId=dX3k_QDnzHE"))

        AppLogger.clear()
        assertTrue(AppLogger.logs.value.isEmpty())
    }

    @Test
    fun `offline stream resolution strictly matches videoId and preserves addedAt list order`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.musicDao()
        val repo = MusicRepository(context, dao)

        val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
        val fileA = File(downloadsDir, "vid_alpha_01.m4a").apply { writeText("audio_alpha") }
        val fileB = File(downloadsDir, "vid_bravo_02.webm").apply { writeText("audio_bravo") }

        dao.insertSong(
            SongEntity(
                videoId = "vid_alpha_01",
                title = "Alpha Offline",
                author = "Artist A",
                localFilePath = fileA.absolutePath,
                addedAt = 1000L
            )
        )
        dao.insertSong(
            SongEntity(
                videoId = "vid_bravo_02",
                title = "Bravo Offline",
                author = "Artist B",
                localFilePath = null, // Simulates DB row missing path even though file exists on disk
                addedAt = 2000L
            )
        )

        val resolvedA = repo.resolveAudioStream("vid_alpha_01", "any", explicitItag = null)
        assertTrue(resolvedA!!.isOffline)
        assertTrue(resolvedA.streamUrl.endsWith("vid_alpha_01.m4a"))

        val resolvedB = repo.resolveAudioStream("vid_bravo_02", "any", explicitItag = null)
        assertTrue(resolvedB!!.isOffline)
        assertTrue(resolvedB.streamUrl.endsWith("vid_bravo_02.webm"))

        repo.saveSong(
            SongEntity(
                videoId = "vid_alpha_01",
                title = "Alpha Offline",
                author = "Artist A",
                lastPlayedAt = 999999L,
                addedAt = 999999L
            )
        )
        val updatedAlpha = dao.getSongById("vid_alpha_01")!!
        assertEquals(1000L, updatedAlpha.addedAt)
        assertEquals(fileA.absolutePath, updatedAlpha.localFilePath)

        val downloadedOrder = repo.downloadedSongs.first().map { it.videoId }
        assertEquals(listOf("vid_bravo_02", "vid_alpha_01"), downloadedOrder)

        val queue = listOf(
            SearchResultItem(kind = "video", videoId = "vid_bravo_02", title = "Bravo Offline"),
            SearchResultItem(kind = "video", videoId = "vid_alpha_01", title = "Alpha Offline")
        )
        MusicPlaybackCoordinator.init(context)
        MusicPlaybackCoordinator.playTrack(queue[1], queue)
        assertEquals("vid_alpha_01", MusicPlaybackCoordinator.currentSong?.videoId)
        assertEquals(1, MusicPlaybackCoordinator.currentIndex)

        assertTrue(repo.verifyStreamMatchesVideoId("vid_alpha_01", "file:///data/downloads/vid_alpha_01.m4a"))
        assertFalse(repo.verifyStreamMatchesVideoId("vid_alpha_01", "file:///data/downloads/vid_bravo_02.m4a"))
        assertFalse(repo.verifyStreamMatchesVideoId("vid_alpha_01", "https://example.com/api/download?v=vid_bravo_02&itag=140"))
        assertFalse(repo.verifyStreamMatchesVideoId("vid_alpha_01", "https://example.com/stream", resolvedMetaVideoId = "vid_bravo_02"))
        assertTrue(repo.verifyStreamMatchesVideoId("vid_alpha_01", "https://example.com/api/download?v=vid_alpha_01&itag=140", resolvedMetaVideoId = "vid_alpha_01"))

        assertTrue(MusicPlaybackCoordinator.verifyPlaybackTarget("vid_alpha_01", "file:///data/downloads/vid_alpha_01.m4a", isOffline = true))
        assertFalse(MusicPlaybackCoordinator.verifyPlaybackTarget("vid_alpha_01", "file:///data/downloads/vid_bravo_02.m4a", isOffline = true))
        assertFalse(MusicPlaybackCoordinator.verifyPlaybackTarget("vid_bravo_02", "https://example.com/api/download?v=vid_bravo_02&itag=140", isOffline = false))

        val bravoSong = queue[0]
        MusicPlaybackCoordinator.playQueueItem(index = 1, expectedSong = bravoSong)
        assertEquals("vid_bravo_02", MusicPlaybackCoordinator.currentSong?.videoId)
        assertEquals(0, MusicPlaybackCoordinator.currentIndex)

        fileA.delete()
        fileB.delete()
        db.close()
    }
}
