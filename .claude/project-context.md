# 🤖 ArbeitszeitTracker - Projekt-Kontext für KI-Assistenten

Diese Datei enthält wichtige Regeln und Richtlinien für die Entwicklung der App.
**Bitte bei jeder neuen KI-Sitzung beachten!**

---

## 📋 WICHTIGE ENTWICKLUNGSREGELN

### 1. 🗄️ Datenbank-Versionen & Migrationen

**Aktuelle Version: 23** (`AppDatabase.kt`)

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

#### Standard-Vorlage Auto-Setup
- **Feature:** Excel-Vorlage wird jetzt automatisch gespeichert
- **Setup-Wizard:** Wenn User KEINE eigene Vorlage hochlädt, wird Standard-Vorlage aus `assets/ANZ_Template.xlsx` automatisch als Template gespeichert
- **Neues Jahr:** Beim Anlegen eines neuen Jahres wird automatisch Standard-Vorlage gespeichert (falls noch keine existiert)
- **Vorteil:**
  - Export funktioniert konsistent für alle Jahre
  - User muss sich nicht um Template-Upload kümmern
  - Standard-Vorlage wird mit Setup-Daten befüllt (Name, Einrichtung, etc.)
  - Für neue Jahre: Nur erster Montag ändern, Rest wird übernommen
- **Änderungen:**
  - `TemplateManager.kt`: Neue Methode `saveDefaultTemplateForYear()` kopiert Standard-Vorlage aus assets
  - `SetupViewModel.kt`: Speichert Standard-Vorlage wenn keine eigene hochgeladen
  - `YearManager.kt`: Speichert Standard-Vorlage beim Erstellen neuer Jahre
- **Dateien:** TemplateManager.kt:112, SetupViewModel.kt:399, YearManager.kt:113

#### KSP Ambiguous Getter Fix
- **Fix:** KSP Compilation Error für urlaubsJahr behoben
- **Problem:** Helper-Methode `getUrlaubsJahr()` konfliktierte mit Kotlins automatischem Property-Getter
- **Lösung:** Methode umbenannt zu `getEffectiveUrlaubsJahr()`
- **Semantic Improvement:** Neuer Name verdeutlicht dass das "effektive" Jahr zurückgegeben wird (urlaubsJahr ?? jahr)
- **Betroffene Dateien:**
  - TimeEntry.kt: Methode umbenannt
  - UeberstundenViewModel.kt: Method-Call aktualisiert
  - VacationPlannerViewModel.kt: Method-Call aktualisiert
- **Datei:** TimeEntry.kt:66, UeberstundenViewModel.kt:193, VacationPlannerViewModel.kt:99

#### Resturlaub-Unterstützung (DB v23)
- **Feature:** Urlaubstage können einem anderen Jahr zugeordnet werden
- **Use Case:** Resturlaub aus 2025 im Januar/Februar 2026 nehmen
- **Datenbank:**
  - Neues Feld: `TimeEntry.urlaubsJahr` (Integer, nullable)
  - Migration MIGRATION_22_23
  - Helper: `getEffectiveUrlaubsJahr()` (gibt urlaubsJahr oder jahr zurück)
- **UI:**
  - EditEntryDialog: Jahr-Auswahl bei Typ "Urlaub"
  - Optionen: "Aktuelles Jahr" oder "Vorjahr (Resturlaub)"
  - Nur sichtbar bei Typ = URLAUB
- **Logik:**
  - Urlaubsberechnung nutzt getEffectiveUrlaubsJahr()
  - Krankheitstage weiter nach Kalenderjahr
  - UeberstundenViewModel + VacationPlannerViewModel angepasst
- **Beispiel:**
  - Urlaub am 2.1.2026 mit urlaubsJahr=2025
  - Wird vom Kontingent 2025 abgezogen, nicht 2026!
- **Dateien:** TimeEntry.kt, DatabaseMigrations.kt, AppDatabase.kt, EditEntryDialog.kt, CalendarScreen.kt, CalendarViewModel.kt, UeberstundenViewModel.kt, VacationPlannerViewModel.kt

