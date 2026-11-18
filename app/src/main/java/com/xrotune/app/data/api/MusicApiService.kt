package com.xrotune.app.data.api

import com.xrotune.app.data.model.ApiDocResponse
import com.xrotune.app.data.model.HealthResponse
import com.xrotune.app.data.model.InfoResponse
import com.xrotune.app.data.model.LinkResponse
import com.xrotune.app.data.model.MusicArtistResponse
import com.xrotune.app.data.model.MusicLyricsResponse
import com.xrotune.app.data.model.MusicSearchResponse
import com.xrotune.app.data.model.MusicShelvesResponse
import com.xrotune.app.data.model.MusicSongResponse
import com.xrotune.app.data.model.MusicTrackListResponse
import com.xrotune.app.data.model.SearchResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface MusicApiService {

    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("api/doc")
    suspend fun getApiDoc(@Query("json") json: Int = 1): ApiDocResponse

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query(value = "continuation", encoded = true) continuation: String? = null
    ): SearchResponse

    @GET("api/info")
    suspend fun getInfo(
        @Query("v") videoIdOrUrl: String,
        @Query("format") format: String? = null,
        @Query("maxHeight") maxHeight: Int? = null
    ): InfoResponse

    @GET("api/link")
    suspend fun getLink(
        @Query("v") videoIdOrUrl: String,
        @Query("itag") itag: Int? = null,
        @Query("quality") quality: String? = null,
        @Query("format") format: String? = "audio"
    ): LinkResponse

    @Streaming
    @GET("api/download")
    suspend fun downloadTrack(
        @Query("v") videoIdOrUrl: String,
        @Query("itag") itag: Int,
        @Query("dl") dl: Int = 1
    ): Response<ResponseBody>

    // ==================== /api/music/* ====================

    @GET("api/music/home")
    suspend fun getMusicHome(): MusicShelvesResponse

    @GET("api/music/trending")
    suspend fun getMusicTrending(): MusicShelvesResponse

    @GET("api/music/search")
    suspend fun searchMusic(
        @Query("q") query: String,
        @Query("type") type: String = "songs",
        @Query(value = "continuation", encoded = true) continuation: String? = null
    ): MusicSearchResponse

    @GET("api/music/artist")
    suspend fun getMusicArtist(
        @Query("id") channelId: String
    ): MusicArtistResponse

    @GET("api/music/artist/songs")
    suspend fun getMusicArtistSongs(
        @Query("id") channelId: String,
        @Query(value = "continuation", encoded = true) continuation: String? = null
    ): MusicTrackListResponse

    @GET("api/music/album")
    suspend fun getMusicAlbum(
        @Query("id") browseId: String
    ): MusicTrackListResponse

    @GET("api/music/playlist")
    suspend fun getMusicPlaylist(
        @Query("id") playlistId: String,
        @Query(value = "continuation", encoded = true) continuation: String? = null
    ): MusicTrackListResponse

    @GET("api/music/radio")
    suspend fun getMusicRadio(
        @Query("id") videoId: String,
        @Query(value = "continuation", encoded = true) continuation: String? = null
    ): MusicTrackListResponse

    @GET("api/music/song")
    suspend fun getMusicSong(
        @Query("id") videoId: String
    ): MusicSongResponse

    @GET("api/music/lyrics")
    suspend fun getMusicLyrics(
        @Query("id") videoId: String
    ): Response<MusicLyricsResponse>
}
