package com.arbeitszeit.tracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Zentrale Migrations-Klasse für die App-Datenbank
 *
 * Version History:
 * - v1-15: Legacy versions (fallback to destructive migration)
 * - v16: Current baseline (spalteP, spalteQ, spalteR hinzugefügt)
 * - v17+: Future migrations with proper schema preservation
 *
 * WICHTIG: Ab v16 werden ALLE Migrations hier dokumentiert und implementiert!
 */
object DatabaseMigrations {

    /**
     * Beispiel-Migration v16 -> v17
     *
     * Diese Migration ist ein Template für zukünftige Schema-Änderungen.
     * Wenn du neue Felder zu einer Entity hinzufügst:
     *
     * 1. Erhöhe die Database-Version in AppDatabase.kt
     * 2. Erstelle eine neue Migration hier
     * 3. Füge die Migration in AppDatabase.kt hinzu
     * 4. Das Schema wird automatisch nach app/schemas/ exportiert
     *
     * Beispiel: Neues Feld zu TimeEntry hinzufügen:
     * ```
     * val MIGRATION_16_17 = object : Migration(16, 17) {
     *     override fun migrate(database: SupportSQLiteDatabase) {
     *         // Neues Feld hinzufügen
     *         database.execSQL(
     *             "ALTER TABLE time_entries ADD COLUMN neuesFeld TEXT NOT NULL DEFAULT ''"
     *         )
     *     }
     * }
     * ```
     */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Beispiel-Migration (aktuell leer, da v17 noch nicht existiert)
            // Wenn v17 kommt, hier die SQL-Statements einfügen
        }
    }

    /**
     * Gibt alle verfügbaren Migrations zurück
     *
     * Wenn neue Migrations hinzugefügt werden, hier in der Liste eintragen!
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            // MIGRATION_16_17,  // Auskommentiert, da v17 noch nicht existiert
            // Zukünftige Migrations hier hinzufügen:
            // MIGRATION_17_18,
            // MIGRATION_18_19,
            // etc.
        )
    }
}
