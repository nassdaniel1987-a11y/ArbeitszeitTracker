# Release-APK Build Anleitung

Diese Anleitung zeigt, wie du eine **signierte Release-APK** baust, die bei allen funktioniert (inkl. Google Drive Backup).

---

## 📋 Übersicht

1. ✅ Release-Keystore erstellen (einmalig)
2. ✅ Signing Config in Gradle konfigurieren
3. ✅ SHA-1 für Google Drive OAuth holen
4. ✅ SHA-1 in Google Cloud Console eintragen
5. ✅ Release-APK bauen
6. ✅ APK testen und verteilen

---

## 🔑 Schritt 1: Release-Keystore erstellen

Der Keystore ist deine **digitale Signatur** für die App. **Sehr wichtig:**
- ⚠️ **Backup anlegen!** Ohne Keystore kannst du keine Updates mehr veröffentlichen!
- ⚠️ **Passwort nicht vergessen!** Aufschreiben oder in Passwort-Manager!

### Option A: In Android Studio (einfach)

1. **Build** → **Generate Signed Bundle / APK**
2. Wähle **APK**
3. Klicke **Create new...**
4. Fülle das Formular aus:
   ```
   Key store path: C:\Users\DEINNAME\arbeitszeit-release-key.jks
   Password: [sicheres Passwort wählen]
   Confirm: [Passwort wiederholen]

   Alias: arbeitszeit-release
   Password: [gleiches oder anderes Passwort]
   Confirm: [Passwort wiederholen]

   Validity (years): 25

   Certificate:
   First and Last Name: Dein Name
   Organization: [Optional]
   City: [Optional]
   Country Code: DE
   ```
5. Klicke **OK**
6. **✅ Keystore erstellt!**

### Option B: Kommandozeile (für Profis)

```bash
# In einem sicheren Verzeichnis (z.B. C:\KeyStore\)
keytool -genkey -v -keystore arbeitszeit-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias arbeitszeit-release

# Passwort eingeben und Fragen beantworten
# WICHTIG: Passwort aufschreiben!
```

---

## 🔧 Schritt 2: Signing Config in Gradle

### 2.1: Keystore-Datei ins Projekt kopieren (OPTIONAL)

**Option A: Im Projekt** (einfacher zu teilen im Team)
```
Kopiere: arbeitszeit-release-key.jks
Nach: /ArbeitszeitTracker/keystore/release.jks
```

**Option B: Außerhalb** (sicherer)
```
Lasse: C:\Users\DEINNAME\arbeitszeit-release-key.jks
```

### 2.2: keystore.properties erstellen

Erstelle eine Datei: **`/ArbeitszeitTracker/keystore.properties`**

```properties
storePassword=DEIN_KEYSTORE_PASSWORT
keyPassword=DEIN_KEY_PASSWORT
keyAlias=arbeitszeit-release
storeFile=../keystore/release.jks
```

**ODER** (falls Keystore außerhalb):
```properties
storePassword=DEIN_KEYSTORE_PASSWORT
keyPassword=DEIN_KEY_PASSWORT
keyAlias=arbeitszeit-release
storeFile=C:\\Users\\DEINNAME\\arbeitszeit-release-key.jks
```

⚠️ **WICHTIG**: Füge zu `.gitignore` hinzu (bereits vorhanden!):
```
keystore.properties
*.jks
*.keystore
```

### 2.3: app/build.gradle.kts anpassen

Füge **nach** `android {` folgendes hinzu:

```kotlin
android {
    namespace = "com.arbeitszeit.tracker"
    compileSdk = 35

    // NEUE SIGNING CONFIG - HIER EINFÜGEN:
    signingConfigs {
        create("release") {
            // Lade keystore.properties
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))

                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.arbeitszeit.tracker"
        minSdk = 26
        // ... rest bleibt gleich
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")  // DIESE ZEILE HINZUFÜGEN
        }
    }
}
```

---

## 🔐 Schritt 3: SHA-1 Fingerprint holen

Der SHA-1 wird für Google Drive OAuth benötigt.

### Windows:

```bash
cd C:\Users\DEINNAME
keytool -list -v -keystore arbeitszeit-release-key.jks -alias arbeitszeit-release
```

**ODER** (falls im Projekt):
```bash
cd C:\Users\DEINNAME\AndroidStudioProjects\ArbeitszeitTracker\keystore
keytool -list -v -keystore release.jks -alias arbeitszeit-release
```

### macOS/Linux:

```bash
keytool -list -v -keystore ~/arbeitszeit-release-key.jks -alias arbeitszeit-release
```

### Passwort eingeben, dann SHA-1 kopieren:

```
Certificate fingerprints:
    SHA1: DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09
    SHA256: ...
```

**→ Kopiere den SHA-1!** (z.B. `DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09`)

---

## ☁️ Schritt 4: SHA-1 in Google Cloud Console

