package com.example.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.example.core.logger.AuraLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * PaletteExtractor: Analyzes album art bitmaps and extracts vibrant, muted, and dominant
 * colors using AndroidX Palette. Retains an in-memory thread-safe cache to avoid repeated
 * CPU bitmap quantization during song transitions.
 */
object PaletteExtractor {

    private const val TAG = "PaletteExtractor"
    private val colorCache = ConcurrentHashMap<String, ExtractedArtworkPalette>()

    data class ExtractedArtworkPalette(
        val dominantColor: Color,
        val vibrantColor: Color,
        val mutedColor: Color,
        val darkVibrantColor: Color,
        val lightVibrantColor: Color,
        val glowColor: Color
    )

    val DefaultPalette = ExtractedArtworkPalette(
        dominantColor = Color(0xFF141828),
        vibrantColor = Color(0xFF8C65FF),
        mutedColor = Color(0xFF4C3580),
        darkVibrantColor = Color(0xFF0F0B1E),
        lightVibrantColor = Color(0xFFC084FC),
        glowColor = Color(0xFF9D4EDD)
    )

    /**
     * Extracts palette from a given bitmap with downsampled sizing for fast 60fps responsiveness.
     */
    suspend fun extractColors(cacheKey: String, bitmap: Bitmap?): ExtractedArtworkPalette = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext DefaultPalette

        colorCache[cacheKey]?.let { return@withContext it }

        try {
            // Generate palette using 16 color quantization for speed
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()

            val dominant = palette.dominantSwatch?.rgb?.let { Color(it) } ?: DefaultPalette.dominantColor
            val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) } ?: dominant
            val muted = palette.mutedSwatch?.rgb?.let { Color(it) } ?: DefaultPalette.mutedColor
            val darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) } ?: DefaultPalette.darkVibrantColor
            val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) } ?: DefaultPalette.lightVibrantColor

            val glow = vibrant.copy(alpha = 1.0f)

            val extracted = ExtractedArtworkPalette(
                dominantColor = dominant,
                vibrantColor = vibrant,
                mutedColor = muted,
                darkVibrantColor = darkVibrant,
                lightVibrantColor = lightVibrant,
                glowColor = glow
            )

            colorCache[cacheKey] = extracted
            extracted
        } catch (e: Exception) {
            AuraLog.e(TAG, "Palette generation error for key $cacheKey", e)
            DefaultPalette
        }
    }

    fun clearCache() {
        colorCache.clear()
    }
}
