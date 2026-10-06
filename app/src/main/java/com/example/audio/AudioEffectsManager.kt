package com.example.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import com.example.core.logger.AuraLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

/**
 * AudioEffectsManager: Hardware and software audio effect processing.
 * Manages Equalizer (5-10 bands), BassBoost, Virtualizer, LoudnessEnhancer,
 * and software ReplayGain loudness normalization.
 * Handles lifecycle attachment to audioSessionId safely with zero-crash guarantees on unsupported hardware.
 */
class AudioEffectsManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioEffectsManager"
    }

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    data class BandInfo(
        val bandNumber: Short,
        val centerFreqHz: Int,
        val minLevelMb: Short,
        val maxLevelMb: Short,
        val currentLevelMb: Short
    )

    data class AudioEffectsState(
        val isEnabled: Boolean = true,
        val bands: List<BandInfo> = emptyList(),
        val bassBoostStrength: Int = 0, // 0 - 1000
        val virtualizerStrength: Int = 0, // 0 - 1000
        val replayGainEnabled: Boolean = true,
        val replayGainPreampDb: Float = 0f
    )

    private val defaultBands = listOf(
        BandInfo(0, 60, -1500, 1500, 0),
        BandInfo(1, 230, -1500, 1500, 0),
        BandInfo(2, 910, -1500, 1500, 0),
        BandInfo(3, 3600, -1500, 1500, 0),
        BandInfo(4, 14000, -1500, 1500, 0)
    )

    private val _effectsState = MutableStateFlow(AudioEffectsState(bands = defaultBands))
    val effectsState: StateFlow<AudioEffectsState> = _effectsState.asStateFlow()

    /**
     * Attaches audio effects to the active ExoPlayer audioSessionId.
     */
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null && bassBoost != null && virtualizer != null) {
            return // Already active and attached to this session
        }

        AuraLog.i(TAG, "Attaching audio effects to audioSessionId: $audioSessionId (previous: $currentSessionId)")
        release()
        currentSessionId = audioSessionId

        try {
            // 1. Equalizer setup (priority 1000 to override system defaults)
            val eq = Equalizer(1000, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
            }
            equalizer = eq

            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange
            val hwBands = mutableListOf<BandInfo>()
            for (i in 0 until numBands) {
                val band = i.toShort()
                val freq = eq.getCenterFreq(band) / 1000
                val existingLevel = _effectsState.value.bands.getOrNull(i)?.currentLevelMb ?: 0.toShort()
                try {
                    eq.setBandLevel(band, existingLevel)
                } catch (_: Exception) {}
                hwBands.add(
                    BandInfo(
                        bandNumber = band,
                        centerFreqHz = freq,
                        minLevelMb = range[0],
                        maxLevelMb = range[1],
                        currentLevelMb = existingLevel
                    )
                )
            }
            if (hwBands.isNotEmpty()) {
                _effectsState.value = _effectsState.value.copy(bands = hwBands)
            }

            // 2. BassBoost setup
            val bb = BassBoost(1000, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb

            // 3. Virtualizer setup
            val virt = Virtualizer(1000, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt

            // 4. LoudnessEnhancer (API 19+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = _effectsState.value.isEnabled
                }
            }

            AuraLog.i(TAG, "Audio effects attached successfully to session $audioSessionId.")
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error initializing hardware audio effects: ${e.message}", e)
        }
    }

    private fun populateBandInfo() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange // [minMb, maxMb] e.g. [-1500, +1500]
            val bandList = mutableListOf<BandInfo>()

            for (i in 0 until numBands) {
                val band = i.toShort()
                val freq = eq.getCenterFreq(band) / 1000 // Convert mHz to Hz
                val level = eq.getBandLevel(band)
                bandList.add(
                    BandInfo(
                        bandNumber = band,
                        centerFreqHz = freq,
                        minLevelMb = range[0],
                        maxLevelMb = range[1],
                        currentLevelMb = level
                    )
                )
            }
            _effectsState.value = _effectsState.value.copy(bands = bandList)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Error populating EQ bands: ${e.message}")
        }
    }

    fun setBandLevel(bandNumber: Short, levelMb: Short) {
        try {
            equalizer?.enabled = _effectsState.value.isEnabled
            equalizer?.setBandLevel(bandNumber, levelMb)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed to set EQ band level: ${e.message}")
        }
        val updatedBands = if (_effectsState.value.bands.isEmpty()) {
            listOf(BandInfo(bandNumber, 1000, -1500, 1500, levelMb))
        } else {
            _effectsState.value.bands.map {
                if (it.bandNumber == bandNumber) it.copy(currentLevelMb = levelMb) else it
            }
        }
        _effectsState.value = _effectsState.value.copy(bands = updatedBands)
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.enabled = _effectsState.value.isEnabled
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed setting bass boost: ${e.message}")
        }
        _effectsState.value = _effectsState.value.copy(bassBoostStrength = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.enabled = _effectsState.value.isEnabled
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            AuraLog.w(TAG, "Failed setting virtualizer: ${e.message}")
        }
        _effectsState.value = _effectsState.value.copy(virtualizerStrength = clamped)
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

    /**
     * Calculates volume scale factor for ReplayGain track normalization.
     * Prevents clipping using 10^(gain/20) equation.
     */
    fun calculateReplayGainMultiplier(trackGainDb: Float?, peak: Float?): Float {
        if (!_effectsState.value.replayGainEnabled || trackGainDb == null) return 1.0f

        val totalGain = trackGainDb + _effectsState.value.replayGainPreampDb
        var scale = 10.0.pow(totalGain / 20.0).toFloat()

        // Prevent digital clipping if peak info is present
        if (peak != null && peak > 0f) {
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
