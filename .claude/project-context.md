# 🤖 ArbeitszeitTracker - Projekt-Kontext für KI-Assistenten

Diese Datei enthält wichtige Regeln und Richtlinien für die Entwicklung der App.
**Bitte bei jeder neuen KI-Sitzung beachten!**

---

## 📋 WICHTIGE ENTWICKLUNGSREGELN

### 1. 🗄️ Datenbank-Versionen & Migrationen

**Aktuelle Version: 22** (`AppDatabase.kt`)

#### ✅ Version MUSS erhöht werden bei:
- Neue Tabellen hinzufügen
- Spalten zu Tabellen hinzufügen/entfernen
- Tabellen umbenennen
- Datentypen ändern
- Indizes hinzufügen/ändern

#### ❌ Version NICHT erhöhen bei:
- UI-Änderungen (Screens, Composables, Themes)
- ViewModel-Logik ohne DB-Schema-Änderung
- Navigation-Änderungen
- Farb-/Design-Anpassungen
- Berechnungen die keine DB-Struktur ändern

#### 📜 Migration-Prozess (ab v16):
1. Version in `AppDatabase.kt` erhöhen
2. Migration-Objekt in `DatabaseMigrations.kt` erstellen
3. SQL-Migration dokumentieren
4. In `getAllMigrations()` registrieren
5. `exportSchema = true` ist aktiviert (Schema wird dokumentiert)

**Beispiel:**
```kotlin
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meine_tabelle ADD COLUMN neue_spalte TEXT")
    }
}
```

#### 🚫 Wichtig:
- **Ab v16:** KEINE destructive Migrations mehr!
- Alle Änderungen MÜSSEN in `DatabaseMigrations.kt` dokumentiert werden
- Version History aktuell halten

---

### 2. 📚 Dokumentation aktualisieren

#### Bei JEDEM neuen Feature:

**HelpScreen.kt MUSS aktualisiert werden!**
- Neue Features zur passenden Sektion hinzufügen
- Bestehende Beschreibungen bei Änderungen aktualisieren
- Screenshots/Icons wo sinnvoll

**Sections in HelpScreen.kt:**
- 🏠 Startseite & Schnellzugriff
- 📅 Kalenderansicht
- 🕐 Überstunden-Verwaltung
- 🏖️ Urlaubsplaner
- 📝 Wochen-Vorlagen
- ⚙️ Einstellungen
- 🗓️ Jahre verwalten
- 📤 Import/Export

---

### 3. 🏗️ Code-Stil & Architektur

#### Material 3 Design System:
- Alle neuen Screens mit Material 3 Komponenten
- `MaterialTheme.colorScheme` für Farben nutzen
- Dark Mode Support beachten
- Elevation sparsam einsetzen

#### Jetpack Compose Best Practices:
- State Hoisting korrekt anwenden
- ViewModel für Business Logic
- Repository Pattern für Datenzugriff
- Hilt für Dependency Injection

#### Navigation:
- Type-safe Routes mit Kotlin Serialization
- NavGraph in `NavGraph.kt` aktualisieren
- Deep Links wo sinnvoll

---

## 📝 CHANGELOG - Letzte Änderungen

**Bitte immer hier die letzten 5-10 Änderungen dokumentieren!**

### ✅ Dezember 2025

#### Setup Wizard Implementation
- **Feature:** 4-Schritte Setup Wizard für neue Nutzer
  - Welcome Screen
  - Template Upload (Excel) - optional
  - User Data Entry (Name, Einrichtung, Wochenstunden, etc.)
  - Completion Screen
- **Integration:** MainActivity.kt erkennt Setup-Bedarf automatisch
- **Fix:** LaunchedEffect entfernt (navigierte zu früh)
- **Fix:** Error Handling mit Step-Reset bei Fehlern
- **Dateien:**
  - `SetupScreen.kt`, `SetupViewModel.kt`
  - `MainActivity.kt` (Setup Detection)

