package com.example.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.oem.OemHelper
import com.example.core.theme.AuraTheme
import com.example.core.theme.PalettePreset
import com.example.core.theme.ThemeMode
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSurface
import com.example.presentation.oem.VivoIqooSurvivalGuide
import kotlinx.coroutines.launch

/**
 * OnboardingScreen: Interactive glassmorphic wizard introducing Aura Music.
 * Handles granular permissions, persistent SAF library folder access, theme personalization,
 * and specialized OEM background optimization setups.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AuraServiceLocator.providePreferences(context) }
    val theme = AuraTheme.current

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4 // 0: Welcome, 1: Permissions, 2: Style, 3: Library Folder & Ready
    var showVivoGuide by remember { mutableStateOf(false) }

    // Permission tracking states
    var isAudioPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var isOverlayPermissionGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }

    // Activity Result Launchers
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isAudioPermissionGranted = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationPermissionGranted = granted
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                selectedFolderUri = uri
                AuraLog.i("Onboarding", "Persisted SAF library folder URI: $uri")
            } catch (e: Exception) {
                AuraLog.e("Onboarding", "Failed persisting SAF URI permission", e)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showVivoGuide) {
                VivoIqooSurvivalGuide(onDismiss = { showVivoGuide = false })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Step Progress Indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until totalSteps) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i <= currentStep) theme.primaryColor else Color.White.copy(alpha = 0.15f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wizard Content
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn(tween(300)))
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn(tween(300)))
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = "OnboardingWizardAnimation"
                    ) { step ->
                        when (step) {
                            0 -> WelcomeStep(theme = theme)
                            1 -> PermissionsStep(
                                isAudioGranted = isAudioPermissionGranted,
                                isNotificationGranted = isNotificationPermissionGranted,
                                isOverlayGranted = isOverlayPermissionGranted,
                                onRequestAudio = {
                                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    audioPermissionLauncher.launch(perm)
                                },
                                onRequestNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestOverlay = {
                                    OemHelper.openOverlaySettings(context)
                                    isOverlayPermissionGranted = Settings.canDrawOverlays(context)
                                },
                                onRequestBattery = {
                                    OemHelper.requestIgnoreBatteryOptimizations(context)
                                },
                                onShowOemGuide = { showVivoGuide = true }
                            )
                            2 -> StylePersonalizerStep(
                                preferences = preferences,
                                theme = theme
                            )
                            3 -> LibraryFolderAndReadyStep(
                                selectedFolderUri = selectedFolderUri,
                                onPickFolder = { safFolderLauncher.launch(null) }
                            )
                        }
                    }

                    // Navigation Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            GlassButton(
                                onClick = { currentStep-- }
                            ) {
                                Text("Back", color = theme.textColorPrimary)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        GlassButton(
                            onClick = {
                                if (currentStep < totalSteps - 1) {
                                    currentStep++
                                } else {
                                    scope.launch {
                                        preferences.setOnboardingCompleted(true)
                                        onFinished()
                                    }
                                }
                            },
                            accentColor = theme.primaryColor
                        ) {
                            Text(
                                text = if (currentStep == totalSteps - 1) "Get Started" else "Next",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(theme: com.example.core.theme.AuraThemeState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(theme.primaryColor, theme.secondaryColor.copy(alpha = 0.4f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Aura Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = theme.textColorPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "100% Offline • Universal Dynamic Island • Hi-Res FLAC",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.secondaryColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        GlassSurface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Aura never collects data and requires no internet access. Experience audiophile audio with zero compromises, system-wide floating controls, and bespoke glass aesthetics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textColorSecondary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    isAudioGranted: Boolean,
    isNotificationGranted: Boolean,
    isOverlayGranted: Boolean,
    onRequestAudio: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBattery: () -> Unit,
    onShowOemGuide: () -> Unit
) {
    val theme = AuraTheme.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Permissions & Island Setup",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = theme.textColorPrimary
        )
        Text(
            text = "Aura requires minimal permissions solely to locate your music and display floating controls.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.textColorSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        PermissionRowItem(
            icon = Icons.Default.Audiotrack,
            title = "Audio Storage Access",
            subtitle = "Required to scan and index local MP3, FLAC, and WAV files.",
            isGranted = isAudioGranted,
            onRequest = onRequestAudio
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionRowItem(
            icon = Icons.Default.Layers,
            title = "Display Floating Overlay",
            subtitle = "Powers the universal Dynamic Island over apps and launcher.",
            isGranted = isOverlayGranted,
            onRequest = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionRowItem(
            icon = Icons.Default.Notifications,
            title = "Media Notifications",
            subtitle = "Standard playback controls in status bar and lockscreen.",
            isGranted = isNotificationGranted,
            onRequest = onRequestNotification
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionRowItem(
            icon = Icons.Default.BatteryChargingFull,
            title = "Unrestricted Battery",
            subtitle = "Prevents aggressive Android battery kill during playback.",
            isGranted = false,
            actionText = "Configure",
            onRequest = onRequestBattery
        )

        if (OemHelper.isVivoOrIqoo) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                onClick = onShowOemGuide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "vivo / iQOO Device Detected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.textColorPrimary
                        )
                        Text(
                            text = "Tap to open our 5-step OriginOS survival guide",
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionText: String = if (isGranted) "Granted" else "Grant",
    onRequest: () -> Unit
) {
    val theme = AuraTheme.current
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else theme.primaryColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF10B981) else theme.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = theme.textColorPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = theme.textColorSecondary,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                GlassButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(10.dp),
                    accentColor = theme.primaryColor
                ) {
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StylePersonalizerStep(
    preferences: com.example.data.local.datastore.AuraPreferences,
    theme: com.example.core.theme.AuraThemeState
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Pick Your Aesthetic",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = theme.textColorPrimary
        )
        Text(
            text = "Personalize Aura's colors and glass styling immediately. You can fine-tune every parameter in Settings later.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.textColorSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Palette presets horizontal list
        Text(
            text = "Color Palette",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = theme.textColorPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(PalettePreset.entries.toTypedArray()) { preset ->
                val isSelected = theme.palettePreset == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        scope.launch { preferences.updatePalettePreset(preset) }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(preset.primary, preset.secondary))
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = preset.displayName,
                        fontSize = 11.sp,
                        color = if (isSelected) theme.primaryColor else theme.textColorSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Theme Mode",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = theme.textColorPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassChip(
                text = "Dark",
                isSelected = theme.themeMode == ThemeMode.DARK,
                onClick = { scope.launch { preferences.updateThemeMode(ThemeMode.DARK) } }
            )
            GlassChip(
                text = "AMOLED Black",
                isSelected = theme.themeMode == ThemeMode.AMOLED_BLACK,
                onClick = { scope.launch { preferences.updateThemeMode(ThemeMode.AMOLED_BLACK) } }
            )
            GlassChip(
                text = "Light",
                isSelected = theme.themeMode == ThemeMode.LIGHT,
                onClick = { scope.launch { preferences.updateThemeMode(ThemeMode.LIGHT) } }
            )
        }
    }
}

@Composable
private fun LibraryFolderAndReadyStep(
    selectedFolderUri: Uri?,
    onPickFolder: () -> Unit
) {
    val theme = AuraTheme.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Library Folders & Lyrics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = theme.textColorPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Android's MediaStore cannot read sidecar files (.lrc lyrics, .cue track sheets, and .m3u playlists). You can grant SAF folder access now or configure it anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.textColorSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        GlassCard(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = theme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedFolderUri != null) "Folder Selected" else "Select Music Folder",
                        fontWeight = FontWeight.Bold,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = selectedFolderUri?.lastPathSegment ?: "Optional: Enables .lrc sidecar lyrics & CUE sheets",
                        fontSize = 11.sp,
                        color = theme.textColorSecondary,
                        maxLines = 1
                    )
                }

                if (selectedFolderUri != null) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Configured",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
