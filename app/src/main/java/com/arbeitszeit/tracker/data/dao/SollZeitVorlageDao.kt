package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.SollZeitVorlage
import kotlinx.coroutines.flow.Flow

@Dao
interface SollZeitVorlageDao {

    @Query("SELECT * FROM soll_zeit_vorlagen ORDER BY isDefault DESC, name ASC")
    fun getAllVorlagenFlow(): Flow<List<SollZeitVorlage>>

    @Query("SELECT * FROM soll_zeit_vorlagen ORDER BY isDefault DESC, name ASC")
    suspend fun getAllVorlagen(): List<SollZeitVorlage>

    @Query("SELECT * FROM soll_zeit_vorlagen WHERE id = :id")
    suspend fun getVorlageById(id: Long): SollZeitVorlage?

    @Query("SELECT * FROM soll_zeit_vorlagen WHERE id = :id")
    fun getVorlageByIdFlow(id: Long): Flow<SollZeitVorlage?>

    @Query("SELECT * FROM soll_zeit_vorlagen WHERE name = :name")
    suspend fun getVorlageByName(name: String): SollZeitVorlage?

    @Query("SELECT * FROM soll_zeit_vorlagen WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultVorlage(): SollZeitVorlage?

    @Query("SELECT * FROM soll_zeit_vorlagen WHERE isDefault = 1 LIMIT 1")
    fun getDefaultVorlageFlow(): Flow<SollZeitVorlage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vorlage: SollZeitVorlage): Long

    @Update
    suspend fun update(vorlage: SollZeitVorlage)

    @Delete
    suspend fun delete(vorlage: SollZeitVorlage)

    @Query("DELETE FROM soll_zeit_vorlagen WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Setzt eine Vorlage als Default und entfernt Default-Flag von allen anderen
     */
    @Transaction
    suspend fun setAsDefault(id: Long) {
        clearAllDefaults()
        setDefaultFlag(id, true)
    }

    @Query("UPDATE soll_zeit_vorlagen SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE soll_zeit_vorlagen SET isDefault = :isDefault WHERE id = :id")
    suspend fun setDefaultFlag(id: Long, isDefault: Boolean)

    @Query("SELECT COUNT(*) FROM soll_zeit_vorlagen")
    suspend fun getVorlagenCount(): Int
}
