package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class MonthSummary(
    val yearMonth: YearMonth,
    val sollMinutenGesamt: Int,
    val istMinutenGesamt: Int,
    val differenzMinuten: Int,
    val anzahlTage: Int
)

data class UeberstundenSummary(
    val gesamtUeberstunden: Int,          // Aktuelle Gesamtüberstunden inkl. Vorjahr
    val laufendesJahr: Int,                // Überstunden nur für laufendes Jahr
    val vorjahrUebertrag: Int,             // Übertrag aus Vorjahr
    val letzterUebertrag: Int,             // Letzter 4-Wochen-Übertrag
    val monatsSummen: List<MonthSummary>   // Aufschlüsselung nach Monaten
)

class UeberstundenViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val userSettingsDao = database.userSettingsDao()

    val userSettings: StateFlow<UserSettings?> = userSettingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val allEntries: StateFlow<List<TimeEntry>> = timeEntryDao.getAllEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ueberstundenSummary: StateFlow<UeberstundenSummary> = combine(
        allEntries,
        userSettings
    ) { entries, settings ->
        calculateUeberstundenSummary(entries, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        UeberstundenSummary(0, 0, 0, 0, emptyList())
    )

    private fun calculateUeberstundenSummary(
        entries: List<TimeEntry>,
        settings: UserSettings?
    ): UeberstundenSummary {
        if (settings == null) {
            return UeberstundenSummary(0, 0, 0, 0, emptyList())
        }

        // Gruppiere Einträge nach Monat
        val entriesByMonth = entries
            .sortedByDescending { it.datum }
            .groupBy { entry ->
                val date = LocalDate.parse(entry.datum)
                YearMonth.of(date.year, date.month)
            }

        // Berechne Monats-Summen
        val monatsSummen = entriesByMonth.map { (yearMonth, monthEntries) ->
            val sollGesamt = monthEntries.sumOf { it.sollMinuten }
            val istGesamt = monthEntries.sumOf { it.getIstMinuten() }
            val differenz = monthEntries.sumOf { it.getDifferenzMinuten() }

            MonthSummary(
                yearMonth = yearMonth,
                sollMinutenGesamt = sollGesamt,
                istMinutenGesamt = istGesamt,
                differenzMinuten = differenz,
                anzahlTage = monthEntries.size
            )
        }.sortedByDescending { it.yearMonth }

        // Berechne Gesamtüberstunden für laufendes Jahr
        val currentYear = LocalDate.now().year
        val laufendesJahrUeberstunden = entries
            .filter {
                val date = LocalDate.parse(it.datum)
                date.year == currentYear
            }
            .sumOf { it.getDifferenzMinuten() }

        // Gesamtüberstunden = laufendes Jahr + Vorjahresübertrag
        val gesamtUeberstunden = laufendesJahrUeberstunden + settings.ueberstundenVorjahrMinuten

        return UeberstundenSummary(
            gesamtUeberstunden = gesamtUeberstunden,
            laufendesJahr = laufendesJahrUeberstunden,
            vorjahrUebertrag = settings.ueberstundenVorjahrMinuten,
            letzterUebertrag = settings.letzterUebertragMinuten,
            monatsSummen = monatsSummen
        )
    }

    /**
     * Konvertiert Minuten in Stunden-String (z.B. "37:16" oder "-2:30")
     */
    fun minutesToHoursString(minutes: Int): String {
        val isNegative = minutes < 0
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val sign = if (isNegative) "-" else ""
        return String.format("%s%d:%02d", sign, hours, mins)
    }
}
