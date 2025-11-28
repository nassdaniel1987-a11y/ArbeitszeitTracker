# 🤖 Gemini API Setup - Schritt-für-Schritt Anleitung

## 📋 Überblick

Diese Anleitung zeigt dir, wie du die **Google Gemini API** für die KI-gestützte Urlaubsplanung in deiner App einrichtest.

**Was du brauchst:**
- ✅ Google-Konto (kostenlos)
- ✅ 5 Minuten Zeit
- ✅ Keine Kreditkarte nötig
- ✅ 100% kostenlos (1500 Anfragen/Tag)

---

## 🎯 Schritt 1: API Key erstellen (2 Minuten)

### 1.1 Google AI Studio öffnen

1. Öffne im Browser: **https://aistudio.google.com/app/apikey**
2. Melde dich mit deinem Google-Konto an

### 1.2 API Key erstellen

1. Klicke auf **"Create API key"**
2. Wähle ein Projekt (oder erstelle ein neues):
   - Projektname: z.B. "ArbeitszeitTracker"
3. Klicke auf **"Create API key in new project"**

### 1.3 API Key kopieren

```
Dein API Key sieht so aus:
AIzaSyC...xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**⚠️ WICHTIG:**
- Speichere den Key sicher!
- Teile ihn mit NIEMANDEM
- Er wird nur einmal angezeigt

---

## 🔧 Schritt 2: API Key in der App speichern

### Option A: Sicher in local.properties (EMPFOHLEN)

1. Öffne die Datei `local.properties` im Projekt-Root
2. Füge folgende Zeile hinzu:
   ```properties
   GEMINI_API_KEY=AIzaSyC...xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
3. Speichern!

**Vorteil:** Der Key wird NICHT in Git committed!

### Option B: In der App eingeben (später)

Du kannst den Key auch später in den App-Einstellungen eingeben.

---

## 📦 Schritt 3: SDK einbinden (bereits vorbereitet!)

Die notwendige Dependency ist bereits in `build.gradle.kts` vorbereitet:

```kotlin
dependencies {
    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
}
```

**➡️ Du musst nichts machen - ist schon drin!**

---

## 🚀 Schritt 4: Features testen

### 4.1 Urlaubsplanung testen

Wenn die KI-Integration implementiert ist:

1. Öffne die App
2. Gehe zu **"Urlaub"**
3. Klicke auf **"KI-Optimierung"**
4. Gib deine Wünsche ein:
   - Urlaubstage: 30
   - Ziel: "Lange am Stück"
5. Klicke auf **"Optimieren"**

Die KI analysiert:
- ✅ Schulferien Baden-Württemberg
- ✅ Brückentage
- ✅ Deine Präferenzen

Und gibt dir Vorschläge wie:
```
🎯 Sommerferien: 12 Tage = 8 Wochen frei!
💡 Ostern: 3 Tage = 2,5 Wochen frei
🎄 Weihnachten: 5 Tage = 2 Wochen frei

✨ Du sparst 10 Tage durch clevere Planung!
```

---

## 💰 Schritt 5: Kosten & Limits verstehen

### Kostenlose Nutzung

```
Gemini API Free Tier:
├── 1500 Anfragen pro Tag
├── 100% KOSTENLOS
├── Keine Kreditkarte nötig
└── Perfekt für persönliche Apps
```

### Realistische Nutzung

| Szenario | Anfragen/Monat | Kosten |
|----------|----------------|--------|
| Normal (10x/Monat) | 10 | **0€** |
| Viel (100x/Monat) | 100 | **0€** |
| Extrem (1000x/Monat) | 1000 | **0€** |

**➡️ Du bleibst IMMER bei 0€!**

---

## 🔒 Schritt 6: Datenschutz & Sicherheit

### Was wird zur KI gesendet?

✅ **Gesendet:**
- Anzahl Urlaubstage
- Bundesland (für Schulferien)
- Deine Wünsche ("lange am Stück")

❌ **NICHT gesendet:**
- Name
- Arbeitszeiten
- Persönliche Daten
- Excel-Dateien

### API Key Sicherheit

```
✅ DO:
- Key in local.properties speichern
- Nicht in Git committen
- Nicht teilen

❌ DON'T:
- Key im Code hardcoden
- Key in Git pushen
- Key öffentlich teilen
```

---

## 🐛 Troubleshooting

### Problem: "API Key ungültig"

**Lösung:**
1. Prüfe ob der Key richtig kopiert wurde
2. Keine Leerzeichen am Anfang/Ende
3. Key in `local.properties` richtig formatiert:
   ```
   GEMINI_API_KEY=AIzaSyC...
   ```

### Problem: "Quota exceeded"

**Lösung:**
1. Du hast 1500 Anfragen/Tag überschritten
2. Warte bis morgen (Reset um Mitternacht UTC)
3. Oder verwende einen zweiten API Key

### Problem: "Internet connection required"

**Lösung:**
1. Gemini API braucht Internet
2. Prüfe WLAN/Mobile Daten
3. Die App funktioniert offline - nur KI braucht Internet

---

## 📚 Nächste Schritte

Nach dem Setup kannst du:

1. ✅ **Urlaubsplanung nutzen**
   - KI-Optimierung
   - Brückentage finden
   - Schulferien berücksichtigen

2. ✅ **Schließtage eintragen**
   - Wann ist deine Einrichtung geschlossen?
   - Automatisch als "Pflicht-Urlaub" markieren

3. ✅ **Überstunden-Abbau planen**
   - KI hilft dir beim Abbau
   - Vorschläge für lange Wochenenden

---

## 🎓 Weitere Infos

- **Gemini API Docs:** https://ai.google.dev/docs
- **Pricing:** https://ai.google.dev/pricing (Free Tier!)
- **Support:** https://ai.google.dev/support

---

## ✅ Checkliste

Bevor du loslegst:

- [ ] API Key erstellt
- [ ] In `local.properties` gespeichert
- [ ] Gradle Sync durchgeführt
- [ ] App gebaut und getestet
- [ ] KI-Features ausprobiert

---

**🎉 Fertig! Viel Spaß mit der KI-gestützten Urlaubsplanung!**

Bei Fragen:
- Schaue in die Docs
- Teste mit kleinen Anfragen
- Der Free Tier reicht locker für persönliche Nutzung

---

*Version: 1.0 | Erstellt: November 2025*
