package com.example.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.core.motion.AnimatedEqualizerBars
import com.example.core.theme.AuraTheme
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.service.AuraPlayerController

/**
 * QueueScreen: Displays active playback queue, active track marker with live equalizer bars,
 * and track re-ordering / removal options.
 */
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val theme = AuraTheme.current

    val queue = if (uiState.queue.isNotEmpty()) uiState.queue else SampleHiResTracks.tracks

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(onClick = onNavigateBack, shape = CircleShape) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.textColorPrimary, modifier = Modifier.size(20.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Playing Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColorPrimary
                )
                Text(
                    text = "${queue.size} tracks queued",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary
                )
            }

            Box(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(queue) { index, track ->
                val isCurrent = track.id == uiState.currentSong?.id

                GlassCard(
                    onClick = { controller.playSong(track, queue) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrent) theme.primaryColor else theme.textColorSecondary,
                            modifier = Modifier.width(22.dp)
                        )

                        com.example.presentation.components.AuraArtworkImage(
                            song = track,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp),
                            fallbackIconSize = 16.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCurrent) theme.primaryColor else theme.textColorPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${track.artist} • ${track.album}",
                                fontSize = 11.sp,
                                color = theme.textColorSecondary,
                                maxLines = 1
                            )
                        }

                        if (isCurrent && uiState.isPlaying) {
                            AnimatedEqualizerBars(isPlaying = true)
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Text(
                            text = track.formattedDuration,
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }
                }
            }
        }
    }
}
