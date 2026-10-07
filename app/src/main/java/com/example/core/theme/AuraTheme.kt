package com.example.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    DARK,
    LIGHT,
    AMOLED_BLACK,
    SYSTEM,
    ARTWORK_DYNAMIC,
    MATERIAL_YOU
}

enum class AccentSource {
    FIXED,
    ARTWORK_DYNAMIC,
    TIME_OF_DAY
}

enum class AlbumArtShape {
    SQUARE,
    ROUNDED,
    CIRCLE,
    SQUIRCLE
}

enum class ButtonStyle {
    FILLED,
    TONAL,
    OUTLINE,
    GLASS
}

enum class LayoutDensity {
    COMPACT,
    COMFORTABLE,
    SPACIOUS
}

enum class NowPlayingStyle {
    CLASSIC,
    BIG_ART,
    VINYL,
    MINIMAL,
    LYRICS_FIRST
}

enum class MiniPlayerStyle {
    FLOATING_GLASS_BAR,
    DOCKED_BAR
}

enum class BottomBarStyle {
    LABELS_ON,
    ICON_ONLY,
    FLOATING_PILL
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
    SUNSET("Sunset", Color(0xFFFF5E62), Color(0xFFFF9966), Color(0xFF140710), Color(0xFFFF416C)),
    OCEAN("Ocean", Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF05131E), Color(0xFF0052D4)),
    FOREST("Forest", Color(0xFF10B981), Color(0xFF34D399), Color(0xFF051C14), Color(0xFF059669)),
    ROSE("Rose", Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFF19070B), Color(0xFFE11D48)),
    MONO("Mono", Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF090D0F), Color(0xFFCBD5E1)),
    NEON("Neon", Color(0xFF39FF14), Color(0xFF00FFF4), Color(0xFF050B07), Color(0xFF7B2CBF)),
    SAND("Sand", Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFF170E0D), Color(0xFFB45309)),
    LAVENDER("Lavender", Color(0xFFB8A4FC), Color(0xFFE0C3FC), Color(0xFF100918), Color(0xFFA07BFC)),
    EMBER("Ember", Color(0xFFFF4500), Color(0xFFFF8C00), Color(0xFF190A04), Color(0xFFD03B0B)),
    GLACIER("Glacier", Color(0xFF70D6FF), Color(0xFFA0E7E5), Color(0xFF070F1E), Color(0xFF00B4D8));

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

data class AuraThemeState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val palettePreset: PalettePreset = PalettePreset.AURORA,
    val accentSource: AccentSource = AccentSource.ARTWORK_DYNAMIC,
    val primaryColor: Color = Color(0xFF8C65FF),
    val secondaryColor: Color = Color(0xFF00E5FF),
    val backgroundColor: Color = Color(0xFF0A0C16),
    val surfaceColor: Color = Color(0xFF141828),
    val surfaceTintColor: Color = Color(0xFF1D233A),
    val glowColor: Color = Color(0xFF9D4EDD),
    val textColorPrimary: Color = Color(0xFFF0F4FC),
    val textColorSecondary: Color = Color(0xFF94A3B8),
    val islandColor: Color = Color(0xFF0F1322),
    val glassBlurRadius: Dp = 24.dp,
    val glassTintOpacity: Float = 0.22f,
    val glassBorderBrightness: Float = 0.25f,
    val glassNoiseAlpha: Float = 0.04f,
    val glassShadowElevation: Dp = 12.dp,
    val globalCornerRadius: Dp = 20.dp,
    val albumArtCornerRadius: Dp = 16.dp,
    val albumArtShape: AlbumArtShape = AlbumArtShape.SQUIRCLE,
    val buttonStyle: ButtonStyle = ButtonStyle.GLASS,
    val density: LayoutDensity = LayoutDensity.COMFORTABLE,
    val nowPlayingStyle: NowPlayingStyle = NowPlayingStyle.CLASSIC,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING_GLASS_BAR,
    val bottomBarStyle: BottomBarStyle = BottomBarStyle.FLOATING_PILL,
    val gridColumns: Int = 2,
    val animationSpeedMultiplier: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val islandCornerRadius: Dp = 28.dp,
    val islandOpacity: Float = 0.95f,
    val islandVinylArt: Boolean = true,
    val chromaticAberration: Boolean = true,
    val adaptiveDownscaling: Boolean = true
) {
    val isLight: Boolean
        get() = themeMode == ThemeMode.LIGHT

    val isAmoled: Boolean
        get() = themeMode == ThemeMode.AMOLED_BLACK
}

val ThemeMode.isLight: Boolean
    get() = this == ThemeMode.LIGHT

val ThemeMode.isAmoled: Boolean
    get() = this == ThemeMode.AMOLED_BLACK


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
    CompositionLocalProvider(
        LocalAuraTheme provides themeState,
        content = content
    )
}
