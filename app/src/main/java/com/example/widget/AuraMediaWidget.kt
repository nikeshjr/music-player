package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.logger.AuraLog
import com.example.service.AuraPlayerController

/**
 * AuraMediaWidget: System home-screen widget for Aura Music.
 * Built using AppWidgetProvider and RemoteViews for maximum compatibility
 * from Android 8.0 through Android 15.
 * Features glassmorphic design, track details, Hi-Res badge, and transport controls.
 */
class AuraMediaWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "AuraMediaWidget"
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AuraMediaWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, AuraMediaWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val controller = AuraPlayerController.getInstance(context)
        val uiState = controller.uiState.value
        val currentSong = uiState.currentSong

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_aura_media)

            // Track metadata
            views.setTextViewText(R.id.widget_song_title, currentSong?.title ?: "Aura Music")
            views.setTextViewText(R.id.widget_song_artist, currentSong?.artist ?: "Tap to play music")
            views.setTextViewText(
                R.id.widget_format_badge,
                if (currentSong?.isHiResTrack == true) "HI-RES FLAC" else currentSong?.codec ?: "OFFLINE"
            )

            // Play/Pause icon
            val playPauseRes = if (uiState.isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseRes)

            // PendingIntents for actions
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                createActionPendingIntent(context, ACTION_PLAY_PAUSE, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                createActionPendingIntent(context, ACTION_NEXT, 102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                createActionPendingIntent(context, ACTION_PREV, 103)
            )

            // Tap widget body to launch app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val controller = AuraPlayerController.getInstance(context)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                AuraLog.d(TAG, "Widget action: Play/Pause")
                controller.togglePlayPause()
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                AuraLog.d(TAG, "Widget action: Next")
                controller.next()
                updateAllWidgets(context)
            }
            ACTION_PREV -> {
                AuraLog.d(TAG, "Widget action: Prev")
                controller.previous()
                updateAllWidgets(context)
            }
        }
    }

    private fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AuraMediaWidget::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
