package com.example.data.metadata

import com.example.core.logger.AuraLog

/**
 * LrcLyricsParser: Parses LRC formatted synchronized lyrics and plain unsynced text.
 * Handles multiple timestamp headers per line, offset adjustments [offset:+/-ms],
 * and extracts accurate millisecond timestamps for synchronized line-by-line scrolling.
 */
object LrcLyricsParser {

    private const val TAG = "LrcLyricsParser"
    private val TIMESTAMP_REGEX = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?]")

    data class LyricLine(
        val timestampMs: Long,
        val text: String
    )

    data class ParsedLyrics(
        val isSynchronized: Boolean,
        val lines: List<LyricLine>,
        val plainText: String
    ) {
        /**
         * Efficient binary search to find the active lyric line for the current player position.
         */
        fun findCurrentLineIndex(positionMs: Long): Int {
            if (!isSynchronized || lines.isEmpty()) return -1
            if (positionMs < lines.first().timestampMs) return 0

            var low = 0
            var high = lines.size - 1
            var result = 0

            while (low <= high) {
                val mid = (low + high) ushr 1
                val midTime = lines[mid].timestampMs
                if (midTime <= positionMs) {
                    result = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            return result
        }
    }

    /**
     * Parses a string containing either LRC synced lyrics or plain text lyrics.
     */
    fun parse(rawContent: String): ParsedLyrics {
        if (rawContent.isBlank()) {
            return ParsedLyrics(isSynchronized = false, lines = emptyList(), plainText = "")
        }

        val parsedLines = mutableListOf<LyricLine>()
        var globalOffsetMs = 0L

        try {
            val rawLines = rawContent.lines()

            for (line in rawLines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Check for [offset:+/-ms] tag
                if (trimmed.startsWith("[offset:", ignoreCase = true)) {
                    val offsetStr = trimmed.substringAfter(":").substringBefore("]").trim()
                    globalOffsetMs = offsetStr.toLongOrNull() ?: 0L
                    continue
                }

                // Check for standard ID tags like [ti:], [ar:], [al:]
                if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                    trimmed.startsWith("[al:") || trimmed.startsWith("[by:")
                ) {
                    continue
                }

                // Match all timestamps on this line (e.g. [00:12.30][00:45.60]Chorus text)
                val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()

                if (matches.isNotEmpty()) {
                    val lyricText = trimmed.replace(TIMESTAMP_REGEX, "").trim()

                    for (match in matches) {
                        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                        val millisRaw = match.groupValues.getOrNull(3) ?: "0"

                        val millis = when (millisRaw.length) {
                            2 -> (millisRaw.toLongOrNull() ?: 0L) * 10L // e.g. 50 -> 500ms
                            3 -> millisRaw.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        val timeMs = (minutes * 60 * 1000L) + (seconds * 1000L) + millis + globalOffsetMs
                        parsedLines.add(LyricLine(timestampMs = timeMs.coerceAtLeast(0L), text = lyricText))
                    }
                }
            }

            if (parsedLines.isNotEmpty()) {
                // Sort by timestamp
                val sorted = parsedLines.sortedBy { it.timestampMs }
                return ParsedLyrics(
                    isSynchronized = true,
                    lines = sorted,
                    plainText = sorted.joinToString("\n") { it.text }
                )
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error parsing LRC lyrics: ${e.message}", e)
        }

        // Fallback to plain text
        val plainLines = rawContent.lines().map { LyricLine(0L, it.trim()) }
        return ParsedLyrics(isSynchronized = false, lines = plainLines, plainText = rawContent.trim())
    }
}
