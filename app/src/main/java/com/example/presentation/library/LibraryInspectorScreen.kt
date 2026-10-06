package com.example.presentation.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AuraServiceLocator
import com.example.core.metadata.SampleHiResTracks
import com.example.core.theme.AuraTheme
import com.example.data.metadata.CueSheetParser
import com.example.data.metadata.LrcLyricsParser
import com.example.data.model.Song
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSlider
import com.example.presentation.components.GlassSurface
import kotlinx.coroutines.launch

/**
 * LibraryInspectorScreen: Presentation and diagnostic screen for the Phase 3 Data Layer.
 * Allows manual and background MediaStore audio scanning, SAF sidecar discovery,
 * CUE sheet track splitting verification, LRC synced lyric preview, and JSON library backups.
 */
@Composable
fun LibraryInspectorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AuraServiceLocator.provideSongRepository(context) }
    val backupManager = remember { AuraServiceLocator.provideLibraryBackupManager(context) }
    val preferences = remember { AuraServiceLocator.providePreferences(context) }
    val theme = AuraTheme.current

    val songs by repository.getAllSongs().collectAsState(initial = emptyList())
    var isScanning by remember { mutableStateOf(false) }
    var scanResultCount by remember { mutableIntStateOf(-1) }
    var minDurationSeconds by remember { mutableIntStateOf(30) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // SAF Backup Launchers
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = backupManager.exportBackupToUri(uri)
                statusMessage = if (res.isSuccess) "Backup exported successfully!" else "Backup export failed."
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = backupManager.restoreBackupFromUri(uri)
                statusMessage = if (res.isSuccess) "Restored ${res.getOrNull()} items!" else "Backup restore failed."
            }
        }
    }

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
                        text = "Data Layer & Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = "FLAC Vorbis • CUE Sheets • LRC Synced • Room DB",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textColorSecondary
                    )
                }
                GlassButton(onClick = onNavigateBack) {
                    Text("Back", color = theme.textColorPrimary, fontSize = 12.sp)
                }
            }
        }

        // Scanner Control Card
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "MediaStore Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorPrimary
                            )
                            Text(
                                text = "Indexes MP3, FLAC, WAV, AAC, OGG, OPUS, WMA",
                                fontSize = 11.sp,
                                color = theme.textColorSecondary
                            )
                        }

                        GlassButton(
                            onClick = {
                                if (!isScanning) {
                                    isScanning = true
                                    scope.launch {
                                        val count = repository.scanMediaStore(minDurationSeconds)
                                        scanResultCount = count
                                        isScanning = false
                                    }
                                }
                            },
                            accentColor = theme.primaryColor
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scanning...", fontSize = 12.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Minimum Duration Filter: ${minDurationSeconds}s",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = minDurationSeconds.toFloat(),
                        onValueChange = { minDurationSeconds = it.toInt() },
                        valueRange = 0f..120f
                    )

                    if (scanResultCount >= 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Last scan indexed $scanResultCount tracks into Room.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            color = theme.primaryColor
                        )
                    }
                }
            }
        }

        // Library Backup / Restore Actions
        item {
            Text(
                text = "Library Backup & Restore (SAF JSON)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    onClick = { backupExportLauncher.launch("aura_library_backup.json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup", fontSize = 12.sp, color = Color.White)
                    }
                }
                GlassButton(
                    onClick = { backupImportLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Backup", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // CUE Sheet & LRC Parser Verification Box
        item {
            Text(
                text = "CUE Sheet & LRC Parsers Verification",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val sampleCue = remember {
                        """
                        PERFORMER "Pink Floyd"
                        TITLE "The Dark Side of the Moon"
                        FILE "album.flac" WAVE
                          TRACK 01 AUDIO
                            TITLE "Speak to Me / Breathe"
                            INDEX 01 00:00:00
                          TRACK 02 AUDIO
                            TITLE "On the Run"
                            INDEX 01 03:58:00
                          TRACK 03 AUDIO
                            TITLE "Time"
                            INDEX 01 07:29:00
                        """.trimIndent()
                    }
                    val parsedCue = remember(sampleCue) { CueSheetParser.parse(sampleCue, 43 * 60 * 1000L) }

                    Text(
                        text = "Parsed CUE Album: ${parsedCue.albumTitle} (${parsedCue.tracks.size} virtual tracks)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.primaryColor
                    )
                    parsedCue.tracks.forEach { t ->
                        Text(
                            text = "• #${t.trackNumber} ${t.title} [${t.startOffsetMs / 1000}s - ${(t.startOffsetMs + t.durationMs) / 1000}s]",
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sampleLrc = remember {
                        """
                        [00:05.20]Ticking away the moments that make up a dull day
                        [00:12.40]Fritter and waste the hours in an offhand way
                        [00:19.80]Kicking around on a piece of ground in your hometown
                        """.trimIndent()
                    }
                    val parsedLrc = remember(sampleLrc) { LrcLyricsParser.parse(sampleLrc) }
                    Text(
                        text = "Parsed LRC: ${parsedLrc.lines.size} synchronized lines (offset search ready)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.secondaryColor
                    )
                    Text(
                        text = "Active line at 14s: \"${parsedLrc.lines.getOrNull(parsedLrc.findCurrentLineIndex(14000L))?.text ?: ""}\"",
                        fontSize = 11.sp,
                        color = theme.textColorSecondary
                    )
                }
            }
        }

        // Room Database Songs Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Indexed Tracks in Room (${if (songs.isNotEmpty()) songs.size else SampleHiResTracks.tracks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColorPrimary
                )
            }
        }

        val displaySongs = if (songs.isNotEmpty()) songs else SampleHiResTracks.tracks

        items(displaySongs) { song ->
            TrackInspectionCard(song = song)
        }
    }
}

@Composable
fun TrackInspectionCard(song: Song) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (song.isHiResTrack) Color(0xFFE5A93C).copy(alpha = 0.2f) else theme.primaryColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = if (song.isHiResTrack) Color(0xFFF5B041) else theme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = theme.textColorPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Hi-Res Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (song.isHiResTrack) Color(0xFFD4AF37) else theme.primaryColor.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.audioBadgeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (song.isHiResTrack) Color.Black else Color.White
                        )
                    }
                }

                Text(
                    text = "${song.artist} • ${song.album}",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary,
                    maxLines = 1
                )

                Text(
                    text = song.technicalSummary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.secondaryColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = song.formattedDuration,
                fontSize = 11.sp,
                color = theme.textColorSecondary
            )
        }
    }
}
