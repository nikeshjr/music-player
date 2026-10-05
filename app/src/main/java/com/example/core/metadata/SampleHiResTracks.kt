package com.example.core.metadata

import com.example.data.model.Song

/**
 * SampleHiResTracks: Pre-populated audiophile audio tracks showcasing
 * 24-bit/96kHz and 24-bit/192kHz FLAC characteristics, Vorbis comments,
 * and ReplayGain metadata for immediate UI testing and verification.
 */
object SampleHiResTracks {

    val tracks = listOf(
        Song(
            id = 1,
            mediaStoreId = 1001,
            title = "Aura Resonance (Master Studio)",
            artist = "Aura Sound Lab feat. Celestial Ensemble",
            album = "Audiophile Acoustic Sessions",
            durationMs = 284000L,
            path = "/storage/emulated/0/Music/Aura_Resonance.flac",
            mimeType = "audio/flac",
            sizeBytes = 85_420_000L,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            codec = "FLAC",
            sampleRate = 96000,
            bitDepth = 24,
            bitrate = 2410,
            channels = 2,
            isHiRes = true,
            replayGainTrack = -4.2f,
            replayGainAlbum = -3.8f,
            embeddedLyrics = "[00:10.00]Soundwaves drifting in the dark\n[00:25.40]Crystal clarity ignites a spark\n[00:45.00]Infinite resolution, audio pure"
        ),
        Song(
            id = 2,
            mediaStoreId = 1002,
            title = "Cosmic Synthesizer Dreams",
            artist = "Neon Horizon",
            album = "Quantum Waves",
            durationMs = 312000L,
            path = "/storage/emulated/0/Music/Cosmic_Dreams.flac",
            mimeType = "audio/flac",
            sizeBytes = 142_300_000L,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            codec = "FLAC",
            sampleRate = 192000,
            bitDepth = 24,
            bitrate = 4608,
            channels = 2,
            isHiRes = true,
            replayGainTrack = -2.1f,
            replayGainAlbum = -2.5f
        ),
        Song(
            id = 3,
            mediaStoreId = 1003,
            title = "Midnight Velvet Groove",
            artist = "The Lunar Quartet",
            album = "Midnight Velvet",
            durationMs = 245000L,
            path = "/storage/emulated/0/Music/Midnight_Velvet.flac",
            mimeType = "audio/flac",
            sizeBytes = 32_100_000L,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            codec = "FLAC",
            sampleRate = 44100,
            bitDepth = 16,
            bitrate = 920,
            channels = 2,
            isHiRes = false,
            replayGainTrack = -1.5f
        )
    )
}
