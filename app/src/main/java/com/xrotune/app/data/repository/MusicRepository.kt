package com.xrotune.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.xrotune.app.data.api.MusicApiService
import com.xrotune.app.data.local.MusicDao
import com.xrotune.app.data.local.PlaylistEntity
import com.xrotune.app.data.local.PlaylistSongCrossRef
import com.xrotune.app.data.local.PlaylistSummary
import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.model.ApiDocResponse
import com.xrotune.app.data.model.FormatItem
import com.xrotune.app.data.model.HealthResponse
import com.xrotune.app.data.model.InfoResponse
import com.xrotune.app.data.model.MusicArtistResponse
import com.xrotune.app.data.model.MusicItem
import com.xrotune.app.data.model.MusicLyricsResponse
import com.xrotune.app.data.model.MusicShelf
import com.xrotune.app.data.model.MusicSongResponse
import com.xrotune.app.data.model.MusicTrackListResponse
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.util.AppLogger
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class SearchPageResult(
    val results: List<SearchResultItem>,
    val continuation: String?,
    val error: String? = null
)

data class MusicSearchPageResult(
    val results: List<MusicItem>,
    val continuation: String?,
    val error: String? = null
)

data class ResolvedAudioStream(
    val streamUrl: String,
    val itag: Int?,
    val container: String?,
    val codec: String?,
    val bitrate: Long?,
    val contentLength: Long?,
    val isOffline: Boolean
)

class MusicRepository(private val context: Context, private val musicDao: MusicDao) {

    companion object {
        const val API_BASE_URL = "https://xro-yt-api.onrender.com/"
    }

    private val moshi = Moshi.Builder().build()

