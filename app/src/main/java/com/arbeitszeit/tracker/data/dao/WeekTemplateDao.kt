package com.arbeitszeit.tracker.data.dao

import androidx.room.*
import com.arbeitszeit.tracker.data.entity.WeekTemplate
import com.arbeitszeit.tracker.data.entity.WeekTemplateEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekTemplateDao {

    // WeekTemplate CRUD
    @Query("SELECT * FROM week_templates ORDER BY createdAt DESC")
    fun getAllTemplatesFlow(): Flow<List<WeekTemplate>>

    @Query("SELECT * FROM week_templates ORDER BY createdAt DESC")
    suspend fun getAllTemplates(): List<WeekTemplate>

    @Query("SELECT * FROM week_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): WeekTemplate?

    @Insert
    suspend fun insertTemplate(template: WeekTemplate): Long

    @Update
    suspend fun updateTemplate(template: WeekTemplate)

    @Delete
    suspend fun deleteTemplate(template: WeekTemplate)

    // WeekTemplateEntry CRUD
    @Query("SELECT * FROM week_template_entries WHERE templateId = :templateId ORDER BY dayOfWeek ASC")
    suspend fun getEntriesByTemplate(templateId: Long): List<WeekTemplateEntry>

    @Query("SELECT * FROM week_template_entries WHERE templateId = :templateId ORDER BY dayOfWeek ASC")
    fun getEntriesByTemplateFlow(templateId: Long): Flow<List<WeekTemplateEntry>>

    @Insert
    suspend fun insertEntry(entry: WeekTemplateEntry): Long

    @Insert
    suspend fun insertEntries(entries: List<WeekTemplateEntry>)

    @Update
    suspend fun updateEntry(entry: WeekTemplateEntry)

    @Delete
    suspend fun deleteEntry(entry: WeekTemplateEntry)

    @Query("DELETE FROM week_template_entries WHERE templateId = :templateId")
    suspend fun deleteEntriesByTemplate(templateId: Long)

    /**
     * Löscht Template inkl. aller Einträge (durch CASCADE)
     */
    @Query("DELETE FROM week_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
}
