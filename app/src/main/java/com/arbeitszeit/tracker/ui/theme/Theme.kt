package com.arbeitszeit.tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight.copy(alpha = 0.15f),
    onPrimaryContainer = BrandPrimaryDark,
    secondary = Green500,
    onSecondary = Color.White,
    secondaryContainer = Green200.copy(alpha = 0.3f),
    onSecondaryContainer = Grey900,
    tertiary = Orange500,
    onTertiary = Color.White,
    tertiaryContainer = Orange500.copy(alpha = 0.15f),
    error = Red500,
    onError = Color.White,
    errorContainer = Red500.copy(alpha = 0.15f),
    onErrorContainer = Red700,
    background = Grey50,
    onBackground = Grey900,
    surface = Color.White,
    onSurface = Grey900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey700,
    outline = Grey300,
    outlineVariant = Grey200
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color(0xFF000000),  // Schwarz für besseren Kontrast
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = Color(0xFFE0E7FF),  // Sehr helles Indigo für bessere Lesbarkeit
    secondary = Green500,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Green700,
    onSecondaryContainer = Color(0xFFD1FAE5),  // Sehr helles Emerald
    tertiary = Orange500,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Orange700,
    error = Red500,
    onError = Color(0xFF000000),
    errorContainer = Red700,
    onErrorContainer = Color(0xFFFECDD3),  // Sehr helles Rot
    background = Grey900,
    onBackground = Color(0xFFF3F4F6),  // Helleres Grau für bessere Lesbarkeit
    surface = Grey800,
    onSurface = Color(0xFFF3F4F6),  // Helleres Grau statt Grey50
    surfaceVariant = Color(0xFF374151),  // Hellerer als Grey800
    onSurfaceVariant = Color(0xFFD1D5DB),  // Deutlich heller für besseren Kontrast
    outline = Grey600,
    outlineVariant = Grey700
)

@Composable
fun ArbeitszeitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color deaktiviert - eigene Farben für bessere Lesbarkeit
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-Edge Display (Android 15 Best Practice)
            // System Bars transparent und Content dahinter
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // System Bars verschieben Content nicht mehr
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // System Bar Icons Farbe (hell/dunkel)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}