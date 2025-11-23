package com.arbeitszeit.tracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Branding Colors - Warm & Inviting
val BrandPrimary = Color(0xFF6366F1)      // Modernes Indigo (statt conservative Dunkelblau)
val BrandPrimaryLight = Color(0xFF818CF8) // Helles Indigo
val BrandPrimaryDark = Color(0xFF4F46E5)  // Sattes Indigo

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

// Modern Gradient Brushes - Dynamisch & Lebendig
val GradientPrimary = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6366F1),  // Indigo
        Color(0xFF8B5CF6)   // Purple
    )
)

val GradientPurple = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8B5CF6),  // Purple
        Color(0xFFA855F7)   // Fuchsia
    )
)

val GradientCyan = Brush.linearGradient(
    colors = listOf(
        Color(0xFF06B6D4),  // Cyan
        Color(0xFF14B8A6)   // Teal
    )
)

val GradientSuccess = Brush.linearGradient(
    colors = listOf(
        Color(0xFF10B981),  // Emerald
        Color(0xFF34D399)   // Light Emerald
    )
)

val GradientWarning = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF59E0B),  // Amber
        Color(0xFFFBBF24)   // Yellow
    )
)

val GradientError = Brush.linearGradient(
    colors = listOf(
        Color(0xFFEF4444),  // Red
        Color(0xFFF87171)   // Light Red
    )
)

// Subtle Card Gradients - Modern & Clean
val GradientDarkCard = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F2937),  // Slate 800
        Color(0xFF111827)   // Slate 900
    )
)

val GradientLightCard = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF9FAFB)   // Sehr leichter Grauton
    )
)

// Glassmorphism Effect
val GlassMorphismLight = Color(0xFAFFFFFF)  // Fast weiß mit leichter Transparenz
val GlassMorphismDark = Color(0xE61F2937)   // Dunkler Slate mit Transparenz

// Primary Colors
val Blue700 = Color(0xFF1976D2)
val Blue500 = Color(0xFF2196F3)
val Blue200 = Color(0xFF90CAF9)

// Secondary Colors - Modernes, freundliches Grün
val Green700 = Color(0xFF059669)  // Emerald 600
val Green500 = Color(0xFF10B981)  // Emerald 500
val Green200 = Color(0xFFA7F3D0)  // Emerald 200
val Green50 = Color(0xFFECFDF5)   // Emerald 50

// Error - Modernes Rot
val Red700 = Color(0xFFDC2626)    // Red 600
val Red500 = Color(0xFFEF4444)    // Red 500

// Warning/Orange - Warme Amber-Töne
val Orange500 = Color(0xFFF59E0B)  // Amber 500
val Orange700 = Color(0xFFD97706)  // Amber 600

// Status Colors - Modern & Clear
val StatusComplete = Color(0xFF10B981)   // Emerald 500
val StatusPartial = Color(0xFFF59E0B)    // Amber 500
val StatusEmpty = Color(0xFFEF4444)      // Red 500
val StatusMissing = Color(0xFF6B7280)    // Gray 500
val StatusSpecial = Color(0xFF6366F1)    // Indigo 500

// TimeEntry Type Colors - Lebendige, unterscheidbare Farben
val TypeNormal = Color(0xFF6366F1)       // Arbeit (Indigo)
val TypeUrlaub = Color(0xFF10B981)       // Urlaub (Emerald)
val TypeKrank = Color(0xFFEF4444)        // Krank (Red)
val TypeFeiertag = Color(0xFF06B6D4)     // Feiertag (Cyan)
val TypeAbwesend = Color(0xFF6B7280)     // Abwesend (Gray 500)

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

// Neutral - Moderne, warme Grautöne (Slate-basiert)
val Grey50 = Color(0xFFF9FAFB)   // Slate 50
val Grey100 = Color(0xFFF3F4F6)  // Slate 100
val Grey200 = Color(0xFFE5E7EB)  // Slate 200
val Grey300 = Color(0xFFD1D5DB)  // Slate 300
val Grey600 = Color(0xFF4B5563)  // Slate 600
val Grey700 = Color(0xFF374151)  // Slate 700
val Grey800 = Color(0xFF1F2937)  // Slate 800
val Grey900 = Color(0xFF111827)  // Slate 900
