package com.arbeitszeit.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
    onNavigateBack: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    val helpSections = listOf(
        HelpSection(
            title = "Schnellstempel & Navigation",
            icon = Icons.Default.TouchApp,
            items = listOf(
                HelpItem("Stempeln-Button", "Erfasst sofort Start- oder Endzeit für heute"),
                HelpItem("Schnellaktionen (⚡)", "Blitz-Icon in TopBar: Urlaub, Krank, Feiertag, Abwesend eintragen"),
                HelpItem("Mehr-Menü (⋮)", "Drei-Punkte-Icon: Wochen-Vorlagen und Hilfe"),
                HelpItem("Pause-Slider", "Pausenzeit einfach per Slider einstellen (0-120 Min)")
            )
        ),
        HelpSection(
            title = "Kalenderansicht",
            icon = Icons.Default.CalendarToday,
            items = listOf(
                HelpItem("Monatswechsel", "Swipen oder Pfeile nutzen zum Wechseln"),
                HelpItem("Eintrag bearbeiten", "Auf Datum tippen zum Bearbeiten"),
                HelpItem("Eintrag löschen", "Nach links wischen für Löschen mit Undo-Funktion"),
                HelpItem("Farbcodierung", "Grün = Soll erfüllt, Orange = Teilzeit, Rot = Fehlzeit, Blau = Urlaub/Krank/Feiertag")
            )
        ),
        HelpSection(
            title = "Wochen-Vorlagen",
            icon = Icons.Default.ContentCopy,
            items = listOf(
                HelpItem("Vorlage erstellen", "Erstelle Dienstplan-Vorlagen mit individuellen Zeiten für jeden Wochentag"),
                HelpItem("Dienstplan eingeben", "Beim Erstellen: Start-Zeit, End-Zeit und Pause für Mo-So eingeben"),
                HelpItem("Vorlage anwenden", "Im HomeScreen: Button 'Vorlage anwenden' in der Wochennavigation"),
                HelpItem("Schneller Zugriff", "Über Mehr-Menü (⋮) → Wochen-Vorlagen"),
                HelpItem("Vorlage löschen", "Ungenutzte Vorlagen entfernen"),
                HelpItem("Mehrere Vorlagen", "Erstelle verschiedene Vorlagen (z.B. Frühdienst 6-14 Uhr, Spätdienst 14-22 Uhr)")
            )
        ),
        HelpSection(
            title = "Überstunden-Tracking",
            icon = Icons.Default.TrendingUp,
            items = listOf(
                HelpItem("Gesamtübersicht", "Zeigt kumulierte Überstunden/Minusstunden"),
                HelpItem("Monatliche Aufschlüsselung", "Jeder Monat mit Soll/Ist-Vergleich"),
                HelpItem("Vorjahresübertrag", "Automatische Berücksichtigung des Übertrags"),
                HelpItem("Fortschrittsbalken", "Visueller Vergleich der Sollzeit-Erfüllung")
            )
        ),
        HelpSection(
            title = "Geofencing (Auto-Zeiterfassung)",
            icon = Icons.Default.LocationOn,
            items = listOf(
                HelpItem("Standorte anlegen", "GPS-Koordinaten + Radius für Arbeitsorte festlegen"),
                HelpItem("Auto-Start", "Beginnt Zeiterfassung beim Betreten des Standorts"),
                HelpItem("Auto-Ende", "Beendet Zeiterfassung beim Verlassen"),
                HelpItem("Zeitfenster", "Nur innerhalb definierter Uhrzeiten aktiv"),
                HelpItem("Arbeitstage", "Nur an ausgewählten Wochentagen aktiv")
            )
        ),
        HelpSection(
            title = "Excel Export & Import",
            icon = Icons.Default.FileDownload,
            items = listOf(
                HelpItem("Export Vorschau", "Vor dem Export eine Übersicht der Daten anzeigen"),
                HelpItem("Template-Export", "Nutzt Excel-Vorlage aus Vorlagen-Verwaltung"),
                HelpItem("Einfacher Export", "Export ohne Vorlage in Wochenblöcken"),
                HelpItem("Import", "Excel-Dateien wieder einlesen mit Option für Stammdaten"),
                HelpItem("Dateiname", "Automatisch: Name_Einrichtung_Jahr.xlsx")
            )
        ),
        HelpSection(
            title = "Einstellungen",
            icon = Icons.Default.Settings,
            items = listOf(
                HelpItem("Persönliche Daten", "Name und Einrichtung für Exporte"),
                HelpItem("Arbeitszeit", "Wochenstunden, Arbeitstage, Arbeitsumfang in %"),
                HelpItem("Arbeitstage", "Wähle deine individuellen Arbeitstage (Mo-So)"),
                HelpItem("Individuelle Sollzeiten", "Unterschiedliche Soll-Stunden pro Wochentag"),
                HelpItem("Dark Mode", "Hell, Dunkel oder System-Einstellung"),
                HelpItem("Ferienbetreuung", "Spezielle Berechnung für Ferienbetreuung"),
                HelpItem("Kalenderwoche", "Custom KW-Start oder ISO 8601 Standard")
            )
        ),
        HelpSection(
            title = "Eintragstypen",
            icon = Icons.Default.Category,
            items = listOf(
                HelpItem("Normal", "Regulärer Arbeitstag mit Start/Ende/Pause"),
                HelpItem("Urlaub", "Urlaubstag (zählt als Sollzeit erfüllt)"),
                HelpItem("Krank", "Krankheitstag (zählt als Sollzeit erfüllt)"),
                HelpItem("Feiertag", "Gesetzlicher Feiertag (zählt als Sollzeit erfüllt)"),
                HelpItem("Bereitschaft", "Bereitschaftsdienst (separates Zeitkonto)")
            )
        ),
        HelpSection(
            title = "Tipps & Tricks",
            icon = Icons.Default.Lightbulb,
            items = listOf(
                HelpItem("Schnellzugriff TopBar", "⚡ für Typen-Eingabe, ⋮ für Vorlagen und Hilfe"),
                HelpItem("Undo-Funktion", "Nach Löschen erscheint Snackbar mit Rückgängig-Option"),
                HelpItem("Wochenansicht", "Im Kalender: Nur deine Arbeitstage werden angezeigt"),
                HelpItem("Vorlagen-Button", "In der Wochennavigation: 'Vorlage anwenden' für schnelle Wocheneintragung"),
                HelpItem("Dienstplan-Vorlagen", "Erstelle Vorlagen für Früh-/Spät-/Nachtdienst mit festen Zeiten"),
                HelpItem("Export-Backup", "Regelmäßige Excel-Exporte als Datensicherung nutzen")
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hilfe & Funktionen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Willkommens-Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Arbeitszeit Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Hier findest du eine Übersicht aller Funktionen und wie du sie nutzt.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onToggle
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
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Einklappen" else "Ausklappen"
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    section.items.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
