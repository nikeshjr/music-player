package com.example.data.local.datastore

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

class AuraPreferences(private val context: Context) {
    companion object {
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
        val KEY_CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val KEY_GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val KEY_REPLAY_GAIN = booleanPreferencesKey("replay_gain")
        val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val KEY_HI_RES_OUTPUT = booleanPreferencesKey("hi_res_output")
        val KEY_ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val KEY_ISLAND_OFFSET_X = intPreferencesKey("island_offset_x")
        val KEY_ISLAND_OFFSET_Y = intPreferencesKey("island_offset_y")
        val KEY_ISLAND_WIDTH = intPreferencesKey("island_width")
        val KEY_ISLAND_HEIGHT = intPreferencesKey("island_height")
        val KEY_ISLAND_CORNER_RADIUS = intPreferencesKey("island_corner_radius")
        val KEY_ISLAND_OPACITY = floatPreferencesKey("island_opacity")
        val KEY_ISLAND_HIDE_FOREGROUND = booleanPreferencesKey("island_hide_foreground")
        val KEY_ISLAND_HIDE_FULLSCREEN = booleanPreferencesKey("island_hide_fullscreen")
        val KEY_MIN_DURATION_SECONDS = intPreferencesKey("min_duration_seconds")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private val dataStore = context.dataStore

    val themeState: Flow<AuraThemeState> = dataStore.data.map { prefs ->
        val modeStr = prefs[KEY_THEME_MODE] ?: ThemeMode.DARK.name
        val mode = try { ThemeMode.valueOf(modeStr) } catch (_: Exception) { ThemeMode.DARK }
        val presetStr = prefs[KEY_PALETTE_PRESET] ?: PalettePreset.AURORA.name
        val preset = try { PalettePreset.valueOf(presetStr) } catch (_: Exception) { PalettePreset.AURORA }
        val npStyleStr = prefs[KEY_NOW_PLAYING_STYLE] ?: NowPlayingStyle.CLASSIC.name
        val npStyle = try { NowPlayingStyle.valueOf(npStyleStr) } catch (_: Exception) { NowPlayingStyle.CLASSIC }
        val mpStyleStr = prefs[KEY_MINI_PLAYER_STYLE] ?: MiniPlayerStyle.FLOATING_GLASS_BAR.name
        val mpStyle = try { MiniPlayerStyle.valueOf(mpStyleStr) } catch (_: Exception) { MiniPlayerStyle.FLOATING_GLASS_BAR }
        val bbStyleStr = prefs[KEY_BOTTOM_BAR_STYLE] ?: BottomBarStyle.FLOATING_PILL.name
        val bbStyle = try { BottomBarStyle.valueOf(bbStyleStr) } catch (_: Exception) { BottomBarStyle.FLOATING_PILL }

        val primary = prefs[KEY_PRIMARY_COLOR]?.let { Color(it.toULong()) } ?: preset.primary
        val secondary = prefs[KEY_SECONDARY_COLOR]?.let { Color(it.toULong()) } ?: preset.secondary
        val bg = prefs[KEY_BACKGROUND_COLOR]?.let { Color(it.toULong()) } ?: preset.background
        val glow = prefs[KEY_GLOW_COLOR]?.let { Color(it.toULong()) } ?: preset.glow

        AuraThemeState(
            themeMode = mode,
            palettePreset = preset,
            primaryColor = primary,
            secondaryColor = secondary,
            backgroundColor = bg,
            glowColor = glow,
            glassBlurRadius = (prefs[KEY_BLUR_RADIUS] ?: 24f).dp,
            glassTintOpacity = prefs[KEY_TINT_OPACITY] ?: 0.22f,
            globalCornerRadius = (prefs[KEY_GLOBAL_CORNER_RADIUS] ?: 20f).dp,
            nowPlayingStyle = npStyle,
            miniPlayerStyle = mpStyle,
            bottomBarStyle = bbStyle,
            animationSpeedMultiplier = prefs[KEY_ANIMATION_SPEED] ?: 1.0f,
            reduceMotion = prefs[KEY_REDUCE_MOTION] ?: false,
            hapticFeedbackEnabled = prefs[KEY_HAPTICS_ENABLED] ?: true
        )
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isIslandEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ISLAND_ENABLED] ?: true
    }

    val minDurationSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_MIN_DURATION_SECONDS] ?: 10
    }

    val crossfadeSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_CROSSFADE_SECONDS] ?: 0
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

    suspend fun updateCustomColors(
        primary: Color,
        secondary: Color,
        background: Color,
        glow: Color
    ) {
        dataStore.edit {
            it[KEY_PRIMARY_COLOR] = primary.value.toLong()
            it[KEY_SECONDARY_COLOR] = secondary.value.toLong()
            it[KEY_BACKGROUND_COLOR] = background.value.toLong()
            it[KEY_GLOW_COLOR] = glow.value.toLong()
        }
    }

    suspend fun updateGlassSettings(blur: Float, tintOpacity: Float) {
        dataStore.edit {
            it[KEY_BLUR_RADIUS] = blur
            it[KEY_TINT_OPACITY] = tintOpacity
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setIslandEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ISLAND_ENABLED] = enabled }
    }

    suspend fun setMinDurationSeconds(seconds: Int) {
        dataStore.edit { it[KEY_MIN_DURATION_SECONDS] = seconds }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        dataStore.edit { it[KEY_CROSSFADE_SECONDS] = seconds }
    }
}
