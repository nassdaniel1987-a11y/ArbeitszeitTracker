package com.arbeitszeit.tracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arbeitszeit.tracker.data.dao.TimeEntryDao
import com.arbeitszeit.tracker.data.dao.UserSettingsDao
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Database(
    entities = [UserSettings::class, TimeEntry::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun timeEntryDao(): TimeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arbeitszeit_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration() // Für Entwicklung: DB wird bei Schema-Änderung neu erstellt
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.userSettingsDao(), database.timeEntryDao())
                    }
                }
            }
            
            suspend fun populateDatabase(
                settingsDao: UserSettingsDao,
                entryDao: TimeEntryDao
            ) {
                // Default Settings erstellen
                val defaultSettings = UserSettings(
                    id = 1,
                    name = "",
                    einrichtung = "",
                    arbeitsumfangProzent = 100,
                    wochenStundenMinuten = 40 * 60, // 40:00
                    arbeitsTageProWoche = 5,
                    ferienbetreuung = false,
                    ueberstundenVorjahrMinuten = 0,
                    letzterUebertragMinuten = 0
                )
                settingsDao.insertOrUpdate(defaultSettings)
                
                // Aktuelle Woche mit leeren Einträgen vorausfüllen
                val today = LocalDate.now()
                val weekField = WeekFields.of(Locale.GERMANY)
                val currentWeek = today.get(weekField.weekOfWeekBasedYear())
                val currentYear = today.get(weekField.weekBasedYear())
                
                // Montag der aktuellen Woche finden
                val monday = today.with(weekField.dayOfWeek(), 1)
                
                for (i in 0..6) {
                    val date = monday.plusDays(i.toLong())
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val wochentag = when (date.dayOfWeek.value) {
                        1 -> "Mo"
                        2 -> "Di"
                        3 -> "Mi"
                        4 -> "Do"
                        5 -> "Fr"
                        6 -> "Sa"
                        7 -> "So"
                        else -> ""
                    }
                    
                    // Soll-Stunden: Werktage (Mo-Fr) bekommen Sollzeit
                    val sollMinuten = if (date.dayOfWeek.value in 1..5) {
                        // 40h / 5 Tage = 8h = 480 Minuten
                        480
                    } else {
                        0
                    }
                    
                    val entry = TimeEntry(
                        datum = date.format(formatter),
                        wochentag = wochentag,
                        kalenderwoche = currentWeek,
                        jahr = currentYear,
                        startZeit = null,
                        endZeit = null,
                        pauseMinuten = 0,
                        sollMinuten = sollMinuten,
                        typ = TimeEntry.TYP_NORMAL
                    )
                    
                    entryDao.insert(entry)
                }
            }
        }
    }
}
