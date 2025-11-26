# 🚀 Release-APK erstellen - Anleitung für Anfänger

**Du brauchst:**
- Windows PC
- Android Studio installiert
- Google-Konto
- 30 Minuten Zeit

---

## 📱 Teil 1: Release-Keystore erstellen (nur einmal!)

### Was ist ein Keystore?
Das ist deine "digitale Unterschrift" für die App. **SEHR WICHTIG:** Ohne diesen kannst du später keine Updates mehr machen!

### Schritt 1: Android Studio öffnen

1. Öffne dein Projekt `ArbeitszeitTracker` in Android Studio
2. Warte bis alles geladen ist (unten rechts siehst du "Indexing..." - warte bis das fertig ist)

### Schritt 2: Keystore erstellen

1. Klicke oben auf **Build**
2. Dann auf **Generate Signed Bundle / APK**
3. Wähle **APK** (nicht Bundle!)
4. Klicke **Next**

5. Du siehst jetzt ein Fenster "Key store path"
6. Klicke auf **Create new...**

### Schritt 3: Keystore-Daten eingeben

Jetzt kommt ein Formular. Fülle es SO aus:

**Key store path:**
- Klicke auf das Ordner-Symbol
- Gehe zu `C:\Users\DEINNAME\`
- Schreibe als Dateinamen: `arbeitszeit-release-key.jks`
- Klicke **OK**

**Password:**
- Denk dir ein **sicheres Passwort** aus
- **WICHTIG:** Schreib es auf einen Zettel oder in Passwort-Manager!
- Beispiel: `MeinSuperPasswort2024!`

**Confirm:**
- Schreib das **gleiche Passwort** nochmal

**Alias:**
- Schreib: `arbeitszeit-release`

**Password (darunter):**
- Schreib das **gleiche Passwort** wie oben

**Confirm:**
- Schreib das **gleiche Passwort** nochmal

**Validity (years):**
- Schreib: `25`

**Certificate:**
- **First and Last Name:** Schreib deinen Namen (z.B. "Max Mustermann")
- **Organizational Unit:** Kannst leer lassen
- **Organization:** Kannst leer lassen
- **City or Locality:** Kannst leer lassen
- **State or Province:** Kannst leer lassen
- **Country Code (XX):** Schreib `DE`

7. Klicke **OK**

**✅ Keystore erstellt!** Die Datei ist jetzt hier: `C:\Users\DEINNAME\arbeitszeit-release-key.jks`

**⚠️ WICHTIG: BACKUP MACHEN!**
1. Kopiere die Datei `arbeitszeit-release-key.jks` auf einen USB-Stick
2. Oder in deine Cloud (Google Drive, Dropbox, etc.)
3. Schreib das Passwort irgendwo sicher auf!

---

## 🔑 Teil 2: SHA-1 Fingerprint holen

### Was ist SHA-1?
Das ist so eine Art "Ausweis-Nummer" von deinem Keystore. Brauchst du für Google Drive.

### Schritt 1: Kommandozeile öffnen

1. Drücke **Windows-Taste**
2. Tippe: `cmd`
3. Drücke **Enter**
4. Ein schwarzes Fenster öffnet sich

### Schritt 2: SHA-1 berechnen

1. Kopiere diesen Befehl:
```
cd C:\Users\DEINNAME
```
(Ersetze `DEINNAME` mit deinem Windows-Benutzernamen!)

2. Klicke ins schwarze Fenster
3. Rechtsklick → **Einfügen**
4. Drücke **Enter**

5. Jetzt kopiere diesen Befehl:
```
keytool -list -v -keystore arbeitszeit-release-key.jks -alias arbeitszeit-release
```

6. Klicke ins schwarze Fenster
7. Rechtsklick → **Einfügen**
8. Drücke **Enter**

9. Es fragt nach einem Passwort
10. Tippe dein Passwort ein (Du siehst nichts beim Tippen - das ist normal!)
11. Drücke **Enter**

### Schritt 3: SHA-1 kopieren

Du siehst jetzt viel Text. Such nach dieser Zeile:
```
SHA1: DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09
```

**Das ist DEIN SHA-1!** (Deine Buchstaben/Zahlen sind anders als im Beispiel)

1. Markiere den SHA-1 mit der Maus (z.B. `DA:39:A3:EE:...`)
2. Drücke **Enter** (kopiert automatisch)
3. **Schreib ihn auf einen Zettel** oder in eine Notiz-App!

---

## ☁️ Teil 3: Google Cloud Console einrichten

### Was machen wir hier?
Wir sagen Google: "Hey, meine App darf Google Drive benutzen!"

### Schritt 1: Google Cloud Console öffnen

1. Öffne deinen Browser
2. Gehe zu: https://console.cloud.google.com/
3. Melde dich mit deinem Google-Konto an

### Schritt 2: Neues Projekt erstellen

1. Klicke oben links auf **"Select a project"** (oder "Projekt auswählen")
2. Klicke auf **NEW PROJECT** (oder "NEUES PROJEKT")
3. Schreib als Name: `Arbeitszeit Tracker`
4. Klicke **CREATE** (oder "ERSTELLEN")
5. Warte 10 Sekunden

### Schritt 3: Google Drive API aktivieren

1. Klicke links auf das **☰ Menü** (3 Striche)
2. Klicke auf **APIs & Services** → **Library**
   (oder auf Deutsch: **APIs und Dienste** → **Bibliothek**)
3. Such oben nach: `Google Drive API`
4. Klicke auf **Google Drive API**
5. Klicke auf **ENABLE** (oder "AKTIVIEREN")

### Schritt 4: OAuth Consent Screen

1. Klicke links auf **OAuth consent screen**
   (oder "OAuth-Zustimmungsbildschirm")
2. Wähle **External** (Extern)
3. Klicke **CREATE** (oder "ERSTELLEN")

**Seite 1 - App information:**
- **App name:** Schreib `Arbeitszeit Tracker`
- **User support email:** Wähle deine E-Mail aus
- **App logo:** Kannst leer lassen
- **App domain:** Kannst leer lassen
- **Developer contact email:** Schreib deine E-Mail
- Klicke **SAVE AND CONTINUE**

**Seite 2 - Scopes:**
1. Klicke **ADD OR REMOVE SCOPES**
2. Such nach: `drive.file`
3. Setze ein **Häkchen** bei:
   - `https://www.googleapis.com/auth/drive.file`
