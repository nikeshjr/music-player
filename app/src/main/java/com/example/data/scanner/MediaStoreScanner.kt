package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.core.logger.AuraLog
import com.example.data.local.db.SongDao
import com.example.data.metadata.ArtistSplitter
import com.example.data.metadata.FlacVorbisCommentParser
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaStoreScanner: Comprehensive audio library scanner.
 * Scans Android MediaStore across internal and external SD/USB volumes.
 * Treats FLAC as first-class by dual MIME and file-extension matching, invoking our pure Kotlin
 * FlacVorbisCommentParser to extract exact bit-depth (16/24-bit), sample rate (up to 192 kHz),
 * ReplayGain tags, and embedded lyrics. Applies ArtistSplitter for collaborative credits.
 */
class MediaStoreScanner(
    private val context: Context,
    private val songDao: SongDao
) {

    companion object {
        private const val TAG = "MediaStoreScanner"
    }

    suspend fun scanAudioFiles(minDurationSeconds: Int = 30): Int = withContext(Dispatchers.IO) {
        AuraLog.i(TAG, "Starting audio library scan with minDuration=${minDurationSeconds}s...")

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val minDurationMs = (minDurationSeconds * 1000L).coerceAtLeast(1000L)
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR ${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR ${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR ${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.ogg' OR ${MediaStore.Audio.Media.DATA} LIKE '%.opus') AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songsList = mutableListOf<Song>()
        val existingPaths = mutableListOf<String>()

        try {
            var cursor: Cursor? = context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            // Fallback if OEM returns 0 songs with strict selection
            if (cursor == null || cursor.count == 0) {
                cursor?.close()
                AuraLog.w(TAG, "Strict query returned 0 items. Falling back to broad audio query.")
                cursor = context.contentResolver.query(
                    collectionUri,
                    projection,
                    "${MediaStore.Audio.Media.DURATION} >= ?",
                    arrayOf(minDurationMs.toString()),
                    sortOrder
                )
            }

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val composerCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (c.moveToNext()) {
                    val mediaStoreId = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol) ?: "Unknown Title"
                    val rawArtist = c.getString(artistCol) ?: "<unknown>"
                    val rawAlbum = c.getString(albumCol) ?: "Unknown Album"
                    val artistId = c.getLong(artistIdCol)
                    val albumId = c.getLong(albumIdCol)
                    val composer = c.getString(composerCol)
                    val year = c.getInt(yearCol)
                    val trackRaw = c.getInt(trackCol)
                    val trackNumber = if (trackRaw >= 1000) trackRaw % 1000 else trackRaw
                    val discNumber = if (trackRaw >= 1000) trackRaw / 1000 else 1
                    var duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""
                    val mime = c.getString(mimeCol) ?: "audio/mpeg"
                    val size = c.getLong(sizeCol)
                    val dateAdded = c.getLong(dateAddedCol)
                    val dateMod = c.getLong(dateModCol)

                    val file = File(path)
                    val extension = file.extension.lowercase()

                    if (path.isBlank() || (!file.exists() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                        continue
                    }

                    // Exclude voice notes, chat audio, ringtones, notifications and call recordings
                    val lowerPath = path.lowercase()
                    if (lowerPath.contains("/whatsapp/") ||
                        lowerPath.contains("/telegram/") ||
                        lowerPath.contains("/ringtones/") ||
                        lowerPath.contains("/notifications/") ||
                        lowerPath.contains("/alarms/") ||
                        lowerPath.contains("/voice recorder/") ||
                        lowerPath.contains("/recordings/") ||
                        lowerPath.contains("/.trashed")
                    ) {
                        continue
                    }

                    // Multi-artist splitting
                    val artistSplit = ArtistSplitter.splitArtists(rawArtist)
                    val primaryArtist = artistSplit.primaryArtist
                    var cleanTitle = cleanTitle(rawTitle)
                    var albumTitle = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") "Unknown Album" else rawAlbum

                    var codec = when {
                        extension == "flac" || mime.contains("flac") -> "FLAC"
                        extension == "wav" || mime.contains("wav") -> "WAV"
                        extension == "m4a" || extension == "aac" || mime.contains("mp4") || mime.contains("aac") -> "AAC"
                        extension == "ogg" || mime.contains("ogg") -> "OGG"
                        extension == "opus" || mime.contains("opus") -> "OPUS"
                        extension == "wma" -> "WMA"
                        else -> "MP3"
                    }

                    var sampleRate = 44100
                    var bitDepth = 16
                    var bitrate = 320
                    var channels = 2
                    var isHiRes = false
                    var embeddedLyrics: String? = null
                    var replayGainTrack: Float? = null
                    var replayGainAlbum: Float? = null
                    var replayGainTrackPeak: Float? = null

                    // FIRST-CLASS FLAC PARSING
                    if (codec == "FLAC") {
                        isHiRes = true
                        val flacMeta = try {
                            if (file.exists() && file.canRead()) {
                                FlacVorbisCommentParser.parse(file)
                            } else {
                                val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
                                context.contentResolver.openInputStream(trackUri)?.use { stream ->
                                    FlacVorbisCommentParser.parse(stream, size)
                                }
                            }
                        } catch (_: Exception) {
                            null
                        }

                        if (flacMeta != null) {
                            sampleRate = flacMeta.sampleRate
                            bitDepth = flacMeta.bitDepth
                            channels = flacMeta.channels
                            bitrate = if (flacMeta.bitrateKbps > 0) flacMeta.bitrateKbps else 1411
                            isHiRes = flacMeta.isHiRes
                            embeddedLyrics = flacMeta.embeddedLyrics
                            replayGainTrack = flacMeta.replayGainTrack
                            replayGainAlbum = flacMeta.replayGainAlbum
                            replayGainTrackPeak = flacMeta.replayGainTrackPeak

                            // Override titles if Vorbis tags provide better precision
                            flacMeta.comments["TITLE"]?.let { cleanTitle = it }
                            flacMeta.comments["ALBUM"]?.let { albumTitle = it }
                            if (duration <= 0 && flacMeta.durationMs > 0) {
                                duration = flacMeta.durationMs
                            }
                        }
                    } else {
                        // Safe inspection for other formats
                        val inspected = inspectGenericFormat(path, mime, extension)
                        sampleRate = inspected.sampleRate
                        bitDepth = inspected.bitDepth
                        bitrate = inspected.bitrate
                        channels = inspected.channels
                        isHiRes = inspected.isHiRes
                    }

                    val song = Song(
                        mediaStoreId = mediaStoreId,
                        title = cleanTitle,
                        artist = primaryArtist,
                        artistId = artistId,
                        album = albumTitle,
                        albumId = albumId,
                        composer = composer,
                        year = year,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        durationMs = duration,
                        path = path,
                        mimeType = mime,
                        sizeBytes = size,
                        dateAdded = dateAdded,
                        dateModified = dateMod,
                        codec = codec,
                        sampleRate = sampleRate,
                        bitDepth = bitDepth,
                        bitrate = bitrate,
                        channels = channels,
                        isHiRes = isHiRes,
                        replayGainTrack = replayGainTrack,
                        replayGainAlbum = replayGainAlbum,
                        replayGainTrackPeak = replayGainTrackPeak,
                        embeddedLyrics = embeddedLyrics
                    )

                    songsList.add(song)
                    existingPaths.add(path)
                }
            }

            if (songsList.isNotEmpty()) {
                songDao.insertSongs(songsList)
                songDao.removeDeletedPaths(existingPaths)
            }

            AuraLog.i(TAG, "Audio scan completed. Indexed ${songsList.size} tracks with full FLAC/Hi-Res inspection.")
            songsList.size
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error scanning MediaStore", e)
            0
        }
    }

    private fun cleanTitle(title: String): String {
        return title.removeSuffix(".mp3")
            .removeSuffix(".flac")
            .removeSuffix(".wav")
            .removeSuffix(".m4a")
            .removeSuffix(".ogg")
            .removeSuffix(".opus")
            .trim()
    }

    private data class GenericAudioInfo(
        val sampleRate: Int,
        val bitDepth: Int,
        val bitrate: Int,
        val channels: Int,
        val isHiRes: Boolean
    )

    private fun inspectGenericFormat(filePath: String, mimeType: String, extension: String): GenericAudioInfo {
        var sampleRate = 44100
        var bitDepth = 16
        var bitrate = 320
        var channels = 2

        if (extension == "wav") {
            bitrate = 1411
        }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            bitrateStr?.toIntOrNull()?.let {
                bitrate = (it / 1000).coerceAtLeast(64)
            }
            retriever.release()
        } catch (_: Exception) { }

        val isHiRes = bitDepth >= 24 || sampleRate >= 88200
        return GenericAudioInfo(sampleRate, bitDepth, bitrate, channels, isHiRes)
    }
}
