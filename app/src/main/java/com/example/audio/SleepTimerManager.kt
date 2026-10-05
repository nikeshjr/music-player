package com.example.audio

import com.example.core.logger.AuraLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * SleepTimerManager: Audio sleep timer supporting minute countdowns, End of Track,
 * and End of Queue modes with volume fade-out prior to stopping playback.
 */
class SleepTimerManager(
    private val onFadeVolume: (Float) -> Unit,
    private val onTimerExpired: () -> Unit
) {

    companion object {
        private const val TAG = "SleepTimerManager"
        private const val FADE_OUT_DURATION_MS = 30_000L // 30 second gentle volume ramp-down
    }

    enum class SleepMode {
        MINUTES, END_OF_TRACK, END_OF_QUEUE
    }

    data class SleepTimerState(
        val isActive: Boolean = false,
        val mode: SleepMode = SleepMode.MINUTES,
        val totalMinutes: Int = 0,
        val remainingSeconds: Long = 0L,
        val formattedRemaining: String = ""
    )

    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Starts a minute-based countdown timer.
     */
    fun startTimerMinutes(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        AuraLog.i(TAG, "Starting sleep timer for $minutes minutes.")
        val totalSeconds = minutes * 60L
        _timerState.value = SleepTimerState(
            isActive = true,
            mode = SleepMode.MINUTES,
            totalMinutes = minutes,
            remainingSeconds = totalSeconds,
            formattedRemaining = formatTime(totalSeconds)
        )

        timerJob = scope.launch {
            var currentSec = totalSeconds
            while (isActive && currentSec > 0) {
                delay(1000L)
                currentSec--

                // Calculate volume ramp in final 30 seconds
                if (currentSec <= (FADE_OUT_DURATION_MS / 1000L)) {
                    val volumeScale = (currentSec.toFloat() / (FADE_OUT_DURATION_MS / 1000L).toFloat()).coerceIn(0f, 1f)
                    onFadeVolume(volumeScale)
                }

                _timerState.value = _timerState.value.copy(
                    remainingSeconds = currentSec,
                    formattedRemaining = formatTime(currentSec)
                )
            }

            if (isActive) {
                AuraLog.i(TAG, "Sleep timer expired. Stopping audio playback.")
                onFadeVolume(1.0f) // Reset volume for next play
                _timerState.value = SleepTimerState(isActive = false)
                onTimerExpired()
            }
        }
    }

    fun startEndOfTrackMode() {
        cancelTimer()
        AuraLog.i(TAG, "Setting sleep timer: End of Current Track.")
        _timerState.value = SleepTimerState(
            isActive = true,
            mode = SleepMode.END_OF_TRACK,
            formattedRemaining = "End of Track"
        )
    }

    /**
     * Called by playback service when a track finishes playing.
     */
    fun onTrackCompleted() {
        if (_timerState.value.isActive && _timerState.value.mode == SleepMode.END_OF_TRACK) {
            AuraLog.i(TAG, "Track completed under End of Track sleep timer. Stopping.")
            cancelTimer()
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        onFadeVolume(1.0f)
        _timerState.value = SleepTimerState(isActive = false)
        AuraLog.d(TAG, "Sleep timer cancelled.")
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