1. Gehe zu [Google Cloud Console](https://console.cloud.google.com/)
2. Wähle dein Projekt: **Arbeitszeit Tracker**
3. **APIs & Services** → **Credentials**

### Falls noch kein Android OAuth Client existiert:

4. Klicke **+ CREATE CREDENTIALS** → **OAuth client ID**
5. Wähle **Android**
6. Fülle aus:
   ```
   Name: Arbeitszeit Tracker Android (Release)
   Package name: com.arbeitszeit.tracker
   SHA-1: DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09
   ```
7. **Create** klicken

### Falls bereits ein Android OAuth Client existiert:

4. Klicke auf deinen **Android OAuth Client**
5. Scrolle zu **SHA-1 certificate fingerprints**
6. Klicke **+ ADD FINGERPRINT**
7. Füge den Release-SHA-1 ein
8. **SAVE** klicken

**⏳ Warte 2-5 Minuten** bis die Änderungen aktiv sind!

---

## 🏗️ Schritt 5: Release-APK bauen

### Option A: Android Studio (GUI)

1. **Build** → **Generate Signed Bundle / APK**
2. Wähle **APK**
3. Wähle deinen Keystore: `arbeitszeit-release-key.jks`
4. Passwörter eingeben
5. Wähle **release** Build Variant
6. **V1 (Jar Signature)** ✅ aktivieren
7. **V2 (Full APK Signature)** ✅ aktivieren
8. **Finish** klicken

**→ APK wird erstellt in:** `app/release/app-release.apk`

### Option B: Kommandozeile (schneller)

```bash
cd C:\Users\DEINNAME\AndroidStudioProjects\ArbeitszeitTracker

# Clean Build
./gradlew clean

# Build Release APK
./gradlew assembleRelease

# APK ist fertig in:
# app/build/outputs/apk/release/app-release.apk
```

---

## ✅ Schritt 6: APK testen

### 6.1: APK installieren

**Option A: Direkt vom PC**
- Verbinde Handy via USB
- Kopiere `app-release.apk` aufs Handy
- Öffne Datei-Manager, tippe auf APK
- Installieren erlauben

**Option B: Per ADB**
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 6.2: Google Drive testen

1. Öffne App
2. Gehe zu **Einstellungen** → **Backup**
3. Klicke **"Mit Google anmelden"**
4. Wähle Google-Konto
5. Erlaube Drive-Zugriff
6. ✅ **Sollte funktionieren!**

**Falls Fehler "DEVELOPER_ERROR":**
- SHA-1 noch nicht aktiv (2-5 Min warten)
- SHA-1 falsch (nochmal prüfen)
- Package Name falsch (muss `com.arbeitszeit.tracker` sein)

---

## 📤 Schritt 7: APK verteilen

### Option A: Direkt per Datei

```
Kopiere: app/build/outputs/apk/release/app-release.apk
Sende an: Kollegen per E-Mail, USB, Cloud, etc.
```

**Der Kollege installiert:**
1. "Unbekannte Quellen" erlauben (Android-Einstellungen)
2. APK öffnen
3. Installieren
4. ✅ Fertig! Google Drive funktioniert sofort.

### Option B: Google Play Store (später)

1. Erstelle Google Play Console Account
2. Erstelle App-Eintrag
3. Lade APK hoch
4. Durchlaufe Review-Prozess
5. Veröffentliche

---

## 🔒 Wichtige Sicherheitshinweise

### ✅ DO's:

- ✅ **Keystore sichern!** (USB-Stick, Cloud-Backup)
- ✅ **Passwörter aufschreiben** (Passwort-Manager)
- ✅ `keystore.properties` in `.gitignore`
- ✅ Keystore außerhalb von Git lagern

### ❌ DON'Ts:

- ❌ **NIEMALS** Keystore in Git committen!
- ❌ **NIEMALS** Passwörter in Code schreiben!
- ❌ **NIEMALS** Keystore verlieren!
- ❌ **NIEMALS** Release-APK mit Debug-Keystore signieren!

---

## 📝 Checkliste für Kollegen

Wenn du die APK an Kollegen weitergibst:

- [ ] Release-APK gebaut (nicht Debug!)
- [ ] Mit Release-Keystore signiert
- [ ] Release-SHA-1 in Google Cloud Console eingetragen
- [ ] 5 Minuten gewartet (bis OAuth aktiv)
- [ ] Selbst getestet (Google Drive Sign-In funktioniert)
- [ ] APK-Datei weitergegeben
- [ ] Kollege installiert & testet

---

## 🆘 Troubleshooting

### Problem: "DEVELOPER_ERROR" beim Google Sign-In

**Ursache:** SHA-1 stimmt nicht oder ist nicht eingetragen

**Lösung:**
```bash
# 1. Prüfe nochmal den SHA-1:
keytool -list -v -keystore arbeitszeit-release-key.jks -alias arbeitszeit-release

# 2. Vergleiche mit Google Cloud Console
# 3. Falls falsch: Korrigiere in Console
# 4. Warte 5 Minuten
```

### Problem: "App not installed"

**Ursache:** Signatur-Konflikt (alte Version mit anderem Keystore)

**Lösung:**
```
1. Alte App deinstallieren
2. Neu installieren
```

### Problem: ProGuard-Fehler beim Build

**Ursache:** Code-Optimierung entfernt zu viel

**Lösung:** In `proguard-rules.pro` hinzufügen:
```proguard
# Google Drive API
-keep class com.google.api.** { *; }
-dontwarn com.google.api.**

# Google Auth
-keep class com.google.android.gms.auth.** { *; }
```

---

## 📚 Zusammenfassung

| Schritt | Was | Wie oft |
|---------|-----|---------|
| 1. Keystore | Erstellen & Sichern | **Einmalig** |
| 2. Signing Config | Gradle anpassen | **Einmalig** |
| 3. SHA-1 | Aus Keystore holen | **Einmalig** |
| 4. Google Console | SHA-1 eintragen | **Einmalig** |
| 5. Build | APK bauen | **Bei jedem Release** |
| 6. Test | Auf Gerät testen | **Bei jedem Release** |
| 7. Verteilen | APK weitergeben | **Bei jedem Release** |

---

## 🎉 Fertig!

Nach diesem Setup können **alle** die APK installieren und Google Drive nutzen - ohne zusätzliches Setup!

**Dein Release-Keystore ist das Wichtigste:**
- Backup anlegen
- Passwörter sicher aufbewahren
- Nie verlieren!

---

## 📞 Nächste Schritte

**Du bist ready!** 🚀

- APK an Kollegen senden
- Feedback sammeln
- Bei Bedarf: Play Store Veröffentlichung vorbereiten
