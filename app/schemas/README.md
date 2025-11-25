# Database Schemas

Dieses Verzeichnis enthält automatisch generierte Database-Schemas von Room.

## Was sind Schemas?

Room exportiert bei jedem Build ein JSON-Schema der Datenbank-Struktur. Diese Schemas dokumentieren:
- Alle Tabellen und ihre Spalten
- Datentypen
- Primary Keys, Foreign Keys, Indizes
- Schema-Versionshistorie

## Wie füge ich eine neue Migration hinzu?

### 1. Entity ändern

Beispiel: Neues Feld zu `TimeEntry` hinzufügen:

```kotlin
@Entity(tableName = "time_entries")
data class TimeEntry(
    // Bestehende Felder...
    val neuesFeld: String = "",  // NEU!
    // ...
)
```

### 2. Database-Version erhöhen

In `AppDatabase.kt`:

```kotlin
@Database(
    entities = [...],
    version = 17,  // War 16, jetzt 17!
    exportSchema = true
)
```

### 3. Migration erstellen

In `DatabaseMigrations.kt`:

```kotlin
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL-Statement für die Änderung
        database.execSQL(
            "ALTER TABLE time_entries ADD COLUMN neuesFeld TEXT NOT NULL DEFAULT ''"
        )
    }
}
```

### 4. Migration registrieren

In `DatabaseMigrations.kt` in der `getAllMigrations()` Funktion:

```kotlin
fun getAllMigrations(): Array<Migration> {
    return arrayOf(
        MIGRATION_16_17,  // NEU!
        // Zukünftige Migrations...
    )
}
```

### 5. Testen

- App installieren
- Daten eingeben
- App updaten (neue Version)
- Prüfen: Daten noch da? ✅

## Typische Migration-Beispiele

### Spalte hinzufügen (nullable)
```kotlin
database.execSQL("ALTER TABLE time_entries ADD COLUMN neuesFeld TEXT")
```

### Spalte hinzufügen (NOT NULL mit Default)
```kotlin
database.execSQL(
    "ALTER TABLE time_entries ADD COLUMN neuesFeld TEXT NOT NULL DEFAULT ''"
)
```

### Tabelle erstellen
```kotlin
database.execSQL("""
    CREATE TABLE neue_tabelle (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )
""")
```

### Index erstellen
```kotlin
database.execSQL(
    "CREATE INDEX index_time_entries_datum ON time_entries(datum)"
)
```

## WICHTIG

- **NIEMALS** eine alte Migration ändern!
- **IMMER** eine neue Migration für Änderungen erstellen
- **IMMER** Migrations testen bevor du sie releast
- **Schema-JSONs** im Git commiten (Dokumentation!)

## Mehr Infos

- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Testing Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
