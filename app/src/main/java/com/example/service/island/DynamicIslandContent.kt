package com.example.service.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.motion.SpinningVinylRecord
import com.example.core.motion.WaveformSeekBar
import com.example.core.theme.AuraTheme
import com.example.domain.model.PlayerUiState
import com.example.presentation.components.AuraArtworkImage
import com.example.presentation.components.glassmorphic
import com.example.presentation.components.liquidGlassmorphic
import kotlinx.coroutines.delay

/**
 * DynamicIslandContent: Complete UI composable rendering the floating Dynamic Island.
 * Morphs seamlessly between IDLE_PILL, COMPACT, and LARGE_CARD with organic spring physics.
 * Features 5-second auto-collapse on LARGE_CARD, real album artwork, and 1-tap app launch.
 */
@Composable
fun DynamicIslandContent(
    uiState: PlayerUiState,
    layoutConfig: IslandLayoutConfig,
    currentState: IslandState,
    onStateChange: (IslandState) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onDismissPill: () -> Unit,
    onOpenApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = AuraTheme.current
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-collapse enlarged card to small capsule after 5 seconds of inactivity
    LaunchedEffect(currentState, lastInteractionTime) {
        if (currentState == IslandState.LARGE_CARD) {
            delay(5000L)
            onStateChange(IslandState.COMPACT)
        }
    }

    // Dynamic width and height animated with signature Island morph spring
    val targetWidth = when (currentState) {
        IslandState.IDLE_PILL -> 118.dp * layoutConfig.scaleMultiplier
        IslandState.COMPACT -> 210.dp * layoutConfig.scaleMultiplier
        IslandState.LARGE_CARD -> 348.dp * layoutConfig.scaleMultiplier
    }

    val targetHeight = when (currentState) {
        IslandState.IDLE_PILL -> 36.dp * layoutConfig.scaleMultiplier
        IslandState.COMPACT -> 42.dp * layoutConfig.scaleMultiplier
        IslandState.LARGE_CARD -> 188.dp * layoutConfig.scaleMultiplier
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.68f, stiffness = 380f),
        label = "IslandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.68f, stiffness = 380f),
        label = "IslandHeight"
    )

    var dragDeltaX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .glassmorphic(
                shape = RoundedCornerShape(layoutConfig.cornerRadiusDp.dp),
                tint = theme.islandColor,
                tintAlpha = layoutConfig.opacity,
                borderBrightness = 0.35f,
                elevation = 14.dp
            )
            .clip(RoundedCornerShape(layoutConfig.cornerRadiusDp.dp))
            .clickable {
                lastInteractionTime = System.currentTimeMillis()
                when (currentState) {
                    IslandState.IDLE_PILL -> onStateChange(IslandState.COMPACT)
                    IslandState.COMPACT -> onStateChange(IslandState.LARGE_CARD)
                    IslandState.LARGE_CARD -> onOpenApp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            },
            label = "IslandStateContent"
        ) { state ->
            when (state) {
                IslandState.IDLE_PILL -> {
                    IdlePillLayout(
                        isPlaying = uiState.isPlaying,
                        visualizerStyle = layoutConfig.visualizerStyle,
                        primaryColor = theme.primaryColor
                    )
                }
                IslandState.COMPACT -> {
                    CompactLayout(
                        uiState = uiState,
                        visualizerStyle = layoutConfig.visualizerStyle,
                        primaryColor = theme.primaryColor,
                        onPlayPause = {
                            lastInteractionTime = System.currentTimeMillis()
                            onPlayPause()
                        },
                        onNext = {
                            lastInteractionTime = System.currentTimeMillis()
                            onNext()
                        },
                        onExpand = {
                            lastInteractionTime = System.currentTimeMillis()
                            onStateChange(IslandState.LARGE_CARD)
                        }
                    )
                }
                IslandState.LARGE_CARD -> {
                    LargeCardLayout(
                        uiState = uiState,
                        onPlayPause = {
                            lastInteractionTime = System.currentTimeMillis()
                            onPlayPause()
                        },
                        onNext = {
                            lastInteractionTime = System.currentTimeMillis()
                            onNext()
                        },
                        onPrevious = {
                            lastInteractionTime = System.currentTimeMillis()
                            onPrevious()
                        },
                        onSeek = {
                            lastInteractionTime = System.currentTimeMillis()
                            onSeek(it)
                        },
                        onToggleFavorite = {
                            lastInteractionTime = System.currentTimeMillis()
                            onToggleFavorite()
                        },
                        onOpenApp = onOpenApp
                    )
                }
            }
        }
    }
}

@Composable
private fun IdlePillLayout(
    isPlaying: Boolean,
    visualizerStyle: VisualizerStyle,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }

        IslandVisualizer(
            isPlaying = isPlaying,
            audioSessionId = 0,
            visualizerStyle = visualizerStyle,
            tintColor = primaryColor,
            modifier = Modifier
                .width(42.dp)
                .height(14.dp)
        )
    }
}

@Composable
private fun CompactLayout(
    uiState: PlayerUiState,
    visualizerStyle: VisualizerStyle,
    primaryColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit
) {
    val track = uiState.currentSong

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Thumbnail Vinyl with Real Album Art
        SpinningVinylRecord(
            isPlaying = uiState.isPlaying,
            modifier = Modifier.size(28.dp),
            albumArtContent = {
                AuraArtworkImage(
                    song = track,
                    shape = CircleShape,
                    modifier = Modifier.size(14.dp),
                    fallbackIconSize = 8.dp
                )
            }
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onExpand() }
        ) {
            Text(
                text = track?.title ?: "Aura Music",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = track?.artist ?: "Ready",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }

        IslandVisualizer(
            isPlaying = uiState.isPlaying,
            audioSessionId = 0,
            visualizerStyle = visualizerStyle,
            tintColor = primaryColor,
            modifier = Modifier
                .width(36.dp)
                .height(18.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .pointerInput(Unit) { detectTapGestures { onPlayPause() } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LargeCardLayout(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenApp: () -> Unit
) {
    val theme = AuraTheme.current
    val track = uiState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Track Header: Tapping opens app directly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenApp() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpinningVinylRecord(
                isPlaying = uiState.isPlaying,
                modifier = Modifier.size(54.dp),
                albumArtContent = {
                    AuraArtworkImage(
                        song = track,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp),
                        fallbackIconSize = 14.dp
                    )
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "No Track Selected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${track?.artist ?: "Unknown"} • ${track?.album ?: "Aura"}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1
                )
                if (track?.isHiResTrack == true) {
                    Text(
                        text = track?.technicalSummary ?: "HI-RES FLAC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5B041)
                    )
                }
            }

            FavoriteHeartBurst(
                isFavorite = uiState.isFavorite,
                onToggle = onToggleFavorite
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Waveform Seeker
        WaveformSeekBar(
            progress = uiState.progress,
            onSeek = onSeek,
            playedColor = theme.primaryColor,
            unplayedColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.height(28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(uiState.formattedPosition, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            Text(uiState.formattedDuration, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .pointerInput(Unit) { detectTapGestures { onPrevious() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(theme.primaryColor)
                    .pointerInput(Unit) { detectTapGestures { onPlayPause() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .pointerInput(Unit) { detectTapGestures { onNext() } },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
