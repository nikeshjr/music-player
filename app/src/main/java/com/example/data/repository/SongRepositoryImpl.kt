package com.example.data.repository

import com.example.data.local.db.SongDao
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Song
import com.example.data.scanner.MediaStoreScanner
import com.example.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SongRepositoryImpl: Implements audio library queries, group aggregations for
 * Albums/Artists, and synchronizes with MediaStore.
 */
class SongRepositoryImpl(
    private val songDao: SongDao,
    private val mediaStoreScanner: MediaStoreScanner
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()

    override fun getRecentlyAddedSongs(limit: Int): Flow<List<Song>> =
        songDao.getRecentlyAddedSongs(limit)

    override fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>> =
        songDao.getRecentlyPlayedSongs(limit)

    override fun getMostPlayedSongs(limit: Int): Flow<List<Song>> =
        songDao.getMostPlayedSongs(limit)

    override fun getSongsByAlbum(album: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(album)

    override fun getSongsByArtist(artist: String): Flow<List<Song>> =
        songDao.getSongsByArtist(artist)

    override fun getSongsByGenre(genre: String): Flow<List<Song>> =
        songDao.getSongsByGenre(genre)

    override fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query)

    override fun getAlbums(): Flow<List<Album>> {
        return songDao.getAllSongs().map { songs ->
            songs.groupBy { it.album }
                .map { (albumTitle, songList) ->
                    val first = songList.first()
                    Album(
                        id = first.albumId,
                        title = albumTitle,
                        artist = first.artist,
                        songCount = songList.size,
                        year = songList.maxOfOrNull { it.year } ?: 0
                    )
                }
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun getArtists(): Flow<List<Artist>> {
        return songDao.getAllSongs().map { songs ->
            songs.groupBy { it.artist }
                .map { (artistName, songList) ->
                    val albumsCount = songList.map { it.album }.distinct().size
                    val first = songList.first()
                    Artist(
                        id = first.artistId,
                        name = artistName,
                        songCount = songList.size,
                        albumCount = albumsCount
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    override suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    override suspend fun toggleFavorite(songId: Long, currentIsFavorite: Boolean) {
        songDao.updateFavorite(songId, !currentIsFavorite)
    }

    override suspend fun recordPlay(songId: Long) {
        songDao.recordPlay(songId)
    }

    override suspend fun recordSkip(songId: Long) {
        songDao.recordSkip(songId)
    }

    override suspend fun deleteSong(songId: Long) {
        songDao.deleteSong(songId)
    }

    override suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }

    override suspend fun scanMediaStore(minDurationSeconds: Int): Int {
        return mediaStoreScanner.scanAudioFiles(minDurationSeconds)
    }
}
