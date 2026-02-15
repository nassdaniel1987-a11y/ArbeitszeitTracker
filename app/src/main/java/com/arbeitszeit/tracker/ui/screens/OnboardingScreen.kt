package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Route-Ziel für einen Onboarding-Schritt
 */
enum class OnboardingTarget {
    NONE,                     // Kein Navigation-Ziel (nur Info)
    SOLLZEIT_VORLAGEN,        // Menü > Sollzeit-Vorlagen
    SETTINGS,                 // Menü > Einstellungen
    GEOFENCING,               // Einstellungen > Geofencing
    DATA_MANAGEMENT,          // Menü > Daten
    VACATION_PLANNER,         // Menü > Urlaubsplaner
    CLOSING_DAYS,             // Urlaubsplaner > Schließtage
    HOME                      // Zurück zur Startseite
}

/**
 * Ein Onboarding-Aufgabe (Setup-Schritt)
 */
private data class SetupTask(
    val icon: ImageVector,
    val title: String,
    val why: String,
    val howTo: String,
    val target: OnboardingTarget,
    val buttonLabel: String,
    val isRequired: Boolean = true
)

/**
 * Alle Setup-Aufgaben für neue Nutzer
 */
private val setupTasks = listOf(
    // --- WILLKOMMEN ---
    SetupTask(
        icon = Icons.Default.Celebration,
        title = "Willkommen! Lass uns die App einrichten",
        why = "Die App braucht ein paar Einstellungen, damit sie für dich richtig funktioniert. Wir gehen jetzt Schritt für Schritt alles durch.",
        howTo = "Bei jedem Schritt kannst du direkt zum richtigen Bereich springen. Du kannst die Einrichtung jederzeit über Hilfe > Feature-Tour fortsetzen.",
        target = OnboardingTarget.NONE,
        buttonLabel = "Los geht's"
    ),

    // --- 1. SOLLZEIT-VORLAGEN ---
    SetupTask(
        icon = Icons.Default.Schedule,
        title = "1. Sollzeit-Vorlagen einrichten",
        why = "Damit die App deine Überstunden korrekt berechnet, muss sie wissen, wie viele Stunden du pro Tag arbeiten SOLLST. Ohne Sollzeiten werden alle Stunden als Überstunden gezählt.",
        howTo = "Auf dem nächsten Screen:\n\n" +
                "1. Tippe auf die Standard-Vorlage (oder erstelle eine neue)\n" +
                "2. Trage für jeden Arbeitstag (Mo-Fr) deine Soll-Stunden ein\n" +
                "   Beispiel: Mo 7:45, Di 7:45, Mi 7:45, Do 7:45, Fr 6:00\n" +
                "3. Für freie Tage (Sa/So) lasse 0:00\n" +
                "4. Speichere die Vorlage\n\n" +
                "Tipp: Falls du Schichtdienst hast, erstelle mehrere Vorlagen (Früh/Spät/Nacht).",
        target = OnboardingTarget.SOLLZEIT_VORLAGEN,
        buttonLabel = "Jetzt Sollzeiten einrichten"
    ),

    // --- 2. ARBEITSTAGE & BUNDESLAND ---
    SetupTask(
        icon = Icons.Default.Settings,
        title = "2. Arbeitstage & Bundesland einstellen",
        why = "Die App muss wissen, an welchen Tagen du arbeitest und in welchem Bundesland du bist. Das Bundesland bestimmt deine gesetzlichen Feiertage – an diesen Tagen wird keine Sollzeit berechnet.",
        howTo = "In den Einstellungen:\n\n" +
                "1. Scrolle zu \"Arbeitszeit\"\n" +
                "   - Stelle deinen Arbeitsumfang ein (z.B. 100% oder 75%)\n" +
                "   - Prüfe die Wochenstunden (z.B. 39:00)\n" +
                "   - Wähle deine Arbeitstage (z.B. Mo-Fr)\n\n" +
                "2. Scrolle zu \"Feiertage & Urlaub\"\n" +
                "   - Wähle dein Bundesland (z.B. Baden-Württemberg)\n" +
                "   - Stelle deinen Urlaubsanspruch ein (z.B. 30 Tage)\n\n" +
                "3. Optional: Wähle unter \"Design\" dein bevorzugtes Theme (Hell/Dunkel)",
        target = OnboardingTarget.SETTINGS,
        buttonLabel = "Jetzt Einstellungen öffnen"
    ),

    // --- 3. ERSTE ZEIT EINTRAGEN ---
    SetupTask(
        icon = Icons.Default.PlayArrow,
        title = "3. Erste Arbeitszeit eintragen",
        why = "Jetzt bist du startklar! Trage deine erste Arbeitszeit ein, damit du siehst wie die App funktioniert.",
        howTo = "Auf der Startseite:\n\n" +
                "Variante A - Manuell:\n" +
                "1. Tippe auf \"Von\" und stelle deine Startzeit ein\n" +
                "2. Tippe auf \"Bis\" und stelle deine Endzeit ein\n" +
                "3. Tippe auf das Pause-Symbol und stelle deine Pause ein\n\n" +
                "Variante B - Live stempeln:\n" +
                "1. Tippe unten rechts auf den \"Aktionen\"-Button\n" +
                "2. Wähle \"Einstempeln\" – die Uhr läuft ab jetzt\n" +
                "3. Am Feierabend: Aktionen > \"Ausstempeln\"\n\n" +
                "Die App berechnet automatisch: Ist-Zeit, Soll-Zeit und die Differenz.",
        target = OnboardingTarget.HOME,
        buttonLabel = "Zur Startseite"
    ),

    // --- 4. AUTO-START & GEOFENCING ---
    SetupTask(
        icon = Icons.Default.LocationOn,
        title = "4. Automatisches Stempeln einrichten",
        why = "Vergisst du manchmal dich einzustempeln? Die App kann das automatisch für dich erledigen – entweder zu festen Zeiten (Auto-Start) oder wenn du an deinem Arbeitsort ankommst (Geofencing).",
        howTo = "In den Einstellungen findest du zwei Optionen:\n\n" +
                "Option A - Auto-Start (zeitbasiert):\n" +
                "1. Öffne Einstellungen > Scrolle zu \"Auto-Start\"\n" +
                "2. Aktiviere den Schalter\n" +
                "3. Stelle für jeden Arbeitstag deine Startzeit ein\n" +
                "   z.B. Mo-Fr jeweils 7:30 Uhr\n" +
                "4. Stelle die Standard-Pause ein (z.B. 30 Min)\n\n" +
                "Option B - Geofencing (standortbasiert):\n" +
                "1. Tippe auf \"Geofencing einrichten\"\n" +
                "2. Aktiviere Geofencing\n" +
                "3. Füge deinen Arbeitsort hinzu (Name + Standort)\n" +
                "4. Stelle den Radius ein (mind. 100m)\n" +
                "5. Wähle Zeitfenster und aktive Tage\n\n" +
                "Wichtig: Für Geofencing muss die Standort-Berechtigung auf \"Immer\" stehen!",
        target = OnboardingTarget.SETTINGS,
        buttonLabel = "Jetzt Auto-Start einrichten",
        isRequired = false
    ),

    // --- 5. BENACHRICHTIGUNGEN ---
    SetupTask(
        icon = Icons.Default.Notifications,
        title = "5. Benachrichtigungen einstellen",
        why = "Die App kann dich morgens ans Einstempeln erinnern, abends ans Ausstempeln und dich warnen wenn Einträge fehlen. Du kannst auch Ruhezeiten einstellen, damit du am Wochenende oder abends nicht gestört wirst.",
        howTo = "In den Einstellungen:\n\n" +
                "1. Scrolle zu \"Benachrichtigungen\"\n" +
                "2. Aktiviere die Ruhezeit wenn gewünscht:\n" +
                "   - Startzeit (z.B. 21:00 – ab dann keine Meldungen)\n" +
                "   - Endzeit (z.B. 7:00 – ab dann wieder Meldungen)\n" +
                "3. Wähle aktive Tage:\n" +
                "   - z.B. nur Mo-Fr (keine Benachrichtigungen am Wochenende)\n\n" +
                "Die Erinnerungen (morgens/abends/fehlende Einträge) laufen automatisch im Hintergrund.",
        target = OnboardingTarget.SETTINGS,
        buttonLabel = "Jetzt Benachrichtigungen einstellen",
        isRequired = false
    ),

    // --- 6. URLAUBSPLANER & SCHLIESS TAGE ---
    SetupTask(
        icon = Icons.Default.BeachAccess,
        title = "6. Urlaubsplaner & Schließtage",
        why = "Die App hilft dir bei der Urlaubsplanung: Sie zeigt deinen Resturlaub, berechnet Brückentage und berücksichtigt Schließtage deiner Einrichtung automatisch.",
        howTo = "So richtest du den Urlaubsplaner ein:\n\n" +
                "1. Dein Urlaubsanspruch wurde bereits in Schritt 2 eingestellt\n" +
                "2. Öffne den Urlaubsplaner:\n" +
                "   - Tab \"Übersicht\": Zeigt verfügbare/genommene Tage\n" +
                "   - Tab \"Planen\": Tippe auf Tage um Urlaub einzutragen\n" +
                "   - Tab \"Brückentage\": KI berechnet die effizientesten Brückentage\n\n" +
                "3. Schließtage eintragen (wichtig!):\n" +
                "   - Über den Urlaubsplaner erreichst du die Schließtage\n" +
                "   - Trage alle Schließtage deiner Einrichtung ein\n" +
                "     (Weihnachtsferien, Sommerferien, Betriebsferien)\n" +
                "   - Die App zählt Schließtage NICHT als Urlaubstage\n" +
                "   - Die KI nutzt sie für bessere Brückentag-Vorschläge",
        target = OnboardingTarget.VACATION_PLANNER,
        buttonLabel = "Jetzt Urlaubsplaner öffnen",
        isRequired = false
    ),

    // --- 7. EXCEL-EXPORT ---
    SetupTask(
        icon = Icons.Default.FileDownload,
        title = "7. Excel-Export vorbereiten",
        why = "Du kannst deine Arbeitszeiten als Excel-Datei exportieren. Falls du eine eigene Excel-Vorlage hast (z.B. von deinem Arbeitgeber), kannst du sie hochladen – die App füllt dann automatisch alle Zeiten in deine Vorlage ein.",
        howTo = "Im Bereich \"Daten\":\n\n" +
                "1. Tab \"Export\":\n" +
                "   - Wähle den Zeitraum (Monat/Jahr)\n" +
                "   - Tippe auf \"Exportieren\"\n" +
                "   - Die Datei wird als .xlsx in deinem Downloads-Ordner gespeichert\n\n" +
                "2. Tab \"Excel-Vorlagen\" (optional):\n" +
                "   - Lade deine eigene Excel-Vorlage hoch\n" +
                "   - Weise sie dem richtigen Jahr zu\n" +
                "   - Beim Export wird deine Vorlage mit Daten gefüllt\n\n" +
                "3. Tab \"Import\":\n" +
                "   - Importiere Daten aus einer Excel-Datei\n" +
                "   - Nützlich beim Gerätewechsel oder Datenverlust",
        target = OnboardingTarget.DATA_MANAGEMENT,
        buttonLabel = "Jetzt Daten-Bereich öffnen",
        isRequired = false
    ),

    // --- FERTIG ---
    SetupTask(
        icon = Icons.Default.CheckCircle,
        title = "Alles eingerichtet!",
        why = "Du hast alle wichtigen Bereiche kennengelernt. Die App ist jetzt bereit für den Einsatz.",
        howTo = "Zusammenfassung – was du jetzt tun kannst:\n\n" +
                "Jeden Tag:\n" +
                "- Stempel dich ein/aus (manuell oder automatisch)\n" +
                "- Die App berechnet Soll, Ist und Überstunden\n\n" +
                "Jede Woche:\n" +
                "- Prüfe deine Wochenübersicht auf der Startseite\n" +
                "- Wende Vorlagen an wenn nötig\n\n" +
                "Jeden Monat:\n" +
                "- Exportiere deine Arbeitszeiten als Excel\n" +
                "- Prüfe deine Überstunden im Überstunden-Bereich\n\n" +
                "Du kannst diese Einrichtung jederzeit über\nHilfe > \"Feature-Tour starten\" wiederholen.",
        target = OnboardingTarget.NONE,
        buttonLabel = "Fertig – App starten!"
    )
)

