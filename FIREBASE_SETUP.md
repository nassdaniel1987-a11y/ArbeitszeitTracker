# Firebase Setup für ArbeitszeitTracker

Diese App verwendet **Firebase Vertex AI** für die KI-gestützte Urlaubsplanung mit Gemini 2.5 Flash.

## 🔥 Einmalige Einrichtung

### 1. Firebase-Projekt erstellen

1. Gehe zu https://console.firebase.google.com
2. Klicke auf "Projekt hinzufügen"
3. Projektname: `ArbeitszeitTracker` (oder beliebig)
4. Google Analytics: Optional
5. Klicke auf "Projekt erstellen"

### 2. Android-App zu Firebase hinzufügen

1. Im Firebase-Projekt: Klicke auf das **Android-Symbol** (</>)
2. **Android-Paketname:** `com.arbeitszeit.tracker` ⚠️ (muss genau so sein!)
3. **App-Spitzname:** `Arbeitszeit Tracker` (optional)
4. **SHA-1:** Kann leer bleiben (optional, nur für Auth/Dynamic Links)
5. Klicke auf "App registrieren"

### 3. google-services.json herunterladen

1. Lade die `google-services.json` Datei herunter
2. Lege sie im **`app/`** Ordner ab:
   ```
   ArbeitszeitTracker/
   ├── app/
   │   ├── google-services.json  ← HIER!
   │   ├── build.gradle.kts
   │   └── src/
   └── ...
   ```

⚠️ **WICHTIG:** Die Datei ist in `.gitignore` und wird **NICHT** ins Git-Repository committed!

### 4. Vertex AI API aktivieren

**Option A: Automatisch (empfohlen)**
- Die Vertex AI API wird automatisch beim ersten Build/Aufruf aktiviert
- Einfach die App bauen und es funktioniert!

**Option B: Manuell über Google Cloud Console**
1. https://console.cloud.google.com
2. Projekt auswählen (oben in der Leiste)
3. Suche nach "Vertex AI API"
4. Klicke auf "Enable" / "Aktivieren"

## 🚀 Fertig!

Die App ist jetzt bereit für KI-gestützte Urlaubsplanung mit Gemini 2.5 Flash.

### Features:
- ✅ Gemini 2.5 Flash (neueste Version)
- ✅ Kein separater API Key nötig
- ✅ Automatische Firebase-Authentifizierung
- ✅ 1500 kostenlose Anfragen pro Tag

## 🔒 Sicherheit

- Die `google-services.json` enthält Firebase-Projekt-Konfiguration
- Sie ist in `.gitignore` und wird nicht committed
- Jeder Entwickler benötigt seine eigene Kopie
- Alternativ: Verwende ein gemeinsames Firebase-Projekt für das Team

## 📝 Für andere Entwickler

Wenn du das Projekt klonst:
1. Erstelle dein eigenes Firebase-Projekt (siehe oben)
2. Oder frage den Team-Lead nach der `google-services.json`
3. Lege sie im `app/` Ordner ab
4. Build die App: `./gradlew assembleDebug`

## ❓ Troubleshooting

**Fehler: "google-services.json missing"**
- Datei noch nicht heruntergeladen oder falsch platziert
- Muss genau in `app/google-services.json` liegen

**Fehler: "Vertex AI API not enabled"**
- Firebase Console → Vertex AI aktivieren
- Oder: Beim ersten Build wird es automatisch aktiviert

**Fehler: "Package name mismatch"**
- In `google-services.json` muss `com.arbeitszeit.tracker` stehen
- App im Firebase-Projekt mit korrektem Package-Namen registriert?

## 🔗 Links

- Firebase Console: https://console.firebase.google.com
- Vertex AI Docs: https://firebase.google.com/docs/vertex-ai
- Gemini API Docs: https://ai.google.dev/gemini-api/docs
