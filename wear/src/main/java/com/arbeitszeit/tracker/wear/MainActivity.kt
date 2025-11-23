package com.arbeitszeit.tracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.arbeitszeit.tracker.wear.ui.screens.HomeScreen
import com.arbeitszeit.tracker.wear.ui.theme.ArbeitszeitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for better UX (Wear OS 6 optimization)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            ArbeitszeitTrackerTheme {
                WearApp()
            }
        }
    }
}

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()

    MaterialTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen()
            }
        }
    }
}
