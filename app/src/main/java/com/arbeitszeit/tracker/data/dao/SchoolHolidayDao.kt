package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.SchoolHoliday
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolHolidayDao {

    /**
     * Gibt alle Schulferien für ein Bundesland und Jahr zurück
     */
    @Query("SELECT * FROM school_holidays WHERE bundesland = :bundesland AND year = :year ORDER BY startDate ASC")
    suspend fun getHolidaysByBundeslandAndYear(bundesland: String, year: Int): List<SchoolHoliday>

    /**
     * Gibt alle Schulferien für ein Bundesland und Jahr als Flow zurück
     */
    @Query("SELECT * FROM school_holidays WHERE bundesland = :bundesland AND year = :year ORDER BY startDate ASC")
    fun getHolidaysByBundeslandAndYearFlow(bundesland: String, year: Int): Flow<List<SchoolHoliday>>

    /**
     * Gibt alle Schulferien in einem Datumsbereich zurück
     */
    @Query("SELECT * FROM school_holidays WHERE bundesland = :bundesland AND endDate >= :startDate AND startDate <= :endDate ORDER BY startDate ASC")
    suspend fun getHolidaysInRange(bundesland: String, startDate: String, endDate: String): List<SchoolHoliday>

    /**
     * Prüft ob ein Datum in den Schulferien liegt
     */
    @Query("SELECT * FROM school_holidays WHERE bundesland = :bundesland AND startDate <= :date AND endDate >= :date")
    suspend fun getHolidaysByDate(bundesland: String, date: String): List<SchoolHoliday>

    /**
     * Fügt Schulferien hinzu
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holiday: SchoolHoliday): Long

    /**
     * Fügt mehrere Schulferien hinzu
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<SchoolHoliday>)

    /**
     * Löscht alle Schulferien eines Bundeslands und Jahres
     */
    @Query("DELETE FROM school_holidays WHERE bundesland = :bundesland AND year = :year")
    suspend fun deleteByBundeslandAndYear(bundesland: String, year: Int)

    /**
     * Prüft ob Schulferien für ein Bundesland und Jahr bereits vorhanden sind
     */
    @Query("SELECT COUNT(*) FROM school_holidays WHERE bundesland = :bundesland AND year = :year")
    suspend fun getCountByBundeslandAndYear(bundesland: String, year: Int): Int
}
