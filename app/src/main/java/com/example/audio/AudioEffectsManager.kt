package com.example.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.core.logger.AuraLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

class AudioEffectsManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioEffectsManager"
    }

    data class BandInfo(
        val bandNumber: Short,
        val centerFreqHz: Int,
        val minLevelMb: Short,
        val maxLevelMb: Short,
        val currentLevelMb: Short
    )

    data class AudioEffectsState(
        val isEnabled: Boolean = false,
        val bands: List<BandInfo> = emptyList(),
        val bassBoostStrength: Int = 0,
        val virtualizerStrength: Int = 0,
        val replayGainEnabled: Boolean = false,
        val replayGainPreampDb: Float = 0.0f
    )

    private val _effectsState = MutableStateFlow(AudioEffectsState())
    val effectsState: StateFlow<AudioEffectsState> = _effectsState.asStateFlow()

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        AuraLog.i(TAG, "Attaching audio effects to new audioSessionId: $audioSessionId")
        release()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
            }
            equalizer = eq
            populateBandInfo()

            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb

            val virt = Virtualizer(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt

            val le = LoudnessEnhancer(audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
            }
            loudnessEnhancer = le

            AuraLog.i(TAG, "Audio effects attached successfully.")
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error initializing hardware audio effects: ${e.message}", e)
            release()
        }
    }

    private fun populateBandInfo() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange
            val bandList = mutableListOf<BandInfo>()
            for (i in 0 until numBands) {
                val band = i.toShort()
                val freq = eq.getCenterFreq(band) / 1000
                val level = eq.getBandLevel(band)
                bandList.add(BandInfo(band, freq, range[0], range[1], level))
            }
            _effectsState.value = _effectsState.value.copy(bands = bandList)
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error populating band info", e)
        }
    }

    fun setBandLevel(bandNumber: Short, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandNumber, levelMb)
            val updatedBands = _effectsState.value.bands.map {
                if (it.bandNumber == bandNumber) it.copy(currentLevelMb = levelMb) else it
            }
            _effectsState.value = _effectsState.value.copy(bands = updatedBands)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed to set EQ band level: ${e.message}")
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(clamped.toShort())
            _effectsState.value = _effectsState.value.copy(bassBoostStrength = clamped)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(clamped.toShort())
            _effectsState.value = _effectsState.value.copy(virtualizerStrength = clamped)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed setting virtualizer: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed toggling audio effects: ${e.message}")
        }
        _effectsState.value = _effectsState.value.copy(isEnabled = enabled)
    }

    fun calculateReplayGainMultiplier(trackGainDb: Float?, peak: Float?): Float {
        if (!_effectsState.value.replayGainEnabled || trackGainDb == null) {
            return 1.0f
        }
        val totalGain = trackGainDb + _effectsState.value.replayGainPreampDb
        var scale = 10.0.pow((totalGain / 20.0).toDouble()).toFloat()
        if (peak != null && peak > 0.0f) {
            val maxAllowedScale = 1.0f / peak
            scale = scale.coerceAtMost(maxAllowedScale)
        }
        return scale.coerceIn(0.1f, 2.0f)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            AuraLog.w(TAG, "Error releasing audio effects: ${e.message}")
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            loudnessEnhancer = null
            currentSessionId = 0
        }
    }
}
