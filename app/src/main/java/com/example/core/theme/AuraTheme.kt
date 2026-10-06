package com.example.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AuraTheme: Central immutable theme model governing the entire visual presentation
 * of Aura Music, the Dynamic Island overlay, Glance widgets, and notifications.
 */
@Immutable
data class AuraThemeState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val palettePreset: PalettePreset = PalettePreset.AURORA,
    val accentSource: AccentSource = AccentSource.ARTWORK_DYNAMIC,
    // Core Colors
    val primaryColor: Color = Color(0xFF8C65FF),
    val secondaryColor: Color = Color(0xFF00E5FF),
    val backgroundColor: Color = Color(0xFF0A0C16),
    val surfaceColor: Color = Color(0xFF141828),
    val surfaceTintColor: Color = Color(0xFF1D233A),
    val glowColor: Color = Color(0xFF9D4EDD),
    val textColorPrimary: Color = Color(0xFFF0F4FC),
    val textColorSecondary: Color = Color(0xFF94A3B8),
    val islandColor: Color = Color(0xFF0F1322),
    // Glassmorphism Specification
    val glassBlurRadius: Dp = 24.dp,
    val glassTintOpacity: Float = 0.22f, // 10% - 40% range
    val glassBorderBrightness: Float = 0.25f,
    val glassNoiseAlpha: Float = 0.04f,
    val glassShadowElevation: Dp = 12.dp,
    // Shapes & Corner Radii
    val globalCornerRadius: Dp = 20.dp,
    val albumArtCornerRadius: Dp = 16.dp,
    val albumArtShape: AlbumArtShape = AlbumArtShape.SQUIRCLE,
    val buttonStyle: ButtonStyle = ButtonStyle.GLASS,
    // Layout & Density
    val density: LayoutDensity = LayoutDensity.COMFORTABLE,
    val nowPlayingStyle: NowPlayingStyle = NowPlayingStyle.CLASSIC,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING_GLASS_BAR,
    val bottomBarStyle: BottomBarStyle = BottomBarStyle.FLOATING_PILL,
    val gridColumns: Int = 2,
    // Motion & Audio Settings
    val animationSpeedMultiplier: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    // Island Overrides
    val islandCornerRadius: Dp = 28.dp,
    val islandOpacity: Float = 0.95f,
    val islandVinylArt: Boolean = true
) {
    val isLight: Boolean get() = themeMode == ThemeMode.LIGHT
    val isAmoled: Boolean get() = themeMode == ThemeMode.AMOLED_BLACK
}

enum class ThemeMode {
    DARK, LIGHT, AMOLED_BLACK, SYSTEM, ARTWORK_DYNAMIC, MATERIAL_YOU
}

enum class AccentSource {
    FIXED, ARTWORK_DYNAMIC, TIME_OF_DAY
}

enum class AlbumArtShape {
    SQUARE, ROUNDED, CIRCLE, SQUIRCLE
}

enum class ButtonStyle {
    FILLED, TONAL, OUTLINE, GLASS
}

enum class LayoutDensity {
    COMPACT, COMFORTABLE, SPACIOUS
}

enum class NowPlayingStyle {
    CLASSIC, BIG_ART, VINYL, MINIMAL, LYRICS_FIRST
}

enum class MiniPlayerStyle {
    FLOATING_GLASS_BAR, DOCKED_BAR
}

enum class BottomBarStyle {
    LABELS_ON, ICON_ONLY, FLOATING_PILL
}

enum class PalettePreset(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val glow: Color
) {
    AURORA("Aurora", Color(0xFF8C65FF), Color(0xFF00E5FF), Color(0xFF0A0C16), Color(0xFF9D4EDD)),
    MIDNIGHT("Midnight", Color(0xFF6366F1), Color(0xFF38BDF8), Color(0xFF030712), Color(0xFF4F46E5)),
    SUNSET("Sunset", Color(0xFFFF5E7E), Color(0xFFFF9900), Color(0xFF140A10), Color(0xFFFF416C)),
    OCEAN("Ocean", Color(0xFF00ADB5), Color(0xFF00FFF5), Color(0xFF05131E), Color(0xFF0077B6)),
    FOREST("Forest", Color(0xFF10B981), Color(0xFF34D399), Color(0xFF06140D), Color(0xFF059669)),
    ROSE("Rose", Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFF19060B), Color(0xFFE11D48)),
    MONO("Mono", Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF090A0F), Color(0xFFCBD5E1)),
    NEON("Neon", Color(0xFF39FF14), Color(0xFF00F5D4), Color(0xFF060D07), Color(0xFF7B2CBF)),
    SAND("Sand", Color(0xFFE0A96D), Color(0xFFF5D061), Color(0xFF16120D), Color(0xFFD4A373)),
    LAVENDER("Lavender", Color(0xFFC084FC), Color(0xFFE879F9), Color(0xFF0F0B18), Color(0xFFA855F7)),
    EMBER("Ember", Color(0xFFFF6B35), Color(0xFFFFA62B), Color(0xFF180A04), Color(0xFFD8315B)),
    GLACIER("Glacier", Color(0xFF70D6FF), Color(0xFFA0C4FF), Color(0xFF08121E), Color(0xFF00B4D8));

    fun toAuraThemeState(): AuraThemeState {
        return AuraThemeState(
            palettePreset = this,
            primaryColor = primary,
            secondaryColor = secondary,
            backgroundColor = background,
            glowColor = glow
        )
    }
}

val LocalAuraTheme = staticCompositionLocalOf { AuraThemeState() }

object AuraTheme {
    val current: AuraThemeState
        @Composable
        @ReadOnlyComposable
        get() = LocalAuraTheme.current
}

@Composable
fun AuraThemeProvider(
    themeState: AuraThemeState,
    content: @Composable () -> Unit
) {
    val isLight = themeState.isLight
    val m3ColorScheme = if (isLight) {
        lightColorScheme(
            primary = themeState.primaryColor,
            secondary = themeState.secondaryColor,
            background = themeState.backgroundColor,
            surface = themeState.surfaceColor,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = themeState.textColorPrimary,
            onSurface = themeState.textColorPrimary
        )
    } else {
        darkColorScheme(
            primary = themeState.primaryColor,
            secondary = themeState.secondaryColor,
            background = themeState.backgroundColor,
            surface = themeState.surfaceColor,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = themeState.textColorPrimary,
            onSurface = themeState.textColorPrimary
        )
    }

    CompositionLocalProvider(LocalAuraTheme provides themeState) {
        MaterialTheme(colorScheme = m3ColorScheme) {
            content()
        }
    }
}
