package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.logger.AuraLog
import com.example.core.theme.AccentSource
import com.example.core.theme.AlbumArtShape
import com.example.core.theme.AuraThemeState
import com.example.core.theme.BottomBarStyle
import com.example.core.theme.ButtonStyle
import com.example.core.theme.LayoutDensity
import com.example.core.theme.MiniPlayerStyle
import com.example.core.theme.NowPlayingStyle
import com.example.core.theme.PalettePreset
import com.example.core.theme.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

/**
 * AuraPreferences: Central DataStore repository persisting app preferences,
 * audio settings, Dynamic Island calibration, and user custom theming.
 */
class AuraPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private const val TAG = "AuraPreferences"

        // Theming Keys
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PALETTE_PRESET = stringPreferencesKey("palette_preset")
        val KEY_ACCENT_SOURCE = stringPreferencesKey("accent_source")
        val KEY_PRIMARY_COLOR = longPreferencesKey("primary_color")
        val KEY_SECONDARY_COLOR = longPreferencesKey("secondary_color")
        val KEY_BACKGROUND_COLOR = longPreferencesKey("background_color")
        val KEY_GLOW_COLOR = longPreferencesKey("glow_color")
        val KEY_BLUR_RADIUS = floatPreferencesKey("blur_radius")
        val KEY_TINT_OPACITY = floatPreferencesKey("tint_opacity")
        val KEY_GLOBAL_CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val KEY_NOW_PLAYING_STYLE = stringPreferencesKey("now_playing_style")
        val KEY_MINI_PLAYER_STYLE = stringPreferencesKey("mini_player_style")
        val KEY_BOTTOM_BAR_STYLE = stringPreferencesKey("bottom_bar_style")
        val KEY_ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")

        // Audio & Playback Keys
        val KEY_CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val KEY_REPLAY_GAIN = booleanPreferencesKey("replay_gain")
        val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val KEY_HI_RES_OUTPUT = booleanPreferencesKey("hi_res_output")

        // Dynamic Island Keys
        val KEY_ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val KEY_ISLAND_OFFSET_X = intPreferencesKey("island_offset_x")
        val KEY_ISLAND_OFFSET_Y = intPreferencesKey("island_offset_y")
        val KEY_ISLAND_WIDTH = intPreferencesKey("island_width")
        val KEY_ISLAND_HEIGHT = intPreferencesKey("island_height")
        val KEY_ISLAND_CORNER_RADIUS = intPreferencesKey("island_corner_radius")
        val KEY_ISLAND_OPACITY = floatPreferencesKey("island_opacity")
        val KEY_ISLAND_HIDE_IN_FOREGROUND = booleanPreferencesKey("island_hide_foreground")
        val KEY_ISLAND_HIDE_IN_FULLSCREEN = booleanPreferencesKey("island_hide_fullscreen")

        // Scanner Keys
        val KEY_MIN_DURATION_SECONDS = intPreferencesKey("min_duration_seconds")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val themeState: Flow<AuraThemeState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                AuraLog.e(TAG, "Error reading theme preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val modeName = prefs[KEY_THEME_MODE] ?: ThemeMode.DARK.name
            val themeMode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.DARK)

            val presetName = prefs[KEY_PALETTE_PRESET] ?: PalettePreset.AURORA.name
            val preset = runCatching { PalettePreset.valueOf(presetName) }.getOrDefault(PalettePreset.AURORA)

            val accentSourceName = prefs[KEY_ACCENT_SOURCE] ?: AccentSource.ARTWORK_DYNAMIC.name
            val accentSource = runCatching { AccentSource.valueOf(accentSourceName) }.getOrDefault(AccentSource.ARTWORK_DYNAMIC)

            val primaryVal = prefs[KEY_PRIMARY_COLOR]
            val primaryColor = if (primaryVal != null) Color(primaryVal.toULong()) else preset.primary

            val secondaryVal = prefs[KEY_SECONDARY_COLOR]
            val secondaryColor = if (secondaryVal != null) Color(secondaryVal.toULong()) else preset.secondary

            val backgroundVal = prefs[KEY_BACKGROUND_COLOR]
            val backgroundColor = if (backgroundVal != null) Color(backgroundVal.toULong()) else preset.background

            val glowVal = prefs[KEY_GLOW_COLOR]
            val glowColor = if (glowVal != null) Color(glowVal.toULong()) else preset.glow

            val blurRadius = (prefs[KEY_BLUR_RADIUS] ?: 24f).dp
            val tintOpacity = prefs[KEY_TINT_OPACITY] ?: 0.22f
            val cornerRadius = (prefs[KEY_GLOBAL_CORNER_RADIUS] ?: 20f).dp

            val npStyleName = prefs[KEY_NOW_PLAYING_STYLE] ?: NowPlayingStyle.CLASSIC.name
            val npStyle = runCatching { NowPlayingStyle.valueOf(npStyleName) }.getOrDefault(NowPlayingStyle.CLASSIC)

            val mpStyleName = prefs[KEY_MINI_PLAYER_STYLE] ?: MiniPlayerStyle.FLOATING_GLASS_BAR.name
            val mpStyle = runCatching { MiniPlayerStyle.valueOf(mpStyleName) }.getOrDefault(MiniPlayerStyle.FLOATING_GLASS_BAR)

            val bbStyleName = prefs[KEY_BOTTOM_BAR_STYLE] ?: BottomBarStyle.FLOATING_PILL.name
            val bbStyle = runCatching { BottomBarStyle.valueOf(bbStyleName) }.getOrDefault(BottomBarStyle.FLOATING_PILL)

            val speed = prefs[KEY_ANIMATION_SPEED] ?: 1.0f
            val reduceMotion = prefs[KEY_REDUCE_MOTION] ?: false
            val haptics = prefs[KEY_HAPTICS_ENABLED] ?: true

            AuraThemeState(
                themeMode = themeMode,
                palettePreset = preset,
                accentSource = accentSource,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                backgroundColor = backgroundColor,
                glowColor = glowColor,
                glassBlurRadius = blurRadius,
                glassTintOpacity = tintOpacity,
                globalCornerRadius = cornerRadius,
                nowPlayingStyle = npStyle,
                miniPlayerStyle = mpStyle,
                bottomBarStyle = bbStyle,
                animationSpeedMultiplier = speed,
                reduceMotion = reduceMotion,
                hapticFeedbackEnabled = haptics
            )
        }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun updatePalettePreset(preset: PalettePreset) {
        dataStore.edit {
            it[KEY_PALETTE_PRESET] = preset.name
            it[KEY_PRIMARY_COLOR] = preset.primary.value.toLong()
            it[KEY_SECONDARY_COLOR] = preset.secondary.value.toLong()
            it[KEY_BACKGROUND_COLOR] = preset.background.value.toLong()
            it[KEY_GLOW_COLOR] = preset.glow.value.toLong()
        }
    }

    suspend fun updateCustomColors(primary: Color, secondary: Color, background: Color, glow: Color) {
        dataStore.edit {
            it[KEY_PRIMARY_COLOR] = primary.value.toLong()
            it[KEY_SECONDARY_COLOR] = secondary.value.toLong()
            it[KEY_BACKGROUND_COLOR] = background.value.toLong()
            it[KEY_GLOW_COLOR] = glow.value.toLong()
        }
    }

    suspend fun updateGlassSettings(blurRadius: Float, tintOpacity: Float) {
        dataStore.edit {
            it[KEY_BLUR_RADIUS] = blurRadius
            it[KEY_TINT_OPACITY] = tintOpacity
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    val isIslandEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_ISLAND_ENABLED] ?: true }

    suspend fun setIslandEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ISLAND_ENABLED] = enabled }
    }

    val minDurationSeconds: Flow<Int> = dataStore.data
        .map { it[KEY_MIN_DURATION_SECONDS] ?: 30 }

    suspend fun setMinDurationSeconds(seconds: Int) {
        dataStore.edit { it[KEY_MIN_DURATION_SECONDS] = seconds }
    }

    val crossfadeSeconds: Flow<Int> = dataStore.data
        .map { it[KEY_CROSSFADE_SECONDS] ?: 0 }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        dataStore.edit { it[KEY_CROSSFADE_SECONDS] = seconds }
    }
}
