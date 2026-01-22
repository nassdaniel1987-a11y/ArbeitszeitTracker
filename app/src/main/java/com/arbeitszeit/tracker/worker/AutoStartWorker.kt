package com.arbeitszeit.tracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.arbeitszeit.tracker.MainActivity
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.autostart.AutoStartManager
import com.arbeitszeit.tracker.autostart.RunningTimeTracker
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.geofencing.GeofencingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * WorkManager-basierter Auto-Start Worker
 *
 * Läuft alle 15 Minuten und prüft ob ein automatischer Start der
 * Zeiterfassung durchgeführt werden soll.
 *
 * Vorteile gegenüber Geofencing-Events:
 * - Zuverlässiger im Hintergrund (WorkManager garantiert Ausführung)
 * - Aktive GPS-Prüfung statt passives Warten auf Events
 * - Funktioniert auch im Doze-Mode
 * - Unabhängig von Google Play Services Geofencing
 */
class AutoStartWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AutoStartWorker"
        private const val WORK_NAME = "auto_start_check"
        private const val CHANNEL_ID = "auto_start_channel"

        // Zeitfenster: Max 2 Stunden nach Startzeit noch Auto-Start erlauben
        private const val MAX_DELAY_MINUTES = 120

        /**
         * Plant den periodischen Auto-Start Worker (alle 15 Minuten)
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Plane AutoStartWorker...")

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Auch bei niedrigem Akku
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AutoStartWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Nicht ersetzen wenn bereits läuft
                workRequest
            )

            Log.i(TAG, "AutoStartWorker geplant (alle 15 Minuten)")
        }

        /**
         * Stoppt den Auto-Start Worker
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "AutoStartWorker gestoppt")
        }

        /**
         * Prüft ob der Worker läuft
         */
        fun isScheduled(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            return workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }

    private val database = AppDatabase.getDatabase(applicationContext)
    private val autoStartManager = AutoStartManager(applicationContext)
    private val runningTimeTracker = RunningTimeTracker(applicationContext)
    private val geofencingManager = GeofencingManager(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "AutoStartWorker gestartet")

        try {
            val settings = database.userSettingsDao().getSettings()

            if (settings == null) {
                Log.d(TAG, "Keine Settings vorhanden")
                return@withContext Result.success()
            }

            // Auto-Start aktiviert?
            if (!settings.autoStartEnabled) {
                Log.d(TAG, "Auto-Start nicht aktiviert")
                return@withContext Result.success()
            }

            // Bereits am Laufen?
            if (runningTimeTracker.isTracking()) {
                Log.d(TAG, "Zeiterfassung läuft bereits")
                return@withContext Result.success()
            }

            // Heute schon Eintrag mit Start-Zeit?
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existingEntry = database.timeEntryDao().getEntryByDate(today)
            if (existingEntry?.startZeit != null) {
                Log.d(TAG, "Heute bereits gestartet")
                return@withContext Result.success()
            }

            // Start-Zeit für heute konfiguriert?
            val dayOfWeek = LocalDate.now().dayOfWeek.value
            val startZeitMinutes = settings.getAutoStartZeitForDay(dayOfWeek)
            if (startZeitMinutes == null) {
                Log.d(TAG, "Kein Auto-Start für heute (Tag $dayOfWeek) konfiguriert")
                return@withContext Result.success()
            }

            // Aktuelle Zeit prüfen
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            val startZeit = LocalTime.of(startZeitMinutes / 60, startZeitMinutes % 60)

            // Noch zu früh?
            if (nowMinutes < startZeitMinutes) {
                Log.d(TAG, "Noch zu früh: jetzt $now, Start $startZeit")
                return@withContext Result.success()
            }

            // Zu spät? (mehr als 2h nach Startzeit)
            if (nowMinutes > startZeitMinutes + MAX_DELAY_MINUTES) {
                Log.d(TAG, "Zu spät für Auto-Start: jetzt $now, Start war $startZeit")
                return@withContext Result.success()
            }

            // Geofencing-Prüfung (wenn erforderlich)
            if (settings.autoStartRequiresGeofencing) {
                Log.d(TAG, "Prüfe Standort...")

                val isAtWorkLocation = geofencingManager.checkActiveLocationAtWork()
                if (!isAtWorkLocation) {
                    Log.d(TAG, "Nicht am Arbeitsort - kein Auto-Start")
                    return@withContext Result.success()
                }

                Log.d(TAG, "Am Arbeitsort bestätigt!")
            }

            // Alle Bedingungen erfüllt → Auto-Start durchführen!
            Log.i(TAG, "Alle Bedingungen erfüllt - führe Auto-Start durch")

            val success = autoStartManager.performAutoStart()

            if (success) {
                Log.i(TAG, "Auto-Start erfolgreich!")
                showSuccessNotification(startZeit)
            } else {
                Log.w(TAG, "Auto-Start fehlgeschlagen")
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Fehler im AutoStartWorker: ${e.message}", e)
            Result.failure()
        }
    }

    private fun showSuccessNotification(startZeit: LocalTime) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_time)
            .setContentTitle("Arbeitszeit gestartet")
            .setContentText("Automatisch gestartet um ${startZeit.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(100, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto-Start Benachrichtigungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für automatischen Start der Zeiterfassung"
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
