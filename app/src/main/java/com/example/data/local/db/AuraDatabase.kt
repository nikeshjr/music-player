package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.logger.AuraLog
import com.example.data.model.EqPreset
import com.example.data.model.PlayHistory
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import com.example.data.model.ThemeProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AuraDatabase: Central SQLite persistence powered by Room.
 * Stores audio tracks, playlists, playback statistics, custom equalizer profiles,
 * and user theme configurations.
 */
@Database(
    entities = [
        Song::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        PlayHistory::class,
        EqPreset::class,
        ThemeProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun themeProfileDao(): ThemeProfileDao

    companion object {
        private const val TAG = "AuraDatabase"
        private const val DATABASE_NAME = "aura_music.db"

        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AuraDatabase {
            return Room.databaseBuilder(
                context,
                AuraDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        AuraLog.i(TAG, "Creating new AuraDatabase. Pre-populating default presets...")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val instance = getInstance(context)
                                prePopulateDefaults(instance)
                            } catch (e: Exception) {
                                AuraLog.e(TAG, "Error during pre-populating database", e)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private suspend fun prePopulateDefaults(database: AuraDatabase) {
            val defaultPresets = listOf(
                EqPreset(name = "Flat", bandLevels = "0,0,0,0,0", isDefault = true),
                EqPreset(name = "Bass Boost", bandLevels = "600,400,200,0,-100", bassBoost = 600),
                EqPreset(name = "Vocal Clarity", bandLevels = "-200,0,400,600,300"),
                EqPreset(name = "Electronic", bandLevels = "500,200,-100,300,500"),
                EqPreset(name = "Rock", bandLevels = "400,200,-100,200,500"),
                EqPreset(name = "Acoustic", bandLevels = "300,200,100,200,400")
            )
            for (preset in defaultPresets) {
                database.eqPresetDao().insertPreset(preset)
            }

            // Create default Smart Playlists
            database.playlistDao().insertPlaylist(
                Playlist(name = "Recently Added", isSmart = true, smartType = "RECENTLY_ADDED")
            )
            database.playlistDao().insertPlaylist(
                Playlist(name = "Most Played", isSmart = true, smartType = "MOST_PLAYED")
            )

            AuraLog.i(TAG, "Default database entries seeded successfully.")
        }
    }
}
