package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AuraServiceLocator
import com.example.core.logger.AuraLog
import com.example.core.theme.AuraTheme
import com.example.core.theme.AuraThemeProvider
import com.example.core.theme.AuraThemeState
import com.example.presentation.appearance.AppearanceScreen
import com.example.presentation.components.liquidglass.backdrop.backdrops.layerBackdrop
import com.example.presentation.components.GlassBottomBar
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSurface
import com.example.presentation.components.MiniPlayerBar
import com.example.presentation.island.IslandCalibrationScreen
import com.example.presentation.library.LibraryInspectorScreen
import com.example.presentation.library.LibraryScreen
import com.example.presentation.lyrics.LyricsScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.player.AudioEngineInspectionScreen
import com.example.presentation.player.NowPlayingScreen
import com.example.presentation.player.QueueScreen
import com.example.service.AuraPlayerController
import kotlinx.coroutines.launch

/**
 * MainActivity: Single-activity architecture entry point for Aura Music.
 * Coordinates type-safe navigation between Library, Now Playing, Synchronized Lyrics,
 * Queue, Audio Effects, Appearance, and Dynamic Island Calibration.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AuraLog.i("MainActivity", "Aura Music MainActivity launched.")

        try {
            val serviceIntent = android.content.Intent(this, com.example.service.PlaybackService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            AuraLog.w("MainActivity", "Could not start PlaybackService: ${e.message}")
        }

        setContent {
            val context = LocalContext.current
            val preferences = remember { AuraServiceLocator.providePreferences(context) }
            val themeState by preferences.themeState.collectAsState(initial = AuraThemeState())
            val isOnboardingCompleted by preferences.isOnboardingCompleted.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            AuraThemeProvider(themeState = themeState) {
                var currentScreen by remember { mutableStateOf("library") }

                Crossfade(
                    targetState = Pair(isOnboardingCompleted, currentScreen),
                    animationSpec = tween(350),
                    label = "MainScreenTransition"
                ) { (completed, screen) ->
                    if (!completed) {
                        OnboardingScreen(
                            onFinished = {
                                AuraLog.i("MainActivity", "Onboarding completed by user.")
                            }
                        )
                    } else {
                        when (screen) {
                            "now_playing" -> {
                                BackHandler { currentScreen = "library" }
                                NowPlayingScreen(
                                    onNavigateBack = { currentScreen = "library" },
                                    onOpenLyrics = { currentScreen = "lyrics" },
                                    onOpenQueue = { currentScreen = "queue" },
                                    onOpenEqualizer = { currentScreen = "audio_inspector" }
                                )
                            }
                            "lyrics" -> {
                                BackHandler { currentScreen = "now_playing" }
                                LyricsScreen(onNavigateBack = { currentScreen = "now_playing" })
                            }
                            "queue" -> {
                                BackHandler { currentScreen = "now_playing" }
                                QueueScreen(onNavigateBack = { currentScreen = "now_playing" })
                            }
                            "appearance" -> {
                                BackHandler { currentScreen = "library" }
                                AppearanceScreen(onNavigateBack = { currentScreen = "library" })
                            }
                            "library_inspector" -> {
                                BackHandler { currentScreen = "dashboard" }
                                LibraryInspectorScreen(onNavigateBack = { currentScreen = "dashboard" })
                            }
                            "audio_inspector" -> {
                                BackHandler { currentScreen = "dashboard" }
                                AudioEngineInspectionScreen(onNavigateBack = { currentScreen = "dashboard" })
                            }
                            "island_calibration" -> {
                                BackHandler { currentScreen = "dashboard" }
                                IslandCalibrationScreen(onNavigateBack = { currentScreen = "dashboard" })
                            }
                            else -> {
                                AuraAppShell(
                                    currentTab = screen,
                                    onTabSelected = { currentScreen = it },
                                    onOpenNowPlaying = { currentScreen = "now_playing" },
                                    onOpenAppearance = { currentScreen = "appearance" },
                                    onOpenLibraryInspector = { currentScreen = "library_inspector" },
                                    onOpenAudioInspector = { currentScreen = "audio_inspector" },
                                    onOpenIslandCalibration = { currentScreen = "island_calibration" },
                                    onResetOnboarding = {
                                        scope.launch { preferences.setOnboardingCompleted(false) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AuraAppShell: Core navigation shell hosting Library and Dashboard tabs,
 * docked persistent MiniPlayerBar, and the floating glass bottom bar.
 */
