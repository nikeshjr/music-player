package com.example.domain.repository

import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * SongRepository: Domain abstraction for retrieving and managing the local audio library.
 */
interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getRecentlyAddedSongs(limit: Int = 50): Flow<List<Song>>
    fun getRecentlyPlayedSongs(limit: Int = 50): Flow<List<Song>>
    fun getMostPlayedSongs(limit: Int = 50): Flow<List<Song>>
    fun getSongsByAlbum(album: String): Flow<List<Song>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getSongsByGenre(genre: String): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>

    fun getAlbums(): Flow<List<Album>>
    fun getArtists(): Flow<List<Artist>>

    suspend fun getSongById(id: Long): Song?
    suspend fun toggleFavorite(songId: Long, currentIsFavorite: Boolean)
    suspend fun recordPlay(songId: Long)
    suspend fun recordSkip(songId: Long)
    suspend fun deleteSong(songId: Long)
    suspend fun insertSongs(songs: List<Song>)

    // Scanner
    suspend fun scanMediaStore(minDurationSeconds: Int): Int
}
