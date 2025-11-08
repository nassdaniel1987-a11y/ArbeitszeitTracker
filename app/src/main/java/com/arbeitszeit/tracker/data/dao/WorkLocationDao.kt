package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.WorkLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkLocationDao {

    @Query("SELECT * FROM work_locations WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabledLocationsFlow(): Flow<List<WorkLocation>>

    @Query("SELECT * FROM work_locations ORDER BY name ASC")
    fun getAllLocationsFlow(): Flow<List<WorkLocation>>

    @Query("SELECT * FROM work_locations WHERE enabled = 1")
    suspend fun getEnabledLocations(): List<WorkLocation>

    @Query("SELECT * FROM work_locations WHERE id = :id")
    suspend fun getLocationById(id: Long): WorkLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: WorkLocation): Long

    @Update
    suspend fun update(location: WorkLocation)

    @Delete
    suspend fun delete(location: WorkLocation)

    @Query("DELETE FROM work_locations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_locations")
    suspend fun deleteAll()
}
