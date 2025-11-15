package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.WeekTemplate
import com.arbeitszeit.tracker.data.entity.WeekTemplateEntry
import com.arbeitszeit.tracker.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeekTemplatesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val weekTemplateDao = database.weekTemplateDao()
    private val timeEntryDao = database.timeEntryDao()
    private val settingsDao = database.userSettingsDao()

    // Alle verfügbaren Vorlagen
    val allTemplates: StateFlow<List<WeekTemplate>> = weekTemplateDao.getAllTemplatesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Erstellt eine neue Vorlage aus einer bestehenden Woche
     */
    fun createTemplateFromWeek(name: String, description: String, weekStartDate: LocalDate) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings()
            val weekDays = DateUtils.getDaysOfWeek(weekStartDate)
            val startDate = DateUtils.dateToString(weekDays.first())
            val endDate = DateUtils.dateToString(weekDays.last())

            // Lade Einträge der Woche
            val weekEntries = timeEntryDao.getEntriesByDateRange(startDate, endDate)

            // Erstelle Template
            val template = WeekTemplate(
                name = name,
                description = description
            )
            val templateId = weekTemplateDao.insertTemplate(template)

            // Erstelle Template-Einträge aus Wochen-Einträgen
            val templateEntries = weekEntries.mapNotNull { entry ->
                val date = LocalDate.parse(entry.datum)
                val dayOfWeek = date.dayOfWeek.value // 1=Montag, 7=Sonntag

                // Nur Einträge mit Daten speichern
                if (entry.startZeit != null || entry.endZeit != null ||
                    entry.typ != "NORMAL" || entry.notiz.isNotEmpty()
                ) {
                    WeekTemplateEntry(
                        templateId = templateId,
                        dayOfWeek = dayOfWeek,
                        startZeit = entry.startZeit,
                        endZeit = entry.endZeit,
                        pauseMinuten = entry.pauseMinuten,
                        typ = entry.typ,
                        notiz = entry.notiz
                    )
                } else null
            }

            weekTemplateDao.insertEntries(templateEntries)
        }
    }

    /**
     * Erstellt eine neue Vorlage manuell mit eingegebenen Zeiten
     */
    fun createTemplateManually(
        name: String,
        description: String,
        dayEntries: Map<Int, DayTimeEntry>  // dayOfWeek -> TimeEntry
    ) {
        viewModelScope.launch {
            // Erstelle Template
            val template = WeekTemplate(
                name = name,
                description = description
            )
            val templateId = weekTemplateDao.insertTemplate(template)

            android.util.Log.d("WeekTemplates", "Creating template '$name' with ${dayEntries.size} days")
            dayEntries.forEach { (dayOfWeek, entry) ->
                android.util.Log.d("WeekTemplates", "Day $dayOfWeek: start=${entry.startTime}, end=${entry.endTime}, pause=${entry.pauseMinutes}")
            }

            // Erstelle Template-Einträge für jeden Tag mit Daten
            val templateEntries = dayEntries.mapNotNull { (dayOfWeek, dayEntry) ->
                if (dayEntry.startTime != null && dayEntry.endTime != null) {
                    WeekTemplateEntry(
                        templateId = templateId,
                        dayOfWeek = dayOfWeek,
                        startZeit = dayEntry.startTime,
                        endZeit = dayEntry.endTime,
                        pauseMinuten = dayEntry.pauseMinutes,
                        typ = "NORMAL",
                        notiz = ""
                    )
                } else null
            }

            android.util.Log.d("WeekTemplates", "Inserting ${templateEntries.size} template entries")

            weekTemplateDao.insertEntries(templateEntries)
        }
    }

    /**
     * Wendet eine Vorlage auf eine Woche an
     */
    fun applyTemplateToWeek(templateId: Long, weekStartDate: LocalDate) {
        viewModelScope.launch {
            val settings = settingsDao.getSettings()
            val weekDays = DateUtils.getDaysOfWeek(weekStartDate)

            // Lade Template-Einträge
            val templateEntries = weekTemplateDao.getEntriesByTemplate(templateId)

            android.util.Log.d("WeekTemplates", "Applying template $templateId, found ${templateEntries.size} entries")
            templateEntries.forEach { entry ->
                android.util.Log.d("WeekTemplates", "Template entry: dayOfWeek=${entry.dayOfWeek}, start=${entry.startZeit}, end=${entry.endZeit}")
            }

            // Wende Template auf jeden Tag an
            templateEntries.forEach { templateEntry ->
                // Finde den entsprechenden Tag in der Zielwoche
                val targetDate = weekDays.find { it.dayOfWeek.value == templateEntry.dayOfWeek }

                android.util.Log.d("WeekTemplates", "Processing dayOfWeek ${templateEntry.dayOfWeek}, targetDate=$targetDate")

                if (targetDate != null) {
                    val dateString = DateUtils.dateToString(targetDate)
                    val existingEntry = timeEntryDao.getEntryByDate(dateString)

                    if (existingEntry != null) {
                        // Update existing entry
                        timeEntryDao.update(existingEntry.copy(
                            startZeit = templateEntry.startZeit,
                            endZeit = templateEntry.endZeit,
                            pauseMinuten = templateEntry.pauseMinuten,
                            typ = templateEntry.typ,
                            notiz = templateEntry.notiz,
                            isManualEntry = true,
                            updatedAt = System.currentTimeMillis()
                        ))
                    } else {
                        // Erstelle neuen Eintrag aus Vorlage
                        val dayOfWeek = targetDate.dayOfWeek.value
                        val weekNumber = DateUtils.getCustomWeekOfYear(targetDate, settings?.ersterMontagImJahr)
                        val year = targetDate.year
                        val sollMinuten = settings?.getSollMinutenForDay(dayOfWeek)
                            ?: (settings?.wochenStundenMinuten?.div(settings.arbeitsTageProWoche) ?: 0)

                        timeEntryDao.insert(
                            com.arbeitszeit.tracker.data.entity.TimeEntry(
                                datum = dateString,
                                wochentag = DateUtils.getWeekdayShort(targetDate),
                                kalenderwoche = weekNumber,
                                jahr = year,
                                startZeit = templateEntry.startZeit,
                                endZeit = templateEntry.endZeit,
                                pauseMinuten = templateEntry.pauseMinuten,
                                sollMinuten = sollMinuten,
                                typ = templateEntry.typ,
                                notiz = templateEntry.notiz,
                                isManualEntry = true
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Löscht eine Vorlage
     */
    fun deleteTemplate(template: WeekTemplate) {
        viewModelScope.launch {
            weekTemplateDao.deleteTemplate(template)
        }
    }

    /**
     * Holt die Einträge einer Vorlage zur Vorschau
     */
    suspend fun getTemplateEntries(templateId: Long): List<WeekTemplateEntry> {
        return weekTemplateDao.getEntriesByTemplate(templateId)
    }
}

/**
 * Data class für manuelle Tages-Eingaben bei Vorlagen-Erstellung
 */
data class DayTimeEntry(
    val startTime: Int?,  // in Minuten seit Mitternacht
    val endTime: Int?,    // in Minuten seit Mitternacht
    val pauseMinutes: Int = 0
)
