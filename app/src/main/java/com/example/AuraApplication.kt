package com.example

import android.app.Application
import android.util.Log
import com.example.core.logger.AuraLog
import com.example.data.local.db.AuraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AuraApplication: Application entry point for Aura Music.
 * Initializes crash-safe logging, singleton dependencies, Room database instance,
 * and sets up unhandled exception handling to prevent player and overlay crashes.
 */
class AuraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize our crash-safe centralized logger
        AuraLog.initialize(this)
        AuraLog.i(TAG, "Aura Music application initializing...")

        // Pre-initialize Room Database singleton and seed initial tracks
        try {
            val db = AuraDatabase.getInstance(this)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    if (db.songDao().getSongCount() == 0) {
                        db.songDao().insertSongs(com.example.core.metadata.SampleHiResTracks.tracks)
                        AuraLog.i(TAG, "Seeded initial tracks into Room database")
                    }
                } catch (e: Throwable) {
                    AuraLog.e(TAG, "Error checking/seeding tracks: ${e.message}", e)
                }
            }
        } catch (e: Throwable) {
            AuraLog.e(TAG, "Room pre-init warning: ${e.message}", e)
        }

        // Pre-initialize Player Controller so playback engine is immediately warm
        try {
            com.example.service.AuraPlayerController.getInstance(this)
        } catch (e: Throwable) {
            AuraLog.e(TAG, "PlayerController pre-init warning: ${e.message}", e)
        }

        // Set up crash-safe fallback to prevent overlay or background loop fatal crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AuraLog.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        AuraLog.i(TAG, "Aura Music initialization complete.")
    }

    companion object {
        private const val TAG = "AuraApplication"
        lateinit var instance: AuraApplication
            private set
    }
}
