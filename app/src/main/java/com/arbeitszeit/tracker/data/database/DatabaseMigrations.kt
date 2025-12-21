package com.arbeitszeit.tracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Zentrale Migrations-Klasse für die App-Datenbank
 *
 * Version History:
 * - v1-15: Legacy versions (fallback to destructive migration)
 * - v16: Baseline (spalteP, spalteQ, spalteR hinzugefügt)
 * - v17: ClosingDay-Tabelle für Schließtage der Einrichtung
 * - v18: SchoolHoliday-Tabelle für Schulferien
 * - v19: YearSettings-Tabelle für Jahres-Management-System
 * - v20+: Future migrations with proper schema preservation
 *
 * WICHTIG: Ab v16 werden ALLE Migrations hier dokumentiert und implementiert!
 */
object DatabaseMigrations {

    /**
     * Migration v16 -> v17: ClosingDay-Tabelle hinzufügen
     *
     * Fügt eine neue Tabelle für die Schließtage der Einrichtung hinzu.
     * Diese werden für die KI-gestützte Urlaubsplanung verwendet.
     */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ClosingDay-Tabelle erstellen
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS closing_days (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    note TEXT,
                    color TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)

            // Index für schnellere Abfragen nach Jahr
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_closing_days_year
                ON closing_days(year)
            """)

            // Index für schnellere Datumsbereich-Abfragen
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_closing_days_dates
                ON closing_days(startDate, endDate)
            """)
        }
    }

    /**
     * Migration v17 -> v18: SchoolHoliday-Tabelle hinzufügen
     *
     * Fügt eine neue Tabelle für Schulferien hinzu.
     * Diese werden für die KI-gestützte Urlaubsplanung verwendet.
     */
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SchoolHoliday-Tabelle erstellen
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS school_holidays (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bundesland TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    source TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)

            // Index für schnellere Abfragen nach Bundesland und Jahr
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_school_holidays_bundesland_year
                ON school_holidays(bundesland, year)
            """)

            // Index für schnellere Datumsbereich-Abfragen
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_school_holidays_dates
                ON school_holidays(startDate, endDate)
            """)
        }
    }

    /**
     * Migration v18 -> v19: YearSettings-Tabelle für Jahres-Management-System
     *
     * Diese Migration führt das neue Jahres-Management-System ein:
     * - Jedes Jahr hat eigene Einstellungen (erster Montag, Urlaubsanspruch, Vorjahresübertrag)
     * - Automatische Erkennung aller Jahre aus vorhandenen TimeEntries
     * - Intelligente Übertragung der aktuellen Einstellungen
     * - Automatische Berechnung von Vorjahresüberträgen
     *
     * Vorteile:
     * - Konsistente KW/Überstunden/Urlaub-Berechnung pro Jahr
     * - Automatischer Überstunden-Übertrag (kein manuelles Eintragen mehr)
     * - Jahr-spezifische Excel-Vorlagen
     * - Klare Trennung zwischen Jahren beim Jahreswechsel
     */
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. YearSettings-Tabelle erstellen
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS year_settings (
                    year INTEGER PRIMARY KEY NOT NULL,
                    ersterMontagImJahr TEXT NOT NULL,
                    urlaubsanspruchTage INTEGER NOT NULL,
                    vorjahresUebertragMinuten INTEGER NOT NULL,
                    hasExcelTemplate INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isArchived INTEGER NOT NULL
                )
            """)

            // 2. Index für aktives Jahr (für schnelle Abfragen)
            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_year_settings_active
                ON year_settings(isActive)
            """)

            // 3. Finde alle Jahre aus vorhandenen TimeEntries
            val yearsCursor = db.query("SELECT DISTINCT jahr FROM time_entries ORDER BY jahr")
            val years = mutableListOf<Int>()
            while (yearsCursor.moveToNext()) {
                years.add(yearsCursor.getInt(0))
            }
            yearsCursor.close()

            // 4. Lade aktuelle Einstellungen aus UserSettings
            val settingsCursor = db.query("SELECT ersterMontagImJahr, urlaubsanspruchTage, ueberstundenVorjahrMinuten FROM user_settings LIMIT 1")
            var currentErsterMontag: String? = null
            var currentUrlaubsanspruch = 30  // Default
            var currentVorjahresUebertrag = 0

            if (settingsCursor.moveToFirst()) {
                currentErsterMontag = settingsCursor.getString(0)
                currentUrlaubsanspruch = settingsCursor.getInt(1)
                currentVorjahresUebertrag = settingsCursor.getInt(2)
            }
            settingsCursor.close()

            // 5. Erstelle YearSettings für jedes gefundene Jahr
            val currentYear = java.time.LocalDate.now().year
            years.forEach { year ->
                // Finde erster Montag für dieses Jahr
                val ersterMontag = if (year == currentYear && currentErsterMontag != null) {
                    // Aktuelles Jahr: Nutze Einstellung aus UserSettings
                    currentErsterMontag
                } else {
                    // Vergangene/zukünftige Jahre: Berechne ersten Montag
                    findFirstMonday(year)
                }

                // Berechne Vorjahresübertrag (aus Differenz des Vorjahres)
                val vorjahresUebertrag = if (year == currentYear) {
                    // Aktuelles Jahr: Nutze aus UserSettings
                    currentVorjahresUebertrag
                } else {
                    // Andere Jahre: Berechne aus Vorjahr
                    calculateYearCarryOver(db, year - 1)
                }

                // Prüfe ob Excel-Vorlage existiert (template_JAHR.xlsx)
                // Hinweis: Wird später vom TemplateManager korrekt gesetzt
                val hasTemplate = 0  // false, wird später aktualisiert

                // Aktuelles Jahr = aktiv
                val isActive = if (year == currentYear) 1 else 0

                // YearSettings einfügen
                db.execSQL("""
                    INSERT INTO year_settings
                    (year, ersterMontagImJahr, urlaubsanspruchTage, vorjahresUebertragMinuten,
                     hasExcelTemplate, isActive, createdAt, isArchived)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                """, arrayOf(
                    year,
                    ersterMontag,
                    currentUrlaubsanspruch,
                    vorjahresUebertrag,
                    hasTemplate,
                    isActive,
                    System.currentTimeMillis()
                ))
            }

            // 6. Falls keine Jahre gefunden wurden, erstelle aktuelles Jahr
            if (years.isEmpty()) {
                val ersterMontag = currentErsterMontag ?: findFirstMonday(currentYear)
                db.execSQL("""
                    INSERT INTO year_settings
                    (year, ersterMontagImJahr, urlaubsanspruchTage, vorjahresUebertragMinuten,
                     hasExcelTemplate, isActive, createdAt, isArchived)
                    VALUES (?, ?, ?, ?, 0, 1, ?, 0)
                """, arrayOf(
                    currentYear,
                    ersterMontag,
                    currentUrlaubsanspruch,
                    currentVorjahresUebertrag,
                    System.currentTimeMillis()
                ))
            }
        }

        /**
         * Findet den ersten Montag eines Jahres
         */
        private fun findFirstMonday(year: Int): String {
            var date = java.time.LocalDate.of(year, 1, 1)
            while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                date = date.plusDays(1)
            }
            return date.toString()  // yyyy-MM-dd
        }

        /**
         * Berechnet den Überstunden-Übertrag eines Jahres
         * (Summe aller Differenzen = Soll - Ist)
         */
        private fun calculateYearCarryOver(db: SupportSQLiteDatabase, year: Int): Int {
            val cursor = db.query("""
                SELECT
                    COALESCE(SUM(sollMinuten), 0) as totalSoll,
                    COALESCE(SUM(
                        CASE
                            WHEN startZeit IS NOT NULL AND endZeit IS NOT NULL
                            THEN (strftime('%s', endZeit) - strftime('%s', startZeit)) / 60 - pauseMinuten
                            ELSE 0
                        END
                    ), 0) as totalIst
                FROM time_entries
                WHERE jahr = ?
            """, arrayOf(year))

            var carryOver = 0
            if (cursor.moveToFirst()) {
                val soll = cursor.getInt(0)
                val ist = cursor.getInt(1)
                carryOver = ist - soll  // Positive = Plus-Stunden, Negative = Minus-Stunden
            }
            cursor.close()

            return carryOver
        }
    }

    /**
     * Migration v19 -> v20: Auto-Switch für Jahreswechsel
     *
     * Fügt ein neues Feld in UserSettings hinzu:
     * - autoSwitchYear: Automatischer Wechsel zum neuen Jahr am ersten Montag
     *
     * Standardmäßig aktiviert (true), kann in den Einstellungen deaktiviert werden.
     */
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // autoSwitchYear-Spalte zu user_settings hinzufügen
            // DEFAULT 1 = true (Auto-Switch standardmäßig aktiviert)
            db.execSQL("""
                ALTER TABLE user_settings
                ADD COLUMN autoSwitchYear INTEGER NOT NULL DEFAULT 1
            """)
        }
    }

    /**
     * Migration v20 -> v21: Auto-Start für Arbeitszeit
     *
     * Fügt neue Felder in UserSettings hinzu:
     * - autoStartEnabled: Auto-Start basierend auf Wochenvorlagen
     * - autoStartRequiresGeofencing: Auto-Start nur mit Geofencing
     * - autoStartReminderMinutes: Vor-Erinnerung in Minuten
     * - autoStartDefaultPauseMinutes: Standard-Pausenzeit
     *
     * Auto-Start standardmäßig deaktiviert (false), kann aktiviert werden.
     */
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Auto-Start Felder zu user_settings hinzufügen
            db.execSQL("""
                ALTER TABLE user_settings
                ADD COLUMN autoStartEnabled INTEGER NOT NULL DEFAULT 0
            """)

            db.execSQL("""
                ALTER TABLE user_settings
                ADD COLUMN autoStartRequiresGeofencing INTEGER NOT NULL DEFAULT 1
            """)

            db.execSQL("""
                ALTER TABLE user_settings
                ADD COLUMN autoStartReminderMinutes INTEGER NOT NULL DEFAULT 5
            """)

            db.execSQL("""
                ALTER TABLE user_settings
                ADD COLUMN autoStartDefaultPauseMinutes INTEGER NOT NULL DEFAULT 30
            """)
        }
    }

    /**
     * Migration v21 -> v22: Start-Zeiten zu SollZeitVorlage hinzufügen
     *
     * Erweitert die soll_zeit_vorlagen Tabelle um Start-Zeiten pro Wochentag
     * für die Auto-Start Funktion der Arbeitszeiterfassung.
     *
     * Start-Zeiten sind optional (nullable) und werden in Minuten seit Mitternacht gespeichert.
     */
    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Start-Zeiten für jeden Wochentag hinzufügen
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN montagStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN dienstagStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN mittwochStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN donnerstagStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN freitagStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN samstagStartZeit INTEGER")
            db.execSQL("ALTER TABLE soll_zeit_vorlagen ADD COLUMN sonntagStartZeit INTEGER")
        }
    }

    /**
     * Gibt alle verfügbaren Migrations zurück
     *
     * Wenn neue Migrations hinzugefügt werden, hier in der Liste eintragen!
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            // Zukünftige Migrations hier hinzufügen:
            // MIGRATION_22_23,
            // etc.
        )
    }
}