4. Klicke **UPDATE**
5. Klicke **SAVE AND CONTINUE**

**Seite 3 - Test users:**
1. Klicke **+ ADD USERS**
2. Schreib deine E-Mail-Adresse rein
3. Klicke **ADD**
4. Klicke **SAVE AND CONTINUE**

**Seite 4 - Summary:**
1. Einfach durchscrollen
2. Klicke **BACK TO DASHBOARD**

### Schritt 5: OAuth Client ID erstellen

1. Klicke links auf **Credentials** (oder "Anmeldedaten")
2. Klicke oben auf **+ CREATE CREDENTIALS**
3. Wähle **OAuth client ID**

4. Bei **Application type:** Wähle **Android**
5. Bei **Name:** Schreib `Arbeitszeit Tracker Android Release`

6. Bei **Package name:** Schreib EXAKT:
```
com.arbeitszeit.tracker
```

7. Bei **SHA-1 certificate fingerprint:**
   - Füge deinen SHA-1 ein (den du vorher kopiert hast!)
   - Beispiel: `DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09`

8. Klicke **CREATE**
9. Es kommt ein Pop-up → Klicke **OK**

**✅ Google Drive ist jetzt eingerichtet!**

**⏳ WICHTIG:** Warte **5 Minuten** bevor du weitermachst! Google braucht Zeit, das zu aktivieren.

---

## 📦 Teil 4: Release-APK bauen

### Schritt 1: Gradle-Dateien anpassen

**1. Datei `keystore.properties` erstellen:**

1. In Android Studio: Links siehst du deine Dateien
2. Rechtsklick auf **ArbeitszeitTracker** (ganz oben)
3. **New** → **File**
4. Schreib als Name: `keystore.properties`
5. Drücke **Enter**

6. Die Datei öffnet sich
7. Schreib da rein (ersetze die Werte!):

```properties
storePassword=DEIN_PASSWORT
keyPassword=DEIN_PASSWORT
keyAlias=arbeitszeit-release
storeFile=C:\\Users\\DEINNAME\\arbeitszeit-release-key.jks
```

**WICHTIG:**
- Ersetze `DEIN_PASSWORT` mit deinem echten Passwort!
- Ersetze `DEINNAME` mit deinem Windows-Benutzernamen!
- Beachte die **doppelten Backslashes** `\\`!

8. Drücke **Strg+S** zum Speichern

### Schritt 2: Release-APK bauen

1. Klicke oben auf **Build**
2. Klicke auf **Generate Signed Bundle / APK**
3. Wähle **APK**
4. Klicke **Next**

5. Jetzt siehst du wieder das Keystore-Fenster:
   - **Key store path:** Sollte schon drin stehen
   - **Key store password:** Tippe dein Passwort
   - **Key alias:** Sollte `arbeitszeit-release` sein
   - **Key password:** Tippe dein Passwort

6. Klicke **Next**

7. Wähle **release** (Häkchen setzen!)
8. Bei **Signature Versions:**
   - Setze **Häkchen** bei **V1 (Jar Signature)**
   - Setze **Häkchen** bei **V2 (Full APK Signature)**

9. Klicke **Finish**

**⏳ Jetzt wird gebaut - Das dauert 1-3 Minuten!**

