package com.example.data.metadata

import com.example.core.logger.AuraLog
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * FlacVorbisCommentParser: Pure Kotlin, 100% offline FLAC stream and Vorbis Comment parser.
 * Reads native FLAC metadata blocks directly from the audio file stream without relying on
 * external C++ libraries or network decoders:
 *  - Block 0: STREAMINFO (Min/Max Block Size, Sample Rate, Channels, Bit Depth, Total Samples)
 *  - Block 4: VORBIS_COMMENT (Title, Artist, Album, ReplayGain, Lyrics, Track/Disc numbers)
 *  - Block 5: CUESHEET (Embedded Cue sheet for single-file FLAC albums)
 *  - Block 6: PICTURE (Embedded Front Cover artwork)
 */
object FlacVorbisCommentParser {

    private const val TAG = "FlacVorbisParser"
    private val FLAC_SIGNATURE = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte())

    data class FlacMetadata(
        val sampleRate: Int = 44100,
        val bitDepth: Int = 16,
        val channels: Int = 2,
        val totalSamples: Long = 0L,
        val durationMs: Long = 0L,
        val bitrateKbps: Int = 0,
        val isHiRes: Boolean = false,
        val comments: Map<String, String> = emptyMap(),
        val embeddedLyrics: String? = null,
        val embeddedCueSheet: String? = null,
        val replayGainTrack: Float? = null,
        val replayGainAlbum: Float? = null,
        val replayGainTrackPeak: Float? = null,
        val hasEmbeddedArt: Boolean = false,
        val embeddedArtBytes: ByteArray? = null
    )

    /**
     * Parses a local FLAC file and returns its complete metadata and audio characteristics.
     */
    fun parse(file: File): FlacMetadata? {
        if (!file.exists() || file.length() < 42) return null

        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4 || !header.contentEquals(FLAC_SIGNATURE)) {
                    return null // Not a valid native FLAC file
                }

                var isLastBlock = false
                var sampleRate = 44100
                var bitDepth = 16
                var channels = 2
                var totalSamples = 0L
                val comments = mutableMapOf<String, String>()
                var embeddedLyrics: String? = null
                var embeddedCue: String? = null
                var trackGain: Float? = null
                var albumGain: Float? = null
                var trackPeak: Float? = null
                var artBytes: ByteArray? = null

                while (!isLastBlock) {
                    val blockHeader = ByteArray(4)
                    if (input.read(blockHeader) != 4) break

                    val byte0 = blockHeader[0].toInt() and 0xFF
                    isLastBlock = (byte0 and 0x80) != 0
                    val blockType = byte0 and 0x7F

                    val blockLength = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                            ((blockHeader[2].toInt() and 0xFF) shl 8) or
                            (blockHeader[3].toInt() and 0xFF)

                    when (blockType) {
                        0 -> {
                            // STREAMINFO (34 bytes)
                            val streamInfo = ByteArray(blockLength)
                            if (input.read(streamInfo) == blockLength && blockLength >= 34) {
                                val buffer = ByteBuffer.wrap(streamInfo).order(ByteOrder.BIG_ENDIAN)
                                buffer.position(10) // Skip min/max block and frame sizes

                                // 20 bits sample rate, 3 bits (channels - 1), 5 bits (bits/sample - 1), 36 bits total samples
                                val b1 = buffer.get().toInt() and 0xFF
                                val b2 = buffer.get().toInt() and 0xFF
                                val b3 = buffer.get().toInt() and 0xFF
                                val b4 = buffer.get().toInt() and 0xFF

                                sampleRate = (b1 shl 12) or (b2 shl 4) or (b3 shr 4)
                                channels = ((b3 shr 1) and 0x07) + 1
                                bitDepth = (((b3 and 0x01) shl 4) or (b4 shr 4)) + 1

                                val sampleCountHigh = (b4 and 0x0F).toLong()
                                val sampleCountLow = buffer.getInt().toLong() and 0xFFFFFFFFL
                                totalSamples = (sampleCountHigh shl 32) or sampleCountLow
                            }
                        }
                        4 -> {
                            // VORBIS_COMMENT
                            val commentData = ByteArray(blockLength)
                            if (input.read(commentData) == blockLength) {
                                val parsed = parseVorbisComments(commentData)
                                comments.putAll(parsed)

                                // Extract lyrics
                                embeddedLyrics = parsed["LYRICS"] ?: parsed["UNSYNCEDLYRICS"]

                                // Extract ReplayGain
                                parsed["REPLAYGAIN_TRACK_GAIN"]?.let {
                                    trackGain = it.removeSuffix("dB").trim().toFloatOrNull()
                                }
                                parsed["REPLAYGAIN_ALBUM_GAIN"]?.let {
                                    albumGain = it.removeSuffix("dB").trim().toFloatOrNull()
                                }
                                parsed["REPLAYGAIN_TRACK_PEAK"]?.let {
                                    trackPeak = it.toFloatOrNull()
                                }
                            }
                        }
                        5 -> {
                            // CUESHEET
                            val cueData = ByteArray(blockLength)
                            if (input.read(cueData) == blockLength) {
                                embeddedCue = String(cueData, Charsets.UTF_8)
                            }
                        }
                        6 -> {
                            // PICTURE (Front cover preferred)
                            if (artBytes == null && blockLength < 5_000_000) { // Limit to 5MB embedded image
                                val picData = ByteArray(blockLength)
                                if (input.read(picData) == blockLength) {
                                    artBytes = extractPictureBytes(picData)
                                }
                            } else {
                                input.skip(blockLength.toLong())
                            }
                        }
                        else -> {
                            // Skip padding, seektable, or application blocks
                            input.skip(blockLength.toLong())
                        }
                    }
                }

                val durationMs = if (sampleRate > 0) (totalSamples * 1000L) / sampleRate else 0L
                val bitrateKbps = if (durationMs > 0) ((file.length() * 8) / durationMs).toInt() else 0
                val isHiRes = bitDepth >= 24 || sampleRate >= 88200

                return FlacMetadata(
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    channels = channels,
                    totalSamples = totalSamples,
                    durationMs = durationMs,
                    bitrateKbps = bitrateKbps,
                    isHiRes = isHiRes,
                    comments = comments,
                    embeddedLyrics = embeddedLyrics,
                    embeddedCueSheet = embeddedCue,
                    replayGainTrack = trackGain,
                    replayGainAlbum = albumGain,
                    replayGainTrackPeak = trackPeak,
                    hasEmbeddedArt = artBytes != null,
                    embeddedArtBytes = artBytes
                )
            }
        } catch (e: Exception) {
            AuraLog.e(TAG, "Error parsing FLAC file: ${file.name}", e)
            return null
        }
    }

    private fun parseVorbisComments(data: ByteArray): Map<String, String> {
        val comments = mutableMapOf<String, String>()
        try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val vendorLength = buffer.getInt()
            if (vendorLength in 1..buffer.remaining()) {
                buffer.position(buffer.position() + vendorLength) // Skip vendor string
            }

            if (buffer.remaining() < 4) return comments
            val userCommentListLength = buffer.getInt()

            for (i in 0 until userCommentListLength) {
                if (buffer.remaining() < 4) break
                val length = buffer.getInt()
                if (length < 0 || length > buffer.remaining()) break

                val commentBytes = ByteArray(length)
                buffer.get(commentBytes)
                val commentStr = String(commentBytes, Charsets.UTF_8)
                val eqIndex = commentStr.indexOf('=')
                if (eqIndex > 0) {
                    val key = commentStr.substring(0, eqIndex).uppercase().trim()
                    val value = commentStr.substring(eqIndex + 1).trim()
                    comments[key] = value
                }
            }
        } catch (e: Exception) {
            AuraLog.w(TAG, "Warning parsing Vorbis comments: ${e.message}")
        }
        return comments
    }

    private fun extractPictureBytes(data: ByteArray): ByteArray? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val pictureType = buffer.getInt() // 3 = Cover (front)
            val mimeLength = buffer.getInt()
            if (mimeLength < 0 || mimeLength > buffer.remaining()) return null
            buffer.position(buffer.position() + mimeLength)

            val descLength = buffer.getInt()
            if (descLength < 0 || descLength > buffer.remaining()) return null
            buffer.position(buffer.position() + descLength)

            buffer.position(buffer.position() + 16) // Skip width, height, color depth, colors used
            val dataLength = buffer.getInt()
            if (dataLength <= 0 || dataLength > buffer.remaining()) return null

            val imageBytes = ByteArray(dataLength)
            buffer.get(imageBytes)
            imageBytes
        } catch (_: Exception) {
            null
        }
    }
}
