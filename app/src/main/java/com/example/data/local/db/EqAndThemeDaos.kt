package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EqPreset
import com.example.data.model.ThemeProfile
import kotlinx.coroutines.flow.Flow

/**
 * EqPresetDao: Manages audio equalizer presets and device-specific audio profiles.
 */
@Dao
interface EqPresetDao {

    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<EqPreset>>

    @Query("SELECT * FROM eq_presets WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPreset(): EqPreset?

    @Query("SELECT * FROM eq_presets WHERE targetDeviceType = :deviceType LIMIT 1")
    suspend fun getPresetForDevice(deviceType: String): EqPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqPreset): Long

    @Update
    suspend fun updatePreset(preset: EqPreset)

    @Query("DELETE FROM eq_presets WHERE id = :id AND isDefault = 0")
    suspend fun deletePreset(id: Long)
}

/**
 * ThemeProfileDao: Manages saved user theme profiles for quick-switching and export.
 */
@Dao
interface ThemeProfileDao {

    @Query("SELECT * FROM theme_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<ThemeProfile>>

    @Query("SELECT * FROM theme_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ThemeProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ThemeProfile): Long

    @Query("DELETE FROM theme_profiles WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteProfile(id: Long)
}
