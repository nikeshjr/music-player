package com.example.presentation.island

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.metadata.SampleHiResTracks
import com.example.core.oem.OemHelper
import com.example.core.theme.AuraTheme
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassSlider
import com.example.presentation.components.GlassSurface
import com.example.presentation.components.GlassSwitch
import com.example.service.AuraPlayerController
import com.example.service.DynamicIslandService
import com.example.service.island.CutoutAlignment
import com.example.service.island.DynamicIslandContent
import com.example.service.island.IslandLayoutConfig
import com.example.service.island.IslandState
import com.example.service.island.VisualizerStyle

/**
 * IslandCalibrationScreen: Interactive in-app calibration environment.
 * Features a draggable island preview with simulated camera punch-hole,
 * geometry tuning (X/Y offsets, scale, corner radius, opacity), cutout alignment,
 * and the vivo/iQOO overlay health check with green/amber/red status indicators.
 */
@Composable
fun IslandCalibrationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AuraPlayerController.getInstance(context) }
    val uiState by controller.uiState.collectAsState()
    val theme = AuraTheme.current

    var islandState by remember { mutableStateOf(IslandState.COMPACT) }
    var scaleMultiplier by remember { mutableFloatStateOf(1.0f) }
    var cornerRadiusDp by remember { mutableIntStateOf(28) }
    var opacity by remember { mutableFloatStateOf(0.95f) }
    var offsetY by remember { mutableIntStateOf(14) }
    var cutoutAlignment by remember { mutableStateOf(CutoutAlignment.CENTER) }
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.SYMMETRIC_BARS) }

    var isServiceRunning by remember { mutableStateOf(false) }

    val layoutConfig = IslandLayoutConfig(
        scaleMultiplier = scaleMultiplier,
        cornerRadiusDp = cornerRadiusDp,
        opacity = opacity,
        offsetY = offsetY,
        cutoutAlignment = cutoutAlignment,
        visualizerStyle = visualizerStyle
    )

    // Check system permissions
    val hasOverlay = Settings.canDrawOverlays(context)
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isBatteryOptimized = powerManager.isIgnoringBatteryOptimizations(context.packageName)

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
                        text = "Dynamic Island Setup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = "Cutout calibration • Morphing spring • OEM health",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textColorSecondary
                    )
                }
                GlassButton(onClick = onNavigateBack) {
                    Text("Back", color = theme.textColorPrimary, fontSize = 12.sp)
                }
            }
        }

        // Service Master Toggle
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Floating Island Overlay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = theme.textColorPrimary
                        )
                        Text(
                            text = if (isServiceRunning) "Running system-wide over all apps" else "Service currently stopped",
                            fontSize = 11.sp,
                            color = if (isServiceRunning) Color(0xFF10B981) else theme.textColorSecondary
                        )
                    }

                    GlassButton(
                        onClick = {
                            if (!hasOverlay) {
                                OemHelper.openOverlaySettings(context)
                            } else {
                                if (isServiceRunning) {
                                    DynamicIslandService.stop(context)
                                    isServiceRunning = false
                                } else {
                                    DynamicIslandService.start(context)
                                    isServiceRunning = true
                                }
                            }
                        },
                        accentColor = if (isServiceRunning) Color(0xFFEF4444) else theme.primaryColor
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!hasOverlay) "Grant Permission" else if (isServiceRunning) "Stop Overlay" else "Start Overlay",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Interactive Draggable Preview Area
        item {
            Text(
                text = "Interactive Screen Top Simulation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF070810))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Simulated Camera Hole Cutout
                val cutoutAlignmentMod = when (cutoutAlignment) {
                    CutoutAlignment.CENTER -> Modifier.align(Alignment.TopCenter)
                    CutoutAlignment.LEFT -> Modifier.align(Alignment.TopStart).padding(start = 24.dp)
                    CutoutAlignment.RIGHT -> Modifier.align(Alignment.TopEnd).padding(end = 24.dp)
                }

                Box(
                    modifier = cutoutAlignmentMod
                        .padding(top = 8.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )

                // The Island Preview with live morphing
                val testState = uiState.copy(
                    currentSong = uiState.currentSong ?: SampleHiResTracks.tracks.first(),
                    isPlaying = true
                )

                Box(modifier = Modifier.padding(top = offsetY.dp)) {
                    DynamicIslandContent(
                        uiState = testState,
                        layoutConfig = layoutConfig,
                        currentState = islandState,
                        onStateChange = { islandState = it },
                        onPlayPause = { controller.togglePlayPause() },
                        onNext = { controller.next() },
                        onPrevious = { controller.previous() },
                        onSeek = { controller.seekToProgress(it) },
                        onToggleFavorite = { controller.toggleFavorite() },
                        onDismissPill = { }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // State Test Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassChip(
                    text = "Idle Pill",
                    isSelected = islandState == IslandState.IDLE_PILL,
                    onClick = { islandState = IslandState.IDLE_PILL },
                    modifier = Modifier.weight(1f)
                )
                GlassChip(
                    text = "Compact",
                    isSelected = islandState == IslandState.COMPACT,
                    onClick = { islandState = IslandState.COMPACT },
                    modifier = Modifier.weight(1f)
                )
                GlassChip(
                    text = "Large Card",
                    isSelected = islandState == IslandState.LARGE_CARD,
                    onClick = { islandState = IslandState.LARGE_CARD },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Geometry Tuning Sliders
        item {
            Text(
                text = "Geometry & Visual Tuning",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Camera Cutout Position",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            text = "Center Cutout",
                            isSelected = cutoutAlignment == CutoutAlignment.CENTER,
                            onClick = { cutoutAlignment = CutoutAlignment.CENTER }
                        )
                        GlassChip(
                            text = "Left Cutout",
                            isSelected = cutoutAlignment == CutoutAlignment.LEFT,
                            onClick = { cutoutAlignment = CutoutAlignment.LEFT }
                        )
                        GlassChip(
                            text = "Right Cutout",
                            isSelected = cutoutAlignment == CutoutAlignment.RIGHT,
                            onClick = { cutoutAlignment = CutoutAlignment.RIGHT }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Scale Multiplier (${(scaleMultiplier * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = scaleMultiplier,
                        onValueChange = { scaleMultiplier = it },
                        valueRange = 0.8f..1.3f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Vertical Distance from Cutout (${offsetY} dp)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = offsetY.toFloat(),
                        onValueChange = { offsetY = it.toInt() },
                        valueRange = 4f..36f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Visualizer Style",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualizerStyle.entries.forEach { style ->
                            GlassChip(
                                text = style.displayName,
                                isSelected = visualizerStyle == style,
                                onClick = { visualizerStyle = style }
                            )
                        }
                    }
                }
            }
        }

        // vivo / iQOO Overlay Health Check Card
        item {
            Text(
                text = "OEM Overlay Health Check",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HealthStatusItem(
                        title = "Display over other apps",
                        isPassed = hasOverlay,
                        onFix = { OemHelper.openOverlaySettings(context) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HealthStatusItem(
                        title = "Unrestricted Battery",
                        isPassed = isBatteryOptimized,
                        onFix = { OemHelper.requestIgnoreBatteryOptimizations(context) }
                    )

                    if (OemHelper.isVivoOrIqoo) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HealthStatusItem(
                            title = "vivo: Display pop-ups in background",
                            isPassed = false,
                            onFix = { OemHelper.openVivoBackgroundPopups(context) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HealthStatusItem(
    title: String,
    isPassed: Boolean,
    onFix: () -> Unit
) {
    val theme = AuraTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isPassed) Color(0xFF10B981) else Color(0xFFFFB703),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColorPrimary
            )
        }

        if (!isPassed) {
            GlassButton(
                onClick = onFix,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
