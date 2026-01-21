package com.arbeitszeit.tracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arbeitszeit.tracker.data.dao.ClosingDayDao
import com.arbeitszeit.tracker.data.dao.SchoolHolidayDao
import com.arbeitszeit.tracker.data.dao.SollZeitVorlageDao
import com.arbeitszeit.tracker.data.dao.TimeEntryDao
import com.arbeitszeit.tracker.data.dao.UserSettingsDao
import com.arbeitszeit.tracker.data.dao.WeekTemplateDao
import com.arbeitszeit.tracker.data.dao.WorkLocationDao
import com.arbeitszeit.tracker.data.dao.YearSettingsDao
import com.arbeitszeit.tracker.data.entity.ClosingDay
import com.arbeitszeit.tracker.data.entity.SchoolHoliday
import com.arbeitszeit.tracker.data.entity.SollZeitVorlage
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings
import com.arbeitszeit.tracker.data.entity.WeekTemplate
import com.arbeitszeit.tracker.data.entity.WeekTemplateEntry
import com.arbeitszeit.tracker.data.entity.WorkLocation
import com.arbeitszeit.tracker.data.entity.YearSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Database(
    entities = [
        UserSettings::class,
        TimeEntry::class,
        WorkLocation::class,
        WeekTemplate::class,
        WeekTemplateEntry::class,
        SollZeitVorlage::class,
        ClosingDay::class,
        SchoolHoliday::class,
        YearSettings::class
    ],
    version = 24,  // v24: Auto-Start Zeiten in UserSettings (statt SollZeitVorlage)
    exportSchema = true  // Schema-Export aktiviert → Dokumentation in app/schemas/
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun workLocationDao(): WorkLocationDao
    abstract fun weekTemplateDao(): WeekTemplateDao
    abstract fun sollZeitVorlageDao(): SollZeitVorlageDao
    abstract fun closingDayDao(): ClosingDayDao
    abstract fun schoolHolidayDao(): SchoolHolidayDao
    abstract fun yearSettingsDao(): YearSettingsDao

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
                    // Migrations für zukünftige Versionen
                    .addMigrations(*DatabaseMigrations.getAllMigrations())
                    // Fallback NUR für alte Versionen (< 16) - ab v16 keine Datenverluste mehr!
                    // Falls jemand von einer sehr alten Version upgraded, wird die DB neu erstellt
                    .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
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
                        populateDatabase(
                            database.userSettingsDao(),
                            database.timeEntryDao(),
                            database.sollZeitVorlageDao()
                        )
                    }
                }
            }
            
            suspend fun populateDatabase(
                settingsDao: UserSettingsDao,
                entryDao: TimeEntryDao,
                vorlageDao: SollZeitVorlageDao
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

                // Standard-Vorlage erstellen (Mo-Fr 8h, Sa/So 0h)
                val defaultVorlage = SollZeitVorlage(
                    name = "Normal",
                    montagSollMinuten = 8 * 60,     // 8:00
                    dienstagSollMinuten = 8 * 60,   // 8:00
                    mittwochSollMinuten = 8 * 60,   // 8:00
                    donnerstagSollMinuten = 8 * 60, // 8:00
                    freitagSollMinuten = 8 * 60,    // 8:00
                    samstagSollMinuten = 0,
                    sonntagSollMinuten = 0,
                    isDefault = true
                )
                vorlageDao.insert(defaultVorlage)
                
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
                        sollZeitVorlageName = "Normal", // Verwende Standard-Vorlage
                        typ = TimeEntry.TYP_NORMAL
                    )
                    
                    entryDao.insert(entry)
                }
            }
        }
    }
}
