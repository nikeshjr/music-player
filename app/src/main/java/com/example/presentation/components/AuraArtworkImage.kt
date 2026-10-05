package com.example.presentation.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.theme.AuraTheme
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Global fast LRU cache for decoded album art bitmaps (up to 32MB).
 */
object AlbumArtCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(8192)

    val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadArtwork(context: Context, song: Song?): Bitmap? = withContext(Dispatchers.IO) {
        if (song == null) return@withContext null
        val cacheKey = if (song.mediaStoreId > 0) "ms_${song.mediaStoreId}" else "path_${song.path}"

        memoryCache.get(cacheKey)?.let { return@withContext it }

        var decoded: Bitmap? = null

        // 1. Try MediaStore Album Art URI
        if (song.albumId > 0) {
            val artUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                song.albumId
            )
            try {
                context.contentResolver.openInputStream(artUri)?.use { stream ->
                    decoded = BitmapFactory.decodeStream(stream)
                }
            } catch (ignored: Exception) {}
        }

        // 2. Try MediaMetadataRetriever embedded picture from file or ContentUri
        if (decoded == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (song.path.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(Uri.parse(song.path), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val pic = retriever.embeddedPicture
                        if (pic != null) {
                            val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                            decoded = BitmapFactory.decodeByteArray(pic, 0, pic.size, options)
                        }
                    }
                } else if (song.path.isNotBlank()) {
                    val file = File(song.path)
                    if (file.exists() && file.canRead()) {
                        retriever.setDataSource(song.path)
                        val pic = retriever.embeddedPicture
                        if (pic != null) {
                            val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                            decoded = BitmapFactory.decodeByteArray(pic, 0, pic.size, options)
                        }
                    }
                }
            } catch (ignored: Exception) {
            } finally {
                try { retriever.release() } catch (ignored: Exception) {}
            }
        }

        if (decoded != null) {
            memoryCache.put(cacheKey, decoded)
        }
        decoded
    }
}

/**
 * AuraArtworkImage: Renders real embedded artwork for audio tracks,
 * falling back gracefully to glassmorphic visual gradients if no image is present.
 */
@Composable
fun AuraArtworkImage(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    fallbackIconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    val context = LocalContext.current
    val theme = AuraTheme.current
    var bitmap by remember(song?.id, song?.path) {
        mutableStateOf<Bitmap?>(
            if (song != null) {
                val key = if (song.mediaStoreId > 0) "ms_${song.mediaStoreId}" else "path_${song.path}"
                AlbumArtCache.memoryCache.get(key)
            } else null
        )
    }

    LaunchedEffect(song?.id, song?.path) {
        if (bitmap == null && song != null) {
            bitmap = AlbumArtCache.loadArtwork(context, song)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        theme.primaryColor.copy(alpha = 0.45f),
                        theme.secondaryColor.copy(alpha = 0.25f),
                        Color(0xFF141724)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Cover for ${song?.title ?: "Music"}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(fallbackIconSize)
            )
        }
    }
}
