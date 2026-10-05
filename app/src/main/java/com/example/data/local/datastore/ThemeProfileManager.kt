package com.example.data.local.datastore

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.core.logger.AuraLog
import com.example.core.theme.AuraThemeState
import com.example.core.theme.PalettePreset
import com.example.core.theme.ThemeMode
import com.example.data.local.db.AuraDatabase
import com.example.data.model.ThemeProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * ThemeProfileManager: Handles theme profile saving, switching, duplication,
 * and versioned JSON serialization/deserialization for export and import via SAF.
 */
class ThemeProfileManager(
    private val context: Context,
    private val database: AuraDatabase,
    private val preferences: AuraPreferences
) {

    companion object {
        private const val TAG = "ThemeProfileManager"
        private const val SCHEMA_VERSION = 1
    }

    /**
     * Serializes an AuraThemeState into a validated, versioned JSON string.
     */
    fun serializeTheme(name: String, state: AuraThemeState): String {
        val json = JSONObject()
        json.put("schemaVersion", SCHEMA_VERSION)
        json.put("profileName", name)
        json.put("themeMode", state.themeMode.name)
        json.put("palettePreset", state.palettePreset.name)
        json.put("primaryColor", state.primaryColor.value.toLong())
        json.put("secondaryColor", state.secondaryColor.value.toLong())
        json.put("backgroundColor", state.backgroundColor.value.toLong())
        json.put("glowColor", state.glowColor.value.toLong())
        json.put("glassBlurRadius", state.glassBlurRadius.value)
        json.put("glassTintOpacity", state.glassTintOpacity)
        json.put("globalCornerRadius", state.globalCornerRadius.value)
        json.put("nowPlayingStyle", state.nowPlayingStyle.name)
        json.put("miniPlayerStyle", state.miniPlayerStyle.name)
        json.put("bottomBarStyle", state.bottomBarStyle.name)
        json.put("animationSpeedMultiplier", state.animationSpeedMultiplier)
        json.put("reduceMotion", state.reduceMotion)
        return json.toString(2)
    }

    /**
     * Deserializes and validates a JSON string into an AuraThemeState.
     */
    fun deserializeTheme(jsonString: String): Result<Pair<String, AuraThemeState>> {
        return try {
            val json = JSONObject(jsonString)
            val schemaVersion = json.optInt("schemaVersion", 1)
            if (schemaVersion > SCHEMA_VERSION) {
                return Result.failure(Exception("Theme profile was created with a newer app version ($schemaVersion)."))
            }

            val name = json.optString("profileName", "Imported Theme")
            val mode = runCatching { ThemeMode.valueOf(json.getString("themeMode")) }.getOrDefault(ThemeMode.DARK)
            val preset = runCatching { PalettePreset.valueOf(json.getString("palettePreset")) }.getOrDefault(PalettePreset.AURORA)

            val primary = Color(json.getLong("primaryColor").toULong())
            val secondary = Color(json.getLong("secondaryColor").toULong())
            val background = Color(json.getLong("backgroundColor").toULong())
            val glow = Color(json.getLong("glowColor").toULong())

            val theme = AuraThemeState(
                themeMode = mode,
                palettePreset = preset,
                primaryColor = primary,
                secondaryColor = secondary,
                backgroundColor = background,
                glowColor = glow,
                glassTintOpacity = json.optDouble("glassTintOpacity", 0.22).toFloat(),
                animationSpeedMultiplier = json.optDouble("animationSpeedMultiplier", 1.0).toFloat(),
                reduceMotion = json.optBoolean("reduceMotion", false)
            )

            Result.success(Pair(name, theme))
        } catch (e: Exception) {
            AuraLog.e(TAG, "Failed parsing theme profile JSON", e)
            Result.failure(e)
        }
    }

    /**
     * Saves active theme as a named profile in Room database.
     */
    suspend fun saveCurrentThemeProfile(name: String, state: AuraThemeState): Long = withContext(Dispatchers.IO) {
        val json = serializeTheme(name, state)
        val profile = ThemeProfile(name = name, themeJson = json)
        database.themeProfileDao().insertProfile(profile)
    }

    /**
     * Exports a theme to a user-selected SAF URI.
     */
    suspend fun exportThemeToUri(uri: Uri, name: String, state: AuraThemeState): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = serializeTheme(name, state)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream for export"))
            Result.success(Unit)
        } catch (e: Exception) {
            AuraLog.e(TAG, "Export theme failed", e)
            Result.failure(e)
        }
    }

    /**
     * Imports a theme from a user-selected SAF JSON URI and applies it.
     */
    suspend fun importThemeFromUri(uri: Uri): Result<AuraThemeState> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream for import"))

            val result = deserializeTheme(jsonString)
            result.map { (name, theme) ->
                // Persist into Room profiles
                saveCurrentThemeProfile(name, theme)
                // Apply immediately to DataStore preferences
                preferences.updatePalettePreset(theme.palettePreset)
                preferences.updateCustomColors(
                    theme.primaryColor,
                    theme.secondaryColor,
                    theme.backgroundColor,
                    theme.glowColor
                )
                theme
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Import theme failed", e)
            Result.failure(e)
        }
    }
}
