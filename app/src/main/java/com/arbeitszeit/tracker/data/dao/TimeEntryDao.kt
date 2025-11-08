package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.TimeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    
    @Query("SELECT * FROM time_entries WHERE datum = :date")
    suspend fun getEntryByDate(date: String): TimeEntry?
    
    @Query("SELECT * FROM time_entries WHERE datum = :date")
    fun getEntryByDateFlow(date: String): Flow<TimeEntry?>
    
    @Query("SELECT * FROM time_entries WHERE jahr = :year ORDER BY datum ASC")
    suspend fun getEntriesByYear(year: Int): List<TimeEntry>
    
    @Query("SELECT * FROM time_entries WHERE jahr = :year AND kalenderwoche = :kw ORDER BY datum ASC")
    suspend fun getEntriesByWeek(year: Int, kw: Int): List<TimeEntry>
    
    @Query("SELECT * FROM time_entries WHERE jahr = :year AND kalenderwoche BETWEEN :startKW AND :endKW ORDER BY datum ASC")
    suspend fun getEntriesByWeekRange(year: Int, startKW: Int, endKW: Int): List<TimeEntry>
    
    @Query("SELECT * FROM time_entries WHERE datum BETWEEN :startDate AND :endDate ORDER BY datum ASC")
    suspend fun getEntriesByDateRange(startDate: String, endDate: String): List<TimeEntry>
    
    @Query("SELECT * FROM time_entries WHERE datum BETWEEN :startDate AND :endDate ORDER BY datum ASC")
    fun getEntriesByDateRangeFlow(startDate: String, endDate: String): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE jahr = :year AND kalenderwoche = :kw")
    fun getWeekEntriesFlow(year: Int, kw: Int): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries ORDER BY datum DESC")
    fun getAllEntriesFlow(): Flow<List<TimeEntry>>
    
    @Query("SELECT * FROM time_entries WHERE startZeit IS NULL AND endZeit IS NULL AND typ = 'NORMAL' AND datum <= :date ORDER BY datum DESC")
    suspend fun getIncompleteEntries(date: String): List<TimeEntry>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimeEntry): Long
    
    @Update
    suspend fun update(entry: TimeEntry)
    
    @Delete
    suspend fun delete(entry: TimeEntry)
    
    @Query("DELETE FROM time_entries WHERE datum = :date")
    suspend fun deleteByDate(date: String)
    
    @Query("SELECT COUNT(*) FROM time_entries WHERE jahr = :year")
    suspend fun getEntryCountByYear(year: Int): Int
}
