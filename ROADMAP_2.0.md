# 🚀 Arbeitszeit Tracker – Roadmap Version 2.0

> Ziel: Ein rundum erneuertes **2.0** auf Android mit drei Säulen –
> **neue Features**, **technische Modernisierung** und **UI/UX-Redesign**.
> Plattform bleibt vorerst Android (Wear OS/Web später möglich).

---

## 1. Ausgangslage (v1.2)

| Aspekt | Stand |
|---|---|
| Plattform | Native Android, Kotlin + Jetpack Compose |
| Architektur | MVVM · Room (Schema v26) · Hilt · WorkManager |
| Umfang | 104 Kotlin-Dateien, ~33.400 Zeilen |
| Version | versionName `1.2`, versionCode `3` |
| **Automatisierte Tests** | **0** |
| Technische Schuld | Einzelne Riesendateien (`SettingsScreen.kt` = 2.448 Zeilen), keine Modularisierung |

**Kernrisiko:** Viel Funktionalität, aber kein Testnetz. Jede große Änderung ist
heute blind. Deshalb ist Härtung des Fundaments **Phase 0**, bevor sichtbar
umgebaut wird.

---

## 2. Leitprinzipien für 2.0

1. **v1.2 einfrieren:** `git tag v1.2` als Rückfallpunkt, 2.0 in eigenem Branch.
2. **Netz vor Trapez:** Erst Tests für die kritische Kernlogik, dann umbauen.
3. **Kleine, mergebare Schritte** statt Big-Bang-Rewrite. Die App bleibt jederzeit lauffähig.
4. **Datenmigration ernst nehmen:** Schema v26 → Nutzer haben echte Daten. Kein Datenverlust, Migrationstests Pflicht.
5. **Redesign inkrementell:** Neues Design-System zuerst, dann Screen für Screen migrieren – nicht alles gleichzeitig.

---

## 3. Phasenplan

### Phase 0 — Fundament härten *(Voraussetzung für alles)*
- [ ] `v1.2` taggen, Branch `feature/v2` von `master` abzweigen
- [ ] CI aufsetzen (Build + Tests bei jedem PR, GitHub Actions)
- [ ] **Unit-Tests für die Kernlogik** (höchste Priorität, größtes Risiko):
  - Zeit-/Überstundenberechnung (`TimeEntry`, `TimeUtils`, `UeberstundenViewModel`)
  - KW-/Datumszuordnung (`DateUtils`, ISO-8601-Kalenderwochen)
  - Excel-Export/-Import (`ExcelExportManager`, `ExcelImportManager`)
  - Room-Migrationstests (v26 muss sauber migrieren)
- [ ] Große Dateien entflechten (Beginn mit `SettingsScreen.kt`)

### Phase 1 — Technische Modernisierung
- [ ] Package-Struktur → klare Schichten (`data` / `domain` / `ui`) oder Gradle-Module
- [ ] Dependency-Versionen aktualisieren (Compose BOM, Kotlin, AGP)
- [ ] Wiederkehrende UI-Muster in gemeinsame Komponenten ziehen
- [ ] Fehlerbehandlung & Logging vereinheitlichen
- [ ] Ergebnis: Codebasis, auf der Features & Redesign sicher aufsetzen

### Phase 2 — UI/UX-Redesign
- [ ] **Design-System** definieren (Farben, Typo, Spacing, Komponenten – teils vorhanden in `ui/theme`)
- [ ] Navigation überarbeiten (Informationsarchitektur, weniger verschachtelte Screens)
- [ ] Home/Dashboard als neues Herzstück
- [ ] Screen-für-Screen-Migration auf das neue Design
- [ ] Dynamic Color / Material You, Dark Mode konsistent

### Phase 3 — Neue Features
*(konkrete Auswahl → siehe Abschnitt 4, wird gemeinsam priorisiert)*

### Phase 4 — Release 2.0
- [ ] versionName `2.0`, versionCode hochzählen
- [ ] Migrations- & Regressionstests auf echten Alt-Daten
- [ ] Release-Notes, Beta-Test, dann Rollout

---

## 4. Feature-Menü für Phase 3 *(zu priorisieren)*

Vorschläge, geordnet nach Wert/Aufwand. **Bitte auswählen, was in 2.0 rein soll:**

| Feature | Nutzen | Aufwand |
|---|---|---|
| **Statistik-Dashboard** (Trends, Monat/Jahr, Überstunden-Verlauf) | Hoch | Mittel |
| **PDF-Export / Reports** (zusätzlich zu Excel) | Hoch | Mittel |
| **Echte Cloud-Sync** (statt nur Drive-Backup) | Hoch | Hoch |
| **Team-/Mehrbenutzer** (mehrere Profile, Rollen) | Mittel | Sehr hoch |
| **Lohn-/Zulagenlogik** (Zuschläge, Nacht/Wochenende) | Mittel | Mittel |
| **Wear OS Companion** (Plan existiert bereits) | Mittel | Hoch |
| **Erweiterte KI** (Auswertungen, Vorschläge via Gemini) | Mittel | Mittel |
| **Widgets 2.0 / Quick-Tiles** | Niedrig | Niedrig |

---

## 5. Nächster konkreter Schritt

Sobald die Feature-Auswahl steht:
1. `v1.2` taggen + `feature/v2`-Branch anlegen
2. CI + erste Kernlogik-Tests (Phase 0) — der sicherste erste Commit
3. Danach iterativ entlang der Phasen

*Dieses Dokument ist die lebende Roadmap und wird pro Phase fortgeschrieben.*
