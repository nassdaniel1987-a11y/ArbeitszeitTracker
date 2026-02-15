package com.arbeitszeit.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Position des Tooltip-Fensters auf dem Bildschirm
 */
private enum class TooltipPosition {
    TOP,     // Oben auf dem Bildschirm (zeigt auf Elemente darunter)
    CENTER,  // Mitte des Bildschirms
    BOTTOM   // Unten auf dem Bildschirm (zeigt auf Elemente darüber)
}

/**
 * Ein einzelner Onboarding-Schritt
 */
private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionHint: String,
    val position: TooltipPosition,
    val pointsTo: String  // Beschreibung wohin der Pfeil zeigt
)

/**
 * Interaktive Onboarding-Schritte die auf echte UI-Elemente zeigen
 */
private val onboardingSteps = listOf(
    OnboardingStep(
        icon = Icons.Default.Celebration,
        title = "Willkommen in deiner App!",
        description = "Lass dich kurz durch die wichtigsten Bereiche führen. Du siehst die echte App im Hintergrund - alles was wir zeigen, kannst du danach direkt ausprobieren.",
        actionHint = "Tippe auf \"Weiter\" um zu starten",
        position = TooltipPosition.CENTER,
        pointsTo = ""
    ),
    OnboardingStep(
        icon = Icons.Default.Today,
        title = "Dein heutiger Eintrag",
        description = "Ganz oben siehst du die Karte für den heutigen Tag. Hier stehen deine Arbeitszeiten: Start (Von), Ende (Bis) und Pause.\n\nTippe einfach auf die Zeitfelder um deine Zeiten einzutragen.",
        actionHint = "Schau dir die Tageskarte oben an",
        position = TooltipPosition.BOTTOM,
        pointsTo = "Tageskarte oben"
    ),
    OnboardingStep(
        icon = Icons.Default.TouchApp,
        title = "Schnellaktionen",
        description = "Unten rechts findest du den \"Aktionen\"-Button. Damit kannst du:\n\n- Dich einstempeln/ausstempeln\n- Urlaub eintragen\n- Krankheit eintragen\n- Feiertage markieren",
        actionHint = "Schau nach unten rechts zum Aktionen-Button",
        position = TooltipPosition.TOP,
        pointsTo = "Aktionen-Button unten rechts"
    ),
    OnboardingStep(
        icon = Icons.Default.ViewWeek,
        title = "Deine Wochenübersicht",
        description = "Unter dem Tageseintrag siehst du die ganze Woche mit allen Tagen. Die Wochen-Statistik zeigt dir Soll vs. Ist auf einen Blick.\n\nMit den Pfeilen kannst du zwischen Wochen navigieren.",
        actionHint = "Scrolle nach unten um die Woche zu sehen",
        position = TooltipPosition.CENTER,
        pointsTo = "Wochenbereich in der Mitte"
    ),
    OnboardingStep(
        icon = Icons.Default.Menu,
        title = "Das Hauptmenü",
        description = "Oben links siehst du das Menü-Symbol (drei Striche). Damit öffnest du das Seitenmenü mit allen Bereichen:\n\n- Überstunden\n- Kalender\n- Urlaubsplaner\n- Sollzeit-Vorlagen\n- Datenexport\n- Einstellungen",
        actionHint = "Tippe oben links auf das Menü-Symbol",
        position = TooltipPosition.CENTER,
        pointsTo = "Menü-Symbol oben links"
    ),
    OnboardingStep(
        icon = Icons.Default.Schedule,
        title = "Sollzeit-Vorlagen einrichten",
        description = "Damit die App deine Überstunden richtig berechnet, solltest du Sollzeit-Vorlagen einrichten. Die legen fest, wie viele Stunden du pro Tag arbeiten sollst.\n\nGehe dazu ins Menü und dann auf \"Sollzeit-Vorlagen\".",
        actionHint = "Menü > Sollzeit-Vorlagen",
        position = TooltipPosition.CENTER,
        pointsTo = "Im Seitenmenü"
    ),
    OnboardingStep(
        icon = Icons.Default.Settings,
        title = "Einstellungen prüfen",
        description = "Unter \"Einstellungen\" kannst du wichtige Dinge anpassen:\n\n- Arbeitstage (welche Tage arbeitest du?)\n- Bundesland (für korrekte Feiertage)\n- Dark Mode\n- Benachrichtigungen\n- Geofencing (automatisches Stempeln)",
        actionHint = "Menü > Einstellungen",
        position = TooltipPosition.CENTER,
        pointsTo = "Im Seitenmenü"
    ),
    OnboardingStep(
        icon = Icons.Default.CheckCircle,
        title = "Du bist startklar!",
        description = "Das war's! Du kennst jetzt die wichtigsten Bereiche. Stempel dich ein, trage deine Zeiten ein und die App erledigt den Rest.\n\nDu kannst diese Tour jederzeit unter Hilfe > \"Feature-Tour\" wiederholen.",
        actionHint = "Tippe auf \"Los geht's\" um zu starten",
        position = TooltipPosition.CENTER,
        pointsTo = ""
    )
)

