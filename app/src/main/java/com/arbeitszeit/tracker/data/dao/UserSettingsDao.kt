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

    @Query("UPDATE user_settings SET onboardingCompleted = 1, updatedAt = :timestamp WHERE id = 1")
    suspend fun markOnboardingCompleted(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_settings SET onboardingCompleted = 0, updatedAt = :timestamp WHERE id = 1")
    suspend fun resetOnboarding(timestamp: Long = System.currentTimeMillis())
}