#### KI-Vorschlag Vollständig Anzeigbar
- **Fix:** KI-Antwort im Urlaubsplaner wurde abgeschnitten
- **Problem:** Text hatte kein Scrolling, lange Antworten nicht sichtbar
- **Lösung:**
  - AiSuggestionCard mit verticalScroll(rememberScrollState())
  - heightIn(max = 400.dp) für maximale Höhe
  - HorizontalDivider zwischen Header und Content
  - Elevation 2dp + shapes.large (modern)
  - Padding 20dp, Icon 24dp (konsistent)
- **Vorher:** Text abgeschnitten
- **Nachher:** Vollständig lesbar mit Scroll
- **Datei:** VacationPlannerScreen.kt

#### Screen-Header & Kalender Fixes
- **Fix:** Überstunden Screen Header kompakter gestaltet
  - TopAppBar: "Überstunden" → "Zeitkonto" (weniger redundant)
  - GesamtUeberstundenCard: Column → Row Layout (kompakter)
  - Icon 48dp → 32dp, displayLarge → displaySmall
  - Keine Dopplung mehr
- **Fix:** Daten Screen TopAppBar entfernt
  - Nur noch PrimaryTabRow (Export/Import)
  - Spart vertikalen Platz
- **Fix:** Kalender Scroll-Bug behoben
  - Problem: Sprang zwischen Monat/Jahr beim Scrollen
  - Lösung: Fester Referenzmonat (YearMonth.of(2020, 1))
  - Alle Berechnungen basieren auf Referenz + offset
  - Heute-Button korrigiert
- **Feature:** Kalender Optik modernisiert
  - Cards: 1dp elevation (statt 0dp), shapes.medium, Border für heute
  - Tag-Nummer: 32dp (statt 36dp), kompakter
  - Status-Balken: 16dp hoch, 90% Breite, Kurzlabels (U/K/F/A)
  - Moderneres, klareres Design
- **Dateien:** UeberstundenScreen.kt, DataManagementScreen.kt, CalendarScreen.kt

#### UI Modernisierung - Cards mit Elevation & Spacing
- **Feature:** Gesamte UI modernisiert für plastischeres, moderneres Design
- **Änderungen:**
  - **Card Elevation**: 2dp (Haupt-Cards) / 1dp (Listen-Cards) statt 0dp
  - **Hover-Effekte**: 4dp bzw. 3dp bei Hover für interaktives Feedback
  - **Konsistente Shapes**: large (24dp) für Haupt-Cards, medium (16dp) für Listen
  - **Optimiertes Spacing**: 18-24dp Padding statt 16dp, luftigeres Layout
- **Betroffene Components**: TimeEntryCard, WeekStatsCard, WeekEntryCard, WeekSummaryCard, GeofencingStatusCard, EmptyStateCard, DarkModeCard
- **Betroffene Screens**: SettingsScreen, ImportScreen, ExportScreen
- **Vorher**: Flaches Design ohne Schatten
- **Nachher**: Subtile Schatten + große Radien = Modern & Clean
- **Design-Philosophie**: Subtile Elevation für Tiefe, konsistente Shapes für Einheitlichkeit

#### Dynamic Color Lesbarkeits-Fix
- **Fix:** Material You Dynamic Color deaktiviert für bessere Lesbarkeit
- **Problem:** Wallpaper-basierte Farben sorgten für schlechte Kontraste (grauer Hintergrund mit dunklem Text)
- **Lösung:** Eigene optimierte Farbschemata verwenden
  - `dynamicColor: Boolean = false` als Default in Theme.kt
  - LightColorScheme und DarkColorScheme mit hohem Kontrast
- **Betroffene Screens:** Alle (besonders Urlaubsplaner, Settings, Kalender)
- **Datei:** `ui/theme/Theme.kt:71`

