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
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue200,
    onPrimaryContainer = Grey900,
    secondary = Green700,
    onSecondary = Color.White,
    secondaryContainer = Green200,
    onSecondaryContainer = Grey900,
    error = Red700,
    onError = Color.White,
    errorContainer = Red500,
    onErrorContainer = Grey900,
    background = Grey50,
    onBackground = Grey900,
    surface = Color.White,
    onSurface = Grey900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey800
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = Grey900,
    primaryContainer = Blue700,
    onPrimaryContainer = Grey50,
    secondary = Green500,
    onSecondary = Grey900,
    secondaryContainer = Green700,
    onSecondaryContainer = Grey50,
    error = Red500,
    onError = Grey900,
    errorContainer = Red700,
    onErrorContainer = Grey50,
    background = Grey900,
    onBackground = Grey50,
    surface = Grey800,
    onSurface = Grey50,
    surfaceVariant = Grey900,
    onSurfaceVariant = Grey200
)

@Composable
fun ArbeitszeitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}