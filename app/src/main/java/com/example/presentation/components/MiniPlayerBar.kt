package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.core.motion.SpinningVinylRecord
import com.example.core.theme.AuraTheme
import com.example.domain.model.PlayerUiState

/**
 * MiniPlayerBar: Floating glass mini-player bar positioned above the bottom navigation bar.
 * Shows track artwork, title, artist, audio format badge, play/pause, and swipe-to-skip.
 */
@Composable
fun MiniPlayerBar(
    uiState: PlayerUiState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = uiState.currentSong ?: return
    val theme = AuraTheme.current
    var dragDeltaX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .liquidGlassmorphic(
                shape = RoundedCornerShape(22.dp),
                tint = theme.surfaceColor,
                tintAlpha = 0.40f,
                borderBrightness = 0.35f,
                elevation = 12.dp,
                blurRadiusDp = 12f,
                lensAmount = 0.5f,
                chromaticAberration = true
            )
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragDeltaX += dragAmount },
                    onDragEnd = {
                        if (dragDeltaX > 40f) onPrevious() else if (dragDeltaX < -40f) onNext()
                        dragDeltaX = 0f
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Spinning Vinyl
                SpinningVinylRecord(
                    isPlaying = uiState.isPlaying,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSong.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColorPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (currentSong.isHiRes) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD4AF37))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("HI-RES", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                        }
                    }

                    Text(
                        text = currentSong.artist,
                        fontSize = 11.sp,
                        color = theme.textColorSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(theme.surfaceColor.copy(alpha = 0.3f))
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = theme.textColorPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Micro progress line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(theme.textColorSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uiState.progress)
                        .height(2.dp)
                        .background(theme.primaryColor)
                )
            }
        }
    }
}
