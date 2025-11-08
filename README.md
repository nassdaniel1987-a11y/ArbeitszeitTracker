# 📱 Arbeitszeit Tracker - Android App

Eine native Android-App zur Arbeitszeiterfassung für Lehrer an Ganztagsschulen mit Excel-Export im vorgegebenen Format.

## 🎯 Features

- ✅ Täg liche Arbeitszeiterfassung (Start, Ende, Pause)
- ✅ Automatische Erinnerungen (Morgen, Abend, fehlende Einträge)
- ✅ Kalenderübersicht mit Statusanzeige
- ✅ Excel-Export im exakten Vorlagenformat
- ✅ Vollständig offline (keine Cloud, kein Internet nötig)
- ✅ Material 3 Design mit Dark Mode
- ✅ Typ-Kennzeichnung (Normal, Urlaub, Krank, Feiertag, Abwesend)

## 🛠️ Installation in Android Studio

### Schritt 1: Voraussetzungen

Stelle sicher, dass du folgendes installiert hast:
- **Android Studio** (Electric Eel oder neuer)
  Download: https://developer.android.com/studio
- **JDK 17** (wird normalerweise mit Android Studio installiert)

### Schritt 2: Projekt öffnen

1. **Android Studio starten**

2. **File → Open** wählen

3. Navigiere zum **ArbeitszeitTracker** Ordner und wähle ihn aus

4. Klicke auf **OK**

5. Android Studio wird das Projekt laden und Gradle synchronisieren
   - Dies kann beim ersten Mal 5-10 Minuten dauern
   - Gradle lädt alle Dependencies herunter

### Schritt 3: Gradle Sync

Falls die Gradle-Synchronisation nicht automatisch startet:

1. Klicke auf **File → Sync Project with Gradle Files**

2. Warte bis der Sync abgeschlossen ist (Statusleiste unten beobachten)

3. Bei Fehlern:
   - Prüfe Internet-Verbindung
   - Klicke auf "Try Again"
   - Ggf. **File → Invalidate Caches / Restart**

### Schritt 4: Android SDK konfigurieren

1. Öffne **Tools → SDK Manager**

2. Stelle sicher, dass folgende Komponenten installiert sind:
   - **Android API 34 (Android 14.0)**
   - **Android SDK Build-Tools 34.0.0**
   - **Android SDK Platform-Tools**
   - **Android SDK Tools**

3. Im Tab "SDK Tools":
   - **Android SDK Build-Tools**
   - **Android Emulator** (falls kein physisches Gerät)
   - **Google Play services**

### Schritt 5: Emulator oder Gerät einrichten

**Option A: Emulator (für Tests ohne Handy)**

1. Öffne **Tools → Device Manager**

2. Klicke auf **Create Device**

3. Wähle ein Gerät (z.B. "Pixel 6")

4. Wähle **System Image**: API Level 34 (Android 14.0)
   - Falls nicht vorhanden: Download klicken

5. Klicke auf **Finish**

**Option B: Physisches Android-Gerät**

1. Aktiviere auf deinem Handy die **Entwickleroptionen**:
   - Gehe zu Einstellungen → Über das Telefon
   - Tippe 7x auf "Build-Nummer"

2. In Entwickleroptionen:
   - Aktiviere **USB-Debugging**
   - Aktiviere **Über USB installieren** (falls vorhanden)

3. Verbinde Handy per USB mit PC

4. Bestätige die USB-Debugging-Anfrage am Handy

### Schritt 6: App bauen und starten

1. **Build → Make Project** (oder Strg+F9)
   - Warte bis Build erfolgreich abgeschlossen ist

2. Wähle dein Gerät/Emulator aus der Dropdown-Liste oben

3. Klicke auf **Run** (grüner Play-Button) oder drücke **Shift+F10**

4. Die App wird installiert und startet automatisch

### Schritt 7: Erste Schritte in der App

1. **Einstellungen ausfüllen** (Bottom Navigation → Einstellungen):
   - Name (z.B. "Nass, Daniel")
   - Einrichtung (z.B. "Österfeldschule Vaihingen")
   - Arbeitsumfang % (z.B. 93)
   - Wochenstunden (z.B. 37 Stunden 16 Minuten)
   - Arbeitstage/Woche (z.B. 5)
   - Ferienbetreuung (ja/nein)

2. **Erste Zeiterfassung** (Home-Screen):
   - Tippe auf "Von" → Wähle Startzeit
   - Tippe auf "Bis" → Wähle Endzeit
   - Tippe auf "Pause" → Gib Pausenminuten ein
   - Soll/Ist/Differenz werden automatisch berechnet

3. **Kalender ansehen** (Bottom Navigation → Kalender):
   - Siehst alle Einträge des Monats
   - Farbcodierung: Grün=Vollständig, Gelb=Teilweise, Rot=Leer

4. **Excel exportieren** (Bottom Navigation → Export):
   - Wähle Kalenderwoche
   - Klicke "Excel exportieren"
   - Datei wird in Downloads gespeichert

## 📁 Projektstruktur

