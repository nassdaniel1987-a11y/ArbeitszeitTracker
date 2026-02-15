package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onStartFeatureTour: () -> Unit = {}
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    val helpSections = listOf(
        HelpSection(
            title = "🆕 Erste Schritte - Setup Wizard",
            icon = Icons.Default.Start,
            items = listOf(
                HelpItem(
                    "Setup-Assistent",
                    "Beim ersten Start führt dich ein 4-Schritte-Wizard durch die Einrichtung:\n• Willkommen & App-Vorstellung\n• Optional: Excel-Vorlage hochladen (Daten werden automatisch übernommen)\n• Benutzerdaten eingeben (Name, Einrichtung, Wochenstunden, etc.)\n• Abschluss & Start der App"
                ),
                HelpItem(
                    "Excel-Import im Setup",
                    "Lade deine Excel-Arbeitszeit-Vorlage hoch:\n• Automatisches Auslesen von Name, Einrichtung, Wochenstunden\n• Arbeitszeiten werden direkt importiert\n• Kann übersprungen werden für manuelle Eingabe"
                ),
                HelpItem(
                    "Was wird beim Setup angelegt?",
                    "Der Wizard erstellt automatisch:\n• Deine persönlichen Einstellungen (UserSettings)\n• Das aktuelle Jahr (z.B. 2025) mit allen Standardwerten\n• Optional: Importierte Zeiteinträge aus Excel"
                )
            )
        ),
        HelpSection(
            title = "🏠 Startseite & Schnellzugriff",
            icon = Icons.Default.Home,
            items = listOf(
                HelpItem(
                    "Schnellaktionen-Menü (FAB)",
                    "Das schwebende Aktions-Menü unten rechts bietet direkten Zugriff auf:\n• Stempeln (Start/Ende erfassen)\n• Urlaub eintragen\n• Krank eintragen\n• Feiertag eintragen\n• Abwesend eintragen"
                ),
                HelpItem(
                    "Heutiger Eintrag-Card",
                    "Die oberste Card zeigt den heutigen Tag:\n• Start-Zeit antippen zum Ändern\n• End-Zeit antippen zum Ändern\n• Pause-Icon für Pause-Slider (0-120 Min)\n• Schnellauswahl: 0, 15, 30, 45, 60 Minuten\n• Typ-Wechsel über Dropdown"
                ),
                HelpItem(
                    "Geofencing-Status",
                    "Status-Card zeigt aktuelle Standort-Erfassung:\n• Zeigt aktiven Standort und Status\n• Aktualisieren-Button für manuelle Prüfung\n• Grün = Aktiv, Grau = Inaktiv"
                ),
                HelpItem(
                    "Wochen-Statistik Dashboard",
                    "Zeigt Live-Übersicht der aktuellen Woche:\n• Ist-Arbeitszeit vs. Soll-Arbeitszeit\n• Verbleibende Arbeitstage bis Wochenende\n• Prognose für Wochen-Überstunden\n• Visueller Fortschrittsbalken"
                ),
                HelpItem(
                    "Wochenansicht mit Navigation",
                    "Navigiere durch Wochen mit Pfeiltasten:\n• Zeigt KW-Nummer und Datumsbereich\n• 'Zur aktuellen Woche' bei anderen Wochen\n• 'Vorlage anwenden' für Dienstplan-Vorlagen\n• Dropdown zur Auswahl von Arbeitszeitvorlagen\n• Long-Press auf Eintrag zum Löschen mit Bestätigung"
                ),
                HelpItem(
                    "Wochen-Zusammenfassung",
                    "Am Ende jeder Woche:\n• Gesamt Ist-/Soll-Zeit der Woche\n• Überstunden/Minusstunden\n• Farbcodierte Darstellung"
                )
            )
        ),
        HelpSection(
            title = "📅 Kalenderansicht (Android-Stil)",
            icon = Icons.Default.CalendarToday,
            items = listOf(
                HelpItem(
                    "Swipe-Navigation",
                    "Moderne Monatsnavigation wie im Google Kalender:\n• Nach links/rechts swipen für Monatswechsel\n• Flüssige Animationen zwischen Monaten\n• 'Heute'-Button zum schnellen Zurückspringen"
                ),
                HelpItem(
                    "Große, lesbare Zellen",
                    "Der Kalender nutzt den gesamten Bildschirm:\n• Große Tag-Nummern (18sp)\n• Event-Balken mit Labels (Urlaub, Krank, etc.)\n• Arbeitszeiten direkt sichtbar (z.B. 8:00-16:00)\n• Differenz-Anzeige (+2h oder -1h)"
                ),
                HelpItem(
                    "Tag bearbeiten",
                    "Auf Datum tippen für Details und Bearbeitung:\n• Start-/End-Zeit ändern\n• Pause einstellen\n• Typ wechseln (Normal/Urlaub/Krank/etc.)\n• Notizen hinzufügen"
                ),
                HelpItem(
                    "Event-Balken (Android-Kalender-Stil)",
                    "Farbige Balken zeigen den Status jedes Tages:\n• Grün (✓) = Vollständig gearbeitet\n• Orange (~) = Teilweise gearbeitet\n• Cyan (Urlaub) = Urlaubstag\n• Rot (Krank) = Krankheitstag\n• Indigo (Feiertag) = Feiertagstyp\n• Gold (⭐) = Gesetzlicher Feiertag"
                ),
                HelpItem(
                    "Legende",
                    "Am unteren Rand des Kalenders:\n• Erklärt alle Farben und Symbole\n• 2-spaltige Übersicht\n• Farbige Balken wie im Kalender\n• Immer sichtbar zur Orientierung"
                ),
                HelpItem(
                    "Arbeitszeit-Anzeige",
                    "Direkt unter dem Event-Balken:\n• Start-Ende-Zeit (z.B. 8:00-16:00)\n• Nur Start oder nur Ende möglich\n• Differenz zu Soll-Zeit (+2h / -1h)\n• Farbcodiert: Grün (Plus) / Rot (Minus)"
                ),
                HelpItem(
                    "Heute-Markierung",
                    "Der heutige Tag ist besonders gekennzeichnet:\n• Blauer Kreis um die Tag-Nummer\n• Leichte Elevation (hebt sich ab)\n• Bold-Schrift für bessere Sichtbarkeit"
                ),
                HelpItem(
                    "Feiertags-Balken",
                    "Goldener Balken über dem Event-Balken:\n• Zeigt gesetzliche Feiertage\n• Basiert auf deinem Bundesland\n• Automatische Erkennung\n• In Einstellungen konfigurierbar"
                )
            )
        ),
        HelpSection(
            title = "⏰ Wochen-Vorlagen (Dienstplan)",
            icon = Icons.Default.ContentCopy,
            items = listOf(
                HelpItem(
                    "Vorlagen erstellen",
                    "Erstelle individuelle Dienstplan-Vorlagen:\n• Name vergeben (z.B. 'Frühdienst', 'Spätdienst')\n• Start-/End-Zeit für jeden Wochentag (Mo-So)\n• Pause pro Tag definieren\n• Mehrere Vorlagen möglich"
                ),
                HelpItem(
                    "Vorlage anwenden",
                    "Zwei Wege zum Anwenden:\n1. HomeScreen → Wochennavigation → 'Vorlage anwenden'\n2. Mehr-Menü (⋮) → Wochen-Vorlagen\n• Wähle Vorlage aus Liste\n• Wähle Zeitraum (Woche/Monat)\n• Bestehende Einträge optional überschreiben"
                ),
                HelpItem(
                    "Beispiel-Vorlagen",
                    "Typische Anwendungsfälle:\n• Frühdienst: 6:00-14:00 Uhr\n• Spätdienst: 14:00-22:00 Uhr\n• Nachtdienst: 22:00-6:00 Uhr\n• Büro: 9:00-17:00 Uhr\n• Teilzeit: Nur Mo-Do"
                ),
                HelpItem(
                    "Vorlagen verwalten",
                    "In der Vorlagen-Übersicht:\n• Bearbeiten durch Antippen\n• Löschen durch Wischen\n• Neue Vorlage mit +-Button"
                )
            )
        ),
        HelpSection(
            title = "📊 Arbeitszeitvorlagen (SollZeitVorlage)",
            icon = Icons.Default.Layers,
            items = listOf(
                HelpItem(
                    "Was sind Arbeitszeitvorlagen?",
                    "ACHTUNG: Nicht verwechseln mit Wochen-Vorlagen!\n• Definieren unterschiedliche SOLL-Zeiten pro Wochentag\n• Nicht die tatsächlichen Start-/End-Zeiten\n• Beispiel: Mo-Do 8h, Fr 6h, Sa-So 0h\n• Wichtig für individuelle Arbeitszeitmodelle"
                ),
                HelpItem(
                    "Vorlage erstellen",
                    "In Einstellungen → Arbeitszeitvorlagen:\n• Name vergeben (z.B. 'Standard', 'Reduziert')\n• Soll-Stunden pro Tag (Mo-So)\n• Standard-Vorlage markieren\n• Mehrere Vorlagen für verschiedene Wochen"
                ),
                HelpItem(
                    "Vorlage auf Woche anwenden",
                    "In der Wochennavigation:\n• Dropdown-Menü unter KW-Anzeige\n• Wähle Arbeitszeitvorlage\n• Gilt für die gesamte Woche\n• Zeigt verwendete Vorlage an"
                ),
                HelpItem(
                    "Standard-Vorlage",
                    "Mit Stern ⭐ markiert:\n• Wird für neue Wochen verwendet\n• Kann jederzeit geändert werden\n• Alle Wochen individuell anpassbar"
                )
            )
        ),
        HelpSection(
            title = "📈 Überstunden-Tracking",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            items = listOf(
                HelpItem(
                    "Gesamt-Übersicht",
                    "Kumulierte Überstunden über alle Monate:\n• Gesamtsaldo (Plus/Minus)\n• Visueller Fortschrittsbalken\n• Farbcodiert: Grün = Plus, Rot = Minus"
                ),
                HelpItem(
                    "Monatliche Aufschlüsselung",
                    "Detaillierte Monatsansicht:\n• Soll-Stunden vs. Ist-Stunden\n• Differenz pro Monat\n• Kumulativer Überstundenverlauf\n• Durchschnitt pro Arbeitstag"
                ),
                HelpItem(
                    "Vorjahresübertrag",
                    "Automatische Berücksichtigung:\n• In Einstellungen konfigurierbar\n• Wird zum Gesamtsaldo addiert\n• Sichtbar in der Übersicht"
                ),
                HelpItem(
                    "Statistiken",
                    "Zusätzliche Auswertungen:\n• Durchschnittliche Tagesarbeitszeit\n• Längster/kürzester Arbeitstag\n• Urlaubstage genommen\n• Krankheitstage"
                )
            )
        ),
        HelpSection(
            title = "📍 Geofencing (Auto-Zeiterfassung)",
            icon = Icons.Default.LocationOn,
            items = listOf(
                HelpItem(
                    "Standorte anlegen",
                    "GPS-basierte Arbeitsorte definieren:\n• Name des Standorts (z.B. 'Büro', 'Baustelle A')\n• Adresse oder GPS-Koordinaten\n• Radius in Metern (z.B. 100m)\n• Karten-Vorschau zur Kontrolle"
                ),
                HelpItem(
                    "Auto-Start/Ende",
                    "Automatische Zeiterfassung:\n• Start: Beim Betreten des Standorts\n• Ende: Beim Verlassen des Standorts\n• Benachrichtigung bei Erfassung\n• Einträge können nachträglich angepasst werden"
                ),
                HelpItem(
                    "Zeitfenster einstellen",
                    "Nur innerhalb bestimmter Uhrzeiten aktiv:\n• Start-Uhrzeit (z.B. 6:00 Uhr)\n• End-Uhrzeit (z.B. 20:00 Uhr)\n• Verhindert ungewollte Erfassung nachts\n• Pro Standort individuell"
                ),
                HelpItem(
                    "Arbeitstage wählen",
                    "Nur an ausgewählten Wochentagen aktiv:\n• Montag bis Sonntag einzeln wählbar\n• Beispiel: Nur Mo-Fr für Büro\n• Spart Akku am Wochenende\n• Pro Standort unterschiedlich"
                ),
                HelpItem(
                    "Berechtigungen",
                    "Erforderliche Einstellungen:\n• Standort-Berechtigung 'Immer'\n• Batterie-Optimierung deaktivieren\n• Android 10+: Hintergrund-Standort\n• Einmalig in Android-Einstellungen nötig"
                )
            )
        ),
        HelpSection(
            title = "📄 Excel Export & Import",
            icon = Icons.Default.FileDownload,
            items = listOf(
                HelpItem(
                    "Export-Vorschau",
                    "Vor dem Export Daten prüfen:\n• Zeigt zu exportierende Einträge\n• Datumsbereich wählen\n• Vorschau der Tabelle\n• 'Exportieren' bestätigt"
                ),
                HelpItem(
                    "Export mit Excel-Vorlage",
                    "Nutzt deine hochgeladene Vorlage:\n• In 'Excel-Vorlagen' Vorlage hochladen\n• Pro Jahr eine Vorlage\n• Automatische Verwendung beim Export\n• Überstunden aus Vorjahr bleiben erhalten"
                ),
                HelpItem(
                    "Einfacher Export",
                    "Export ohne Vorlage:\n• Standard-Layout in Wochenblöcken\n• Spalten: Datum, Start, Ende, Pause, Ist, Soll\n• Überstunden pro Tag\n• Monatszusammenfassung"
                ),
                HelpItem(
                    "Import aus Excel",
                    "Daten wieder einlesen:\n• Excel-Datei auswählen\n• Option: Mit/ohne Stammdaten\n• Stammdaten = Name, Einrichtung, etc.\n• Bestehende Daten optional überschreiben"
                ),
                HelpItem(
                    "Dateiname",
                    "Automatisch generiert:\n• Format: Name_Einrichtung_Jahr.xlsx\n• Beispiel: MaxMustermann_KitaSonnenschein_2025.xlsx\n• Gespeichert in Downloads-Ordner"
                )
            )
        ),
        HelpSection(
            title = "📝 Excel-Vorlagen verwalten",
            icon = Icons.Default.Upload,
            items = listOf(
                HelpItem(
                    "Vorlage hochladen",
                    "Eigene Excel-Vorlage verwenden:\n• Button 'Neue Vorlage hochladen'\n• Excel-Datei auswählen (.xlsx)\n• Jahr zuweisen (z.B. 2025, 2026)\n• Wird beim Export automatisch verwendet"
                ),
                HelpItem(
                    "Jahr-basierte Vorlagen",
                    "Pro Jahr eine Vorlage:\n• 2024-Vorlage für 2024-Exporte\n• 2025-Vorlage für 2025-Exporte\n• Aktuelles Jahr markiert mit ✓\n• Standard-Vorlage als Fallback"
                ),
                HelpItem(
                    "Vorlagen löschen",
                    "Ungenutzte Vorlagen entfernen:\n• Löschen-Icon auf Vorlage\n• Bestätigung erforderlich\n• Standard-Vorlage wird dann verwendet"
                ),
                HelpItem(
                    "Vorlagen-Struktur",
                    "Deine Excel-Vorlage sollte enthalten:\n• Platzhalter für Daten (werden ersetzt)\n• Formatierung bleibt erhalten\n• Formeln werden respektiert\n• Mehrere Tabellenblätter möglich"
                )
            )
        ),
        HelpSection(
            title = "🏖️ Urlaubsplaner (mit KI)",
            icon = Icons.Default.BeachAccess,
            items = listOf(
                HelpItem(
                    "Übersicht-Tab",
                    "Deine Urlaubsübersicht:\n• Verfügbare Urlaubstage (Jahresanspruch)\n• Bereits genommene Urlaubstage\n• Verbleibende Urlaubstage\n• Countdown zum nächsten Urlaub\n• Resturlaub-Warnung bei wenig Tagen"
                ),
                HelpItem(
                    "Planen-Tab (Interaktiver Kalender)",
                    "Urlaub direkt eintragen:\n• Auf Tag tippen → Urlaub eintragen\n• Nochmal tippen → Urlaub entfernen\n• Mehrere Tage durch Antippen\n• Feiertage automatisch markiert ⭐\n• Schließtage automatisch berücksichtigt\n• Jahr wechseln mit Pfeilen (◀ 2026 ▶)"
                ),
                HelpItem(
                    "Brückentage-Tab (KI-Optimierung)",
                    "Intelligente Urlaubsplanung:\n• Gemini AI analysiert Feiertage, Schulferien & Schließtage\n• Berechnet effizienteste Brückentage\n• Effizienz-Score: z.B. '1 Tag Urlaub → 4 Tage frei'\n• Alle Jahre verfügbar (2025, 2026, ...)\n• Sortiert nach Effizienz (beste zuerst)\n• Auf Brückentag tippen → Automatisch eintragen"
                ),
                HelpItem(
                    "Bundesland-spezifische Feiertage",
                    "Präzise Planung:\n• In Einstellungen Bundesland wählen\n• Korrekte Feiertage im Kalender\n• Schulferien-Berücksichtigung\n• Goldener Stern ⭐ im Kalender"
                ),
                HelpItem(
                    "Schließtage-Integration",
                    "Automatisch berücksichtigt:\n• Schließtage werden nicht als Urlaubstage gezählt\n• Angezeigt im Urlaubskalender\n• KI nutzt Schließtage für Brückentage\n• In 'Schließtage' verwalten"
                )
            )
        ),
        HelpSection(
            title = "🔒 Schließtage-Verwaltung",
            icon = Icons.Default.Lock,
            items = listOf(
                HelpItem(
                    "Was sind Schließtage?",
                    "Tage, an denen deine Einrichtung geschlossen ist:\n• Weihnachtsferien\n• Sommerferien\n• Betriebsferien\n• Brückentage der Einrichtung\n• Werden bei Urlaubsplanung automatisch abgezogen"
                ),
                HelpItem(
                    "Schließtag hinzufügen",
                    "Mit +-Button neuen Schließtag anlegen:\n• Bezeichnung (z.B. 'Weihnachtsferien 2025')\n• Startdatum (yyyy-MM-dd)\n• Enddatum (yyyy-MM-dd)\n• Optionale Notiz\n• Zeigt Anzahl der Tage an"
                ),
                HelpItem(
                    "Jahr-Navigation",
                    "Wechsel zwischen Jahren:\n• Pfeile < > zum Navigieren\n• Jahr-Anzeige in der Mitte\n• Nur relevante Schließtage\n• Vergangene Jahre zur Dokumentation"
                ),
                HelpItem(
                    "Schließtage bearbeiten/löschen",
                    "Verwaltung der Schließtage:\n• Bearbeiten-Icon zum Ändern\n• Löschen-Icon mit Bestätigung\n• Automatische Aktualisierung der Urlaubsplanung"
                ),
                HelpItem(
                    "Integration mit Urlaubsplaner",
                    "Automatische Berücksichtigung:\n• Schließtage zählen nicht als Urlaubstage\n• KI nutzt Schließtage für optimale Brückentage\n• Anzeige im Urlaubskalender\n• Keine manuelle Anpassung nötig"
                )
            )
        ),
        HelpSection(
            title = "⚙️ Einstellungen",
            icon = Icons.Default.Settings,
            items = listOf(
                HelpItem(
                    "Persönliche Daten",
                    "Für Excel-Exporte:\n• Vorname & Nachname\n• Einrichtung/Firma\n• Erscheint in exportierten Dateien\n• Optional: E-Mail, Personalnummer"
                ),
                HelpItem(
                    "Arbeitszeit-Konfiguration",
                    "Deine Arbeitszeitmodelle:\n• Wochenstunden (z.B. 40h)\n• Anzahl Arbeitstage (z.B. 5)\n• Arbeitsumfang in % (z.B. 100%, 75%)\n• Berechnet automatisch Tages-Sollzeit"
                ),
                HelpItem(
                    "Arbeitstage wählen",
                    "Individuelle Arbeitstage:\n• Montag bis Sonntag einzeln wählbar\n• Standard: Mo-Fr\n• Auch Sa/So für Schichtdienst\n• Beeinflusst Soll-Berechnung"
                ),
                HelpItem(
                    "Arbeitszeitvorlagen",
                    "Individuelle Sollzeiten pro Wochentag:\n• Unterschiedliche Stunden Mo-So\n• Mehrere Vorlagen erstellen\n• Standard-Vorlage festlegen\n• Pro Woche anwendbar"
                ),
                HelpItem(
                    "Bundesland",
                    "Für Feiertage & Schulferien:\n• 16 Bundesländer wählbar\n• Bundesland-spezifische Feiertage\n• Schulferien für KI-Urlaubsplanung\n• Automatische Aktualisierung"
                ),
                HelpItem(
                    "Urlaubsanspruch",
                    "Jahresurlaub in Tagen:\n• Standard: 30 Tage\n• Individuell anpassbar\n• Teilzeit-Anpassung\n• Wird im Urlaubsplaner verwendet"
                ),
                HelpItem(
                    "Vorjahresübertrag",
                    "Überstunden aus dem Vorjahr:\n• Plus- oder Minusstunden\n• In Stunden und Minuten\n• Wird zu Gesamtüberstunden addiert\n• Jährlich aktualisierbar"
                ),
                HelpItem(
                    "Design",
                    "App-Erscheinungsbild:\n• Hell-Modus\n• Dunkel-Modus\n• System (folgt Geräteeinstellung)\n• Material You Design (Android 12+)"
                ),
                HelpItem(
                    "Kalenderwoche",
                    "KW-Berechnung einstellen:\n• ISO 8601 Standard (empfohlen)\n• Custom: Erster Montag im Jahr\n• Wichtig für korrekte Wochennummern\n• Beeinflusst Wochenansicht"
                ),
                HelpItem(
                    "Ferienbetreuung (Spezial)",
                    "Für Ferienbetreuungs-Einrichtungen:\n• Spezielle Berechnungslogik\n• Nur während Schulferien\n• Automatische Soll-Anpassung\n• Nur aktivieren wenn zutreffend"
                )
            )
        ),
        HelpSection(
            title = "🏷️ Eintragstypen",
            icon = Icons.Default.Category,
            items = listOf(
                HelpItem(
                    "Normal",
                    "Regulärer Arbeitstag:\n• Mit Start-Zeit, End-Zeit, Pause\n• Ist-Zeit wird berechnet\n• Vergleich mit Soll-Zeit\n• Standard-Eintragstyp"
                ),
                HelpItem(
                    "Urlaub (U)",
                    "Urlaubstag:\n• Zählt als Sollzeit erfüllt\n• Keine Eingabe von Zeiten nötig\n• Wird vom Urlaubsanspruch abgezogen\n• Blaue Farbe im Kalender"
                ),
                HelpItem(
                    "Krank (K)",
                    "Krankheitstag:\n• Zählt als Sollzeit erfüllt\n• Keine Zeiten erforderlich\n• Statistik separat\n• Blaue Farbe im Kalender"
                ),
                HelpItem(
                    "Feiertag (F)",
                    "Gesetzlicher Feiertag:\n• Zählt als Sollzeit erfüllt\n• Automatisch erkannt (wenn Bundesland gesetzt)\n• Goldener Stern ⭐ im Kalender\n• Blaue Farbe"
                ),
                HelpItem(
                    "Abwesend (AB)",
                    "Sonstige Abwesenheit:\n• Fortbildung, Dienstreise, etc.\n• Zählt als Sollzeit erfüllt\n• Keine Zeiten nötig\n• Blaue Farbe im Kalender"
                )
            )
        ),
        HelpSection(
            title = "💡 Tipps & Tricks",
            icon = Icons.Default.Lightbulb,
            items = listOf(
                HelpItem(
                    "Schnellzugriff nutzen",
                    "Zeitsparende Shortcuts:\n• Schnellaktionen-FAB für häufige Aktionen\n• Long-Press auf Einträge zum schnellen Löschen\n• Pause-Slider mit Schnellauswahl (0/15/30/45/60)\n• Dropdown-Menüs für Vorlagen"
                ),
                HelpItem(
                    "Undo-Funktion",
                    "Fehler rückgängig machen:\n• Nach Löschen erscheint Snackbar\n• 'Rückgängig' klicken innerhalb weniger Sekunden\n• Eintrag wird wiederhergestellt\n• Auch bei versehentlichem Wischen"
                ),
                HelpItem(
                    "Haptisches Feedback",
                    "Vibrations-Rückmeldung:\n• Bei Long-Press\n• Bei wichtigen Aktionen\n• Bestätigt Eingaben\n• In Android-Einstellungen konfigurierbar"
                ),
                HelpItem(
                    "Dienstplan effizient nutzen",
                    "Vorlagen für Schichtdienst:\n• Erstelle Vorlagen für alle Schichten\n• Frühdienst, Spätdienst, Nachtdienst\n• Mit einem Klick ganze Woche/Monat füllen\n• Spart viel Zeit bei Wechselschicht"
                ),
                HelpItem(
                    "Arbeitszeitvorlagen clever einsetzen",
                    "Für individuelle Arbeitszeitmodelle:\n• Standard-Vorlage für normale Wochen\n• Reduzierte Vorlage für Teilzeit-Wochen\n• Urlaubswochen mit angepassten Sollzeiten\n• Überstunden-Tracking bleibt präzise"
                ),
                HelpItem(
                    "KI-Urlaubsplaner optimal nutzen",
                    "Maximiere deine freien Tage:\n1. Bundesland in Einstellungen setzen\n2. Schließtage eintragen\n3. Brückentage-Tab öffnen\n4. Beste Optionen anschauen\n5. Urlaub direkt eintragen\n6. Bis zu 4 freie Tage mit 1 Urlaubstag!"
                ),
                HelpItem(
                    "Schließtage pflegen",
                    "Für präzise Urlaubsplanung:\n• Zu Jahresbeginn alle Schließtage eintragen\n• KI nutzt sie automatisch\n• Keine manuellen Abzüge nötig\n• Urlaubs-Kalkulation korrekt"
                ),
                HelpItem(
                    "Export-Backup regelmäßig",
                    "Datensicherheit:\n• Monatlich Excel-Export\n• Sichere Datei auf Cloud/PC\n• Bei Handy-Verlust: Import möglich\n• Mit Stammdaten exportieren"
                ),
                HelpItem(
                    "Geofencing richtig konfigurieren",
                    "Für zuverlässige Auto-Erfassung:\n• Radius großzügig wählen (min. 100m)\n• Zeitfenster setzen (verhindert nachts)\n• Nur Arbeitstage aktivieren\n• Batterie-Optimierung aus!\n• Standort-Berechtigung 'Immer'"
                ),
                HelpItem(
                    "Überstunden im Blick",
                    "Regelmäßig prüfen:\n• Überstunden-Screen öffnen\n• Monatlichen Verlauf beobachten\n• Bei zu vielen Plus-Stunden: Überstundenabbau planen\n• Bei Minus-Stunden: Arbeitszeit erhöhen"
                ),
                HelpItem(
                    "Wochenstatistik zur Planung",
                    "Auf der Startseite:\n• Zeigt Prognose für Wochenende\n• Hilft bei Planung zusätzlicher Stunden\n• Verbleibende Arbeitstage nutzen\n• Ziel: Woche ausgeglichen beenden"
                ),
                HelpItem(
                    "Excel-Vorlagen pro Jahr",
                    "Für professionelle Exporte:\n• Lade Excel-Vorlage mit Firmen-Layout hoch\n• Pro Jahr eine Vorlage\n• Automatische Verwendung\n• Überstunden aus Vorjahr bleiben erhalten\n• Export mit Corporate Design"
                )
            )
        ),
        HelpSection(
            title = "🔧 Häufige Probleme & Lösungen",
            icon = Icons.Default.Build,
            items = listOf(
                HelpItem(
                    "Geofencing funktioniert nicht",
                    "Prüfe folgende Einstellungen:\n1. Standort-Berechtigung auf 'Immer' (nicht nur 'Bei App-Nutzung')\n2. Batterie-Optimierung für App deaktivieren\n3. Standort-Dienste aktiviert\n4. Radius groß genug (mind. 100m)\n5. Zeitfenster und Arbeitstage richtig\n6. Android-Version 10+: Extra-Berechtigung"
                ),
                HelpItem(
                    "Überstunden stimmen nicht",
                    "Mögliche Ursachen:\n• Soll-Zeit falsch konfiguriert (Einstellungen)\n• Arbeitstage nicht korrekt (z.B. Sa/So)\n• Vorjahresübertrag vergessen\n• Arbeitszeitvorlage falsch zugewiesen\n• Pause nicht eingetragen"
                ),
                HelpItem(
                    "Feiertage fehlen",
                    "So beheben:\n• Einstellungen → Bundesland prüfen\n• Korrektes Bundesland auswählen\n• App neu starten\n• Feiertage erscheinen automatisch\n• Oder manuell als Feiertag eintragen"
                ),
                HelpItem(
                    "Excel-Export leer/fehlerhaft",
                    "Lösungsschritte:\n• Vorschau vor Export prüfen\n• Zeitraum korrekt gewählt?\n• Bei eigener Vorlage: Vorlage korrekt?\n• Ohne Vorlage exportieren (Standard)\n• Berechtigungen für Speicher erlauben"
                ),
                HelpItem(
                    "Wochennummer falsch",
                    "Einstellung anpassen:\n• Einstellungen → Kalenderwoche\n• ISO 8601 Standard (empfohlen)\n• Oder: Custom mit erstem Montag\n• Datum des ersten Montags eingeben"
                ),
                HelpItem(
                    "KI-Brückentage laden nicht",
                    "Prüfen:\n• Internet-Verbindung aktiv?\n• Bundesland in Einstellungen gesetzt\n• Gemini API verfügbar (Server-Problem?)\n• Später erneut versuchen\n• Brückentage manuell eintragen möglich"
                )
            )
        ),
        HelpSection(
            title = "ℹ️ Über die App",
            icon = Icons.Default.Info,
            items = listOf(
                HelpItem(
                    "Features auf einen Blick",
                    "Vollständiger Arbeitszeiterfassungs-Assistent:\n✅ Zeitstempel für Start/Ende/Pause\n✅ GPS-basierte Auto-Erfassung (Geofencing)\n✅ Wochen- und Monatsübersicht\n✅ Überstunden-Tracking mit Verlauf\n✅ Urlaubsplaner mit KI-Optimierung\n✅ Brückentage automatisch berechnen\n✅ Schließtage-Verwaltung\n✅ Excel Export/Import mit Vorlagen\n✅ Dienstplan-Vorlagen (Schichtdienst)\n✅ Arbeitszeitvorlagen (individuelle Sollzeiten)\n✅ Feiertage nach Bundesland\n✅ Dark Mode & Material Design\n✅ Offline-fähig (außer KI-Features)"
                ),
                HelpItem(
                    "Datenschutz",
                    "Deine Daten bleiben auf deinem Gerät:\n• Lokale Datenbank (Room)\n• Keine Cloud-Synchronisation\n• Keine Registrierung nötig\n• GPS nur für Geofencing\n• Export-Dateien lokal gespeichert\n• KI-Anfragen anonym (nur Feiertage/Ferien)\n• Quellcode einsehbar (Open Source)"
                ),
                HelpItem(
                    "Support & Feedback",
                    "Brauchst du Hilfe?\n• Diese Hilfeseite durchsuchen\n• GitHub: Issues & Feature Requests\n• E-Mail: [Entwickler-Kontakt]\n• Bewertung im Play Store hilft\n• Updates regelmäßig installieren"
                ),
                HelpItem(
                    "Credits",
                    "Verwendete Technologien:\n• Kotlin & Jetpack Compose\n• Room Database\n• Google Gemini AI\n• Material Design 3\n• Apache POI (Excel)\n• OSS-Lizenzen in App"
                )
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hilfe & Funktionen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Willkommens-Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "Arbeitszeit Tracker",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Dein vollständiger Assistent",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        Text(
                            "Willkommen in der umfassenden Hilfe! Hier findest du alles über die App: Von Basis-Funktionen über fortgeschrittene Features bis hin zu Profi-Tipps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Schnellzugriff-Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "🎯 13 Bereiche",
                                "💡 80+ Tipps",
                                "🚀 Pro-Features"
                            ).forEach { badge ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        badge,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Version und Datum
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Version 3.0.0",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "Aktualisiert: ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Suchhinweis
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Tipp: Klicke auf die Bereiche unten, um Details zu sehen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Feature-Tour Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    onClick = onStartFeatureTour
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Feature-Tour starten",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Lass dich durch alle wichtigen Funktionen der App führen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Hilfe-Sections
            items(helpSections) { section ->
                HelpSectionCard(
                    section = section,
                    isExpanded = expandedSection == section.title,
                    onToggle = {
                        expandedSection = if (expandedSection == section.title) null else section.title
                    }
                )
            }

            // Abschluss-Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            "Viel Erfolg mit der App!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Bei Fragen oder Problemen: Schau in 'Häufige Probleme' oder kontaktiere den Support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Abstand am Ende
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    section: HelpSection,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isExpanded) {
                            Text(
                                "${section.items.size} Themen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    section.items.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 2.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class HelpSection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val items: List<HelpItem>
)

data class HelpItem(
    val title: String,
    val description: String
)
