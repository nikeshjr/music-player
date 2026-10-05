package com.example.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.metadata.SampleHiResTracks
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.motion.SpinningVinylRecord
import com.example.core.motion.WaveformSeekBar
import com.example.core.theme.AuraTheme
import com.example.domain.model.RepeatMode
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSlider
import com.example.presentation.components.GlassSurface
import com.example.presentation.components.GlassSwitch
import com.example.service.AuraPlayerController

/**
 * AudioEngineInspectionScreen: Interactive demonstration and testing interface for
 * Phase 4 Media3 ExoPlayer, AudioEffectsManager (Equalizer, BassBoost, Virtualizer),
 * Crossfade, and Sleep Timer with volume fade-out.
 */
@Composable
fun AudioEngineInspectionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val effectsState by controller.audioEffectsManager.effectsState.collectAsState()
    val sleepTimerState by controller.sleepTimerManager.timerState.collectAsState()
    val theme = AuraTheme.current

    val currentTrack = uiState.currentSong ?: SampleHiResTracks.tracks.first()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Audio Engine & Media3",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = "ExoPlayer • 10-Band EQ • Crossfade • Sleep Timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textColorSecondary
                    )
                }
                GlassButton(onClick = onNavigateBack) {
                    Text("Back", color = theme.textColorPrimary, fontSize = 12.sp)
                }
            }
        }

        // Active Player Card
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpinningVinylRecord(
                            isPlaying = uiState.isPlaying,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = theme.textColorPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = currentTrack.artist,
                                fontSize = 12.sp,
                                color = theme.textColorSecondary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Format Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (currentTrack.isHiRes) Color(0xFFD4AF37) else theme.primaryColor.copy(alpha = 0.3f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentTrack.audioBadgeLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentTrack.isHiRes) Color.Black else Color.White
                                )
                            }
                        }
                        FavoriteHeartBurst(
                            isFavorite = uiState.isFavorite,
                            onToggle = { controller.toggleFavorite() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Waveform Seeker
                    WaveformSeekBar(
                        progress = uiState.progress,
                        onSeek = { controller.seekToProgress(it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(uiState.formattedPosition, fontSize = 11.sp, color = theme.textColorSecondary)
                        Text(uiState.formattedDuration, fontSize = 11.sp, color = theme.textColorSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transport Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        GlassButton(
                            onClick = { controller.setShuffle(!uiState.isShuffleEnabled) },
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (uiState.isShuffleEnabled) theme.primaryColor else theme.textColorSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Previous
                        GlassButton(
                            onClick = { controller.previous() },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = theme.textColorPrimary, modifier = Modifier.size(22.dp))
                        }

                        // Play / Pause
                        GlassButton(
                            onClick = {
                                if (uiState.currentSong == null) {
                                    controller.playSong(SampleHiResTracks.tracks.first(), SampleHiResTracks.tracks)
                                } else {
                                    controller.togglePlayPause()
                                }
                            },
                            shape = CircleShape,
                            accentColor = theme.primaryColor
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next
                        GlassButton(
                            onClick = { controller.next() },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = theme.textColorPrimary, modifier = Modifier.size(22.dp))
                        }

                        // Repeat
                        GlassButton(
                            onClick = { controller.toggleRepeatMode() },
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (uiState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (uiState.repeatMode != RepeatMode.OFF) theme.primaryColor else theme.textColorSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hardware Equalizer & Effects
        item {
            Text(
                text = "Audio Effects & Equalizer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Audio Effects",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textColorPrimary
                        )
                        GlassSwitch(
                            checked = effectsState.isEnabled,
                            onCheckedChange = { controller.audioEffectsManager.setEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Bass Boost (${effectsState.bassBoostStrength / 10}%)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = effectsState.bassBoostStrength.toFloat(),
                        onValueChange = { controller.audioEffectsManager.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "3D Virtualizer (${effectsState.virtualizerStrength / 10}%)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = effectsState.virtualizerStrength.toFloat(),
                        onValueChange = { controller.audioEffectsManager.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Equalizer Frequency Bands",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColorPrimary
                    )

                    val bands = effectsState.bands
                    if (bands.isNotEmpty()) {
                        bands.forEach { band ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${band.centerFreqHz}Hz",
                                    fontSize = 11.sp,
                                    color = theme.textColorSecondary,
                                    modifier = Modifier.width(55.dp)
                                )
                                GlassSlider(
                                    value = band.currentLevelMb.toFloat(),
                                    onValueChange = { controller.audioEffectsManager.setBandLevel(band.bandNumber, it.toInt().toShort()) },
                                    valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${band.currentLevelMb / 100}dB",
                                    fontSize = 10.sp,
                                    color = theme.textColorSecondary,
                                    modifier = Modifier.width(45.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Equalizer attaches automatically when audio is playing.",
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }
                }
            }
        }

        // Sleep Timer Card
        item {
            Text(
                text = "Sleep Timer with Volume Fade-Out",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (sleepTimerState.isActive) "Active: ${sleepTimerState.formattedRemaining}" else "Timer Off",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (sleepTimerState.isActive) Color(0xFF10B981) else theme.textColorPrimary
                        )

                        if (sleepTimerState.isActive) {
                            GlassButton(
                                onClick = { controller.sleepTimerManager.cancelTimer() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp, color = theme.textColorPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(text = "15m", isSelected = false, onClick = { controller.sleepTimerManager.startTimerMinutes(15) })
                        GlassChip(text = "30m", isSelected = false, onClick = { controller.sleepTimerManager.startTimerMinutes(30) })
                        GlassChip(text = "60m", isSelected = false, onClick = { controller.sleepTimerManager.startTimerMinutes(60) })
                        GlassChip(text = "End of Track", isSelected = false, onClick = { controller.sleepTimerManager.startEndOfTrackMode() })
                    }
                }
            }
        }

        // Playback Speed Slider
        item {
            Text(
                text = "Playback Speed (${uiState.playbackSpeed}x)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    GlassSlider(
                        value = uiState.playbackSpeed,
                        onValueChange = { controller.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
