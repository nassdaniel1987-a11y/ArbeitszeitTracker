# Google Drive Integration Setup

Diese Anleitung erklärt, wie du OAuth 2.0 für die Google Drive Integration konfigurierst.

## 📋 Voraussetzungen

- Google-Konto
- Android Studio
- Die App muss mindestens einmal gebaut worden sein

## 🔧 Schritt 1: Google Cloud Console Projekt erstellen

1. Gehe zu [Google Cloud Console](https://console.cloud.google.com/)
2. Erstelle ein neues Projekt oder wähle ein bestehendes aus
3. Notiere die **Project ID**

## 🔑 Schritt 2: Google Drive API aktivieren

1. In der Cloud Console: **APIs & Services** → **Library**
2. Suche nach "**Google Drive API**"
3. Klicke auf "**Enable**"

## 🔐 Schritt 3: OAuth 2.0 Consent Screen konfigurieren

1. Gehe zu **APIs & Services** → **OAuth consent screen**
2. Wähle **External** (für Testphase) oder **Internal** (nur für deine Organisation)
3. Fülle die Pflichtfelder aus:
   - **App name**: `Arbeitszeit Tracker`
   - **User support email**: Deine E-Mail
   - **Developer contact email**: Deine E-Mail
4. Klicke auf **Save and Continue**
5. **Scopes**: Klicke auf "**Add or Remove Scopes**"
   - Suche und wähle: `https://www.googleapis.com/auth/drive.file`
   - Dieser Scope erlaubt nur Zugriff auf von der App erstellte Dateien
6. **Test users** (nur bei External): Füge deine E-Mail als Testuser hinzu
7. Klicke auf **Save and Continue**

## 📱 Schritt 4: SHA-1 Fingerprint abrufen

### Debug-Keystore (für Entwicklung):

```bash
# Windows (in Android Studio Terminal):
cd "%USERPROFILE%\.android"
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android

# macOS/Linux:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Notiere den **SHA-1** Fingerprint (z.B. `DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09`)

### Release-Keystore (für Production):

```bash
keytool -list -v -keystore /pfad/zu/deinem/release.keystore -alias dein-alias
```

## 🔑 Schritt 5: OAuth 2.0 Client ID erstellen

1. Gehe zu **APIs & Services** → **Credentials**
2. Klicke auf "**+ CREATE CREDENTIALS**" → **OAuth client ID**
3. Wähle **Android** als Application type
4. Fülle die Felder aus:
   - **Name**: `Arbeitszeit Tracker Android`
   - **Package name**: `com.arbeitszeit.tracker`
   - **SHA-1 certificate fingerprint**: Füge den Debug-SHA-1 ein
5. Klicke auf **Create**
6. **Wichtig**: Wiederhole für Release-Keystore (separater Client)

## 📄 Schritt 6: OAuth Client ID in App einfügen

Die OAuth Client ID wird **nicht** direkt in der App verwendet! Google Sign-In verwendet automatisch:
- Package Name: `com.arbeitszeit.tracker`
- SHA-1 Fingerprint vom Signing-Keystore

Die App ist jetzt korrekt konfiguriert und funktioniert automatisch!

## 🔍 Schritt 7: Web Client ID (Optional)

Wenn du auch Web-Zugriff benötigst (z.B. für Backend):

1. Erstelle eine weitere **OAuth client ID** mit Typ **Web application**
2. Notiere die **Client ID** (endet mit `.apps.googleusercontent.com`)
3. Diese kann für Server-zu-Server Auth verwendet werden

## ✅ Verifizierung

Nach dem Setup kannst du testen:

1. Baue die App neu: `./gradlew clean assembleDebug`
2. Installiere die App auf deinem Gerät
3. Gehe zu **Einstellungen** → **Backup** → **Cloud-Backup**
4. Klicke auf "**Mit Google anmelden**"
5. Wähle dein Google-Konto aus
6. Erlaube den Zugriff auf Google Drive

## 🐛 Troubleshooting

### "API not enabled" Error
- Stelle sicher, dass die **Google Drive API** aktiviert ist
- Warte 1-2 Minuten nach Aktivierung

### "Sign in failed: DEVELOPER_ERROR"
- SHA-1 Fingerprint stimmt nicht überein
- Überprüfe Package Name (`com.arbeitszeit.tracker`)
- Stelle sicher, dass du den richtigen Keystore verwendest (debug vs release)

### "Access not configured"
- OAuth Consent Screen muss vollständig konfiguriert sein
- Drive Scope (`https://www.googleapis.com/auth/drive.file`) muss hinzugefügt sein

### "App not verified"
- Bei External User Type: Füge dich als Test User hinzu
- Für Production: Durchlaufe Google's Verification Process

## 📝 Wichtige Links

- [Google Cloud Console](https://console.cloud.google.com/)
- [Google Drive API Documentation](https://developers.google.com/drive/api/v3/about-sdk)
- [OAuth 2.0 Setup Guide](https://developers.google.com/identity/protocols/oauth2)

## 🔒 Sicherheit

- **Niemals** die `google-services.json` in Git committen!
- SHA-1 Fingerprints sind öffentlich sichtbar (nicht geheim)
- OAuth Client IDs sind auch öffentlich (kein Problem)
- Der eigentliche Auth-Token ist serverseitig geschützt

## 📌 Notizen

- Der Scope `drive.file` erlaubt nur Zugriff auf Dateien, die die App selbst erstellt hat
- Die App kann NICHT auf andere Dateien in Google Drive zugreifen
- Backups werden im App-spezifischen Ordner gespeichert
- Bei App-Deinstallation bleiben Backups in Drive erhalten
