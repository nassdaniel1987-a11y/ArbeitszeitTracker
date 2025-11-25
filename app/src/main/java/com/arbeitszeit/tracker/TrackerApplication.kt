package com.arbeitszeit.tracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application-Klasse für Hilt Dependency Injection
 *
 * @HiltAndroidApp triggert die Code-Generierung für Hilt DI
 * und macht die Application als Dependency Container verfügbar
 */
@HiltAndroidApp
class TrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Hilt initialisiert sich automatisch
    }
}
