package com.example.data.metadata

import com.example.core.logger.AuraLog

/**
 * CueSheetParser: Parses CUE sheets (.cue files or embedded FLAC CUESHEET tags).
 * Splits single-file album recordings (e.g. whole-album FLAC/WAV/APE rips) into virtual individual
 * tracks with exact start offsets and calculated durations based on 75 frames/second CD-DA timing.
 */
object CueSheetParser {

    private const val TAG = "CueSheetParser"

    data class CueTrack(
        val trackNumber: Int,
        val title: String,
        val performer: String,
        val startOffsetMs: Long,
        val durationMs: Long = 0L,
        val fileName: String? = null
    )

    data class ParsedCueSheet(
        val albumTitle: String,
        val albumPerformer: String,
        val tracks: List<CueTrack>
    )

    /**
     * Parses the textual content of a CUE sheet.
     */
    fun parse(cueContent: String, totalAlbumDurationMs: Long = 0L): ParsedCueSheet {
        var globalPerformer = "Unknown Artist"
        var globalTitle = "Unknown Album"
        var currentFile: String? = null

        val tracks = mutableListOf<CueTrack>()
        var currentTrackNumber = -1
        var currentTrackTitle = ""
        var currentTrackPerformer = ""
        var currentTrackStartMs = 0L

        fun commitCurrentTrack() {
            if (currentTrackNumber > 0) {
                tracks.add(
                    CueTrack(
                        trackNumber = currentTrackNumber,
                        title = if (currentTrackTitle.isNotBlank()) currentTrackTitle else "Track $currentTrackNumber",
                        performer = if (currentTrackPerformer.isNotBlank()) currentTrackPerformer else globalPerformer,
                        startOffsetMs = currentTrackStartMs,
                        fileName = currentFile
                    )
                )
            }
        }

        try {
            val lines = cueContent.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("REM", ignoreCase = true)) continue

                val upper = trimmed.uppercase()
                when {
                    upper.startsWith("PERFORMER") && currentTrackNumber == -1 -> {
                        globalPerformer = extractQuotedValue(trimmed)
                    }
                    upper.startsWith("TITLE") && currentTrackNumber == -1 -> {
                        globalTitle = extractQuotedValue(trimmed)
                    }
                    upper.startsWith("FILE") -> {
                        currentFile = extractQuotedValue(trimmed)
                    }
                    upper.startsWith("TRACK") -> {
                        commitCurrentTrack()
                        val parts = trimmed.split(Regex("\\s+"))
                        currentTrackNumber = parts.getOrNull(1)?.toIntOrNull() ?: (tracks.size + 1)
                        currentTrackTitle = ""
                        currentTrackPerformer = globalPerformer
                        currentTrackStartMs = 0L
                    }
                    upper.startsWith("TITLE") && currentTrackNumber > 0 -> {
                        currentTrackTitle = extractQuotedValue(trimmed)
                    }
                    upper.startsWith("PERFORMER") && currentTrackNumber > 0 -> {
                        currentTrackPerformer = extractQuotedValue(trimmed)
                    }
                    upper.startsWith("INDEX 01") -> {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 3) {
                            currentTrackStartMs = parseMmSsFf(parts[2])
                        }
                    }
                }
            }
            commitCurrentTrack()

            // Calculate durations from offsets
            val resolvedTracks = tracks.mapIndexed { index, track ->
                val nextStart = if (index + 1 < tracks.size) {
                    tracks[index + 1].startOffsetMs
                } else {
                    totalAlbumDurationMs.coerceAtLeast(track.startOffsetMs)
                }
                val duration = (nextStart - track.startOffsetMs).coerceAtLeast(0L)
                track.copy(durationMs = duration)
            }

            return ParsedCueSheet(
                albumTitle = globalTitle,
                albumPerformer = globalPerformer,
                tracks = resolvedTracks
            )
        } catch (e: Exception) {
            AuraLog.e(TAG, "Failed parsing CUE sheet: ${e.message}", e)
            return ParsedCueSheet("Unknown Album", "Unknown Artist", emptyList())
        }
    }

    private fun extractQuotedValue(line: String): String {
        val firstQuote = line.indexOf('"')
        val lastQuote = line.lastIndexOf('"')
        return if (firstQuote != -1 && lastQuote > firstQuote) {
            line.substring(firstQuote + 1, lastQuote).trim()
        } else {
            val spaceIndex = line.indexOf(' ')
            if (spaceIndex != -1) line.substring(spaceIndex + 1).trim() else ""
        }
    }

    /**
     * Converts mm:ss:ff timing (where ff is 1/75th of a second) to milliseconds.
     */
    private fun parseMmSsFf(timeStr: String): Long {
        val parts = timeStr.split(":")
        if (parts.size < 3) return 0L
        val minutes = parts[0].toLongOrNull() ?: 0L
        val seconds = parts[1].toLongOrNull() ?: 0L
        val frames = parts[2].toLongOrNull() ?: 0L
        val framesMs = (frames * 1000L) / 75L
        return (minutes * 60 * 1000L) + (seconds * 1000L) + framesMs
    }
}
