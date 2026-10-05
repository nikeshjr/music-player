package com.example.presentation.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.metadata.SampleHiResTracks
import com.example.core.motion.AnimatedEqualizerBars
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.motion.SpinningVinylRecord
import com.example.core.motion.WaveformSeekBar
import com.example.core.theme.AuraTheme
import com.example.core.theme.NowPlayingStyle
import com.example.domain.model.RepeatMode
import com.example.presentation.components.AuraArtworkImage
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSurface
import com.example.service.AuraPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NowPlayingScreen: Flagship audiophile playback screen supporting switchable styles
 * (Classic, Vinyl, Minimal, Lyrics-First), real album artwork, vertical swipe-to-adjust volume gesture,
 * edge-to-edge status bar protection, interactive waveform seekbar, and ambient glowing backdrops.
 */
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val sleepTimerState by controller.sleepTimerManager.timerState.collectAsState()
    val theme = AuraTheme.current

    val currentSong = uiState.currentSong ?: SampleHiResTracks.tracks.first()
    var selectedStyle by remember { mutableStateOf(theme.nowPlayingStyle) }

    // Volume drag gesture state
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeIndicator by remember { mutableStateOf<Int?>(null) }
    var volumeIndicatorJob by remember { mutableStateOf<Job?>(null) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    // Ambient Artwork Glow
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        theme.primaryColor.copy(alpha = 0.35f),
                        theme.backgroundColor,
                        Color(0xFF030408)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(onClick = onNavigateBack, shape = CircleShape) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textColorPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Layout Switcher Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(
                        text = "Classic",
                        isSelected = selectedStyle == NowPlayingStyle.CLASSIC || selectedStyle == NowPlayingStyle.BIG_ART,
                        onClick = { selectedStyle = NowPlayingStyle.CLASSIC }
                    )
                    GlassChip(
                        text = "Vinyl",
                        isSelected = selectedStyle == NowPlayingStyle.VINYL,
                        onClick = { selectedStyle = NowPlayingStyle.VINYL }
                    )
                    GlassChip(
                        text = "Minimal",
                        isSelected = selectedStyle == NowPlayingStyle.MINIMAL,
                        onClick = { selectedStyle = NowPlayingStyle.MINIMAL }
                    )
                }

                GlassButton(onClick = onOpenQueue, shape = CircleShape) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = theme.textColorPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switchable Artwork / Central Area with Vertical Volume Drag
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulatorY = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                dragAccumulatorY += dragAmount
                                val threshold = 36f // px per volume step

                                if (dragAccumulatorY <= -threshold) { // Swipe UP -> Increase Volume
                                    change.consume()
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    volumeIndicator = (current * 100) / maxVolume
                                    dragAccumulatorY = 0f
                                    volumeIndicatorJob?.cancel()
                                    volumeIndicatorJob = scope.launch {
                                        delay(1200L)
                                        volumeIndicator = null
                                    }
                                } else if (dragAccumulatorY >= threshold) { // Swipe DOWN -> Decrease Volume
                                    change.consume()
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    volumeIndicator = (current * 100) / maxVolume
                                    dragAccumulatorY = 0f
                                    volumeIndicatorJob?.cancel()
                                    volumeIndicatorJob = scope.launch {
                                        delay(1200L)
                                        volumeIndicator = null
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (selectedStyle) {
                    NowPlayingStyle.VINYL -> {
                        SpinningVinylRecord(
                            isPlaying = uiState.isPlaying,
                            modifier = Modifier.size(260.dp),
                            albumArtContent = {
                                AuraArtworkImage(
                                    song = currentSong,
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp),
                                    fallbackIconSize = 28.dp
                                )
                            }
                        )
                    }
                    NowPlayingStyle.CLASSIC, NowPlayingStyle.BIG_ART -> {
                        AuraArtworkImage(
                            song = currentSong,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            fallbackIconSize = 80.dp
                        )
                    }
                    NowPlayingStyle.MINIMAL -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedEqualizerBars(
                                isPlaying = uiState.isPlaying,
                                modifier = Modifier.height(40.dp),
                                barColor = theme.primaryColor
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = currentSong.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = theme.textColorPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSong.artist,
                                fontSize = 15.sp,
                                color = theme.textColorSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    NowPlayingStyle.LYRICS_FIRST -> {
                        onOpenLyrics()
                    }
                }

                // Floating Volume HUD overlay when swiping up/down
                if (volumeIndicator != null) {
                    GlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if ((volumeIndicator ?: 0) > 0) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Volume",
                                tint = theme.primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${volumeIndicator ?: 0}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Meta & Hi-Res Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSong.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = theme.textColorPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Hi-Res Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (currentSong.isHiRes) Color(0xFFD4AF37) else theme.primaryColor.copy(alpha = 0.3f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentSong.audioBadgeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentSong.isHiRes) Color.Black else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${currentSong.artist} • ${currentSong.album}",
                        fontSize = 13.sp,
                        color = theme.textColorSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong.technicalSummary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryColor
                    )
                }

                FavoriteHeartBurst(
                    isFavorite = uiState.isFavorite,
                    onToggle = { controller.toggleFavorite() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Waveform Seekbar
            WaveformSeekBar(
                progress = uiState.progress,
                onSeek = { controller.seekToProgress(it) },
                playedColor = theme.primaryColor,
                unplayedColor = theme.surfaceColor.copy(alpha = 0.4f),
                modifier = Modifier.height(34.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(uiState.formattedPosition, fontSize = 11.sp, color = theme.textColorSecondary)
                if (sleepTimerState.isActive) {
                    Text("Sleep: ${sleepTimerState.formattedRemaining}", fontSize = 11.sp, color = Color(0xFF10B981))
                }
                Text(uiState.formattedDuration, fontSize = 11.sp, color = theme.textColorSecondary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Transport Buttons
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
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous
                GlassButton(
                    onClick = { controller.previous() },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = theme.textColorPrimary, modifier = Modifier.size(26.dp))
                }

                // Play / Pause (Large Primary Button with spring scale)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor))
                        )
                        .clickable { controller.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                GlassButton(
                    onClick = { controller.next() },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = theme.textColorPrimary, modifier = Modifier.size(26.dp))
                }

                // Repeat Mode
                GlassButton(
                    onClick = { controller.toggleRepeatMode() },
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (uiState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (uiState.repeatMode != RepeatMode.OFF) theme.primaryColor else theme.textColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Quick Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(onClick = onOpenLyrics, shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = theme.textColorPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lyrics", fontSize = 11.sp, color = theme.textColorPrimary)
                    }
                }

                GlassButton(onClick = onOpenEqualizer, shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = theme.textColorPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Equalizer", fontSize = 11.sp, color = theme.textColorPrimary)
                    }
                }
            }
        }
    }
}
