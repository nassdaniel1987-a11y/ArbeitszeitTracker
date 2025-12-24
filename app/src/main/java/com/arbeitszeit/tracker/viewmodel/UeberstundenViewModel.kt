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

data class UrlaubsSummary(
    val urlaubsanspruch: Int,              // Jahresurlaub in Tagen
    val verbraucht: Int,                   // Bereits verbrauchte Urlaubstage
    val resturlaub: Int,                   // Verbleibender Urlaub
    val krankheitstage: Int                // Krankheitstage im Jahr (Info)
)

data class WeekData(
    val weekNumber: Int,                   // KW-Nummer
    val year: Int,                         // Jahr
    val differenzMinuten: Int              // Überstunden/Minderstunden dieser Woche
)

class UeberstundenViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val timeEntryDao = database.timeEntryDao()
    private val userSettingsDao = database.userSettingsDao()
    private val yearSettingsDao = database.yearSettingsDao()

    // YearBoundaryService für Custom-Year Filterung
    private val yearBoundaryService = com.arbeitszeit.tracker.year.YearBoundaryService(application)

    val userSettings: StateFlow<UserSettings?> = userSettingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val activeYear = yearSettingsDao.getActiveYearFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val allEntries: StateFlow<List<TimeEntry>> = timeEntryDao.getAllEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ueberstundenSummary: StateFlow<UeberstundenSummary> = combine(
        allEntries,
        userSettings,
        activeYear
    ) { entries, settings, year ->
        calculateUeberstundenSummary(entries, settings, year)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        UeberstundenSummary(0, 0, 0, 0, emptyList())
    )

    val urlaubsSummary: StateFlow<UrlaubsSummary> = combine(
        allEntries,
        userSettings,
        activeYear
    ) { entries, settings, year ->
        calculateUrlaubsSummary(entries, settings, year)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        UrlaubsSummary(30, 0, 30, 0)
    )

    val weeklyData: StateFlow<List<WeekData>> = allEntries.map { entries ->
        calculateWeeklyData(entries)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private fun calculateWeeklyData(entries: List<TimeEntry>): List<WeekData> {
        // Gruppiere nach KW und Jahr
        val weekGroups = entries
            .groupBy { "${it.jahr}-${it.kalenderwoche}" }
            .mapNotNull { (key, weekEntries) ->
                val year = weekEntries.firstOrNull()?.jahr ?: return@mapNotNull null
                val week = weekEntries.firstOrNull()?.kalenderwoche ?: return@mapNotNull null
                val differenz = weekEntries.sumOf { it.getDifferenzMinuten() }

                WeekData(
                    weekNumber = week,
                    year = year,
                    differenzMinuten = differenz
                )
            }
            .sortedWith(compareBy({ it.year }, { it.weekNumber }))

        // Nimm die letzten 12 Wochen
        return weekGroups.takeLast(12)
    }

    private fun calculateUeberstundenSummary(
        entries: List<TimeEntry>,
        settings: UserSettings?,
        yearSettings: com.arbeitszeit.tracker.data.entity.YearSettings?
    ): UeberstundenSummary {
        if (settings == null || yearSettings == null) {
            return UeberstundenSummary(0, 0, 0, 0, emptyList())
        }

        // Filtere Einträge für das aktive Jahr (basierend auf Custom-Jahr)
        // Nutze jahr-Feld (wurde bei Erstellung/Neuberechnung korrekt gesetzt)
        // UND nur bis heute (keine zukünftigen Einträge)
        val today = LocalDate.now()
        val activeYearEntries = entries.filter { entry ->
            val date = LocalDate.parse(entry.datum)
            entry.jahr == yearSettings.year && !date.isAfter(today)
        }

        // Gruppiere Einträge nach Monat
        val entriesByMonth = activeYearEntries
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

        // Berechne Gesamtüberstunden für aktives Jahr (Custom-Jahr)
        val laufendesJahrUeberstunden = activeYearEntries.sumOf { it.getDifferenzMinuten() }

        // Gesamtüberstunden = laufendes Jahr + Vorjahresübertrag (aus YearSettings!)
        val gesamtUeberstunden = laufendesJahrUeberstunden + yearSettings.vorjahresUebertragMinuten

        return UeberstundenSummary(
            gesamtUeberstunden = gesamtUeberstunden,
            laufendesJahr = laufendesJahrUeberstunden,
            vorjahrUebertrag = yearSettings.vorjahresUebertragMinuten,
            letzterUebertrag = settings.letzterUebertragMinuten,
            monatsSummen = monatsSummen
        )
    }

    private fun calculateUrlaubsSummary(
        entries: List<TimeEntry>,
        settings: UserSettings?,
        yearSettings: com.arbeitszeit.tracker.data.entity.YearSettings?
    ): UrlaubsSummary {
        if (settings == null || yearSettings == null) {
            return UrlaubsSummary(30, 0, 30, 0)
        }

        // Filtere Einträge für das aktive Jahr (basierend auf Custom-Jahr)
        // Nutze jahr-Feld (wurde bei Erstellung/Neuberechnung korrekt gesetzt)
        // UND nur bis heute (keine zukünftigen Einträge)
        val today = LocalDate.now()
        val currentYearEntries = entries.filter { entry ->
            val date = LocalDate.parse(entry.datum)
            entry.jahr == yearSettings.year && !date.isAfter(today)
        }

        // Zähle Urlaubstage (typ == TYP_URLAUB)
        val urlaubstage = currentYearEntries.count { it.typ == TimeEntry.TYP_URLAUB }

        // Zähle Krankheitstage (typ == TYP_KRANK)
        val krankheitstage = currentYearEntries.count { it.typ == TimeEntry.TYP_KRANK }

        // Urlaubsanspruch aus YearSettings (nicht mehr aus UserSettings!)
        val urlaubsanspruch = yearSettings.urlaubsanspruchTage
        val resturlaub = urlaubsanspruch - urlaubstage

        return UrlaubsSummary(
            urlaubsanspruch = urlaubsanspruch,
            verbraucht = urlaubstage,
            resturlaub = resturlaub,
            krankheitstage = krankheitstage
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
