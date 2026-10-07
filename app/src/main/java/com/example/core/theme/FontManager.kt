package com.example.core.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import com.example.core.logger.AuraLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FontManager {
    private const val TAG = "FontManager"
    private const val FONTS_DIR_NAME = "aura_custom_fonts"

    data class AuraFontDefinition(
        val id: String,
        val displayName: String,
        val fontFamily: FontFamily,
        val isCustomImported: Boolean = false,
        val localFilePath: String? = null
    )

    val Presets: List<AuraFontDefinition> = listOf(
        AuraFontDefinition("system_default", "Default Sans-Serif", FontFamily.Default),
        AuraFontDefinition("inter", "Inter (Clean & Modern)", FontFamily.SansSerif),
        AuraFontDefinition("outfit", "Outfit (Geometric)", FontFamily.SansSerif),
        AuraFontDefinition("manrope", "Manrope (Grotesque)", FontFamily.SansSerif),
        AuraFontDefinition("space_grotesk", "Space Grotesk (Tech)", FontFamily.Monospace),
        AuraFontDefinition("jetbrains_mono", "JetBrains Mono (Numbers/Code)", FontFamily.Monospace),
        AuraFontDefinition("playfair", "Playfair Display (Editorial)", FontFamily.Serif),
        AuraFontDefinition("poppins", "Poppins (Rounded Geometric)", FontFamily.SansSerif)
    )

    private val customFonts = mutableListOf<AuraFontDefinition>()

    fun initialize(context: Context) {
        loadImportedFonts(context)
    }

    fun getAllAvailableFonts(): List<AuraFontDefinition> {
        return Presets + customFonts
    }

    fun findFontById(id: String): AuraFontDefinition {
        return getAllAvailableFonts().firstOrNull { it.id == id } ?: Presets.first()
    }

    suspend fun importFontFromUri(
        context: Context,
        uri: Uri,
        fontDisplayName: String
    ): Result<AuraFontDefinition> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, FONTS_DIR_NAME)
            dir.mkdirs()
            val cleanName = Regex("[^a-zA-Z0-9_-]").replace(fontDisplayName, "_")
            val targetFile = File(dir, "${cleanName}_${System.currentTimeMillis()}.ttf")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val typeface = Typeface.createFromFile(targetFile)
                ?: return@withContext Result.failure(IllegalStateException("Invalid font file"))

            val def = AuraFontDefinition(
                id = "custom_${targetFile.nameWithoutExtension}",
                displayName = fontDisplayName,
                fontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface)),
                isCustomImported = true,
                localFilePath = targetFile.absolutePath
            )
            customFonts.add(def)
            Result.success(def)
        } catch (e: Exception) {
            AuraLog.e(TAG, "Failed to import font from uri", e)
            Result.failure(e)
        }
    }

    suspend fun removeCustomFont(context: Context, fontId: String): Boolean = withContext(Dispatchers.IO) {
        val found = customFonts.firstOrNull { it.id == fontId }
        if (found != null) {
            found.localFilePath?.let { File(it).delete() }
            customFonts.remove(found)
            true
        } else {
            false
        }
    }

    private fun loadImportedFonts(context: Context) {
        val dir = File(context.filesDir, FONTS_DIR_NAME)
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            try {
                if (file.extension.equals("ttf", true) || file.extension.equals("otf", true)) {
                    val typeface = Typeface.createFromFile(file)
                    if (typeface != null) {
                        customFonts.add(
                            AuraFontDefinition(
                                id = "custom_${file.nameWithoutExtension}",
                                displayName = file.nameWithoutExtension.substringBeforeLast('_', file.nameWithoutExtension),
                                fontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface)),
                                isCustomImported = true,
                                localFilePath = file.absolutePath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                AuraLog.e(TAG, "Could not load cached font file: ${file.name}", e)
            }
        }
    }
}
