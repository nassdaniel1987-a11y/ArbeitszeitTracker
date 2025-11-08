package com.arbeitszeit.tracker.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START_WORK -> handleStartWork(context)
            ACTION_STOP_WORK -> handleStopWork(context)
            ACTION_DISMISS -> handleDismiss(context)
            else -> handleGeofenceEvent(context, intent)
        }
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
        // TODO: Implementiere Arbeitszeit-Start-Logik
        // Dies würde den TimeStamp in der Datenbank setzen

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        // Öffne die App
        val appIntent = Intent(context, MainActivity::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(appIntent)
    }

    private fun handleStopWork(context: Context) {
        // TODO: Implementiere Arbeitszeit-Stop-Logik

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        // Öffne die App
        val appIntent = Intent(context, MainActivity::class.java)
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(appIntent)
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
