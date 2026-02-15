package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Datenklasse für eine Onboarding-Seite
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val tip: String
)

/**
 * Die Onboarding-Seiten mit allen wichtigen Features
 */
private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.AccessTime,
        title = "Zeiterfassung",
        description = "Erfasse deine Arbeitszeit ganz einfach. Auf der Startseite siehst du den heutigen Tag mit Start, Ende und Pause. Tippe auf die Felder, um Zeiten einzutragen.",
        tip = "Tipp: Über den \"Aktionen\"-Button unten rechts kannst du dich mit einem Tipp einstempeln."
    ),
    OnboardingPage(
        icon = Icons.Default.TouchApp,
        title = "Schnellaktionen",
        description = "Der \"Aktionen\"-Button bietet dir schnellen Zugriff auf die wichtigsten Funktionen: Einstempeln/Ausstempeln, Urlaub, Krankheit, Feiertag oder Abwesenheit eintragen.",
        tip = "Tipp: Wenn die Zeiterfassung läuft, wechselt der Button automatisch zu \"Ausstempeln\"."
    ),
    OnboardingPage(
        icon = Icons.Default.ViewWeek,
        title = "Wochenübersicht",
        description = "Unter dem heutigen Eintrag siehst du die gesamte Woche. Navigiere mit den Pfeilen zwischen den Wochen und behalte Soll- und Ist-Stunden im Blick.",
        tip = "Tipp: Du kannst Sollzeit-Vorlagen auf ganze Wochen anwenden, um die Soll-Zeiten automatisch zu setzen."
    ),
    OnboardingPage(
        icon = Icons.Default.Timeline,
        title = "Überstunden",
        description = "Im Bereich \"Überstunden\" siehst du dein Stundenkonto: Wie viele Plus- oder Minusstunden du insgesamt hast, aufgeschlüsselt nach Wochen.",
        tip = "Tipp: Die Überstunden werden automatisch aus der Differenz von Soll und Ist berechnet."
    ),
    OnboardingPage(
        icon = Icons.Default.CalendarMonth,
        title = "Kalender",
        description = "Der Kalender gibt dir einen Monatsüberblick. Tage sind farblich markiert: Grün bedeutet alles ausgefüllt, Gelb heißt unvollständig.",
        tip = "Tipp: Tippe auf einen Tag im Kalender, um die Details zu sehen."
    ),
    OnboardingPage(
        icon = Icons.Default.BeachAccess,
        title = "Urlaubsplaner",
        description = "Verwalte deinen Urlaub: Sieh deinen Urlaubsanspruch, verbrauchte Tage und den Resturlaub auf einen Blick. Urlaub trägst du direkt über den Aktionen-Button ein.",
        tip = "Tipp: Resturlaub vom Vorjahr wird automatisch übertragen, wenn du ein neues Jahr anlegst."
    ),
    OnboardingPage(
        icon = Icons.Default.Schedule,
        title = "Sollzeit-Vorlagen",
        description = "Erstelle verschiedene Sollzeit-Vorlagen für unterschiedliche Arbeitsrhythmen (z.B. Vollzeit, Teilzeit). Vorlagen definieren die Soll-Stunden pro Wochentag.",
        tip = "Tipp: Auf der Startseite kannst du eine Vorlage direkt auf die aktuelle Woche anwenden."
    ),
    OnboardingPage(
        icon = Icons.Default.FileDownload,
        title = "Excel-Export",
        description = "Exportiere deine Arbeitszeiten als fertige Excel-Datei. Die App nutzt deine hochgeladene Vorlage und füllt alle Zeiten automatisch ein.",
        tip = "Tipp: Du findest den Export unter \"Daten\" im Seitenmenü."
    ),
    OnboardingPage(
        icon = Icons.Default.Menu,
        title = "Navigation",
        description = "Über das Menü-Symbol (drei Striche) oben links erreichst du alle Bereiche: Home, Überstunden, Kalender, Urlaubsplaner, Vorlagen, Daten und Einstellungen.",
        tip = "Los geht's! Du kannst diese Tour jederzeit unter \"Hilfe\" erneut starten."
    )
)

/**
 * Feature-Tour / Onboarding Screen
 *
 * Zeigt neue Nutzer durch alle wichtigen Features der App.
 * Verwendet einen HorizontalPager für swipbare Seiten.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Skip-Button oben rechts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text("Überspringen")
                }
            }

            // Pager mit Seiten
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    pageNumber = page + 1,
                    totalPages = onboardingPages.size
                )
            }

            // Page Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(300),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zurück-Button (nur wenn nicht auf erster Seite)
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Zurück")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Weiter/Fertig-Button
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isLastPage) "Starten" else "Weiter")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Inhalt einer einzelnen Onboarding-Seite
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageNumber: Int,
    totalPages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Seitennummer
        Text(
            text = "$pageNumber / $totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Icon im Kreis
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Titel
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        // Beschreibung
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Tipp-Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = page.tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
