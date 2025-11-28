package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.ClosingDay
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosingDayDao {

    /**
     * Gibt alle Schließtage zurück, sortiert nach Startdatum
     */
    @Query("SELECT * FROM closing_days ORDER BY startDate ASC")
    fun getAllClosingDaysFlow(): Flow<List<ClosingDay>>

    /**
     * Gibt alle Schließtage eines bestimmten Jahres zurück
     */
    @Query("SELECT * FROM closing_days WHERE year = :year ORDER BY startDate ASC")
    suspend fun getClosingDaysByYear(year: Int): List<ClosingDay>

    /**
     * Gibt alle Schließtage eines bestimmten Jahres als Flow zurück
     */
    @Query("SELECT * FROM closing_days WHERE year = :year ORDER BY startDate ASC")
    fun getClosingDaysByYearFlow(year: Int): Flow<List<ClosingDay>>

    /**
     * Gibt alle Schließtage in einem Datumsbereich zurück
     * (Schließtage, die den Zeitraum überlappen)
     */
    @Query("SELECT * FROM closing_days WHERE endDate >= :startDate AND startDate <= :endDate ORDER BY startDate ASC")
    suspend fun getClosingDaysInRange(startDate: String, endDate: String): List<ClosingDay>

    /**
     * Gibt alle Schließtage in einem Datumsbereich als Flow zurück
     */
    @Query("SELECT * FROM closing_days WHERE endDate >= :startDate AND startDate <= :endDate ORDER BY startDate ASC")
    fun getClosingDaysInRangeFlow(startDate: String, endDate: String): Flow<List<ClosingDay>>

    /**
     * Prüft ob ein bestimmtes Datum ein Schließtag ist
     * @param date yyyy-MM-dd Format
     * @return Liste aller Schließtage, die das Datum enthalten
     */
    @Query("SELECT * FROM closing_days WHERE startDate <= :date AND endDate >= :date")
    suspend fun getClosingDaysByDate(date: String): List<ClosingDay>

    /**
     * Gibt einen bestimmten Schließtag zurück
     */
    @Query("SELECT * FROM closing_days WHERE id = :id")
    suspend fun getClosingDayById(id: Long): ClosingDay?

    /**
     * Gibt einen bestimmten Schließtag als Flow zurück
     */
    @Query("SELECT * FROM closing_days WHERE id = :id")
    fun getClosingDayByIdFlow(id: Long): Flow<ClosingDay?>

    /**
     * Fügt einen neuen Schließtag hinzu
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(closingDay: ClosingDay): Long

    /**
     * Aktualisiert einen bestehenden Schließtag
     */
    @Update
    suspend fun update(closingDay: ClosingDay)

    /**
     * Löscht einen Schließtag
     */
    @Delete
    suspend fun delete(closingDay: ClosingDay)

    /**
     * Löscht einen Schließtag anhand der ID
     */
    @Query("DELETE FROM closing_days WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Löscht alle Schließtage eines bestimmten Jahres
     */
    @Query("DELETE FROM closing_days WHERE year = :year")
    suspend fun deleteByYear(year: Int)

    /**
     * Löscht alle Schließtage
     */
    @Query("DELETE FROM closing_days")
    suspend fun deleteAll()

    /**
     * Gibt die Anzahl der Schließtage eines Jahres zurück
     */
    @Query("SELECT COUNT(*) FROM closing_days WHERE year = :year")
    suspend fun getCountByYear(year: Int): Int
}