/**
 * Interaktives Onboarding-Overlay
 *
 * Wird AUF der echten App angezeigt (nicht stattdessen).
 * Zeigt kontextuelle Tooltips die auf echte UI-Elemente zeigen.
 */
@Composable
fun OnboardingOverlay(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = onboardingSteps[currentStep]
    val isLastStep = currentStep == onboardingSteps.size - 1
    val isFirstStep = currentStep == 0

    // Dim-Overlay über die echte App
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block touches to underlying app */ }
    ) {
        // Skip-Button oben rechts
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .statusBarsPadding()
        ) {
            Text(
                "Überspringen",
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Schritt-Indikator oben
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            onboardingSteps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == currentStep) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentStep) Color.White
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Pfeil der auf das Element zeigt
        if (step.pointsTo.isNotEmpty()) {
            when (step.position) {
                TooltipPosition.TOP -> {
                    // Pfeil nach unten (zeigt auf Element unterhalb)
                    Text(
                        text = step.pointsTo + " \u2193",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 200.dp)
                    )
                }
                TooltipPosition.BOTTOM -> {
                    // Pfeil nach oben (zeigt auf Element oberhalb)
                    Text(
                        text = "\u2191 " + step.pointsTo,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 100.dp)
                    )
                }
                TooltipPosition.CENTER -> { /* Kein Pfeil bei Center */ }
            }
        }

        // Tooltip-Card
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 })
                    .togetherWith(fadeOut(tween(200)))
            },
            modifier = Modifier.align(
                when (step.position) {
                    TooltipPosition.TOP -> Alignment.TopCenter
                    TooltipPosition.CENTER -> Alignment.Center
                    TooltipPosition.BOTTOM -> Alignment.BottomCenter
                }
            ),
            label = "onboarding_step"
        ) { stepIndex ->
            val currentStepData = onboardingSteps[stepIndex]

            Card(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = when (currentStepData.position) {
                            TooltipPosition.TOP -> 80.dp
                            TooltipPosition.CENTER -> 24.dp
                            TooltipPosition.BOTTOM -> 100.dp
                        }
                    )
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Schrittnummer
                    Text(
                        text = "Schritt ${stepIndex + 1} von ${onboardingSteps.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Icon
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                currentStepData.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Titel
                    Text(
                        text = currentStepData.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Beschreibung
                    Text(
                        text = currentStepData.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Action-Hint
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentStepData.actionHint,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Zurück-Button
                        if (!isFirstStep) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Zurück")
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        // Weiter/Fertig-Button
                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onComplete()
                                } else {
                                    currentStep++
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                when {
                                    isFirstStep -> "Weiter"
                                    isLastStep -> "Los geht's!"
                                    else -> "Weiter"
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (isLastStep) Icons.Default.Check
                                else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standalone OnboardingScreen für Navigation (z.B. aus Hilfe-Bereich)
 *
 * Zeigt die Feature-Tour als eigenständigen Screen.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Hintergrund
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        // Overlay drüber
        OnboardingOverlay(onComplete = onComplete)
    }
}
