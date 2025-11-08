package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<UserSettings?>
    
    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettings(): UserSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettings)
    
    @Update
    suspend fun update(settings: UserSettings)
    
    @Query("UPDATE user_settings SET updatedAt = :timestamp WHERE id = 1")
    suspend fun updateTimestamp(timestamp: Long = System.currentTimeMillis())
}
