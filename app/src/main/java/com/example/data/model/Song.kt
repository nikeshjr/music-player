package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.File

/**
 * Song: Central representation of an audio track across Aura Music.
 * Holds audio metadata, filesystem references, and technical details for Hi-Res audio badges.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["path"])
    ]
)
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val album: String,
    val albumId: Long = 0,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val durationMs: Long,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val dateModified: Long,
    // Hi-Res & Codec Technical Audio Info
    val codec: String = "MP3",
    val sampleRate: Int = 44100, // in Hz e.g. 44100, 96000, 192000
    val bitDepth: Int = 16,     // in bits e.g. 16, 24, 32
    val bitrate: Int = 320,      // in kbps
    val channels: Int = 2,      // 1 = Mono, 2 = Stereo, >2 = Multichannel
    val isHiRes: Boolean = false, // 24-bit or sampleRate >= 88200Hz
    // ReplayGain tags
    val replayGainTrack: Float? = null,
    val replayGainAlbum: Float? = null,
    val replayGainTrackPeak: Float? = null,
    // Embedded Lyrics
    val embeddedLyrics: String? = null,
    // Play statistics & flags
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val lastPlayedTime: Long = 0
) {
    val fileExtension: String
        get() = File(path).extension.lowercase()

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val audioBadgeLabel: String
        get() = when {
            codec.equals("FLAC", ignoreCase = true) && isHiRes -> "HI-RES FLAC"
            codec.equals("FLAC", ignoreCase = true) -> "FLAC"
            isHiRes -> "HI-RES"
            else -> codec.uppercase()
        }

    val technicalSummary: String
        get() = "${codec.uppercase()} • ${bitDepth}-bit / ${sampleRate / 1000.0} kHz • ${bitrate} kbps"
}
