package com.xrotune.app.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val ok: Boolean,
    val error: String? = null,
    val query: String? = null,
    val type: String? = null,
    val count: Int? = null,
    val continuation: String? = null,
    val results: List<SearchResultItem>? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultItem(
    val kind: String? = null, // video, channel, playlist
    val videoId: String? = null,
    val title: String? = null,
    val channel: String? = null,
    val channelId: String? = null,
    val lengthSeconds: Int? = null,
    val lengthText: String? = null,
    val viewCount: Long? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val infoUrl: String? = null,
    val downloadUrl: String? = null,
    val audioUrl: String? = null,
    val linkUrl: String? = null,
    val handle: String? = null,
    val subscriberCountText: String? = null,
    val playlistId: String? = null,
    val videoCountText: String? = null,
    val browseId: String? = null
)

@JsonClass(generateAdapter = true)
data class InfoResponse(
    val ok: Boolean,
    val error: String? = null,
    val video: VideoMeta? = null,
    val hlsManifestUrl: String? = null,
    val dashManifestUrl: String? = null,
    val expiresInSeconds: Long? = null,
    val count: Int? = null,
    val formats: List<FormatItem>? = null
)

@JsonClass(generateAdapter = true)
data class VideoMeta(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val lengthSeconds: Int? = null,
    val viewCount: Long? = null
)

@JsonClass(generateAdapter = true)
data class FormatItem(
    val itag: Int? = null,
    val kind: String? = null, // video, audio, muxed
    val quality: String? = null,
    val container: String? = null,
    val codec: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val audioSampleRate: String? = null,
    val fps: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val directUrl: String? = null,
    val proxyUrl: String? = null,
    val mergeUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class LinkResponse(
    val ok: Boolean,
    val error: String? = null,
    val video: VideoMeta? = null,
    val expiresInSeconds: Long? = null,
    val selected: SelectedTracks? = null,
    val downloadUrl: String? = null,
    val directUrl: String? = null,
    val audioUrl: String? = null,
    val audioDirectUrl: String? = null,
    val mergeUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SelectedTracks(
    val video: FormatItem? = null,
    val audio: FormatItem? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val ok: Boolean,
    val error: String? = null,
    val uptimeSeconds: Long? = null,
    val client: String? = null,
    val clientVersion: String? = null,
    val musicClient: String? = null,
    val proxy: String? = null,
    val host: String? = null
)

// ==================== /api/doc?json=1 ====================

@JsonClass(generateAdapter = true)
data class ApiDocResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val name: String? = null,
    val base: String? = null,
    val conventions: List<String>? = null,
    val count: Int? = null,
    val routes: List<ApiRouteDoc>? = null,
    val recipes: List<ApiRecipeDoc>? = null
)

@JsonClass(generateAdapter = true)
data class ApiRouteDoc(
    val group: String? = null,
    val method: String? = null,
    val path: String? = null,
    val summary: String? = null,
    val params: List<ApiParamDoc>? = null,
    val example: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiParamDoc(
    val name: String? = null,
    val required: Boolean? = null,
    val desc: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiRecipeDoc(
    val title: String? = null,
    val steps: List<String>? = null
)

// ==================== /api/music/* (YouTube Music WEB_REMIX) ====================

@JsonClass(generateAdapter = true)
data class MusicItem(
    val kind: String? = null, // song, video, album, artist, playlist
    val title: String? = null,
    val subtitle: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val playlistId: String? = null,
    val duration: String? = null,
    val durationSeconds: Int? = null,
    val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicShelf(
    val title: String? = null,
    val items: List<MusicItem>? = null
)

@JsonClass(generateAdapter = true)
data class MusicShelvesResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val count: Int? = null,
    val shelves: List<MusicShelf>? = null
)

@JsonClass(generateAdapter = true)
data class MusicSearchResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val clientVersion: String? = null,
    val host: String? = null,
    val query: String? = null,
    val type: String? = null,
    val count: Int? = null,
    val results: List<MusicItem>? = null,
    val continuation: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicArtistInfo(
    val title: String? = null,
    val description: String? = null,
    val subscribers: String? = null,
    val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicArtistResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val artist: MusicArtistInfo? = null,
    val topSongs: List<MusicItem>? = null,
    val albums: List<MusicItem>? = null,
    val singles: List<MusicItem>? = null,
    val videos: List<MusicItem>? = null,
    val related: List<MusicItem>? = null,
    val shelves: List<MusicShelf>? = null
)

@JsonClass(generateAdapter = true)
data class MusicCollectionHeader(
    val title: String? = null,
    val subtitle: String? = null,
    val secondSubtitle: String? = null,
    val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicTrackListResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val header: MusicCollectionHeader? = null,
    val items: List<MusicItem>? = null,
    val continuation: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicSongMeta(
    val videoId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val lengthSeconds: Int? = null,
    val viewCount: Long? = null,
    val thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicSongLinks(
    val info: String? = null,
    val download: String? = null
)

@JsonClass(generateAdapter = true)
data class MusicSongResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val song: MusicSongMeta? = null,
    val expiresInSeconds: Long? = null,
    val formatCount: Int? = null,
    val links: MusicSongLinks? = null
)

@JsonClass(generateAdapter = true)
data class MusicLyricsResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val client: String? = null,
    val lyrics: String? = null,
    val browseId: String? = null
)
