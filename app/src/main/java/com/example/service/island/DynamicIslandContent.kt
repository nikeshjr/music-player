package com.example.service.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.motion.SpinningVinylRecord
import com.example.core.motion.WaveformSeekBar
import com.example.core.theme.AuraTheme
import com.example.domain.model.PlayerUiState
import com.example.presentation.components.AuraArtworkImage
import com.example.presentation.components.glassmorphic
import kotlinx.coroutines.delay

/**
 * DynamicIslandContent: Complete UI composable rendering the floating Dynamic Island.
 * Morphs seamlessly between IDLE_PILL, COMPACT, and LARGE_CARD with organic spring physics.
 * Features 5-second auto-collapse on LARGE_CARD, real album artwork, responsive transport buttons, and 1-tap app launch.
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

    // Dynamic width and height animated with fluid critically-damped spring
    val targetWidth = when (currentState) {
        IslandState.IDLE_PILL -> 118.dp * layoutConfig.scaleMultiplier
        IslandState.COMPACT -> 220.dp * layoutConfig.scaleMultiplier
        IslandState.LARGE_CARD -> 348.dp * layoutConfig.scaleMultiplier
    }

    val targetHeight = when (currentState) {
        IslandState.IDLE_PILL -> 36.dp * layoutConfig.scaleMultiplier
        IslandState.COMPACT -> 44.dp * layoutConfig.scaleMultiplier
        IslandState.LARGE_CARD -> 192.dp * layoutConfig.scaleMultiplier
    }

    val targetCornerRadius = when (currentState) {
        IslandState.IDLE_PILL -> 18.dp * layoutConfig.scaleMultiplier
        IslandState.COMPACT -> 22.dp * layoutConfig.scaleMultiplier
        IslandState.LARGE_CARD -> 28.dp * layoutConfig.scaleMultiplier
    }

    // Critically damped spring specification to prevent jittery/stuttering oscillations
    val islandSpec = spring<Dp>(
        dampingRatio = 0.85f,
        stiffness = 320f
    )

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = islandSpec,
        label = "IslandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = islandSpec,
        label = "IslandHeight"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = islandSpec,
        label = "IslandCornerRadius"
    )

    val currentShape = RoundedCornerShape(animatedCornerRadius)

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .clip(currentShape)
            .glassmorphic(
                shape = currentShape,
                tint = theme.islandColor,
                tintAlpha = layoutConfig.opacity,
                borderBrightness = 0.35f,
                elevation = 14.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                lastInteractionTime = System.currentTimeMillis()
                when (currentState) {
                    IslandState.IDLE_PILL -> onStateChange(IslandState.COMPACT)
                    IslandState.COMPACT -> onStateChange(IslandState.LARGE_CARD)
                    IslandState.LARGE_CARD -> { /* Let header click handle app opening */ }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)) +
                 scaleIn(initialScale = 0.94f, animationSpec = tween(durationMillis = 180, delayMillis = 40)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 120)) +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(durationMillis = 120))
                    )
            },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpand() }
        ) {
            Text(
                text = track?.title ?: "Aura Music",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = track?.artist ?: "Ready to play",
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
                .width(32.dp)
                .height(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Play / Pause Button with responsive ripple
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Skip Next Button in Compact View
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Track",
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
        // Track Header: Tapping header opens the main player
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onOpenApp() }
                .padding(4.dp),
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
                    text = track?.title ?: "No Track Playing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (track != null) "${track.artist} • ${track.album}" else "Tap to choose a song",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1
                )
                if (track?.isHiResTrack == true) {
                    Text(
                        text = track.technicalSummary,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Waveform Seeker with live scrubbing
        WaveformSeekBar(
            progress = uiState.progress,
            onSeek = onSeek,
            playedColor = theme.primaryColor,
            unplayedColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.height(28.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(uiState.formattedPosition, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            Text(uiState.formattedDuration, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controls Row: previous, play/pause, next with responsive ripples and touch targets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onPrevious() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play / Pause button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(theme.primaryColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Next button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