@Composable
fun AuraAppShell(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenLibraryInspector: () -> Unit,
    onOpenAudioInspector: () -> Unit,
    onOpenIslandCalibration: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val theme = AuraTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (theme.isLight) {
                        listOf(
                            theme.backgroundColor,
                            Color(0xFFEFF2F8),
                            Color(0xFFE2E8F0)
                        )
                    } else if (theme.isAmoled) {
                        listOf(
                            Color.Black,
                            Color.Black,
                            Color.Black
                        )
                    } else {
                        listOf(
                            theme.backgroundColor,
                            Color(0xFF070810),
                            Color(0xFF030408)
                        )
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    // Persistent Mini Player above bottom bar with glassmorphic styling
                    if (uiState.currentSong != null) {
                        MiniPlayerBar(
                            uiState = uiState,
                            onClick = onOpenNowPlaying,
                            onPlayPause = { controller.togglePlayPause() },
                            onNext = { controller.next() },
                            onPrevious = { controller.previous() }
                        )
                    }

                    // Floating Glass Bottom Bar
                    GlassBottomBar {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassChip(
                                text = "Library",
                                isSelected = currentTab == "library",
                                onClick = { onTabSelected("library") }
                            )
                            GlassChip(
                                text = "Dashboard",
                                isSelected = currentTab == "dashboard",
                                onClick = { onTabSelected("dashboard") }
                            )
                            GlassChip(
                                text = "Styling",
                                isSelected = currentTab == "appearance",
                                onClick = onOpenAppearance
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                    if (currentTab == "library") {
                        LibraryScreen(
                            onSongClick = { onOpenNowPlaying() }
                        )
                    } else {
                        AuraDashboardContent(
                            onOpenAppearance = onOpenAppearance,
                            onOpenLibraryInspector = onOpenLibraryInspector,
                            onOpenAudioInspector = onOpenAudioInspector,
                            onOpenIslandCalibration = onOpenIslandCalibration,
                            onResetOnboarding = onResetOnboarding
                        )
                    }
                }
            }
        }
    }

@Composable
fun AuraDashboardContent(
    onOpenAppearance: () -> Unit,
    onOpenLibraryInspector: () -> Unit,
    onOpenAudioInspector: () -> Unit,
    onOpenIslandCalibration: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    val theme = AuraTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header Brand Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(theme.primaryColor, theme.secondaryColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Aura Music Logo",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AURA MUSIC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.textColorPrimary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "100% Offline • Dynamic Island • Hi-Res FLAC",
                    fontSize = 11.sp,
                    color = theme.textColorSecondary
                )
            }

            GlassButton(
                onClick = onResetOnboarding,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    color = theme.textColorPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Engine & Feature Diagnostics",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textColorPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        ModuleBadge(
            icon = Icons.Default.Security,
            title = "Phase 1: Manifest, Permissions & OEM Guides",
            subtitle = "SpecialUse FGS, SYSTEM_ALERT_WINDOW, vivo/iQOO survival"
        )
        Spacer(modifier = Modifier.height(10.dp))

        ModuleBadge(
            icon = Icons.Default.Palette,
            title = "Phase 2: Ultra-Premium Glassmorphism & Themes",
            subtitle = "12 Presets, Dynamic Island sync, custom HSV picker",
            onClick = onOpenAppearance
        )
        Spacer(modifier = Modifier.height(10.dp))

        ModuleBadge(
            icon = Icons.Default.Audiotrack,
            title = "Phase 3: MediaStore, FLAC & Room Data Layer",
            subtitle = "Hi-Res FLAC 24-bit/192kHz, Vorbis tags, CUE sheets",
            onClick = onOpenLibraryInspector
        )
        Spacer(modifier = Modifier.height(10.dp))

        ModuleBadge(
            icon = Icons.Default.GraphicEq,
            title = "Phase 4: Media3 ExoPlayer & Audio Engine",
            subtitle = "Gapless, crossfade, 10-band EQ, ReplayGain",
            onClick = onOpenAudioInspector
        )
        Spacer(modifier = Modifier.height(10.dp))

        ModuleBadge(
            icon = Icons.Default.Dashboard,
            title = "Phase 5: Universal Floating Dynamic Island",
            subtitle = "Pill to large card morph, audio visualizer, gesture control",
            onClick = onOpenIslandCalibration
        )
    }
}

@Composable
fun ModuleBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.primaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColorPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = theme.textColorSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