```
ArbeitszeitTracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/arbeitszeit/tracker/
│   │   │   │   ├── data/
│   │   │   │   │   ├── entity/         # Room Entities
│   │   │   │   │   ├── dao/            # Data Access Objects
│   │   │   │   │   └── database/       # Room Database
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/        # Compose Screens
│   │   │   │   │   ├── components/     # Wiederverwendbare UI-Komponenten
│   │   │   │   │   ├── theme/          # Material 3 Theme
│   │   │   │   │   └── navigation/     # Navigation Graph
│   │   │   │   ├── viewmodel/          # ViewModels
│   │   │   │   ├── export/             # Excel Export Manager
│   │   │   │   ├── worker/             # Background Workers
│   │   │   │   ├── utils/              # Utility-Klassen
│   │   │   │   └── MainActivity.kt
│   │   │   ├── assets/
│   │   │   │   └── ANZ_Template.xlsx   # Excel-Vorlage
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   ├── drawable/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🔧 Technischer Stack

- **Sprache**: Kotlin
- **UI**: Jetpack Compose mit Material 3
- **Architektur**: MVVM (Model-View-ViewModel)
- **Datenbank**: Room (SQLite) - komplett offline
- **Excel-Export**: Apache POI 5.2.5
- **Background Tasks**: WorkManager
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14.0)

## 📝 Wichtige Hinweise

### Excel-Template
Die App verwendet die hochgeladene **ANZ_2025.xlsx** als Template. Diese Datei befindet sich in `app/src/main/assets/ANZ_Template.xlsx`.

**KRITISCH**: Die Excel-Datei wird NICHT verändert, sondern:
1. Template wird geladen
2. Nur Datenzellen werden gefüllt (Spalten C-F, H, J)
3. Alle Formeln, Formatierungen, Spaltenbreiten bleiben erhalten
4. Neue Datei wird in Downloads gespeichert

### Berechtigungen
Die App benötigt folgende Berechtigungen:
- **POST_NOTIFICATIONS** (Android 13+): Für Erinnerungen
- **SCHEDULE_EXACT_ALARM**: Für genaue Alarm-Zeiten
- **WRITE_EXTERNAL_STORAGE** (bis Android 9): Für Excel-Export
- **READ_EXTERNAL_STORAGE** (bis Android 12): Für Excel-Export

Diese werden zur Laufzeit angefragt (außer bei älteren Android-Versionen).

### Notifications
Die App plant automatisch:
- **Morgen-Reminder**: 07:30 Uhr
- **Abend-Reminder**: 17:00 Uhr
- **Fehlende-Einträge-Check**: 20:00 Uhr

Diese können in zukünftigen Versionen konfigurierbar gemacht werden.

## 🐛 Troubleshooting

### Problem: "Gradle sync failed"
**Lösung**:
1. Prüfe Internet-Verbindung
2. File → Invalidate Caches / Restart
3. Lösche `.gradle` Ordner und sync erneut

### Problem: "SDK not found"
**Lösung**:
1. Tools → SDK Manager
2. Installiere Android SDK 34
3. Sync Project

### Problem: "Unable to find androidx..."
**Lösung**:
1. Stelle sicher dass in gradle.properties steht:
   `android.useAndroidX=true`
2. Gradle Sync

### Problem: Excel-Export funktioniert nicht
**Lösung**:
1. Prüfe ob ANZ_Template.xlsx in app/src/main/assets/ vorhanden ist
2. Prüfe Speicherberechtigungen
3. Schaue in Downloads-Ordner

### Problem: App stürzt beim Start ab
**Lösung**:
1. Schaue in Logcat (unten in Android Studio)
2. Filter auf "Error"
3. Bei Room-Fehlern: App deinstallieren und neu installieren

## 📱 App testen

### Test-Szenario 1: Erste Zeiterfassung
1. App starten
2. Einstellungen ausfüllen
3. Home → "Von" setzen (z.B. 09:00)
4. "Bis" setzen (z.B. 17:00)
5. "Pause" setzen (z.B. 30)
6. Prüfe: Soll/Ist/Differenz werden angezeigt

### Test-Szenario 2: Wochenübersicht
1. Mehrere Tage erfassen
2. Home → Scrolle runter
3. Prüfe: Wochen-Zusammenfassung zeigt korrekte Summe

### Test-Szenario 3: Kalender
1. Kalender öffnen
2. Prüfe: Tage mit Einträgen sind grün
3. Prüfe: Tage ohne Einträge sind rot

### Test-Szenario 4: Excel-Export
1. Mindestens eine Woche erfassen
2. Export → KW auswählen
3. "Excel exportieren" klicken
4. Prüfe: Datei in Downloads
5. Öffne mit Excel/LibreOffice
6. Prüfe: Formeln funktionieren, Daten sind korrekt

## 🚀 Deployment (APK erstellen)

### Debug-APK (zum Testen)
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Warte bis Build fertig
3. Klicke auf "locate" in der Notification
4. APK befindet sich in `app/build/outputs/apk/debug/`

### Release-APK (für Veröffentlichung)
1. Build → Generate Signed Bundle / APK
2. Wähle "APK"
3. Erstelle neuen Keystore (einmalig) oder wähle bestehenden
4. Wähle "release" Build Variant
5. APK wird in `app/build/outputs/apk/release/` erstellt

**Wichtig**: Keystore-Datei und Passwort GUT AUFBEWAHREN! Ohne diese kannst du keine Updates veröffentlichen.

## 📄 Lizenz

Privates Projekt für Daniel - Österfeldschule Vaihingen

## 🙋 Support

Bei Fragen oder Problemen:
1. Schaue in Logcat nach Fehlermeldungen
2. Prüfe die Troubleshooting-Sektion
3. Kontaktiere den Entwickler

---

**Version**: 1.0  
**Build**: Android 8.0+ (API 26+)  
**Erstellt**: November 2025
