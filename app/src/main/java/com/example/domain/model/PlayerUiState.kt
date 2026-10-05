package com.example.domain.model

import com.example.data.model.Song

/**
 * PlayerUiState: Reactive UI state governing all player interfaces (Now Playing, Mini Player,
 * Dynamic Island overlay, and Lockscreen).
 */
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val isFavorite: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val sleepTimerMinutesRemaining: Int? = null,
    val currentLyricLine: String? = null,
    val isAudioSessionId: Int = 0
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedPosition: String
        get() = formatTime(currentPositionMs)

    val formattedDuration: String
        get() = formatTime(durationMs)

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

enum class RepeatMode {
    OFF, ALL, ONE
}
