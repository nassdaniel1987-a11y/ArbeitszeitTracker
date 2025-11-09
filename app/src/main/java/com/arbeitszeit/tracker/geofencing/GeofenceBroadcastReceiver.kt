package com.arbeitszeit.tracker.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import com.arbeitszeit.tracker.widget.TimeStampWidget
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "geofencing_channel"
        const val ACTION_START_WORK = "com.arbeitszeit.tracker.ACTION_START_WORK"
        const val ACTION_STOP_WORK = "com.arbeitszeit.tracker.ACTION_STOP_WORK"
        const val ACTION_DISMISS = "com.arbeitszeit.tracker.ACTION_DISMISS"
        const val ACTION_TEST_GEOFENCE_ENTER = "com.arbeitszeit.tracker.TEST_GEOFENCE_ENTER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START_WORK -> handleStartWork(context)
            ACTION_STOP_WORK -> handleStopWork(context)
            ACTION_DISMISS -> handleDismiss(context)
            ACTION_TEST_GEOFENCE_ENTER -> handleTestGeofence(context, intent)
            else -> handleGeofenceEvent(context, intent)
        }
    }

    private fun handleTestGeofence(context: Context, intent: Intent) {
        val locationName = intent.getStringExtra("location_name") ?: "Test-Arbeitsort"

        // Zeige Test-Benachrichtigung
        showNotification(
            context,
            "TEST: Arbeitsort erreicht - $locationName",
            "Möchtest du die Arbeitszeit jetzt starten?",
            ACTION_START_WORK
        )
    }

    private fun handleGeofenceEvent(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Prüfe ob Geofencing zu dieser Zeit aktiv sein soll
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val settings = database.userSettingsDao().getSettings()

            if (settings == null || !settings.geofencingEnabled) {
                return@launch
            }

            val now = LocalTime.now()
            val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value
            val hourOfDay = now.hour

            if (!settings.isGeofencingActiveNow(dayOfWeek, hourOfDay)) {
                return@launch
            }

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    // Benutzer hat Arbeitsort betreten
                    showNotification(
                        context,
                        "Arbeitsort erreicht",
                        "Möchtest du die Arbeitszeit jetzt starten?",
                        ACTION_START_WORK
                    )
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // Benutzer hat Arbeitsort verlassen
                    showNotification(
                        context,
                        "Arbeitsort verlassen",
                        "Möchtest du die Arbeitszeit jetzt beenden?",
                        ACTION_STOP_WORK
                    )
                }
            }
        }
    }

    private fun handleStartWork(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()

            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    startZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            // Widget aktualisieren
            refreshWidget(context)
        }

        // Benachrichtigung schließen
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        // Erfolgs-Benachrichtigung anzeigen
        showSuccessNotification(
            context,
            "Arbeitszeit gestartet",
            "Start: ${TimeUtils.currentTimeString()}"
        )
    }

    private fun handleStopWork(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val timeEntryDao = database.timeEntryDao()

            val today = DateUtils.today()
            val entry = timeEntryDao.getEntryByDate(today)
            val currentTime = TimeUtils.currentTimeInMinutes()

            if (entry != null) {
                timeEntryDao.update(entry.copy(
                    endZeit = currentTime,
                    updatedAt = System.currentTimeMillis()
                ))
            }

            // Widget aktualisieren
            refreshWidget(context)
        }

        // Benachrichtigung schließen
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        // Erfolgs-Benachrichtigung anzeigen
        showSuccessNotification(
            context,
            "Arbeitszeit beendet",
            "Ende: ${TimeUtils.currentTimeString()}"
        )
    }

    private fun handleDismiss(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        action: String
    ) {
        createNotificationChannel(context)

        val startIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            this.action = action
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            this.action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = if (action == ACTION_START_WORK) "Starten" else "Beenden"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, actionText, startPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Ignorieren", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun refreshWidget(context: Context) {
        val intent = Intent(context, TimeStampWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TimeStampWidget::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        context.sendBroadcast(intent)
    }

    private fun showSuccessNotification(
        context: Context,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification) // Different ID than action notification
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofencing Benachrichtigungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für automatische Zeiterfassung"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
