package com.example.audio

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*

class CrossfadePlayerManager(
    private val player: ExoPlayer
) {
    companion object {
        private const val FADE_STEP_INTERVAL_MS = 40L
        private const val TAG = "CrossfadeManager"
    }

    var crossfadeSeconds: Int = 0
    var isFadeOnPlayPauseEnabled: Boolean = true

    private val scope = CoroutineScope(Dispatchers.Main)
    private var fadeJob: Job? = null

    fun animateVolume(
        targetVolume: Float,
        durationMs: Long,
        onComplete: (() -> Unit)? = null
    ) {
        fadeJob?.cancel()
        if (durationMs <= 0) {
            player.volume = targetVolume
            onComplete?.invoke()
            return
        }

        val startVolume = player.volume
        val steps = (durationMs / FADE_STEP_INTERVAL_MS).toInt().coerceAtLeast(1)
        val volumeDelta = (targetVolume - startVolume) / steps

        fadeJob = scope.launch {
            var currentVol = startVolume
            repeat(steps) {
                delay(FADE_STEP_INTERVAL_MS)
                currentVol += volumeDelta
                player.volume = currentVol.coerceIn(0f, 1f)
            }
            player.volume = targetVolume.coerceIn(0f, 1f)
            onComplete?.invoke()
        }
    }

    fun smoothPause(onPaused: () -> Unit) {
        if (!isFadeOnPlayPauseEnabled || !player.isPlaying) {
            player.pause()
            onPaused()
        } else {
            animateVolume(0f, 250L) {
                player.pause()
                player.volume = 1f
                onPaused()
            }
        }
    }

    fun smoothPlay(onStarted: () -> Unit) {
        if (!isFadeOnPlayPauseEnabled) {
            player.play()
            onStarted()
        } else {
            player.volume = 0f
            player.play()
            onStarted()
            animateVolume(1f, 300L)
        }
    }
}
