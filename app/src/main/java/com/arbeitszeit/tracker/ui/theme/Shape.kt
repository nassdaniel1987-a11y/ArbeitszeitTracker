package com.arbeitszeit.tracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape System für konsistente Ecken-Radius
 *
 * ExtraSmall: Kleine Buttons, Chips
 * Small: Standard-Buttons, kleine Cards
 * Medium: Standard-Cards, Dialoge
 * Large: Große Cards, Bottom Sheets
 * ExtraLarge: Hero-Cards, Floating Panels
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