Unten rechts siehst du einen Fortschrittsbalken. Warte bis da steht:
```
BUILD SUCCESSFUL in 1m 23s
```

### Schritt 3: APK finden

Wenn der Build fertig ist:

1. Unten rechts kommt eine Meldung: **locate**
2. Klicke auf **locate**
3. Ein Ordner öffnet sich
4. **DA IST DEINE APK!** → `app-release.apk`

**Oder manuell suchen:**
1. Gehe zu deinem Projekt-Ordner
2. Öffne: `app` → `build` → `outputs` → `apk` → `release`
3. DA: `app-release.apk`

---

## 📱 Teil 5: APK weitergeben

### An dich selbst testen:

1. Verbinde dein Handy mit dem PC (USB-Kabel)
2. Kopiere `app-release.apk` aufs Handy
3. Öffne die Datei auf dem Handy
4. Android fragt: "Unbekannte App installieren?"
5. Erlaube es
6. App installiert sich
7. **FERTIG!** 🎉

### An Kollegen weitergeben:

**Option 1: Per E-Mail**
1. Schreib eine E-Mail
2. Hänge `app-release.apk` als Anhang an
3. Sende ab

**Option 2: Per USB-Stick**
1. Kopiere `app-release.apk` auf USB-Stick
2. Gib USB-Stick dem Kollegen

**Option 3: Per Cloud**
1. Lade `app-release.apk` zu Google Drive / Dropbox hoch
2. Teile den Link mit dem Kollegen

---

## ✅ Checkliste: Habe ich alles?

Bevor du die APK weitergibst:

- [ ] Keystore erstellt und **gesichert**? (Backup auf USB-Stick!)
- [ ] Passwort **aufgeschrieben**?
- [ ] SHA-1 in Google Cloud Console eingetragen?
- [ ] 5 Minuten gewartet nach Google Setup?
- [ ] Release-APK erfolgreich gebaut?
- [ ] APK auf eigenem Handy getestet?
- [ ] Google Drive Sign-In funktioniert?

**Wenn alles ✅ ist: GLÜCKWUNSCH!** Du kannst die APK jetzt verteilen! 🎉

---

## 🆘 Häufige Probleme

### Problem: "BUILD FAILED" beim Bauen

**Lösung 1:** Gradle Daemon neu starten
1. In Android Studio: Unten links **Terminal** klicken
2. Tippe: `./gradlew --stop`
3. Warte 5 Sekunden
4. Versuche nochmal zu bauen

**Lösung 2:** Clean Build
1. Klicke oben auf **Build** → **Clean Project**
2. Warte bis fertig
3. Klicke **Build** → **Rebuild Project**
4. Versuche nochmal

### Problem: "DEVELOPER_ERROR" beim Google Sign-In

**Ursache:** SHA-1 stimmt nicht oder ist noch nicht aktiv

**Lösung:**
1. Prüfe ob SHA-1 korrekt in Google Cloud Console eingetragen ist
2. Warte **5 Minuten** (Google braucht Zeit!)
3. Versuche nochmal

### Problem: Keystore-Passwort vergessen

**Ursache:** Passwort nicht aufgeschrieben

**Lösung:**
❌ **LEIDER KEINE!** Du musst einen **neuen Keystore** erstellen.
Das heißt: Die App gilt als "neue App" - Updates gehen nicht mehr.

**DESHALB: BACKUP MACHEN UND PASSWORT AUFSCHREIBEN!** ⚠️

### Problem: APK lässt sich nicht installieren

**Lösung:**
1. Auf dem Handy: **Einstellungen** öffnen
2. **Sicherheit** → **Unbekannte Apps installieren**
3. Erlaube es für deinen Datei-Manager

---

## 📞 Fragen?

Wenn du nicht weiterkommst:
1. Lies nochmal die Anleitung Schritt-für-Schritt
2. Prüfe ob du wirklich **alles** gemacht hast
3. Schau in "Häufige Probleme"
4. Frag mich! 😊

---

## 🎯 Zusammenfassung - Was haben wir gemacht?

1. ✅ **Keystore erstellt** → Deine digitale Signatur für die App
2. ✅ **SHA-1 geholt** → Ausweis-Nummer vom Keystore
3. ✅ **Google Cloud eingerichtet** → Google Drive Zugriff erlaubt
4. ✅ **Release-APK gebaut** → Fertige App als Datei
5. ✅ **APK verteilt** → Kollegen können sie installieren

**Die APK funktioniert jetzt bei JEDEM der sie installiert!** 🚀

Jeder kann:
- Die App installieren
- Sich mit Google anmelden
- Google Drive Backup nutzen
- Alle Features verwenden

**WICHTIG:** Jeder User hat sein **eigenes** Google Drive Backup!
Die Daten sind **nicht** geteilt zwischen den Usern.

---

**Viel Erfolg!** 🎉