    private val apiTelemetryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val startNs = System.nanoTime()
        val pathWithQuery = buildString {
            append(request.url.encodedPath)
            request.url.encodedQuery?.let { append("?").append(it) }
        }
        try {
            val response = chain.proceed(request)
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            val summary = "${request.method} $pathWithQuery -> ${response.code} (${elapsedMs}ms)"
            if (response.isSuccessful) {
                AppLogger.api("HTTP", summary)
            } else if (response.code == 404) {
                AppLogger.warn("HTTP", summary, "Resource or lyrics not found")
            } else {
                AppLogger.error("HTTP", summary)
            }
            response
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            AppLogger.error(
                "HTTP",
                "${request.method} $pathWithQuery FAILED (${elapsedMs}ms)",
                e.localizedMessage ?: e.javaClass.simpleName
            )
            throw e
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiTelemetryInterceptor)
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    private val apiService: MusicApiService = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MusicApiService::class.java)

    fun resolveRelativeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("/")) {
            API_BASE_URL.trimEnd('/') + url
        } else {
            url
        }
    }

    fun buildDownloadStreamUrl(videoId: String, itag: Int, dl: Boolean = false): String {
        val encodedId = Uri.encode(videoId)
        val suffix = if (dl) "&dl=1" else ""
        return "${API_BASE_URL}api/download?v=$encodedId&itag=$itag$suffix"
    }

    fun looksLikeMediaUrlOrId(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.matches(Regex("^[A-Za-z0-9_-]{11}$"))) return true
        val lower = trimmed.lowercase()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.contains("watch?v=") ||
            lower.contains("youtu.be/") ||
            lower.contains("/shorts/") ||
            lower.contains("/live/") ||
            lower.contains("/embed/")
    }

    suspend fun getHealthInfo(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getHealth()
            if (resp.ok) {
                Result.success(resp)
            } else {
                Result.failure(Exception(resp.error ?: "Server reported unhealthy status"))
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Service health check failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getApiDocumentation(): Result<ApiDocResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getApiDoc(json = 1)
            if (resp.ok) {
                Result.success(resp)
            } else {
                Result.failure(Exception(resp.error ?: "Failed to load /api/doc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Boolean = getHealthInfo().isSuccess

    // ==================== /api/music/* Endpoints ====================

    suspend fun getMusicHomeShelves(): Result<List<MusicShelf>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicHome()
            if (resp.ok) {
                Result.success(resp.shelves.orEmpty())
            } else {
                Result.failure(Exception(resp.error ?: "Failed to load Music Home"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicTrendingShelves(): Result<List<MusicShelf>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicTrending()
            if (resp.ok) {
                Result.success(resp.shelves.orEmpty())
            } else {
                Result.failure(Exception(resp.error ?: "Failed to load Music Trending"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMusicCatalog(
        query: String,
        type: String = "songs",
        continuation: String? = null
    ): MusicSearchPageResult = withContext(Dispatchers.IO) {
        try {
            val effectiveQuery = query.ifBlank { "top hits" }
            val resp = apiService.searchMusic(query = effectiveQuery, type = type, continuation = continuation)
            if (resp.ok && resp.results != null) {
                MusicSearchPageResult(
                    results = resp.results,
                    continuation = resp.continuation,
                    error = null
                )
            } else {
                MusicSearchPageResult(
                    results = emptyList(),
                    continuation = null,
                    error = resp.error ?: "Unable to fetch music search results"
                )
            }
        } catch (e: Exception) {
            MusicSearchPageResult(
                results = emptyList(),
                continuation = null,
                error = e.localizedMessage ?: "Network error while searching music"
            )
        }
    }

    suspend fun getMusicArtist(channelId: String): Result<MusicArtistResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicArtist(channelId)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load artist profile"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicArtistSongs(
        channelId: String,
        continuation: String? = null
    ): Result<MusicTrackListResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicArtistSongs(channelId, continuation)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load artist songs"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicAlbum(browseId: String): Result<MusicTrackListResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicAlbum(browseId)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load album tracks"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicPlaylist(
        playlistId: String,
        continuation: String? = null
    ): Result<MusicTrackListResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicPlaylist(playlistId, continuation)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load playlist tracks"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicRadio(
        videoId: String,
        continuation: String? = null
    ): Result<MusicTrackListResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicRadio(videoId, continuation)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load radio mix"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicSongMeta(videoId: String): Result<MusicSongResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getMusicSong(videoId)
            if (resp.ok) Result.success(resp)
            else Result.failure(Exception(resp.error ?: "Unable to load song details"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicLyrics(videoId: String): Result<MusicLyricsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMusicLyrics(videoId)
            val body = response.body()
            when {
                response.isSuccessful && body != null && body.ok -> Result.success(body)
                response.code() == 404 -> Result.failure(Exception("Lyrics unavailable for this track"))
                else -> Result.failure(Exception(body?.error ?: "Failed to load lyrics (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== General Video / Stream Endpoints ====================

    suspend fun searchMedia(
        query: String,
        type: String = "video",
        continuation: String? = null
    ): SearchPageResult = withContext(Dispatchers.IO) {
        try {
            val effectiveQuery = query.ifBlank { "trending music" }
            val resp = apiService.search(query = effectiveQuery, type = type, continuation = continuation)
            if (resp.ok && resp.results != null) {
                SearchPageResult(
                    results = resp.results,
                    continuation = resp.continuation,
                    error = null
                )
            } else {
                SearchPageResult(
                    results = emptyList(),
                    continuation = null,
                    error = resp.error ?: "Unable to fetch results"
                )
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Search failed: ${e.message}")
            SearchPageResult(
                results = emptyList(),
                continuation = null,
                error = e.localizedMessage ?: "Network error while searching"
            )
        }
    }

    suspend fun getMediaInfo(
        videoIdOrUrl: String,
        format: String? = null
    ): Result<InfoResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getInfo(videoIdOrUrl = videoIdOrUrl, format = format)
            if (resp.ok) {
                val fixedFormats = resp.formats?.map { fmt ->
                    fmt.copy(
                        proxyUrl = resolveRelativeUrl(fmt.proxyUrl),
                        mergeUrl = resolveRelativeUrl(fmt.mergeUrl)
                    )
                }
                Result.success(resp.copy(formats = fixedFormats))
            } else {
                Result.failure(Exception(resp.error ?: "Failed to load track info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyStreamMatchesVideoId(
        expectedVideoId: String,
        streamUrl: String,
        resolvedMetaVideoId: String? = null
    ): Boolean {
        val cleanExpected = expectedVideoId.trim()
        if (cleanExpected.isBlank() || streamUrl.isBlank()) return false

        val cleanMeta = resolvedMetaVideoId?.trim()?.takeIf { it.isNotBlank() }
        if (cleanMeta != null && cleanMeta != cleanExpected) {
            AppLogger.error("Verify", "Stream meta videoId mismatch", "expected=$cleanExpected actual=$cleanMeta")
            return false
        }

        val parsed = runCatching { Uri.parse(streamUrl) }.getOrNull() ?: return false
        if (parsed.scheme.equals("file", ignoreCase = true)) {
            val fileName = parsed.path?.let { File(it).nameWithoutExtension }
            if (fileName != cleanExpected) {
                AppLogger.error("Verify", "Offline file mismatch", "expected=$cleanExpected actual=$fileName")
                return false
            }
            return true
        }

        val queryVideoId = parsed.getQueryParameter("v")?.trim()?.takeIf { it.isNotBlank() }
        if (queryVideoId != null && queryVideoId != cleanExpected) {
            AppLogger.error("Verify", "Stream URL query v= mismatch", "expected=$cleanExpected actual=$queryVideoId")
            return false
        }

        return true
    }

    suspend fun resolveAudioStream(
        videoId: String,
        preferredContainer: String,
        explicitItag: Int? = null
    ): ResolvedAudioStream? = withContext(Dispatchers.IO) {
        val cleanId = videoId.trim()
        if (cleanId.isBlank()) return@withContext null

        // 1. Play verified offline local file immediately if downloaded (unless user forced a remote itag)
        val cachedSong = musicDao.getSongById(cleanId)
        if (explicitItag == null) {
            val recordedFile = cachedSong?.localFilePath
                ?.takeIf { it.isNotBlank() }
                ?.let { File(it) }
                ?.takeIf { it.exists() && it.length() > 0L && it.nameWithoutExtension == cleanId }

            val localFile = recordedFile ?: run {
                val downloadsDir = File(context.filesDir, "downloads")
                downloadsDir.listFiles()
                    ?.firstOrNull { it.isFile && it.nameWithoutExtension == cleanId && it.length() > 0L }
            }

            if (localFile != null) {
                val localUri = Uri.fromFile(localFile).toString()
                if (verifyStreamMatchesVideoId(cleanId, localUri)) {
                    if (cachedSong != null && cachedSong.localFilePath != localFile.absolutePath) {
                        musicDao.updateLocalFilePath(cleanId, localFile.absolutePath)
                    }
                    AppLogger.playback("Resolve", "Resolved OFFLINE local file for $cleanId", localFile.name)
                    return@withContext ResolvedAudioStream(
                        streamUrl = localUri,
                        itag = cachedSong?.preferredItag,
                        container = cachedSong?.container ?: localFile.extension.ifBlank { null },
                        codec = cachedSong?.codec,
                        bitrate = cachedSong?.bitrate,
                        contentLength = localFile.length(),
                        isOffline = true
                    )
                }
            }
        }

        // 2. Query /api/info when selecting explicit itag or container preference
        if (explicitItag != null || preferredContainer != "any") {
            val infoRes = getMediaInfo(cleanId, format = "audio").getOrNull()
            val formats = infoRes?.formats.orEmpty()
            val chosen: FormatItem? = when {
                explicitItag != null -> formats.firstOrNull { it.itag == explicitItag }
                preferredContainer != "any" -> formats
                    .filter { it.container.equals(preferredContainer, ignoreCase = true) }
                    .maxByOrNull { it.bitrate ?: 0L }
                    ?: formats.maxByOrNull { it.bitrate ?: 0L }
                else -> formats.maxByOrNull { it.bitrate ?: 0L }
            }
            if (chosen != null) {
                val streamUrl = resolveRelativeUrl(chosen.proxyUrl)
                    ?: chosen.itag?.let { buildDownloadStreamUrl(cleanId, it) }
                if (!streamUrl.isNullOrBlank() &&
                    verifyStreamMatchesVideoId(cleanId, streamUrl, infoRes?.video?.videoId)
                ) {
                    AppLogger.playback("Resolve", "Resolved /api/info stream for $cleanId", "itag=${chosen.itag} ${chosen.container}")
                    return@withContext ResolvedAudioStream(
                        streamUrl = streamUrl,
                        itag = chosen.itag,
                        container = chosen.container,
                        codec = chosen.codec,
                        bitrate = chosen.bitrate,
                        contentLength = chosen.contentLength,
                        isOffline = false
                    )
                }
            }
        }

        // 3. Query /api/link?v=<id>&format=audio
        try {
            val linkResp = apiService.getLink(videoIdOrUrl = cleanId, itag = explicitItag, format = "audio")
            if (linkResp.ok) {
                val audioFmt = linkResp.selected?.audio
                val streamUrl = resolveRelativeUrl(linkResp.audioUrl)
                    ?: resolveRelativeUrl(linkResp.downloadUrl)
                    ?: audioFmt?.itag?.let { buildDownloadStreamUrl(cleanId, it) }
                if (!streamUrl.isNullOrBlank() &&
                    verifyStreamMatchesVideoId(cleanId, streamUrl, linkResp.video?.videoId)
                ) {
                    AppLogger.playback("Resolve", "Resolved /api/link stream for $cleanId", "itag=${audioFmt?.itag}")
                    return@withContext ResolvedAudioStream(
                        streamUrl = streamUrl,
                        itag = audioFmt?.itag,
                        container = audioFmt?.container,
                        codec = audioFmt?.codec,
                        bitrate = audioFmt?.bitrate,
                        contentLength = audioFmt?.contentLength,
                        isOffline = false
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.warn("Resolve", "getLink failed for $cleanId", e.message)
        }

        // 4. Reconstruct download stream from saved itag if available
        if (cachedSong?.preferredItag != null) {
            val fallbackUrl = buildDownloadStreamUrl(cleanId, cachedSong.preferredItag)
            if (verifyStreamMatchesVideoId(cleanId, fallbackUrl)) {
                AppLogger.playback("Resolve", "Reconstructed fallback stream for $cleanId", "itag=${cachedSong.preferredItag}")
                return@withContext ResolvedAudioStream(
                    streamUrl = fallbackUrl,
                    itag = cachedSong.preferredItag,
                    container = cachedSong.container,
                    codec = cachedSong.codec,
                    bitrate = cachedSong.bitrate,
                    contentLength = cachedSong.contentLength,
                    isOffline = false
                )
            }
        }

        AppLogger.error("Resolve", "No playable stream resolved for $cleanId")
        null
    }

    fun selectAudioFormatForTier(
        formats: List<FormatItem>,
        qualityTier: String,
        preferredContainer: String = "any"
    ): FormatItem? {
        val audioFormats = formats.filter { it.kind == "audio" && it.itag != null }
        if (audioFormats.isEmpty()) return null
        val sortedAsc = audioFormats.sortedBy { it.bitrate ?: Long.MAX_VALUE }
        return when (qualityTier.lowercase()) {
            "low" -> sortedAsc.firstOrNull()
            "medium" -> {
                if (sortedAsc.size >= 3) {
                    sortedAsc[sortedAsc.size / 2]
                } else {
                    sortedAsc.minByOrNull { kotlin.math.abs((it.bitrate ?: 70_000L) - 70_000L) }
                }
            }
            else -> {
                val containerFiltered = if (preferredContainer != "any") {
                    audioFormats.filter { it.container.equals(preferredContainer, ignoreCase = true) }
                } else {
                    emptyList()
                }
                val pool = containerFiltered.ifEmpty { audioFormats }
                pool.sortedWith(
                    compareByDescending<FormatItem> { it.bitrate ?: 0L }
                        .thenByDescending { it.container.equals("mp4", true) }
                ).firstOrNull()
            }
        }
    }

    suspend fun downloadTrackOffline(
        song: SearchResultItem,
        explicitItag: Int? = null,
        qualityTier: String = "high",
        preferredContainer: String = "any",
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val videoId = song.videoId ?: return@withContext Result.failure(Exception("Missing track ID"))
        try {
            val info = getMediaInfo(videoId, format = "audio").getOrNull()
            val chosen: FormatItem? = if (explicitItag != null) {
                info?.formats?.firstOrNull { it.itag == explicitItag }
            } else {
                selectAudioFormatForTier(
                    formats = info?.formats.orEmpty(),
                    qualityTier = qualityTier,
                    preferredContainer = preferredContainer
                )
            }

            val targetItag = chosen?.itag
                ?: return@withContext Result.failure(Exception("No downloadable audio track available"))
            val rawContainer = chosen.container ?: "mp4"
            val fileExt = if (rawContainer.equals("mp4", true)) "m4a" else rawContainer

            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            downloadsDir.listFiles()
                ?.filter { it.isFile && it.nameWithoutExtension == videoId }
                ?.forEach { it.delete() }
            val outFile = File(downloadsDir, "$videoId.$fileExt")

            AppLogger.info("Download", "Starting offline download for $videoId", "itag=$targetItag ($fileExt)")
            val response = apiService.downloadTrack(videoIdOrUrl = videoId, itag = targetItag, dl = 1)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: HTTP ${response.code()}"))
            }
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty audio stream"))
            val total = if (body.contentLength() > 0L) body.contentLength() else (chosen.contentLength ?: -1L)

            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (total > 0L) {
                            onProgress((bytesCopied.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            val existing = musicDao.getSongById(videoId)
            val updated = (existing ?: SongEntity(
                videoId = videoId,
                title = song.title ?: "Unknown Track",
                author = song.channel ?: "Unknown Artist",
                thumbnail = song.thumbnail,
                durationSeconds = song.lengthSeconds ?: 0,
                audioUrl = buildDownloadStreamUrl(videoId, targetItag)
            )).copy(
                preferredItag = targetItag,
                container = fileExt,
                codec = chosen.codec ?: existing?.codec,
                bitrate = chosen.bitrate ?: existing?.bitrate,
                contentLength = outFile.length(),
                localFilePath = outFile.absolutePath,
                addedAt = existing?.addedAt ?: System.currentTimeMillis()
            )
            musicDao.insertSong(updated)
            onProgress(1f)
            AppLogger.info("Download", "Completed offline save for $videoId", "${outFile.length()} bytes")
            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            AppLogger.error("Download", "Offline download failed for $videoId", e.message)
            Result.failure(e)
        }
    }

    suspend fun removeOfflineDownload(videoId: String) = withContext(Dispatchers.IO) {
        val song = musicDao.getSongById(videoId)
        if (!song?.localFilePath.isNullOrBlank()) {
            runCatching { File(song!!.localFilePath!!).delete() }
            musicDao.updateLocalFilePath(videoId, null)
        }
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.filesDir, "downloads")
        if (downloadsDir.exists()) {
            downloadsDir.listFiles()?.forEach { it.delete() }
        }
    }

    // Room Flows & CRUD
    val allSavedSongs: Flow<List<SongEntity>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = musicDao.getFavoriteSongs()
    val downloadedSongs: Flow<List<SongEntity>> = musicDao.getDownloadedSongs()
    val playlistSummaries: Flow<List<PlaylistSummary>> = musicDao.getPlaylistSummaries()

    suspend fun saveSong(song: SongEntity) {
        val existing = musicDao.getSongById(song.videoId)
        val merged = if (existing != null) {
            song.copy(
                isFavorite = existing.isFavorite,
                localFilePath = existing.localFilePath ?: song.localFilePath,
                preferredItag = song.preferredItag ?: existing.preferredItag,
                container = song.container ?: existing.container,
                codec = song.codec ?: existing.codec,
                bitrate = song.bitrate ?: existing.bitrate,
                contentLength = song.contentLength ?: existing.contentLength,
                addedAt = existing.addedAt
            )
        } else {
            song
        }
        musicDao.insertSong(merged)
    }

    suspend fun toggleFavorite(song: SearchResultItem): Boolean {
        val videoId = song.videoId ?: return false
        val existing = musicDao.getSongById(videoId)
        return if (existing != null) {
            val next = !existing.isFavorite
            musicDao.updateFavorite(videoId, next)
            next
        } else {
            musicDao.insertSong(
                SongEntity(
                    videoId = videoId,
                    title = song.title ?: "Unknown Track",
                    author = song.channel ?: "Unknown Artist",
                    thumbnail = song.thumbnail,
                    durationSeconds = song.lengthSeconds ?: 0,
                    audioUrl = song.audioUrl,
                    isFavorite = true
                )
            )
            true
        }
    }

    suspend fun createPlaylist(name: String): Long = musicDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        musicDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        musicDao.clearPlaylistSongs(playlistId)
        musicDao.deletePlaylist(PlaylistEntity(id = playlistId, name = ""))
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        saveSong(song)
        musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, videoId = song.videoId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, videoId: String) {
        musicDao.removeSongFromPlaylist(playlistId, videoId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        musicDao.getSongsForPlaylist(playlistId)
}
