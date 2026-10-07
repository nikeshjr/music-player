package com.example.service

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.audio.AudioEffectsManager
import com.example.audio.CrossfadePlayerManager
import com.example.audio.SleepTimerManager
import com.example.core.logger.AuraLog
import com.example.data.model.Song
import com.example.domain.model.PlayerUiState
import com.example.domain.model.RepeatMode
import com.example.widget.AuraMediaWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AuraPlayerController private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AuraPlayerController"

        @Volatile
        private var INSTANCE: AuraPlayerController? = null

        fun getInstance(context: Context): AuraPlayerController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuraPlayerController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    val audioEffectsManager: AudioEffectsManager = AudioEffectsManager(context)
    var crossfadeManager: CrossfadePlayerManager? = null
        private set
    val sleepTimerManager: SleepTimerManager = SleepTimerManager(
        onFadeVolume = { vol ->
            exoPlayer?.volume = vol
        },
        onTimerExpired = {
            pause()
        }
    )

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionTickerJob: Job? = null

    init {
        try {
            getOrCreatePlayer()
        } catch (e: Throwable) {
            AuraLog.e(TAG, "ExoPlayer pre-warming postponed: ${e.message}", e)
        }
    }

    fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        AuraLog.i(TAG, "Creating new ExoPlayer instance in AuraPlayerController...")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        val player = try {
            ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
                .build()
        } catch (e: Throwable) {
            AuraLog.e(TAG, "Standard ExoPlayer creation failed, using bare builder: ${e.message}", e)
            ExoPlayer.Builder(context).build()
        }

        attachPlayer(player)
        return player
    }

    fun attachPlayer(player: ExoPlayer) {
        if (exoPlayer == player) return
        exoPlayer = player
        crossfadeManager = CrossfadePlayerManager(player)
        audioEffectsManager.attachToAudioSession(player.audioSessionId)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(
                    isPlaying = isPlaying,
                    currentPositionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                AuraMediaWidget.updateAllWidgets(context)
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    sleepTimerManager.onTrackCompleted()
                }
                _uiState.value = _uiState.value.copy(
                    currentPositionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L),
                    bufferedPositionMs = player.bufferedPosition
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentIdx = player.currentMediaItemIndex
                val queue = _uiState.value.queue
                val activeSong = queue.getOrNull(currentIdx)
                if (activeSong != null) {
                    _uiState.value = _uiState.value.copy(
                        currentSong = activeSong,
                        durationMs = activeSong.durationMs,
                        isFavorite = activeSong.isFavorite,
                        queueIndex = currentIdx
                    )
                    val multiplier = audioEffectsManager.calculateReplayGainMultiplier(
                        activeSong.replayGainTrack,
                        activeSong.replayGainTrackPeak
                    )
                    player.volume = multiplier
                    AuraMediaWidget.updateAllWidgets(context)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                AuraLog.e(TAG, "PlaybackException: ${error.message} (code: ${error.errorCodeName})", error)
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        })
    }

    private fun resolveTrackUri(track: Song): Uri {
        return try {
            if (track.path.startsWith("content://")) {
                Uri.parse(track.path)
            } else if (Build.VERSION.SDK_INT >= 29 && track.mediaStoreId > 0) {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.mediaStoreId)
            } else if (track.path.startsWith("/")) {
                val file = File(track.path)
                if (file.exists() && file.canRead()) {
                    Uri.fromFile(file)
                } else if (track.mediaStoreId > 0) {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.mediaStoreId)
                } else {
                    Uri.parse(track.path)
                }
            } else if (track.mediaStoreId > 0) {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.mediaStoreId)
            } else {
                Uri.parse(track.path)
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error resolving track URI for ${track.title}: ${e.message}", e)
            Uri.parse(track.path)
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val player = getOrCreatePlayer()
        val targetIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _uiState.value = _uiState.value.copy(
            currentSong = song,
            durationMs = song.durationMs,
            isFavorite = song.isFavorite,
            queue = queue,
            queueIndex = targetIndex
        )

        val mediaItems = queue.map { songItem ->
            MediaItem.Builder()
                .setMediaId(songItem.id.toString())
                .setUri(resolveTrackUri(songItem))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(songItem.title)
                        .setArtist(songItem.artist)
                        .setAlbumTitle(songItem.album)
                        .build()
                )
                .build()
        }

        try {
            player.setMediaItems(mediaItems, targetIndex, 0L)
            player.playWhenReady = true
            player.volume = 1.0f
            player.prepare()
            AuraLog.i(TAG, "Playback queued for: ${song.title} at index $targetIndex with playWhenReady=true")
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error playing song ${song.title}: ${e.message}", e)
        }

        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Could not start PlaybackService: ${e.message}")
        }
    }

    fun togglePlayPause() {
        val player = getOrCreatePlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            player.volume = 1.0f
            player.play()
        }
    }

    fun pause() {
        getOrCreatePlayer().pause()
    }

    fun resume() {
        val player = getOrCreatePlayer()
        player.volume = 1.0f
        player.play()
    }

    fun seekTo(positionMs: Long) {
        val player = getOrCreatePlayer()
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _uiState.value = _uiState.value.copy(currentPositionMs = clamped)
    }

    fun seekToProgress(progress: Float) {
        val player = getOrCreatePlayer()
        val duration = if (player.duration > 0) player.duration else _uiState.value.durationMs
        if (duration > 0) {
            val targetMs = (duration.toFloat() * progress.coerceIn(0f, 1f)).toLong()
            seekTo(targetMs)
        }
    }

    fun next() {
        val player = getOrCreatePlayer()
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_uiState.value.repeatMode == RepeatMode.ALL && player.mediaItemCount > 0) {
            player.seekTo(0, 0L)
        }
    }

    fun previous() {
        val player = getOrCreatePlayer()
        if (player.currentPosition > 3000) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }

    fun setShuffle(enabled: Boolean) {
        val player = getOrCreatePlayer()
        player.shuffleModeEnabled = enabled
        _uiState.value = _uiState.value.copy(isShuffleEnabled = enabled)
    }

    fun toggleRepeatMode() {
        val player = getOrCreatePlayer()
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val exoRepeat = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        player.repeatMode = exoRepeat
        _uiState.value = _uiState.value.copy(repeatMode = nextMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = getOrCreatePlayer()
        val clamped = speed.coerceIn(0.5f, 2.0f)
        player.playbackParameters = PlaybackParameters(clamped)
        _uiState.value = _uiState.value.copy(playbackSpeed = clamped)
    }

    fun toggleFavorite() {
        val currentSong = _uiState.value.currentSong ?: return
        val newFav = !_uiState.value.isFavorite
        _uiState.value = _uiState.value.copy(isFavorite = newFav)
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition
                    )
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }
}