#### Kalender Redesign - Android-Stil
- **Design:** Komplett überarbeiteter Kalender im Android/Google-Stil
- **Features:**
  - ✅ HorizontalPager für Swipe-Gesten zwischen Monaten
  - ✅ Heute-Button zum schnellen Zurückspringen
  - ✅ GROßE, gut lesbare Zellen
  - ✅ Event-Balken mit Labels (Urlaub, Krank, etc.)
  - ✅ Arbeitszeiten direkt sichtbar (8:00-16:00)
  - ✅ Differenz-Anzeige (+2h / -1h)
  - ✅ Legende am unteren Rand
- **Theme:** Perfekt lesbar in Light & Dark Mode
- **Dateien:**
  - `CalendarScreen.kt` (komplett neu)
  - `CalendarViewModel.kt` (setMonth() Methode)

#### Kalender-Legende
- **Feature:** Farbcode-Erklärung unter dem Kalender
- **Layout:** 2 Spalten mit farbigen Balken
- **Einträge:**
  - Grün ✓ = Vollständig
  - Orange ~ = Teilweise
  - Cyan U = Urlaub
  - Rot K = Krank
  - Indigo F = Feiertag (Typ)
  - Gold ⭐ = Feiertag (Balken)
- **Datei:** `CalendarScreen.kt`

---

## 🎯 Kommende Aufgaben / TODOs

<!-- Hier können zukünftige Aufgaben notiert werden -->

- [ ] Optional: Google Kalender Export-Feature
- [ ] Optional: Wochenansicht im Kalender
- [ ] Performance: Kalender-Rendering optimieren bei vielen Einträgen

---

## 📁 Wichtige Dateipfade

### Datenbank:
- `data/database/AppDatabase.kt` - Hauptdatenbank
- `data/database/DatabaseMigrations.kt` - Alle Migrationen ab v16
- `data/entity/*.kt` - Entity-Klassen
- `data/dao/*.kt` - DAOs

### UI:
- `ui/screens/*.kt` - Alle Screens
- `ui/components/*.kt` - Wiederverwendbare Komponenten
- `ui/theme/*.kt` - Theme, Farben, Typography

### Navigation:
- `ui/navigation/NavGraph.kt` - Hauptnavigation

### ViewModels:
- `viewmodel/*.kt` - Alle ViewModels

### Utils:
- `utils/TimeUtils.kt` - Zeit-Formatierung
- `utils/HolidayUtils.kt` - Feiertags-Logik
- `utils/DateUtils.kt` - Datums-Utilities

---

## ⚠️ Häufige Fehler vermeiden

### 1. Padding-Parameter in Compose:
```kotlin
// ❌ FALSCH:
.padding(horizontal = 16.dp, bottom = 12.dp)

// ✅ RICHTIG:
.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
```

### 2. Smart Cast bei delegated properties:
```kotlin
// ❌ FALSCH:
val settings by settingsFlow.collectAsState(initial = null)
if (settings != null) {
    settings.name // Smart cast error!
}

// ✅ RICHTIG:
val currentSettings = settings
if (currentSettings != null) {
    currentSettings.name // OK
}
```

### 3. LaunchedEffect für Navigation:
```kotlin
// ❌ FALSCH (bei State-Änderung):
LaunchedEffect(setupComplete) {
    if (setupComplete) onComplete()
}

// ✅ RICHTIG (nur bei User-Aktion):
Button(onClick = onComplete) { }
```

---

## 🔧 Build & Test

### Gradle:
```bash
./gradlew assembleDebug   # Debug-Build
./gradlew assembleRelease # Release-Build
./gradlew test            # Unit-Tests
```

### Wichtige Build-Configs:
- **minSdk:** 26 (Android 8.0)
- **targetSdk:** 35 (Android 15)
- **Kotlin:** 2.0+
- **Compose:** 1.7+

---

## 📞 Support & Feedback

Bei Problemen oder Fragen:
- GitHub Issues: https://github.com/anthropics/claude-code/issues (für Claude Code)
- App-spezifische Fragen: Im Code dokumentieren

---

**Zuletzt aktualisiert:** 25. Dezember 2025
**Projekt-Version:** v1.x (siehe build.gradle.kts für exakte Version)
**DB-Version:** 22
