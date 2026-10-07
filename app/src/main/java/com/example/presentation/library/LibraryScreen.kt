package com.example.presentation.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.di.AuraServiceLocator
import com.example.core.logger.AuraLog
import com.example.core.metadata.SampleHiResTracks
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.theme.AuraTheme
import com.example.data.model.Song
import com.example.presentation.components.AuraArtworkImage
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSurface
import com.example.presentation.components.GlassTextField
import com.example.service.AuraPlayerController
import kotlinx.coroutines.launch

/**
 * LibraryScreen: Primary music browsing experience for Aura Music.
 * Features 4 sub-tabs (Songs, Albums, Artists, Playlists), real-time search,
 * real album artwork on every row, strict single-folder access mode,
 * status bar edge-to-edge protection, and seamless audio playback.
 */
@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AuraServiceLocator.provideSongRepository(context) }
    val scanner = remember { AuraServiceLocator.provideMediaStoreScanner(context) }
    val safScanner = remember { AuraServiceLocator.provideSafFolderScanner(context) }
    val controller = remember { AuraPlayerController.getInstance(context) }
    val songsFromDb by repository.getAllSongs().collectAsState(initial = emptyList())
    val theme = AuraTheme.current

    // Audio storage permission detection
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    // Folder selection & strict single-folder filter state
    var selectedFolderName by remember { mutableStateOf<String?>(null) }
    var selectedFolderUriString by remember { mutableStateOf<String?>(null) }
    var isOnlyFolderMode by remember { mutableStateOf(false) }
    var folderSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            scope.launch {
                isScanning = true
                scanMessage = "Scanning device for music..."
                val count = scanner.scanAudioFiles(minDurationSeconds = 5)
                scanMessage = "Scan complete: found $count songs."
                isScanning = false
            }
        }
    }

    // SAF Folder Picker Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    isScanning = true
                    val lastSegment = uri.lastPathSegment?.substringAfterLast(':') ?: "Music Folder"
                    selectedFolderName = lastSegment
                    selectedFolderUriString = uri.toString()
                    scanMessage = "Scanning $lastSegment..."

                    val safSongs = safScanner.scanAudioFilesInTree(uri)
                    if (safSongs.isNotEmpty()) {
                        folderSongs = safSongs
                        repository.insertSongs(safSongs)
                        isOnlyFolderMode = true // Automatically switch to folder-only mode
                        scanMessage = "Loaded ${safSongs.size} tracks from $lastSegment"
                    } else {
                        scanMessage = "No audio files found in selected folder."
                    }
                } catch (e: Exception) {
                    AuraLog.e("LibraryScreen", "Error scanning SAF folder", e)
                    scanMessage = "Folder scan error: ${e.message}"
                } finally {
                    isScanning = false
                }
            }
        }
    }

    // Auto-scan on screen enter if permission is granted and DB is empty
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission && songsFromDb.isEmpty() && !isScanning && !isOnlyFolderMode) {
            isScanning = true
            scanMessage = "Scanning device for audio..."
            val count = scanner.scanAudioFiles(minDurationSeconds = 5)
            scanMessage = if (count > 0) "Found $count local songs" else "No audio in MediaStore"
            isScanning = false
        }
    }

    // Base songs list according to folder mode
    val baseSongs = remember(songsFromDb, isOnlyFolderMode, folderSongs) {
        if (isOnlyFolderMode && folderSongs.isNotEmpty()) {
            folderSongs
        } else if (songsFromDb.isNotEmpty()) {
            songsFromDb
        } else {
            SampleHiResTracks.tracks
        }
    }

    var selectedTab by remember { mutableStateOf("Songs") }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filteredSongs = remember(baseSongs, searchQuery) {
        if (searchQuery.isBlank()) baseSongs
        else baseSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input
        GlassTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search ${filteredSongs.size} songs, artists, albums, or FLAC...",
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = theme.textColorSecondary, modifier = Modifier.size(18.dp))
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Banner (if not granted)
        if (!hasAudioPermission) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB703).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Access Local Music",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.textColorPrimary
                        )
                        Text(
                            text = "Grant storage permission to see all songs on your device.",
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }
                    GlassButton(
                        onClick = { permissionLauncher.launch(requiredPermission) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Active Folder Filter Indicator Bar (when a folder is selected)
        if (selectedFolderName != null) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = if (isOnlyFolderMode) theme.primaryColor else theme.textColorSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnlyFolderMode) "Showing: ${selectedFolderName} (${folderSongs.size} tracks)" else "Folder loaded: ${selectedFolderName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textColorPrimary,
                            maxLines = 1
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassChip(
                            text = if (isOnlyFolderMode) "Folder Only" else "All Files",
                            isSelected = isOnlyFolderMode,
                            onClick = { isOnlyFolderMode = !isOnlyFolderMode }
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    selectedFolderName = null
                                    selectedFolderUriString = null
                                    isOnlyFolderMode = false
                                    folderSongs = emptyList()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Action & Scan Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = theme.primaryColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = scanMessage ?: "${filteredSongs.size} tracks ${if (isOnlyFolderMode) "in folder" else "in library"}",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Rescan Button
                GlassButton(
                    onClick = {
                        scope.launch {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(requiredPermission)
                            } else {
                                isScanning = true
                                scanMessage = "Rescanning device..."
                                val count = scanner.scanAudioFiles(minDurationSeconds = 5)
                                scanMessage = "Scan complete: found $count songs."
                                isScanning = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = theme.textColorPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescan", fontSize = 11.sp, color = theme.textColorPrimary)
                    }
                }

                // Pick Specific Folder Button (SAF)
                GlassButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Folder", tint = theme.textColorPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Folder", fontSize = 11.sp, color = theme.textColorPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-tabs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Songs", "Albums", "Artists", "Playlists")
            tabs.forEach { tab ->
                GlassChip(
                    text = tab,
                    isSelected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            "Songs" -> {
                val songsToDisplay = if (showOnlyFavorites) filteredSongs.filter { it.isFavorite } else filteredSongs
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassChip(
                            text = "All Songs (${filteredSongs.size})",
                            isSelected = !showOnlyFavorites,
                            onClick = { showOnlyFavorites = false }
                        )
                        GlassChip(
                            text = "Favorites (${baseSongs.count { it.isFavorite }})",
                            isSelected = showOnlyFavorites,
                            onClick = { showOnlyFavorites = true }
                        )
                    }

                    if (songsToDisplay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showOnlyFavorites) "No favorite songs yet. Tap the heart icon on any song to add it!" else "No songs found.",
                                color = theme.textColorSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(songsToDisplay, key = { it.id }) { song ->
                                SongListItem(
                                    song = song,
                                    onClick = {
                                        controller.playSong(song, songsToDisplay)
                                        onSongClick(song)
                                    },
                                    onToggleFavorite = {
                                        controller.toggleFavoriteForSong(song)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            "Albums" -> {
                val albums = remember(filteredSongs) {
                    filteredSongs.groupBy { it.album }.map { (album, tracks) ->
                        AlbumSummary(album, tracks.first().artist, tracks.size, tracks.any { it.isHiResTrack }, tracks.first())
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albums, key = { it.title }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = {
                                val albumTracks = filteredSongs.filter { it.album == album.title }
                                if (albumTracks.isNotEmpty()) {
                                    controller.playSong(albumTracks.first(), albumTracks)
                                    onSongClick(albumTracks.first())
                                }
                            }
                        )
                    }
                }
            }
            "Artists" -> {
                val artists = remember(filteredSongs) {
                    filteredSongs.groupBy { it.artist }.map { (artist, tracks) ->
                        ArtistSummary(artist, tracks.size)
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(artists, key = { it.name }) { artist ->
                        ArtistListItem(
                            artist = artist,
                            onClick = {
                                val artistTracks = filteredSongs.filter { it.artist == artist.name }
                                if (artistTracks.isNotEmpty()) {
                                    controller.playSong(artistTracks.first(), artistTracks)
                                    onSongClick(artistTracks.first())
                                }
                            }
                        )
                    }
                }
            }
            "Playlists" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SmartPlaylistCard(
                            title = "Favorites",
                            subtitle = "${filteredSongs.count { it.isFavorite }} tracks",
                            icon = Icons.Default.Favorite,
                            gradientColors = listOf(Color(0xFFFF3366), Color(0xFFFF6584)),
                            onClick = {
                                val favs = filteredSongs.filter { it.isFavorite }
                                if (favs.isNotEmpty()) {
                                    controller.playSong(favs.first(), favs)
                                    onSongClick(favs.first())
                                }
                                showOnlyFavorites = true
                                selectedTab = "Songs"
                            }
                        )
                    }
                    item {
                        SmartPlaylistCard(
                            title = "Hi-Res Audiophile Collection",
                            subtitle = "${filteredSongs.count { it.isHiResTrack }} Hi-Res / Lossless tracks",
                            icon = Icons.Default.Stars,
                            gradientColors = listOf(Color(0xFFD4AF37), Color(0xFFF5B041)),
                            onClick = {
                                val hiRes = filteredSongs.filter { it.isHiResTrack }
                                if (hiRes.isNotEmpty()) {
                                    controller.playSong(hiRes.first(), hiRes)
                                    onSongClick(hiRes.first())
                                }
                            }
                        )
                    }
                    item {
                        SmartPlaylistCard(
                            title = "Recently Added",
                            subtitle = "${filteredSongs.size} tracks",
                            icon = Icons.Default.PlaylistPlay,
                            gradientColors = listOf(theme.primaryColor, theme.secondaryColor),
                            onClick = {
                                if (filteredSongs.isNotEmpty()) {
                                    controller.playSong(filteredSongs.first(), filteredSongs)
                                    onSongClick(filteredSongs.first())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

data class AlbumSummary(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val isHiRes: Boolean,
    val representativeSong: Song
)

data class ArtistSummary(val name: String, val trackCount: Int)

@Composable
private fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real Album Artwork Thumbnail
            AuraArtworkImage(
                song = song,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp),
                fallbackIconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = theme.textColorPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.isHiResTrack) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD4AF37))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(song.audioBadgeLabel, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        }
                    }
                }
                Text(
                    text = "${song.artist} • ${song.album}",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = song.formattedDuration,
                fontSize = 11.sp,
                color = theme.textColorSecondary
            )

            Spacer(modifier = Modifier.width(8.dp))

            FavoriteHeartBurst(
                isFavorite = song.isFavorite,
                onToggle = onToggleFavorite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit = {}
) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Real Album Artwork Thumbnail
            AuraArtworkImage(
                song = album.representativeSong,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                fallbackIconSize = 48.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = album.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = theme.textColorPrimary,
                maxLines = 1
            )
            Text(
                text = "${album.artist} • ${album.trackCount} tracks",
                fontSize = 11.sp,
                color = theme.textColorSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ArtistListItem(
    artist: ArtistSummary,
    onClick: () -> Unit = {}
) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(theme.primaryColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColorPrimary
                )
                Text(
                    text = "${artist.trackCount} tracks",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit = {}
) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = onClick,
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColorPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = theme.textColorSecondary
                )
            }
        }
    }
}
