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
import com.example.data.metadata.FlacVorbisCommentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

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
        val cacheKey = if (song.mediaStoreId > 0) "ms_${song.mediaStoreId}" else "path_${song.path}_${song.id}"

        memoryCache.get(cacheKey)?.let { return@withContext it }

        var decoded: Bitmap? = null

        // 1. Android Q+ official MediaStore Track Thumbnail
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && song.mediaStoreId > 0) {
            try {
                val trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.mediaStoreId
                )
                decoded = context.contentResolver.loadThumbnail(trackUri, android.util.Size(512, 512), null)
            } catch (_: Exception) {}
        }

        // 2. Android Q+ official MediaStore Album Thumbnail
        if (decoded == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && song.albumId > 0) {
            try {
                val albumUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    song.albumId
                )
                decoded = context.contentResolver.loadThumbnail(albumUri, android.util.Size(512, 512), null)
            } catch (_: Exception) {}
        }

        // 3. Try MediaStore Album Art URI (older Android / legacy albumart table)
        if (decoded == null && song.albumId > 0) {
            val artUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                song.albumId
            )
            try {
                context.contentResolver.openInputStream(artUri)?.use { stream ->
                    decoded = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }

        // 4. Try MediaMetadataRetriever via Scoped Storage ContentUri / FileDescriptor
        if (decoded == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (song.mediaStoreId > 0) {
                    val trackUri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.mediaStoreId
                    )
                    context.contentResolver.openFileDescriptor(trackUri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val pic = retriever.embeddedPicture
                        if (pic != null) {
                            decoded = BitmapFactory.decodeByteArray(pic, 0, pic.size)
                        }
                    }
                } else if (song.path.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(Uri.parse(song.path), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val pic = retriever.embeddedPicture
                        if (pic != null) {
                            decoded = BitmapFactory.decodeByteArray(pic, 0, pic.size)
                        }
                    }
                } else if (song.path.isNotBlank()) {
                    val file = File(song.path)
                    if (file.exists() && file.canRead()) {
                        retriever.setDataSource(song.path)
                        val pic = retriever.embeddedPicture
                        if (pic != null) {
                            decoded = BitmapFactory.decodeByteArray(pic, 0, pic.size)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        // 5. Try FLAC embedded picture parser if FLAC format
        if (decoded == null && (song.codec.equals("FLAC", ignoreCase = true) || song.path.endsWith(".flac", ignoreCase = true))) {
            try {
                val stream = if (song.mediaStoreId > 0) {
                    val trackUri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.mediaStoreId
                    )
                    context.contentResolver.openInputStream(trackUri)
                } else if (song.path.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(song.path))
                } else {
                    val file = File(song.path)
                    if (file.exists() && file.canRead()) FileInputStream(file) else null
                }
                stream?.use { inputStream ->
                    val flacMeta = FlacVorbisCommentParser.parse(inputStream)
                    flacMeta?.embeddedArtBytes?.let { bytes ->
                        decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            } catch (_: Exception) {}
        }

        // 6. Sibling cover image file in the same folder (common in lossless collections)
        if (decoded == null && song.path.startsWith("/")) {
            try {
                val parent = File(song.path).parentFile
                if (parent != null && parent.exists() && parent.isDirectory) {
                    val coverNames = listOf("cover.jpg", "folder.jpg", "front.jpg", "cover.png", "folder.png", "artwork.jpg")
                    for (name in coverNames) {
                        val artFile = File(parent, name)
                        if (artFile.exists() && artFile.canRead()) {
                            decoded = BitmapFactory.decodeFile(artFile.absolutePath)
                            if (decoded != null) break
                        }
                    }
                }
            } catch (_: Exception) {}
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
    val trackKey = remember(song?.id, song?.mediaStoreId, song?.path) {
        song?.let {
            if (it.mediaStoreId > 0) "ms_${it.mediaStoreId}" else "path_${it.path}_${it.id}"
        }
    }

    var bitmap by remember(trackKey) {
        mutableStateOf(trackKey?.let { AlbumArtCache.memoryCache.get(it) })
    }

    LaunchedEffect(trackKey) {
        if (bitmap == null && song != null) {
            val loaded = AlbumArtCache.loadArtwork(context, song)
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }

    val fallbackGradient = if (theme.isLight) {
        Brush.linearGradient(
            listOf(
                theme.primaryColor.copy(alpha = 0.22f),
                theme.secondaryColor.copy(alpha = 0.14f),
                Color(0xFFE2E8F0)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                theme.primaryColor.copy(alpha = 0.45f),
                theme.secondaryColor.copy(alpha = 0.25f),
                Color(0xFF141724)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackGradient),
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
                tint = if (theme.isLight) theme.primaryColor else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(fallbackIconSize)
            )
        }
    }
}