#### Widget-Ladeproblem behoben (goAsync Pattern)
- **Fix:** "Widget kann nicht geladen werden" Fehler in allen 6 Widgets behoben
- **Problem:** Async DB-Operationen wurden vor Abschluss unterbrochen
- **Lösung:** goAsync() Pattern implementiert
  - `onUpdate()` nutzt jetzt `goAsync()` + `pendingResult.finish()`
  - `updateAppWidget()` als `suspend fun` statt `runBlocking`
  - `refreshWidget()` verwendet CoroutineScope
  - Unnötiges `withContext(Dispatchers.Main)` entfernt
- **Betroffene Widgets:**
  - TimeStampWidget (2x2 - Hauptwidget)
  - TimeStampWidgetSmall (2x1 - Quick Stamp)
  - TimeStampWidgetLarge (4x2 - mit Wochenübersicht)
  - StatistikWidget (Heute, Woche, Überstunden)
  - LockScreenGlanceWidget (Sperrbildschirm)
  - LiveActivityWidget (Live-Anzeige)
- **Technischer Hintergrund:**
  - `goAsync()` verlängert Prozess-Lebenszeit in BroadcastReceiver
  - Ermöglicht Abschluss von async DB-Operationen
  - Wichtig für Room Database Zugriffe in Widgets
- **Dateien:** `app/src/main/java/com/arbeitszeit/tracker/widget/*.kt`

#### Projekt-Modernisierung - 5 Major Updates
- **Feature:** Gradle Version Catalog für zentrales Dependency Management
  - Datei: `gradle/libs.versions.toml`
  - Alle Dependencies migriert und in Bundles organisiert
  - Vereinfacht Updates und vermeidet Version-Konflikte
- **Feature:** Detekt Code Quality Tool
  - Static Code Analysis aktiviert
  - Konfiguration: `app/config/detekt/detekt.yml`
  - Android-spezifische Rules + Baseline
- **Feature:** Baseline Profiles für Performance
  - Automatische Generierung bei Release Builds
  - Reduziert App-Startzeit um 30-40%
- **Feature:** Edge-to-Edge Display (Android 15)
  - Transparente System Bars
  - Material 3 Dynamic Color (Material You)
  - Nutzt Wallpaper-Farben auf Android 12+
- **Feature:** Google Sign-In → Credential Manager Migration
  - NEUE Klasse: `CredentialAuthManager.kt`
  - Moderne Credential Manager API (Android 14+)
  - `CloudBackupSection.kt` migriert
  - `GoogleSignInManager.kt` als @Deprecated markiert
  - Migration Guide: `drive/MIGRATION_GUIDE.md`
- **Dateien:**
  - `gradle/libs.versions.toml` (NEU)
  - `app/build.gradle.kts` (komplett überarbeitet)
  - `app/config/detekt/detekt.yml` (NEU)
  - `ui/theme/Theme.kt` (Edge-to-Edge + Dynamic Color)
  - `drive/CredentialAuthManager.kt` (NEU)
  - `drive/MIGRATION_GUIDE.md` (NEU)

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
- **compileSdk:** 35 (Android 15)
- **Kotlin:** 2.2.20
- **KSP:** 2.2.20-2.0.4
- **Compose BOM:** 2025.11.01
- **AGP:** 8.7.3
- **Room:** 2.8.4
- **Hilt:** 2.57.1

### Code Quality Tools:
- **Detekt:** 1.23.7
  - Konfiguration: `app/config/detekt/detekt.yml`
  - Baseline: `app/config/detekt/baseline.xml`
- **Baseline Profile:** 1.3.1
  - Automatisch bei Release Builds

---

## 📞 Support & Feedback

Bei Problemen oder Fragen:
- GitHub Issues: https://github.com/anthropics/claude-code/issues (für Claude Code)
- App-spezifische Fragen: Im Code dokumentieren

---

**Zuletzt aktualisiert:** 28. Dezember 2025
**Projekt-Version:** v1.2 (versionCode 3)
**DB-Version:** 23
