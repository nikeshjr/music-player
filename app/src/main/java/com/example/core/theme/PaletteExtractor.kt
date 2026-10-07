package com.example.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.example.core.logger.AuraLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object PaletteExtractor {
    private const val TAG = "PaletteExtractor"
    private val colorCache = ConcurrentHashMap<String, ExtractedArtworkPalette>()

    val DefaultPalette = ExtractedArtworkPalette(
        dominantColor = Color(0xFF0C1028),
        vibrantColor = Color(0xFF8C65FF),
        mutedColor = Color(0xFF4C5580),
        darkVibrantColor = Color(0xFF060B1E),
        lightVibrantColor = Color(0xFFB8A4FC),
        glowColor = Color(0xFF9D4EDD)
    )

    data class ExtractedArtworkPalette(
        val dominantColor: Color,
        val vibrantColor: Color,
        val mutedColor: Color,
        val darkVibrantColor: Color,
        val lightVibrantColor: Color,
        val glowColor: Color
    )

    suspend fun extractColors(cacheKey: String, bitmap: Bitmap?): ExtractedArtworkPalette =
        withContext(Dispatchers.Default) {
            if (bitmap == null) return@withContext DefaultPalette
            val cached = colorCache[cacheKey]
            if (cached != null) return@withContext cached

            try {
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
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
