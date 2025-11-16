package com.arbeitszeit.tracker.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing System für konsistente Abstände
 *
 * Verwendung:
 * - padding(AppSpacing.medium)
 * - verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
 * - Modifier.height(AppSpacing.xl)
 */
object AppSpacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val large: Dp = 20.dp
    val extraLarge: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp
    val xxxl: Dp = 48.dp
}

/**
 * Elevation System für konsistente Schatten
 */
object AppElevation {
    val none: Dp = 0.dp
    val low: Dp = 2.dp
    val medium: Dp = 4.dp
    val high: Dp = 8.dp
    val veryHigh: Dp = 12.dp
}

/**
 * Icon Sizes für konsistente Icon-Größen
 */
object AppIconSize {
    val tiny: Dp = 12.dp
    val small: Dp = 16.dp
    val medium: Dp = 24.dp
    val large: Dp = 32.dp
    val extraLarge: Dp = 48.dp
    val huge: Dp = 64.dp
}

/**
 * Min Touch Target Größen für Accessibility
 */
object AppTouchTarget {
    val minimum: Dp = 48.dp  // Material Design Minimum
    val comfortable: Dp = 56.dp
}
