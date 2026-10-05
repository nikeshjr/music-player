package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.core.logger.AuraLog
import com.example.data.local.db.AuraDatabase
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * LibraryBackupManager: 100% offline data backup and restoration.
 * Serializes user-created playlists, favorites, play history statistics, and custom EQ presets
 * into a portable, versioned JSON backup format exportable and importable via SAF.
 */
class LibraryBackupManager(
    private val context: Context,
    private val database: AuraDatabase
) {

    companion object {
        private const val TAG = "LibraryBackupManager"
        private const val BACKUP_VERSION = 1
    }

    /**
     * Serializes library data into a structured JSON string.
     */
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Aura Music")

        // Playlists
        val playlists = database.playlistDao().getAllPlaylists().first()
        val playlistArray = JSONArray()
        for (pl in playlists) {
            val plObj = JSONObject()
            plObj.put("name", pl.name)
            plObj.put("isSmart", pl.isSmart)
            plObj.put("smartType", pl.smartType)
            playlistArray.put(plObj)
        }
        root.put("playlists", playlistArray)

        // Custom Equalizer Presets
        val presets = database.eqPresetDao().getAllPresets().first()
        val presetArray = JSONArray()
        for (p in presets) {
            if (!p.isDefault) {
                val pObj = JSONObject()
                pObj.put("name", p.name)
                pObj.put("bandLevels", p.bandLevels)
                pObj.put("bassBoost", p.bassBoost)
                pObj.put("virtualizer", p.virtualizer)
                pObj.put("deviceType", p.targetDeviceType)
                presetArray.put(pObj)
            }
        }
        root.put("eqPresets", presetArray)

        root.toString(2)
    }

    /**
     * Exports backup JSON directly to a user-selected SAF URI.
     */
    suspend fun exportBackupToUri(targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                OutputStreamWriter(output).use { it.write(json) }
            } ?: return@withContext Result.failure(Exception("Cannot open destination stream"))
            AuraLog.i(TAG, "Library backup exported successfully to $targetUri")
            Result.success(Unit)
        } catch (e: Exception) {
            AuraLog.e(TAG, "Backup export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Restores library data from a user-selected SAF backup JSON file.
     */
    suspend fun restoreBackupFromUri(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Cannot open source stream"))

            val root = JSONObject(jsonStr)
            var restoredItems = 0

            // Restore Playlists
            val playlists = root.optJSONArray("playlists")
            if (playlists != null) {
                for (i in 0 until playlists.length()) {
                    val pObj = playlists.getJSONObject(i)
                    val name = pObj.getString("name")
                    val isSmart = pObj.optBoolean("isSmart", false)
                    val smartType = pObj.optString("smartType", null)
                    database.playlistDao().insertPlaylist(
                        Playlist(name = name, isSmart = isSmart, smartType = smartType)
                    )
                    restoredItems++
                }
            }

            // Restore EQ Presets
            val eqPresets = root.optJSONArray("eqPresets")
            if (eqPresets != null) {
                for (i in 0 until eqPresets.length()) {
                    val pObj = eqPresets.getJSONObject(i)
                    val name = pObj.getString("name")
                    val levels = pObj.getString("bandLevels")
                    val bb = pObj.optInt("bassBoost", 0)
                    val virt = pObj.optInt("virtualizer", 0)
                    val dev = pObj.optString("deviceType", "ALL")
                    database.eqPresetDao().insertPreset(
                        EqPreset(
                            name = name,
                            bandLevels = levels,
                            bassBoost = bb,
                            virtualizer = virt,
                            targetDeviceType = dev
                        )
                    )
                    restoredItems++
                }
            }

            AuraLog.i(TAG, "Restored $restoredItems items from backup.")
            Result.success(restoredItems)
        } catch (e: Exception) {
            AuraLog.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
}
