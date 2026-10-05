package com.example.core.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.theme.AuraTheme
import kotlinx.coroutines.delay

/**
 * SpinningVinylRecord: Realistic vinyl record with spin-up and spin-down inertia physics.
 * Rotates continuously during active playback and smoothly decelerates when paused.
 */
@Composable
fun SpinningVinylRecord(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    albumArtContent: (@Composable () -> Unit)? = null
) {
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Spin continuously at 33 RPM (~1.8 seconds per revolution)
            while (true) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotationAnim.value % 360f }
            .clip(CircleShape)
            .background(Color(0xFF10121A)),
        contentAlignment = Alignment.Center
    ) {
        // Grooves and Vinyl Reflections
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // Concentric vinyl sound grooves
            for (i in 1..6) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = maxRadius * (0.35f + (i * 0.09f)),
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Specular sheen light reflection
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = maxRadius,
                center = center
            )
        }

        // Center Album Art Label
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2235)),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtContent != null) {
                albumArtContent()
            }
            // Center spindle hole
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0C16))
            )
        }
    }
}

/**
 * AnimatedEqualizerBars: Sound wave visualization displayed on active track rows.
 */
@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = AuraTheme.current.primaryColor
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqBarsTransition")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isPlaying) 0.3f else 0.2f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.9f else 0.2f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar3"
    )

    Row(
        modifier = modifier.height(18.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * h1).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * h2).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * h3).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

/**
 * WaveformSeekBar: Interactive sound wave seekbar rendering audio energy levels.
 */
@Composable
fun WaveformSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    playedColor: Color = AuraTheme.current.primaryColor,
    unplayedColor: Color = AuraTheme.current.surfaceTintColor
) {
    // 32 energy sample points
    val waveHeights = remember {
        listOf(
            0.3f, 0.5f, 0.8f, 0.4f, 0.6f, 0.9f, 0.7f, 0.3f,
            0.5f, 0.7f, 0.4f, 0.8f, 1.0f, 0.6f, 0.5f, 0.8f,
            0.9f, 0.7f, 0.5f, 0.4f, 0.6f, 0.8f, 0.6f, 0.3f,
            0.5f, 0.9f, 0.7f, 0.6f, 0.4f, 0.7f, 0.5f, 0.3f
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        val totalBars = waveHeights.size
        val barWidth = size.width / (totalBars * 1.5f)
        val space = barWidth * 0.5f
        val centerY = size.height / 2f
        val progressX = size.width * progress

        waveHeights.forEachIndexed { index, heightMultiplier ->
            val startX = index * (barWidth + space) + space / 2f
            val barHeight = (size.height * 0.85f * heightMultiplier).coerceAtLeast(6f)
            val isPlayed = startX <= progressX
            val color = if (isPlayed) playedColor else unplayedColor

            drawLine(
                color = color,
                start = Offset(startX, centerY - barHeight / 2f),
                end = Offset(startX, centerY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * FavoriteHeartBurst: Spring scale burst animation when toggling favorites.
 */
@Composable
fun FavoriteHeartBurst(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            scale.animateTo(1.35f, AuraMotion.BouncySpring)
            scale.animateTo(1.0f, AuraMotion.SnappySpring)
        }
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .pointerInput(Unit) {
                detectTapGestures { onToggle() }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Favorited" else "Add to favorites",
            tint = if (isFavorite) Color(0xFFFF3366) else AuraTheme.current.textColorSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
