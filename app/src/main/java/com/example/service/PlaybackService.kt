package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.core.logger.AuraLog
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * PlaybackService: Foreground MediaLibraryService powering Android Auto, lockscreen media controls,
 * and gapless playback. Connects directly to AuraPlayerController.
 */
class PlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        private const val TAG = "PlaybackService"
        private const val ROOT_ID = "aura_root_id"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AuraLog.i(TAG, "Creating PlaybackService...")

        // Reuse the single canonical ExoPlayer from AuraPlayerController
        val exoPlayer = AuraPlayerController.getInstance(this).getOrCreatePlayer()
        player = exoPlayer

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Aura Music Library")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val children = mutableListOf<MediaItem>()

                if (parentId == ROOT_ID) {
                    val categories = listOf(
                        Pair("recent_id", "Recently Added"),
                        Pair("albums_id", "Albums"),
                        Pair("artists_id", "Artists"),
                        Pair("playlists_id", "Playlists"),
                        Pair("favorites_id", "Favorites")
                    )
                    for ((id, title) in categories) {
                        children.add(
                            MediaItem.Builder()
                                .setMediaId(id)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(title)
                                        .build()
                                )
                                .build()
                        )
                    }
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    "TOGGLE_FAVORITE" -> AuraPlayerController.getInstance(this@PlaybackService).toggleFavorite()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, callback)
            .setSessionActivity(sessionActivityIntent)
            .build()

        AuraLog.i(TAG, "PlaybackService initialized with MediaLibrarySession.")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        AuraLog.i(TAG, "Destroying PlaybackService...")
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        player = null
        super.onDestroy()
    }
}
