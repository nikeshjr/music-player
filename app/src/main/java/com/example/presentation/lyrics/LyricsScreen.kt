package com.example.presentation.lyrics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.metadata.SampleHiResTracks
import com.example.core.theme.AuraTheme
import com.example.data.metadata.LrcLyricsParser
import com.example.presentation.components.GlassButton
import com.example.service.AuraPlayerController

/**
 * LyricsScreen: Karaoke-style synchronized line-by-line lyrics view.
 * Highlights the active line with radiant glow, dims background lines,
 * automatically scrolls to keep the current singing line centered,
 * and allows clicking any line to immediately jump player position.
 */
@Composable
fun LyricsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val theme = AuraTheme.current

    val currentSong = uiState.currentSong ?: SampleHiResTracks.tracks.first()
    val rawLyrics = currentSong.embeddedLyrics ?: """
        [00:04.00]Ticking away the moments that make up a dull day
        [00:11.40]Fritter and waste the hours in an offhand way
        [00:18.80]Kicking around on a piece of ground in your hometown
        [00:26.00]Waiting for someone or something to show you the way
        [00:33.20]Tired of lying in the sunshine, staying home to watch the rain
        [00:41.00]You are young and life is long, and there is time to kill today
        [00:48.50]And then one day you find ten years have got behind you
        [00:56.00]No one told you when to run, you missed the starting gun
    """.trimIndent()

    val parsedLyrics = remember(rawLyrics) { LrcLyricsParser.parse(rawLyrics) }
    val activeIndex = remember(uiState.currentPositionMs, parsedLyrics) {
        parsedLyrics.findCurrentLineIndex(uiState.currentPositionMs)
    }

    val listState = rememberLazyListState()

    // Smooth auto-scroll to keep active line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex in 0 until parsedLyrics.lines.size) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (theme.isLight) {
                        listOf(
                            theme.primaryColor.copy(alpha = 0.15f),
                            theme.backgroundColor,
                            Color(0xFFE2E8F0)
                        )
                    } else {
                        listOf(
                            theme.primaryColor.copy(alpha = 0.3f),
                            theme.backgroundColor,
                            Color(0xFF030408)
                        )
                    }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(onClick = onNavigateBack, shape = CircleShape) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.textColorPrimary, modifier = Modifier.size(20.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentSong.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = currentSong.artist,
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                }

                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lyrics List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(parsedLyrics.lines) { index, line ->
                    val isActive = index == activeIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { controller.seekTo(line.timestampMs) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = line.text,
                            fontSize = if (isActive) 22.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isActive) Color.White else theme.textColorSecondary.copy(alpha = 0.5f),
                            lineHeight = if (isActive) 28.sp else 22.sp
                        )
                    }
                }
            }
        }
    }
}
