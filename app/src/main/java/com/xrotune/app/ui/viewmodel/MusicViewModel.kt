package com.xrotune.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xrotune.app.data.local.MusicDatabase
import com.xrotune.app.data.local.PlaylistSummary
import com.xrotune.app.data.local.SongEntity
import com.xrotune.app.data.local.XroTuneDocument
import com.xrotune.app.data.model.ApiDocResponse
import com.xrotune.app.data.model.FormatItem
import com.xrotune.app.data.model.HealthResponse
import com.xrotune.app.data.model.InfoResponse
import com.xrotune.app.data.model.MusicArtistResponse
import com.xrotune.app.data.model.MusicCollectionHeader
import com.xrotune.app.data.model.MusicItem
import com.xrotune.app.data.model.MusicShelf
import com.xrotune.app.data.model.MusicSongResponse
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.data.preferences.UserPreferencesRepository
import com.xrotune.app.data.preferences.UserSettings
import com.xrotune.app.data.repository.MusicRepository
import com.xrotune.app.player.MusicPlayerManager
import com.xrotune.app.player.RepeatMode
import com.xrotune.app.ui.util.toSearchResultItem
import com.xrotune.app.util.AppLogEntry
import com.xrotune.app.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CollectionTarget {
    data class Artist(val browseId: String, val fallbackTitle: String) : CollectionTarget()
    data class Album(val browseId: String, val fallbackTitle: String) : CollectionTarget()
    data class RemotePlaylist(val playlistId: String, val fallbackTitle: String) : CollectionTarget()
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MusicDatabase.getDatabase(application)
    private val prefsRepo = UserPreferencesRepository.getInstance(application)
    val repository = MusicRepository(application, db.musicDao())
    val playerManager = MusicPlayerManager(application)

    val userSettings: StateFlow<UserSettings> = prefsRepo.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val xroTuneDocument: StateFlow<XroTuneDocument> = prefsRepo.xroTuneDocument
    val appLogs: StateFlow<List<AppLogEntry>> = AppLogger.logs

    // ==================== Backend Health & /api/doc ====================
    var healthInfo by mutableStateOf<HealthResponse?>(null)
        private set
    var isLoadingHealth by mutableStateOf(false)
        private set
    var apiDoc by mutableStateOf<ApiDocResponse?>(null)
        private set
    var isLoadingApiDoc by mutableStateOf(false)
        private set
    var apiDocError by mutableStateOf<String?>(null)
        private set
    var isServiceReachable by mutableStateOf(true)
        private set

    // ==================== Home Feed & /api/music/home + trending ====================
    var homeFeedMode by mutableStateOf("home") // home, trending, tracks
        private set
    var musicHomeShelves by mutableStateOf<List<MusicShelf>>(emptyList())
        private set
    var musicTrendingShelves by mutableStateOf<List<MusicShelf>>(emptyList())
        private set
    var isLoadingMusicShelves by mutableStateOf(false)
        private set
    var musicShelvesError by mutableStateOf<String?>(null)
        private set

    var homeFeedQuery by mutableStateOf("trending music")
        private set
    var homeResults by mutableStateOf<List<SearchResultItem>>(emptyList())
        private set
    var isLoadingHome by mutableStateOf(false)
        private set
    var homeError by mutableStateOf<String?>(null)
        private set

    // ==================== Search (/api/music/search + /api/search) ====================
    var searchQuery by mutableStateOf("")
    // Music modes: songs, videos, albums, artists, playlists; plus general YouTube: yt_video, yt_channel
    var searchType by mutableStateOf("songs")
        private set
    var musicSearchResults by mutableStateOf<List<MusicItem>>(emptyList())
        private set
    var searchResults by mutableStateOf<List<SearchResultItem>>(emptyList())
        private set
    var continuationToken by mutableStateOf<String?>(null)
        private set
    var isSearching by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    // ==================== Artist / Album / Playlist Drill-Down Sheet ====================
    var activeCollectionTarget by mutableStateOf<CollectionTarget?>(null)
        private set
    var isLoadingCollection by mutableStateOf(false)
        private set
    var collectionError by mutableStateOf<String?>(null)
        private set
    var artistDetail by mutableStateOf<MusicArtistResponse?>(null)
        private set
    var collectionHeader by mutableStateOf<MusicCollectionHeader?>(null)
        private set
    var collectionTracks by mutableStateOf<List<MusicItem>>(emptyList())
        private set
    var collectionContinuation by mutableStateOf<String?>(null)
        private set
    var isLoadingMoreCollection by mutableStateOf(false)
        private set

    // ==================== Now Playing Lyrics, Song Meta & Radio ====================
    var showNowPlayingSheet by mutableStateOf(false)
    var activeLyricsVideoId by mutableStateOf<String?>(null)
        private set
    var lyricsText by mutableStateOf<String?>(null)
        private set
    var isLoadingLyrics by mutableStateOf(false)
        private set
    var lyricsError by mutableStateOf<String?>(null)
        private set
    var currentSongDetail by mutableStateOf<MusicSongResponse?>(null)
        private set
    var radioTracks by mutableStateOf<List<MusicItem>>(emptyList())
        private set
    var isLoadingRadio by mutableStateOf(false)
        private set
    var radioError by mutableStateOf<String?>(null)
        private set

    // ==================== Format Inspector & Download Quality ====================
    var inspectedInfo by mutableStateOf<InfoResponse?>(null)
        private set
    var isLoadingInfo by mutableStateOf(false)
        private set
    var infoError by mutableStateOf<String?>(null)
        private set
    var showInfoSheet by mutableStateOf(false)
        private set

    var pendingOpenPlaylist by mutableStateOf<Pair<Long, String>?>(null)
        private set
    var shouldRequestNotificationPermission by mutableStateOf(false)
    private var promptedNotificationThisSession = false

    var pendingDownloadSong by mutableStateOf<SearchResultItem?>(null)
        private set
    var downloadQualityFormats by mutableStateOf<List<FormatItem>>(emptyList())
        private set
    var isLoadingDownloadQualities by mutableStateOf(false)
        private set
    var downloadQualityError by mutableStateOf<String?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    val downloadProgress = mutableStateMapOf<String, Float>()

    val playlistSummaries: StateFlow<List<PlaylistSummary>> = repository.playlistSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<SongEntity>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSongs: StateFlow<List<SongEntity>> = repository.allSavedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchDebounceJob: Job? = null

    init {
        AppLogger.info("App", "XroTune initialized (${MusicRepository.API_BASE_URL})")
        viewModelScope.launch {
            val initial = prefsRepo.userSettings.first()
            playerManager.setPlaybackModes(
                shuffle = initial.shuffleEnabled,
                repeat = RepeatMode.entries.getOrElse(initial.repeatMode) { RepeatMode.OFF }
            )
            launch { refreshHealthAndDocs() }
            launch { loadMusicHomeAndTrending() }
            launch { loadHomeFeed(homeFeedQuery) }
            launch { performSearch(query = "top hits", type = searchType) }
        }
    }

    fun refreshHealthAndDocs() {
        viewModelScope.launch {
            isLoadingHealth = true
            repository.getHealthInfo()
                .onSuccess { h ->
                    healthInfo = h
                    isServiceReachable = h.ok
                }
                .onFailure {
                    isServiceReachable = false
                }
            isLoadingHealth = false
        }
        viewModelScope.launch {
            isLoadingApiDoc = true
            apiDocError = null
            repository.getApiDocumentation()
                .onSuccess { doc -> apiDoc = doc }
                .onFailure { err -> apiDocError = err.message ?: "Failed to load /api/doc" }
            isLoadingApiDoc = false
        }
    }

    fun clearLogs() {
        AppLogger.clear()
        AppLogger.info("Logs", "Log buffer cleared by user")
    }

    fun getFormattedLogs(): String = AppLogger.formatAllForExport()

    fun setHomeMode(mode: String) {
        homeFeedMode = mode
        if (mode == "home" && musicHomeShelves.isEmpty()) {
            loadMusicHomeAndTrending()
        } else if (mode == "trending" && musicTrendingShelves.isEmpty()) {
            loadMusicHomeAndTrending()
        }
    }

    fun loadMusicHomeAndTrending() {
        viewModelScope.launch {
            isLoadingMusicShelves = true
            musicShelvesError = null
            val homeRes = repository.getMusicHomeShelves()
            homeRes.onSuccess { shelves ->
                musicHomeShelves = shelves
                isServiceReachable = true
            }.onFailure { err ->
                musicShelvesError = err.message
            }
            repository.getMusicTrendingShelves().onSuccess { shelves ->
                musicTrendingShelves = shelves
            }
            isLoadingMusicShelves = false
        }
    }

    fun requestOpenPlaylist(playlistId: Long, name: String) {
        pendingOpenPlaylist = playlistId to name
    }

    fun consumePendingOpenPlaylist() {
        pendingOpenPlaylist = null
    }

    fun clearStatusMessage() {
        statusMessage = null
    }

    fun showMessage(msg: String) {
        statusMessage = msg
    }

    fun setPreferredContainer(container: String) {
        viewModelScope.launch { prefsRepo.setPreferredContainer(container) }
    }

    fun setPreferredDownloadQuality(quality: String) {
        viewModelScope.launch { prefsRepo.setPreferredDownloadQuality(quality) }
    }

    fun setAskDownloadQuality(ask: Boolean) {
        viewModelScope.launch { prefsRepo.setAskDownloadQuality(ask) }
    }

    fun updateUserProfile(
        displayName: String,
        bio: String,
        favoriteGenre: String,
        avatarColorIndex: Int
    ) {
        prefsRepo.updateUserProfile(displayName, bio, favoriteGenre, avatarColorIndex)
        statusMessage = "Profile saved to .xrotune"
    }

    fun getXroTuneFileSizeBytes(): Long = prefsRepo.getXroTuneFileSizeBytes()

    fun exportXroTuneFile(uri: Uri) {
        viewModelScope.launch {
            prefsRepo.exportXroTuneToUri(uri).onSuccess {
                statusMessage = "Exported .xrotune backup successfully"
            }.onFailure { err ->
                statusMessage = "Export failed: ${err.message}"
            }
        }
    }

    fun importXroTuneFile(uri: Uri) {
        viewModelScope.launch {
            prefsRepo.importXroTuneFromUri(uri).onSuccess {
                statusMessage = "Restored profile & settings from .xrotune"
            }.onFailure { err ->
                statusMessage = "Import failed: ${err.message}"
            }
        }
    }

    fun refreshHomeFeed(query: String = homeFeedQuery) {
        loadHomeFeed(query)
        loadMusicHomeAndTrending()
    }

    private fun loadHomeFeed(query: String) {
        homeFeedQuery = query
        viewModelScope.launch {
            isLoadingHome = true
            homeError = null
            val page = repository.searchMedia(query = query, type = "video", continuation = null)
            homeResults = page.results
            homeError = page.error
            if (page.error == null) isServiceReachable = true
            isLoadingHome = false
        }
    }

    // ==================== Search (/api/music/search + /api/search) ====================

    private fun isYouTubeMusicSearchType(type: String): Boolean {
        return type in setOf("songs", "videos", "albums", "artists", "playlists")
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery = newQuery
        searchDebounceJob?.cancel()
        if (repository.looksLikeMediaUrlOrId(newQuery)) {
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(450)
            performSearch(query = newQuery, type = searchType)
        }
    }

    fun setSearchFilterType(type: String) {
        if (searchType != type) {
            searchType = type
            performSearch(query = searchQuery, type = type)
        }
    }

    fun submitSearchOrUrl() {
        searchDebounceJob?.cancel()
        val trimmed = searchQuery.trim()
        if (repository.looksLikeMediaUrlOrId(trimmed)) {
            openFormatInspector(trimmed)
        } else {
            performSearch(query = trimmed, type = searchType)
        }
    }

    fun searchByKeyword(keyword: String, type: String = "songs") {
        searchDebounceJob?.cancel()
        searchQuery = keyword
        searchType = type
        performSearch(query = keyword, type = type)
    }

    fun retrySearch() {
        performSearch(query = searchQuery, type = searchType)
    }

    private fun performSearch(query: String, type: String) {
        viewModelScope.launch {
            isSearching = true
            searchError = null
            if (isYouTubeMusicSearchType(type)) {
                val page = repository.searchMusicCatalog(query = query, type = type, continuation = null)
                musicSearchResults = page.results
                searchResults = page.results.map { it.toSearchResultItem() }
                continuationToken = page.continuation
                searchError = page.error
                if (page.error == null) isServiceReachable = true
            } else {
                val apiType = when (type) {
                    "yt_channel" -> "channel"
                    else -> "video"
                }
                val page = repository.searchMedia(query = query, type = apiType, continuation = null)
                musicSearchResults = emptyList()
                searchResults = page.results
                continuationToken = page.continuation
                searchError = page.error
                if (page.error == null) isServiceReachable = true
            }
            isSearching = false
        }
    }

    fun loadMoreSearchResults() {
        val token = continuationToken ?: return
        if (isLoadingMore) return
        viewModelScope.launch {
            isLoadingMore = true
            if (isYouTubeMusicSearchType(searchType)) {
                val page = repository.searchMusicCatalog(query = searchQuery, type = searchType, continuation = token)
                if (page.results.isNotEmpty()) {
                    val existingIds = musicSearchResults.mapNotNull { it.videoId ?: it.browseId ?: it.playlistId }.toSet()
                    val fresh = page.results.filter { item ->
                        val id = item.videoId ?: item.browseId ?: item.playlistId
                        id == null || id !in existingIds
                    }
                    musicSearchResults = musicSearchResults + fresh
                    searchResults = musicSearchResults.map { it.toSearchResultItem() }
                }
                continuationToken = page.continuation
            } else {
                val apiType = when (searchType) {
                    "yt_channel" -> "channel"
                    else -> "video"
                }
                val page = repository.searchMedia(query = searchQuery, type = apiType, continuation = token)
                if (page.results.isNotEmpty()) {
                    val existingIds = searchResults.mapNotNull { it.videoId ?: it.channelId ?: it.playlistId }.toSet()
                    val fresh = page.results.filter { item ->
                        val id = item.videoId ?: item.channelId ?: item.playlistId
                        id == null || id !in existingIds
                    }
                    searchResults = searchResults + fresh
                }
                continuationToken = page.continuation
            }
            isLoadingMore = false
        }
    }

    // ==================== Interactive Artist / Album / Playlist Explorer ====================

    fun openMusicItem(item: MusicItem, shelfContext: List<MusicItem> = emptyList()) {
        val kind = item.kind?.lowercase()
        val videoId = item.videoId?.trim()?.takeIf { it.isNotBlank() }
        val browseId = item.browseId?.trim()?.takeIf { it.isNotBlank() }
        val playlistId = item.playlistId?.trim()?.takeIf { it.isNotBlank() }

        when {
            (kind == "song" || kind == "video" || kind == null) && videoId != null -> {
                val queueItems = shelfContext
                    .filter { !it.videoId.isNullOrBlank() }
                    .map { it.toSearchResultItem() }
                    .ifEmpty { listOf(item.toSearchResultItem()) }
                playTrack(item.toSearchResultItem(), queueItems, openPlayerSheet = true)
            }
            kind == "artist" && browseId != null -> {
                openCollection(CollectionTarget.Artist(browseId, item.title ?: "Artist"))
            }
            kind == "album" && browseId != null -> {
                openCollection(CollectionTarget.Album(browseId, item.title ?: "Album"))
            }
            kind == "playlist" && (playlistId != null || browseId != null) -> {
                val targetId = playlistId ?: browseId!!
                openCollection(CollectionTarget.RemotePlaylist(targetId, item.title ?: "Playlist"))
            }
            videoId != null -> {
                playTrack(item.toSearchResultItem(), openPlayerSheet = true)
            }
        }
    }

    fun openCollection(target: CollectionTarget) {
        activeCollectionTarget = target
        isLoadingCollection = true
        collectionError = null
        artistDetail = null
        collectionHeader = null
        collectionTracks = emptyList()
        collectionContinuation = null

        viewModelScope.launch {
            when (target) {
                is CollectionTarget.Artist -> {
                    repository.getMusicArtist(target.browseId)
                        .onSuccess { artistResp ->
                            artistDetail = artistResp
                            collectionTracks = artistResp.topSongs.orEmpty()
                        }
                        .onFailure { err ->
                            collectionError = err.message ?: "Unable to load artist"
                        }
                }
                is CollectionTarget.Album -> {
                    repository.getMusicAlbum(target.browseId)
                        .onSuccess { albumResp ->
                            collectionHeader = albumResp.header
                            collectionTracks = albumResp.items.orEmpty()
                            collectionContinuation = albumResp.continuation
                        }
                        .onFailure { err ->
                            collectionError = err.message ?: "Unable to load album"
                        }
                }
                is CollectionTarget.RemotePlaylist -> {
                    repository.getMusicPlaylist(target.playlistId)
                        .onSuccess { plResp ->
                            collectionHeader = plResp.header
                            collectionTracks = plResp.items.orEmpty()
                            collectionContinuation = plResp.continuation
                        }
                        .onFailure { err ->
                            collectionError = err.message ?: "Unable to load playlist"
                        }
                }
            }
            isLoadingCollection = false
        }
    }

    fun loadAllArtistSongs(channelId: String) {
        viewModelScope.launch {
            isLoadingMoreCollection = true
            repository.getMusicArtistSongs(channelId)
                .onSuccess { resp ->
                    val merged = (collectionTracks + resp.items.orEmpty())
                        .distinctBy { it.videoId ?: "${it.title}_${it.subtitle}" }
                    collectionTracks = merged
                    collectionContinuation = resp.continuation
                }
                .onFailure { err ->
                    statusMessage = err.message ?: "Could not load additional artist tracks"
                }
            isLoadingMoreCollection = false
        }
    }

    fun loadMoreCollectionTracks() {
        val target = activeCollectionTarget ?: return
        val token = collectionContinuation ?: return
        if (isLoadingMoreCollection) return
        viewModelScope.launch {
            isLoadingMoreCollection = true
            val res = when (target) {
                is CollectionTarget.Artist -> repository.getMusicArtistSongs(target.browseId, token)
                is CollectionTarget.RemotePlaylist -> repository.getMusicPlaylist(target.playlistId, token)
                is CollectionTarget.Album -> Result.failure(Exception("No pagination for albums"))
            }
            res.onSuccess { resp ->
                val fresh = resp.items.orEmpty()
                val existing = collectionTracks.mapNotNull { it.videoId }.toSet()
                collectionTracks = collectionTracks + fresh.filter { it.videoId == null || it.videoId !in existing }
                collectionContinuation = resp.continuation
            }
            isLoadingMoreCollection = false
        }
    }

    fun closeCollectionSheet() {
        activeCollectionTarget = null
    }

    // ==================== Now Playing Lyrics, Song Meta & Radio ====================

    fun loadLyricsAndDetailsForCurrentSong(videoId: String, forceRefresh: Boolean = false) {
        val cleanId = videoId.trim()
        if (cleanId.isBlank()) return
        if (!forceRefresh && activeLyricsVideoId == cleanId && (lyricsText != null || lyricsError != null)) {
            return
        }
        activeLyricsVideoId = cleanId
        isLoadingLyrics = true
        lyricsError = null
        lyricsText = null
        currentSongDetail = null

        viewModelScope.launch {
            val lyricsJob = launch {
                repository.getMusicLyrics(cleanId)
                    .onSuccess { resp ->
                        if (activeLyricsVideoId == cleanId) {
                            lyricsText = resp.lyrics
                            lyricsError = if (resp.lyrics.isNullOrBlank()) "Lyrics unavailable for this track" else null
                        }
                    }
                    .onFailure { err ->
                        if (activeLyricsVideoId == cleanId) {
                            lyricsError = err.message ?: "Lyrics unavailable"
                        }
                    }
                if (activeLyricsVideoId == cleanId) {
                    isLoadingLyrics = false
                }
            }
            launch {
                repository.getMusicSongMeta(cleanId).onSuccess { detail ->
                    if (activeLyricsVideoId == cleanId) {
                        currentSongDetail = detail
                    }
                }
            }
            lyricsJob.join()
        }
    }

    fun loadRadioForTrack(videoId: String) {
        val cleanId = videoId.trim()
        if (cleanId.isBlank()) return
        isLoadingRadio = true
        radioError = null
        viewModelScope.launch {
            repository.getMusicRadio(cleanId)
                .onSuccess { resp ->
                    radioTracks = resp.items.orEmpty()
                    if (radioTracks.isEmpty()) {
                        radioError = "No radio stations returned for this track"
                    }
                }
                .onFailure { err ->
                    radioError = err.message ?: "Failed to load radio mix"
                }
            isLoadingRadio = false
        }
    }

    // ==================== Format Inspector & Playback ====================

    fun openFormatInspector(videoIdOrUrl: String) {
        showInfoSheet = true
        isLoadingInfo = true
        infoError = null
        inspectedInfo = null
        viewModelScope.launch {
            val res = repository.getMediaInfo(videoIdOrUrl)
            res.onSuccess { info ->
                inspectedInfo = info
            }.onFailure { err ->
                infoError = err.message ?: "Unable to load media formats. Please check your internet connection."
            }
            isLoadingInfo = false
        }
    }

    fun closeFormatInspector() {
        showInfoSheet = false
    }

    fun playTrack(
        song: SearchResultItem,
        playlist: List<SearchResultItem>? = null,
        explicitItag: Int? = null,
        openPlayerSheet: Boolean = false
    ) {
        val vid = song.videoId?.trim()?.takeIf { it.isNotBlank() } ?: return
        if (!promptedNotificationThisSession) {
            promptedNotificationThisSession = true
            shouldRequestNotificationPermission = true
        }
        if (openPlayerSheet) {
            showNowPlayingSheet = true
        }
        playerManager.playTrack(song, playlist, explicitItag)
        loadLyricsAndDetailsForCurrentSong(vid)
    }

    fun stopAndDismissPlayer() {
        showNowPlayingSheet = false
        playerManager.stopAndClear()
    }

    fun toggleFavorite(song: SearchResultItem) {
        viewModelScope.launch {
            val nowFav = repository.toggleFavorite(song)
            statusMessage = if (nowFav) "Added to Liked Songs" else "Removed from Liked Songs"
        }
    }

    fun requestDownloadTrack(song: SearchResultItem) {
        val videoId = song.videoId ?: return
        if (downloadProgress.containsKey(videoId)) return
        val currentSettings = userSettings.value
        if (!currentSettings.askDownloadQuality) {
            downloadTrackOffline(
                song = song,
                qualityTier = currentSettings.preferredDownloadQuality,
                preferredContainer = currentSettings.preferredContainer
            )
            return
        }

        pendingDownloadSong = song
        downloadQualityFormats = emptyList()
        downloadQualityError = null
        isLoadingDownloadQualities = true

        viewModelScope.launch {
            val res = repository.getMediaInfo(videoId, format = "audio")
            res.onSuccess { info ->
                downloadQualityFormats = info.formats
                    .orEmpty()
                    .filter { it.kind == "audio" && it.itag != null }
                    .sortedByDescending { it.bitrate ?: 0L }
            }.onFailure { err ->
                downloadQualityError = err.message ?: "Unable to fetch exact formats"
            }
            isLoadingDownloadQualities = false
        }
    }

    fun dismissDownloadQualityPicker() {
        pendingDownloadSong = null
        downloadQualityFormats = emptyList()
        downloadQualityError = null
        isLoadingDownloadQualities = false
    }

    fun confirmDownloadWithQuality(
        song: SearchResultItem,
        selectedFormat: FormatItem?,
        qualityTier: String,
        askEveryTime: Boolean
    ) {
        pendingDownloadSong = null
        viewModelScope.launch {
            prefsRepo.setPreferredDownloadQuality(qualityTier)
            prefsRepo.setAskDownloadQuality(askEveryTime)
        }
        downloadTrackOffline(
            song = song,
            explicitItag = selectedFormat?.itag,
            qualityTier = qualityTier,
            preferredContainer = userSettings.value.preferredContainer
        )
    }

    fun downloadTrackOffline(
        song: SearchResultItem,
        explicitItag: Int? = null,
        qualityTier: String = userSettings.value.preferredDownloadQuality,
        preferredContainer: String = userSettings.value.preferredContainer
    ) {
        val videoId = song.videoId ?: return
        if (downloadProgress.containsKey(videoId)) return
        downloadProgress[videoId] = 0.01f
        viewModelScope.launch {
            val res = repository.downloadTrackOffline(
                song = song,
                explicitItag = explicitItag,
                qualityTier = qualityTier,
                preferredContainer = preferredContainer
            ) { progress ->
                downloadProgress[videoId] = progress
            }
            downloadProgress.remove(videoId)
            res.onSuccess {
                statusMessage = "Downloaded \"${song.title ?: "track"}\" for offline playback"
            }.onFailure { err ->
                statusMessage = "Download failed: ${err.message}"
            }
        }
    }

    fun removeOfflineDownload(videoId: String) {
        viewModelScope.launch {
            repository.removeOfflineDownload(videoId)
            statusMessage = "Removed offline download"
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val currentDownloads = downloadedSongs.value
            currentDownloads.forEach { repository.removeOfflineDownload(it.videoId) }
            repository.clearAllDownloads()
            statusMessage = "Cleared all offline audio downloads"
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name.trim())
                statusMessage = "Created playlist \"${name.trim()}\""
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, song: SearchResultItem) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = repository.createPlaylist(name.trim())
                val videoId = song.videoId ?: return@launch
                val entity = SongEntity(
                    videoId = videoId,
                    title = song.title ?: "Unknown Track",
                    author = song.channel ?: "Unknown Artist",
                    thumbnail = song.thumbnail,
                    durationSeconds = song.lengthSeconds ?: 0,
                    audioUrl = song.audioUrl
                )
                repository.addSongToPlaylist(newId, entity)
                statusMessage = "Added to new playlist \"${name.trim()}\""
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.renamePlaylist(playlistId, newName.trim())
                statusMessage = "Renamed playlist"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            statusMessage = "Deleted playlist"
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: SearchResultItem) {
        viewModelScope.launch {
            val videoId = song.videoId ?: return@launch
            val entity = SongEntity(
                videoId = videoId,
                title = song.title ?: "Unknown Track",
                author = song.channel ?: "Unknown Artist",
                thumbnail = song.thumbnail,
                durationSeconds = song.lengthSeconds ?: 0,
                audioUrl = song.audioUrl
            )
            repository.addSongToPlaylist(playlistId, entity)
            statusMessage = "Added to playlist"
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, videoId)
            statusMessage = "Removed from playlist"
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        repository.getSongsForPlaylist(playlistId)

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
