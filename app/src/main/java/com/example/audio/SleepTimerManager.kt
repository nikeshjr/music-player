package com.example.audio

import com.example.core.logger.AuraLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(
    private val onFadeVolume: (Float) -> Unit,
    private val onTimerExpired: () -> Unit
) {
    companion object {
        private const val TAG = "SleepTimerManager"
    }

    enum class SleepMode {
        MINUTES, END_OF_TRACK
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

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    fun startTimerMinutes(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return
        AuraLog.i(TAG, "Starting sleep timer for $minutes minutes.")
        val totalSeconds = (minutes * 60).toLong()
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
                if (currentSec <= 30) {
                    val vol = (currentSec.toFloat() / 30f).coerceIn(0f, 1f)
                    onFadeVolume(vol)
                }
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = currentSec,
                    formattedRemaining = formatTime(currentSec)
                )
            }
            if (isActive) {
                AuraLog.i(TAG, "Sleep timer expired. Stopping audio playback.")
                onFadeVolume(1f)
                _timerState.value = SleepTimerState()
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
        onFadeVolume(1f)
        _timerState.value = SleepTimerState()
        AuraLog.d(TAG, "Sleep timer cancelled.")
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
