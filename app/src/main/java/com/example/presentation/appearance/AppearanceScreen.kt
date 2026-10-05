package com.example.presentation.appearance

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.core.di.AuraServiceLocator
import com.example.core.motion.AnimatedEqualizerBars
import com.example.core.motion.FavoriteHeartBurst
import com.example.core.motion.SpinningVinylRecord
import com.example.core.motion.WaveformSeekBar
import com.example.core.theme.AuraTheme
import com.example.core.theme.FontManager
import com.example.core.theme.PalettePreset
import com.example.core.theme.ThemeMode
import com.example.presentation.components.GlassButton
import com.example.presentation.components.GlassCard
import com.example.presentation.components.GlassChip
import com.example.presentation.components.GlassDialog
import com.example.presentation.components.GlassSlider
import com.example.presentation.components.GlassSurface
import com.example.presentation.components.GlassSwitch
import com.example.presentation.components.HsvColorPicker
import kotlinx.coroutines.launch

/**
 * AppearanceScreen: The comprehensive customization hub for Aura Music.
 * Empowers the user to configure palette presets, custom HSV colors, glass blur/opacity,
 * offline font typography, custom .ttf/.otf SAF font imports, motion speeds, and theme profiles.
 */
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AuraServiceLocator.providePreferences(context) }
    val profileManager = remember { AuraServiceLocator.provideThemeProfileManager(context) }
    val theme = AuraTheme.current

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var activeColorSlot by remember { mutableStateOf("Primary") }
    var previewSeekProgress by remember { mutableFloatStateOf(0.42f) }
    var previewIsFavorite by remember { mutableStateOf(true) }

    // SAF launchers for Font and Theme JSON
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                FontManager.importFontFromUri(context, uri, "Custom Font")
            }
        }
    }

    val themeExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                profileManager.exportThemeToUri(uri, "My Custom Aura Theme", theme)
            }
        }
    }

    val themeImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                profileManager.importThemeFromUri(uri)
            }
        }
    }

    if (showColorPickerDialog) {
        GlassDialog(onDismissRequest = { showColorPickerDialog = false }) {
            HsvColorPicker(
                initialColor = when (activeColorSlot) {
                    "Primary" -> theme.primaryColor
                    "Secondary" -> theme.secondaryColor
                    "Glow" -> theme.glowColor
                    else -> theme.primaryColor
                },
                title = "Pick $activeColorSlot Color",
                onColorSelected = { selected ->
                    scope.launch {
                        when (activeColorSlot) {
                            "Primary" -> preferences.updateCustomColors(selected, theme.secondaryColor, theme.backgroundColor, theme.glowColor)
                            "Secondary" -> preferences.updateCustomColors(theme.primaryColor, selected, theme.backgroundColor, theme.glowColor)
                            "Glow" -> preferences.updateCustomColors(theme.primaryColor, theme.secondaryColor, theme.backgroundColor, selected)
                        }
                    }
                    showColorPickerDialog = false
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Appearance & Styling",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.textColorPrimary
                    )
                    Text(
                        text = "Real glassmorphism • 100% Offline • Fluid physics",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textColorSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                GlassButton(onClick = onNavigateBack) {
                    Text("Back", color = theme.textColorPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Interactive Preview Card
        item {
            SectionTitle(title = "Live Theme & Motion Preview")
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spinning Vinyl
                        SpinningVinylRecord(
                            isPlaying = true,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aura Sonic Odyssey",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorPrimary
                            )
                            Text(
                                text = "Hi-Res FLAC • 24-bit / 96 kHz",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedEqualizerBars(isPlaying = true)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "01:42 / 04:05",
                                    fontSize = 11.sp,
                                    color = theme.textColorSecondary
                                )
                            }
                        }
                        FavoriteHeartBurst(
                            isFavorite = previewIsFavorite,
                            onToggle = { previewIsFavorite = !previewIsFavorite }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    WaveformSeekBar(
                        progress = previewSeekProgress,
                        onSeek = { previewSeekProgress = it }
                    )
                }
            }
        }

        // Theme Mode Selector
        item {
            SectionTitle(title = "Theme Mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // 12 Preset Palettes
        item {
            SectionTitle(title = "Preset Palettes")
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
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(listOf(preset.primary, preset.secondary))
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
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
        }

        // Custom Color Slots
        item {
            SectionTitle(title = "Custom Color Slots")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ColorSlotItem(
                    label = "Primary",
                    color = theme.primaryColor,
                    onClick = {
                        activeColorSlot = "Primary"
                        showColorPickerDialog = true
                    }
                )
                ColorSlotItem(
                    label = "Secondary",
                    color = theme.secondaryColor,
                    onClick = {
                        activeColorSlot = "Secondary"
                        showColorPickerDialog = true
                    }
                )
                ColorSlotItem(
                    label = "Ambient Glow",
                    color = theme.glowColor,
                    onClick = {
                        activeColorSlot = "Glow"
                        showColorPickerDialog = true
                    }
                )
            }
        }

        // Glassmorphism Sliders
        item {
            SectionTitle(title = "Glassmorphism Tuning")
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Glass Tint Opacity (${(theme.glassTintOpacity * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = theme.glassTintOpacity,
                        onValueChange = {
                            scope.launch { preferences.updateGlassSettings(theme.glassBlurRadius.value, it) }
                        },
                        valueRange = 0.10f..0.40f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Backdrop Blur Radius (${theme.glassBlurRadius.value.toInt()} dp)",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    GlassSlider(
                        value = theme.glassBlurRadius.value,
                        onValueChange = {
                            scope.launch { preferences.updateGlassSettings(it, theme.glassTintOpacity) }
                        },
                        valueRange = 8f..48f
                    )
                }
            }
        }

        // Typography & Font Import
        item {
            SectionTitle(title = "Offline Fonts & Typography")
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "8 Bundled OFL Fonts + Custom .ttf / .otf Import via SAF",
                        fontSize = 12.sp,
                        color = theme.textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FontManager.getAllAvailableFonts()) { fontDef ->
                            GlassChip(
                                text = fontDef.displayName,
                                isSelected = fontDef.id == "inter" || fontDef.id == "system_default",
                                onClick = { }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassButton(
                        onClick = { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf")) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Custom Font (.ttf / .otf)", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Theme Profiles (JSON Export/Import via SAF)
        item {
            SectionTitle(title = "Theme Profiles & Backup")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    onClick = { themeExportLauncher.launch("aura_theme_custom.json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", fontSize = 12.sp, color = Color.White)
                    }
                }
                GlassButton(
                    onClick = { themeImportLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = AuraTheme.current.textColorPrimary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun ColorSlotItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val theme = AuraTheme.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.width(105.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColorPrimary
            )
        }
    }
}
