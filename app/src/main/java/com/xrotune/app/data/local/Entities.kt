package com.xrotune.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnail: String? = null,
    val durationSeconds: Int = 0,
    val preferredItag: Int? = null,
    val container: String? = null,
    val codec: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val audioUrl: String? = null,
    val localFilePath: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "videoId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["videoId"])
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val videoId: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int,
    val coverThumbnail: String?
)
