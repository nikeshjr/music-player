package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Album: Grouped model for albums with calculated statistics.
 */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val albumArtUri: String? = null
)

/**
 * Artist: Grouped model for artists with song and album counts.
 */
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

/**
 * Playlist: User-created and smart playlists.
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false,
    val smartType: String? = null // "RECENTLY_ADDED", "MOST_PLAYED", "NEVER_PLAYED"
)

/**
 * PlaylistSongCrossRef: Relates playlists with ordered songs.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId", "orderIndex"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int
)

/**
 * PlayHistory: Tracks song plays and skips for analytics and smart playlists.
 */
@Entity(
    tableName = "play_history",
    indices = [Index("songId"), Index("playedAt")]
)
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = true
)

/**
 * EqPreset: Saved equalizer presets including user-defined profiles.
 */
@Entity(tableName = "eq_presets")
data class EqPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bandLevels: String, // comma-separated millibel values, e.g. "300,100,0,-100,200"
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val isDefault: Boolean = false,
    val targetDeviceType: String = "ALL" // "SPEAKER", "WIRED", "BLUETOOTH", "ALL"
)

/**
 * ThemeProfile: User-saved custom themes that can be switched or exported as JSON.
 */
@Entity(tableName = "theme_profiles")
data class ThemeProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val themeJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)
