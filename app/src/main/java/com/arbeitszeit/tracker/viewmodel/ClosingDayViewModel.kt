package com.arbeitszeit.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.data.entity.ClosingDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ClosingDayViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val closingDayDao = database.closingDayDao()

    /**
     * Alle Schließtage als Flow
     */
    val allClosingDays: StateFlow<List<ClosingDay>> = closingDayDao.getAllClosingDaysFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Schließtage für das aktuelle Jahr
     */
    fun getClosingDaysForYear(year: Int): StateFlow<List<ClosingDay>> {
        return closingDayDao.getClosingDaysByYearFlow(year)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    /**
     * Erstellt einen neuen Schließtag
     */
    fun addClosingDay(
        title: String,
        startDate: String,
        endDate: String,
        note: String? = null,
        color: String? = null
    ) {
        viewModelScope.launch {
            // Jahr aus Startdatum extrahieren
            val year = LocalDate.parse(startDate).year

            val closingDay = ClosingDay(
                title = title,
                startDate = startDate,
                endDate = endDate,
                year = year,
                note = note,
                color = color
            )

            closingDayDao.insert(closingDay)
        }
    }

    /**
     * Aktualisiert einen bestehenden Schließtag
     */
    fun updateClosingDay(closingDay: ClosingDay) {
        viewModelScope.launch {
            // Jahr aus Startdatum extrahieren (falls geändert)
            val year = LocalDate.parse(closingDay.startDate).year

            closingDayDao.update(closingDay.copy(
                year = year,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Löscht einen Schließtag
     */
    fun deleteClosingDay(closingDay: ClosingDay) {
        viewModelScope.launch {
            closingDayDao.delete(closingDay)
        }
    }

    /**
     * Prüft ob ein Datum ein Schließtag ist
     */
    suspend fun isClosingDay(date: String): Boolean {
        val closingDays = closingDayDao.getClosingDaysByDate(date)
        return closingDays.isNotEmpty()
    }

    /**
     * Gibt alle Schließtage zurück, die ein bestimmtes Datum enthalten
     */
    suspend fun getClosingDaysForDate(date: String): List<ClosingDay> {
        return closingDayDao.getClosingDaysByDate(date)
    }

    /**
     * Gibt die Anzahl der Schließtage in einem Jahr zurück
     */
    suspend fun getClosingDaysCount(year: Int): Int {
        return closingDayDao.getCountByYear(year)
    }

    /**
     * Löscht alle Schließtage eines Jahres
     */
    fun deleteYearClosingDays(year: Int) {
        viewModelScope.launch {
            closingDayDao.deleteByYear(year)
        }
    }
}
