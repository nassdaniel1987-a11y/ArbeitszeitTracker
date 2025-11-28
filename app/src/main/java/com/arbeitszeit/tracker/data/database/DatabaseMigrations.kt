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
 * - v19+: Future migrations with proper schema preservation
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
     * Gibt alle verfügbaren Migrations zurück
     *
     * Wenn neue Migrations hinzugefügt werden, hier in der Liste eintragen!
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_16_17,
            MIGRATION_17_18,
            // Zukünftige Migrations hier hinzufügen:
            // MIGRATION_18_19,
            // MIGRATION_19_20,
            // etc.
        )
    }
}
