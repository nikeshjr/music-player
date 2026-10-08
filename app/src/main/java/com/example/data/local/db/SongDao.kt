package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

/** Lightweight projection used to diff the database against a fresh MediaStore scan. */
data class SongPathRow(val id: Long, val path: String)

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 50): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int = 50): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 50): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getSongByMediaStoreId(mediaStoreId: Long): Song?

    @Query("SELECT * FROM songs WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun getSongsByMediaStoreIds(mediaStoreIds: List<Long>): List<Song>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM songs WHERE album = :albumTitle ORDER BY trackNumber ASC, title ASC")
    fun getSongsByAlbum(albumTitle: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artistName ORDER BY album ASC, trackNumber ASC")
    fun getSongsByArtist(artistName: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE id = :id")
    suspend fun recordPlay(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET skipCount = skipCount + 1 WHERE id = :id")
    suspend fun recordSkip(id: Long)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: Long)

    // MediaStore rows only (positive ids); SAF songs use negative ids and are never pruned.
    @Query("SELECT id, path FROM songs WHERE mediaStoreId > 0")
    suspend fun getMediaStoreRows(): List<SongPathRow>

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs WHERE path IN (:paths) AND mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteByPathAndMediaStoreId(paths: List<String>, mediaStoreIds: List<Long>)
}
