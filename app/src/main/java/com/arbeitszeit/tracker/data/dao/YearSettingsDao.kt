package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.YearSettings
import kotlinx.coroutines.flow.Flow

/**
 * DAO für Jahr-Einstellungen
 */
@Dao
interface YearSettingsDao {

    /**
     * Fügt ein neues Jahr hinzu oder aktualisiert es
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(yearSettings: YearSettings)

    /**
     * Aktualisiert ein Jahr
     */
    @Update
    suspend fun update(yearSettings: YearSettings)

    /**
     * Löscht ein Jahr
     */
    @Delete
    suspend fun delete(yearSettings: YearSettings)

    /**
     * Holt ein Jahr anhand der Jahreszahl
     */
    @Query("SELECT * FROM year_settings WHERE year = :year")
    suspend fun getYear(year: Int): YearSettings?

    /**
     * Holt ein Jahr als Flow (für Live-Updates)
     */
    @Query("SELECT * FROM year_settings WHERE year = :year")
    fun getYearFlow(year: Int): Flow<YearSettings?>

    /**
     * Holt das aktuell aktive Jahr
     */
    @Query("SELECT * FROM year_settings WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveYear(): YearSettings?

    /**
     * Holt das aktuell aktive Jahr als Flow
     */
    @Query("SELECT * FROM year_settings WHERE isActive = 1 LIMIT 1")
    fun getActiveYearFlow(): Flow<YearSettings?>

    /**
     * Holt alle Jahre (sortiert nach Jahr absteigend)
     */
    @Query("SELECT * FROM year_settings ORDER BY year DESC")
    suspend fun getAllYears(): List<YearSettings>

    /**
     * Holt alle Jahre als Flow
     */
    @Query("SELECT * FROM year_settings ORDER BY year DESC")
    fun getAllYearsFlow(): Flow<List<YearSettings>>

    /**
     * Holt nur die Jahreszahlen (für Dropdown)
     */
    @Query("SELECT year FROM year_settings ORDER BY year DESC")
    suspend fun getYearNumbers(): List<Int>

    /**
     * Setzt ein Jahr als aktiv (deaktiviert alle anderen)
     */
    @Transaction
    suspend fun setActiveYear(year: Int) {
        // Deaktiviere alle Jahre
        deactivateAllYears()
        // Aktiviere das gewünschte Jahr
        activateYear(year)
    }

    @Query("UPDATE year_settings SET isActive = 0")
    suspend fun deactivateAllYears()

    @Query("UPDATE year_settings SET isActive = 1 WHERE year = :year")
    suspend fun activateYear(year: Int)

    /**
     * Prüft ob ein Jahr existiert
     */
    @Query("SELECT EXISTS(SELECT 1 FROM year_settings WHERE year = :year)")
    suspend fun yearExists(year: Int): Boolean

    /**
     * Aktualisiert die Excel-Vorlage-Flags für ein Jahr
     */
    @Query("UPDATE year_settings SET hasExcelTemplate = :hasTemplate WHERE year = :year")
    suspend fun updateExcelTemplateFlag(year: Int, hasTemplate: Boolean)

    /**
     * Aktualisiert den Vorjahresübertrag für ein Jahr
     */
    @Query("UPDATE year_settings SET vorjahresUebertragMinuten = :minutes WHERE year = :year")
    suspend fun updateVorjahresUebertrag(year: Int, minutes: Int)

    /**
     * Holt das vorherige Jahr (für Übertrag-Berechnung)
     */
    @Query("SELECT * FROM year_settings WHERE year = :year - 1")
    suspend fun getPreviousYear(year: Int): YearSettings?

    /**
     * Archiviert ein Jahr
     */
    @Query("UPDATE year_settings SET isArchived = 1 WHERE year = :year")
    suspend fun archiveYear(year: Int)

    /**
     * Holt nicht-archivierte Jahre
     */
    @Query("SELECT * FROM year_settings WHERE isArchived = 0 ORDER BY year DESC")
    fun getActiveYearsFlow(): Flow<List<YearSettings>>
}
