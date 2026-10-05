package com.example.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.core.logger.AuraLog
import com.example.data.metadata.CueSheetParser
import com.example.data.metadata.LrcLyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SafFolderScanner: Scans Storage Access Framework (SAF) trees for sidecar files
 * (.lrc synced lyrics, .cue track sheets, .m3u playlists, and album art files)
 * that cannot be accessed directly via MediaStore.Audio.
 */
class SafFolderScanner(private val context: Context) {

    companion object {
        private const val TAG = "SafFolderScanner"
    }

    data class SafSidecarResources(
        val lyricsFiles: Map<String, Uri> = emptyMap(),      // BaseName -> Uri
        val cueFiles: List<Uri> = emptyList(),
        val playlistFiles: List<Uri> = emptyList(),
        val folderArtFiles: Map<String, Uri> = emptyMap()     // Directory/Album -> Uri
    )

    /**
     * Traverses a persisted SAF tree URI to catalog sidecar metadata files.
     */
    suspend fun scanTree(treeUri: Uri): SafSidecarResources = withContext(Dispatchers.IO) {
        AuraLog.i(TAG, "Scanning SAF tree for sidecars: $treeUri")
        val lyricsMap = mutableMapOf<String, Uri>()
        val cueList = mutableListOf<Uri>()
        val playlistList = mutableListOf<Uri>()
        val artMap = mutableMapOf<String, Uri>()

        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext SafSidecarResources()
            traverseDirectory(rootDoc, lyricsMap, cueList, playlistList, artMap, maxDepth = 4)
            AuraLog.i(TAG, "SAF scan found ${lyricsMap.size} LRC files, ${cueList.size} CUE sheets, ${playlistList.size} playlists.")
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error traversing SAF tree: ${e.message}", e)
        }

        SafSidecarResources(
            lyricsFiles = lyricsMap,
            cueFiles = cueList,
            playlistFiles = playlistList,
            folderArtFiles = artMap
        )
    }

    /**
     * Traverses a persisted SAF tree URI to index audio files directly from the folder.
     */
    suspend fun scanAudioFilesInTree(treeUri: Uri): List<com.example.data.model.Song> = withContext(Dispatchers.IO) {
        AuraLog.i(TAG, "Scanning SAF tree for audio tracks: $treeUri")
        val audioList = mutableListOf<com.example.data.model.Song>()
        val retriever = android.media.MediaMetadataRetriever()

        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            traverseAudioFiles(rootDoc, audioList, retriever, maxDepth = 4)
            AuraLog.i(TAG, "SAF audio scan completed: found ${audioList.size} audio files.")
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error traversing SAF audio: ${e.message}", e)
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }
        audioList
    }

    private fun traverseAudioFiles(
        directory: DocumentFile,
        audioList: MutableList<com.example.data.model.Song>,
        retriever: android.media.MediaMetadataRetriever,
        maxDepth: Int
    ) {
        if (maxDepth <= 0 || !directory.isDirectory) return

        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                traverseAudioFiles(file, audioList, retriever, maxDepth - 1)
            } else {
                val name = file.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in listOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma")) {
                    val baseName = name.substringBeforeLast('.')
                    var title = baseName
                    var artist = "Unknown Artist"
                    var album = directory.name ?: "Unknown Album"
                    var durationMs = 0L

                    try {
                        context.contentResolver.openFileDescriptor(file.uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                                if (it.isNotBlank()) title = it
                            }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                                if (it.isNotBlank()) artist = it
                            }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                                if (it.isNotBlank()) album = it
                            }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                                durationMs = it.toLongOrNull() ?: 0L
                            }
                        }
                    } catch (e: Exception) {
                        AuraLog.w(TAG, "Error retrieving metadata for ${file.uri}: ${e.message}")
                    }

                    val isHiRes = ext == "flac"
                    val currentTime = System.currentTimeMillis() / 1000L
                    val song = com.example.data.model.Song(
                        mediaStoreId = 0L,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        path = file.uri.toString(),
                        mimeType = "audio/$ext",
                        codec = ext.uppercase(),
                        isHiRes = isHiRes,
                        sizeBytes = file.length(),
                        dateAdded = currentTime,
                        dateModified = currentTime
                    )
                    audioList.add(song)
                }
            }
        }
    }

    private fun traverseDirectory(
        directory: DocumentFile,
        lyricsMap: MutableMap<String, Uri>,
        cueList: MutableList<Uri>,
        playlistList: MutableList<Uri>,
        artMap: MutableMap<String, Uri>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0 || !directory.isDirectory) return

        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                traverseDirectory(file, lyricsMap, cueList, playlistList, artMap, maxDepth - 1)
            } else {
                val name = file.name ?: continue
                val extension = name.substringAfterLast('.', "").lowercase()
                val baseName = name.substringBeforeLast('.').lowercase().trim()

                when (extension) {
                    "lrc" -> lyricsMap[baseName] = file.uri
                    "cue" -> cueList.add(file.uri)
                    "m3u", "m3u8" -> playlistList.add(file.uri)
                    "jpg", "jpeg", "png" -> {
                        if (baseName == "cover" || baseName == "folder" || baseName == "front") {
                            directory.name?.let { dirName ->
                                artMap[dirName.lowercase()] = file.uri
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads text content of a sidecar URI.
     */
    suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error reading text from SAF URI: $uri", e)
            null
        }
    }
}
