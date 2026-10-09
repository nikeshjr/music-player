package com.example.data.local.db

import com.example.data.model.Song

private const val SQL_CHUNK = 500 // stays well under SQLite's bound-variable limit

/**
 * Inserts [songs] without losing user data. Rows that already exist (matched by mediaStoreId)
 * keep their database id, favorite flag and play statistics, so a rescan never resets them
 * or orphans playlist entries. Returns the stored rows with their real database ids.
 */
suspend fun SongDao.upsertPreservingStats(songs: List<Song>): List<Song> {
    if (songs.isEmpty()) return emptyList()

    val existing = songs.map { it.mediaStoreId }.chunked(SQL_CHUNK)
        .flatMap { getSongsByMediaStoreIds(it) }
        .associateBy { it.mediaStoreId }

    val merged = songs.map { song ->
        val old = existing[song.mediaStoreId] ?: return@map song
        song.copy(
            id = old.id,
            isFavorite = old.isFavorite,
            playCount = old.playCount,
            skipCount = old.skipCount,
            lastPlayedTime = old.lastPlayedTime
        )
    }
    insertSongs(merged)

    val stored = merged.map { it.mediaStoreId }.chunked(SQL_CHUNK)
        .flatMap { getSongsByMediaStoreIds(it) }
        .associateBy { it.mediaStoreId }
    return merged.mapNotNull { stored[it.mediaStoreId] }
}

/**
 * Removes MediaStore rows whose file is no longer present. Diffed in memory and deleted in
 * chunks, so it works for libraries of any size. SAF songs (negative ids) are never touched.
 */
suspend fun SongDao.pruneMissingMediaStoreSongs(existingPaths: Set<String>): Int {
    val staleIds = getMediaStoreRows()
        .filter { it.path !in existingPaths }
        .map { it.id }
    staleIds.chunked(SQL_CHUNK).forEach { deleteSongsByIds(it) }
    return staleIds.size
}
