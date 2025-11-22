package com.arbeitszeit.tracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Custom Branding Colors
val BrandPrimary = Color(0xFF0D47A1)      // Dunkles Professional Blau
val BrandPrimaryLight = Color(0xFF5472D3) // Helles Akzent Blau
val BrandPrimaryDark = Color(0xFF002171)  // Sehr dunkles Blau

// Moderne Akzentfarben - Mutig & Lebhaft
val Purple700 = Color(0xFF7C3AED)         // Violett für Premium-Features
val Purple500 = Color(0xFF9333EA)         // Helles Violett
val Purple200 = Color(0xFFD8B4FE)         // Sehr helles Violett

val Cyan700 = Color(0xFF0891B2)           // Dunkles Cyan
val Cyan500 = Color(0xFF06B6D4)           // Cyan für Highlights
val Cyan200 = Color(0xFFA5F3FC)           // Helles Cyan

val Indigo700 = Color(0xFF4338CA)         // Indigo
val Indigo500 = Color(0xFF6366F1)         // Helles Indigo
val Indigo200 = Color(0xFFC7D2FE)         // Sehr helles Indigo

val Teal700 = Color(0xFF0F766E)           // Teal
val Teal500 = Color(0xFF14B8A6)           // Helles Teal
val Teal200 = Color(0xFF99F6E4)           // Sehr helles Teal

// Gradient Brushes für moderne Card-Backgrounds
val GradientPrimary = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF0D47A1),
        Color(0xFF1976D2)
    )
)

val GradientPurple = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7C3AED),
        Color(0xFF9333EA)
    )
)

val GradientCyan = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF0891B2),
        Color(0xFF06B6D4)
    )
)

val GradientSuccess = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF059669),
        Color(0xFF10B981)
    )
)

val GradientWarning = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFEA580C),
        Color(0xFFF59E0B)
    )
)

// Gradient für Dark Mode - subtiler
val GradientDarkCard = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2C2C2C),
        Color(0xFF242424)
    )
)

val GradientLightCard = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFFAFAFA)
    )
)

// Primary Colors
val Blue700 = Color(0xFF1976D2)
val Blue500 = Color(0xFF2196F3)
val Blue200 = Color(0xFF90CAF9)

// Secondary Colors
val Green700 = Color(0xFF388E3C)
val Green500 = Color(0xFF4CAF50)
val Green200 = Color(0xFFA5D6A7)
val Green50 = Color(0xFFE8F5E9)   // Sehr helles Grün für Backgrounds

// Error
val Red700 = Color(0xFFC62828)
val Red500 = Color(0xFFF44336)

// Warning/Orange
val Orange500 = Color(0xFFFF9800)
val Orange700 = Color(0xFFF57C00)

// Status Colors
val StatusComplete = Color(0xFF4CAF50)   // Grün
val StatusPartial = Color(0xFFFFC107)     // Gelb
val StatusEmpty = Color(0xFFF44336)       // Rot
val StatusMissing = Color(0xFF9E9E9E)     // Grau
val StatusSpecial = Color(0xFF2196F3)     // Blau

// TimeEntry Type Colors
val TypeNormal = Blue700                  // Arbeit (Blau)
val TypeUrlaub = Green500                 // Urlaub (Grün)
val TypeKrank = Red500                    // Krank (Rot)
val TypeFeiertag = Blue500                // Feiertag (Hellblau)
val TypeAbwesend = Color(0xFF9E9E9E)      // Abwesend (Grau)

// Time Tracking Colors
val OvertimeColor = Green500              // Überstunden (Grün)
val UndertimeColor = Red500               // Fehlstunden (Rot)
val ProgressGood = Green500               // ≥100% (Grün)
val ProgressWarning = Orange500           // 80-99% (Orange)
val ProgressBad = Red500                  // <80% (Rot)

// Location Status Colors
val LocationAtWork = Green500             // Am Arbeitsort (Grün)
val LocationNotAtWork = Orange500         // Nicht am Arbeitsort (Orange)
val LocationInactive = Color(0xFF9E9E9E)  // Inaktiv (Grau)

// Neutral
val Grey50 = Color(0xFFFAFAFA)
val Grey100 = Color(0xFFF5F5F5)
val Grey200 = Color(0xFFEEEEEE)
val Grey800 = Color(0xFF424242)
val Grey900 = Color(0xFF212121)
