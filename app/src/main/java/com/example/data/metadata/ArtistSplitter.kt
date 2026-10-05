package com.example.data.metadata

/**
 * ArtistSplitter: Handles multi-artist splitting and collaborative metadata extraction.
 * Parses common artist delimiter strings (feat., ft., &, with, ;, /, vs.) into distinct,
 * searchable artist profiles without corrupting original track tags.
 */
object ArtistSplitter {

    // Ordered list of regex delimiters
    private val DELIMITERS = listOf(
        Regex("\\s+feat\\.?\\s+", RegexOption.IGNORE_CASE),
        Regex("\\s+ft\\.?\\s+", RegexOption.IGNORE_CASE),
        Regex("\\s+featuring\\s+", RegexOption.IGNORE_CASE),
        Regex("\\s+with\\s+", RegexOption.IGNORE_CASE),
        Regex("\\s+vs\\.?\\s+", RegexOption.IGNORE_CASE),
        Regex("\\s*&\\s*"),
        Regex("\\s*;\\s*"),
        Regex("\\s*/\\s*"),
        Regex("\\s*,\\s*")
    )

    data class SplitResult(
        val primaryArtist: String,
        val allArtists: List<String>
    )

    /**
     * Splits a raw artist string into individual artists.
     */
    fun splitArtists(rawArtist: String): SplitResult {
        if (rawArtist.isBlank() || rawArtist == "<unknown>") {
            return SplitResult("Unknown Artist", listOf("Unknown Artist"))
        }

        // Check if artist is a known single band or entity with '&' or ','
        val trimmed = rawArtist.trim()
        if (isKnownSingleArtist(trimmed)) {
            return SplitResult(trimmed, listOf(trimmed))
        }

        var currentArtists = mutableListOf(trimmed)

        for (pattern in DELIMITERS) {
            val nextList = mutableListOf<String>()
            for (artist in currentArtists) {
                val parts = artist.split(pattern)
                for (p in parts) {
                    val cleanPart = p.trim().removeSurrounding("(", ")").removeSurrounding("[", "]").trim()
                    if (cleanPart.isNotEmpty() && !nextList.contains(cleanPart)) {
                        nextList.add(cleanPart)
                    }
                }
            }
            currentArtists = nextList
        }

        val primary = currentArtists.firstOrNull() ?: "Unknown Artist"
        return SplitResult(primaryArtist = primary, allArtists = currentArtists)
    }

    private fun isKnownSingleArtist(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "simon & garfunkel" ||
                lower == "earth, wind & fire" ||
                lower == "kool & the gang" ||
                lower == "hall & oates" ||
                lower == "crosby, stills, nash & young" ||
                lower == "tom petty and the heartbreakers" ||
                lower == "florence + the machine" ||
                lower == "ac/dc"
    }
}
