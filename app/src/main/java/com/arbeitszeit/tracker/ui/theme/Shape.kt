package com.arbeitszeit.tracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Modern Shape System - Weichere, rundere Ecken für friendlier UI
 *
 * ExtraSmall: Kleine Buttons, Chips
 * Small: Standard-Buttons, kleine Cards
 * Medium: Standard-Cards, Dialoge
 * Large: Große Cards, Bottom Sheets
 * ExtraLarge: Hero-Cards, Floating Panels
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Runder als vorher (4dp -> 8dp)
    small = RoundedCornerShape(12.dp),       // Runder (8dp -> 12dp)
    medium = RoundedCornerShape(16.dp),      // Runder (12dp -> 16dp)
    large = RoundedCornerShape(24.dp),       // Deutlich runder (16dp -> 24dp)
    extraLarge = RoundedCornerShape(32.dp)   // Runder (28dp -> 32dp)
)

// Spezielle Shapes für kreative Designs
val ShapeTopRounded = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val ShapeBottomRounded = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

val ShapeHeroCard = RoundedCornerShape(20.dp)

val ShapeChip = RoundedCornerShape(16.dp)

val ShapeFullRounded = RoundedCornerShape(50) // Prozent für Pill-Form
