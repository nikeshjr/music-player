package com.example.core.di

import android.content.Context
import com.example.data.backup.LibraryBackupManager
import com.example.data.local.datastore.AuraPreferences
import com.example.data.local.datastore.ThemeProfileManager
import com.example.data.local.db.AuraDatabase
import com.example.data.repository.SongRepositoryImpl
import com.example.data.scanner.MediaStoreScanner
import com.example.data.scanner.SafFolderScanner
import com.example.domain.repository.SongRepository

/**
 * AuraServiceLocator: Central dependency container providing singletons for
 * repositories, database instances, and DataStore preferences across Aura Music.
 */
object AuraServiceLocator {

    @Volatile
    private var database: AuraDatabase? = null

    @Volatile
    private var preferences: AuraPreferences? = null

    @Volatile
    private var songRepository: SongRepository? = null

    @Volatile
    private var themeProfileManager: ThemeProfileManager? = null

    @Volatile
    private var safFolderScanner: SafFolderScanner? = null

    @Volatile
    private var libraryBackupManager: LibraryBackupManager? = null

    fun provideDatabase(context: Context): AuraDatabase {
        return database ?: synchronized(this) {
            database ?: AuraDatabase.getInstance(context).also { database = it }
        }
    }

    fun providePreferences(context: Context): AuraPreferences {
        return preferences ?: synchronized(this) {
            preferences ?: AuraPreferences(context.applicationContext).also { preferences = it }
        }
    }

    fun provideThemeProfileManager(context: Context): ThemeProfileManager {
        return themeProfileManager ?: synchronized(this) {
            val db = provideDatabase(context)
            val prefs = providePreferences(context)
            themeProfileManager ?: ThemeProfileManager(context.applicationContext, db, prefs).also { themeProfileManager = it }
        }
    }

    fun provideSafFolderScanner(context: Context): SafFolderScanner {
        return safFolderScanner ?: synchronized(this) {
            safFolderScanner ?: SafFolderScanner(context.applicationContext).also { safFolderScanner = it }
        }
    }

    fun provideMediaStoreScanner(context: Context): MediaStoreScanner {
        val db = provideDatabase(context)
        return MediaStoreScanner(context.applicationContext, db.songDao())
    }

    fun provideLibraryBackupManager(context: Context): LibraryBackupManager {
        return libraryBackupManager ?: synchronized(this) {
            val db = provideDatabase(context)
            libraryBackupManager ?: LibraryBackupManager(context.applicationContext, db).also { libraryBackupManager = it }
        }
    }

    fun provideSongRepository(context: Context): SongRepository {
        return songRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val scanner = MediaStoreScanner(context.applicationContext, db.songDao())
            songRepository ?: SongRepositoryImpl(db.songDao(), scanner).also { songRepository = it }
        }
    }
}
