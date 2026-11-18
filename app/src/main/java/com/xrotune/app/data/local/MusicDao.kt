package com.xrotune.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    @Query("SELECT * FROM songs ORDER BY lastPlayedAt DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE localFilePath IS NOT NULL ORDER BY addedAt DESC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE videoId = :videoId")
    suspend fun getSongById(videoId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE videoId = :videoId")
    suspend fun updateFavorite(videoId: String, isFavorite: Boolean)

    @Query("UPDATE songs SET localFilePath = :localPath WHERE videoId = :videoId")
    suspend fun updateLocalFilePath(videoId: String, localPath: String?)

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE videoId = :videoId")
    suspend fun updateLastPlayed(videoId: String, timestamp: Long)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query(
        """
        SELECT p.id, p.name, p.createdAt,
               COUNT(ref.videoId) AS songCount,
               (SELECT s.thumbnail FROM songs s
                INNER JOIN playlist_song_cross_ref r2 ON s.videoId = r2.videoId
                WHERE r2.playlistId = p.id
                ORDER BY r2.addedAt DESC LIMIT 1) AS coverThumbnail
        FROM playlists p
        LEFT JOIN playlist_song_cross_ref ref ON p.id = ref.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun getPlaylistSummaries(): Flow<List<PlaylistSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeSongFromPlaylist(playlistId: Long, videoId: String)

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref ref ON s.videoId = ref.videoId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.addedAt ASC
        """
    )
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>
}