/**
 * Interaktives Onboarding-Overlay
 *
 * Zeigt eine Setup-Checkliste über der echten App.
 * Jeder Schritt hat einen Button der zur richtigen Stelle navigiert.
 */
@Composable
fun OnboardingOverlay(
    onComplete: () -> Unit,
    onNavigate: (OnboardingTarget) -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val task = setupTasks[currentStep]
    val isLastStep = currentStep == setupTasks.size - 1
    val isFirstStep = currentStep == 0
    val totalSteps = setupTasks.size

    // Dim-Overlay über die echte App
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block touches to underlying app */ }
    ) {
        // Schritt-Indikator + Skip oben
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fortschritt
            Text(
                text = "${currentStep + 1} / $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            // Progress dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                setupTasks.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (index == currentStep) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentStep -> Color.White.copy(alpha = 0.9f)
                                    index == currentStep -> Color.White
                                    else -> Color.White.copy(alpha = 0.25f)
                                }
                            )
                    )
                }
            }

            // Skip
            TextButton(onClick = onComplete) {
                Text("Beenden", color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Haupt-Card
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 3 })
                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { -it / 3 })
                } else {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { -it / 3 })
                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { it / 3 })
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            label = "setup_step"
        ) { stepIndex ->
            val currentTask = setupTasks[stepIndex]
            val scrollState = rememberScrollState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollbarer Inhalt
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = if (currentTask.isRequired)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    currentTask.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (currentTask.isRequired)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Optional-Badge
                        if (!currentTask.isRequired) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "Optional",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Titel
                        Text(
                            text = currentTask.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Warum?
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Warum?",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = currentTask.why,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Schritt-für-Schritt Anleitung
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "So geht's:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = currentTask.howTo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Buttons am unteren Rand (nicht scrollbar)
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Haupt-Button: Navigieren oder Fertig
                        if (currentTask.target != OnboardingTarget.NONE) {
                            Button(
                                onClick = {
                                    onNavigate(currentTask.target)
                                    // Nach Navigation: nächsten Schritt vorbereiten
                                    if (!isLastStep) currentStep++
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    currentTask.buttonLabel,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Weiter/Fertig-Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Zurück
                            if (!isFirstStep) {
                                OutlinedButton(
                                    onClick = { currentStep-- },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Zurück")
                                }
                            }

                            // Weiter/Überspringen/Fertig
                            if (currentTask.target == OnboardingTarget.NONE) {
                                // Kein Navigation-Ziel → Weiter/Fertig als Haupt-Button
                                Button(
                                    onClick = {
                                        if (isLastStep) onComplete()
                                        else currentStep++
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (isLastStep) currentTask.buttonLabel else "Weiter",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Navigation-Ziel vorhanden → "Überspringen" als Alternative
                                TextButton(
                                    onClick = {
                                        if (isLastStep) onComplete()
                                        else currentStep++
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (!currentTask.isRequired) "Überspringen" else "Später",
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
}

/**
 * Standalone OnboardingScreen für Navigation (z.B. aus Hilfe-Bereich)
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onNavigate: (OnboardingTarget) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        OnboardingOverlay(
            onComplete = onComplete,
            onNavigate = onNavigate
        )
    }
}
