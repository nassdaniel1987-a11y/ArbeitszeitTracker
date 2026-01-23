package com.arbeitszeit.tracker.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.arbeitszeit.tracker.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * AlarmManager-basierter Auto-Start
 *
 * Setzt exakte Alarme für die konfigurierten Start-Zeiten.
 * Zuverlässiger als WorkManager für zeitkritische Aufgaben.
 */
class AutoStartAlarmManager(private val context: Context) {

    companion object {
        private const val TAG = "AutoStartAlarmManager"
        private const val REQUEST_CODE = 1001
        const val ACTION_AUTO_START = "com.arbeitszeit.tracker.AUTO_START_ALARM"

        /**
         * Plant den nächsten Auto-Start Alarm
         */
        fun scheduleNextAlarm(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val settings = database.userSettingsDao().getSettings()

                    if (settings == null || !settings.autoStartEnabled) {
                        Log.d(TAG, "Auto-Start nicht aktiviert")
                        cancelAlarm(context)
                        return@launch
                    }

                    // Finde nächste Start-Zeit
                    val nextAlarmTime = findNextStartTime(settings)
                    if (nextAlarmTime == null) {
                        Log.d(TAG, "Keine Start-Zeit konfiguriert")
                        return@launch
                    }

                    setExactAlarm(context, nextAlarmTime)

                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Planen des Alarms: ${e.message}", e)
                }
            }
        }

        private fun findNextStartTime(settings: com.arbeitszeit.tracker.data.entity.UserSettings): LocalDateTime? {
            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            // Prüfe die nächsten 7 Tage
            for (daysAhead in 0..6) {
                val checkDate = today.plusDays(daysAhead.toLong())
                val dayOfWeek = checkDate.dayOfWeek.value // 1=Mo, 7=So

                val startMinutes = settings.getAutoStartZeitForDay(dayOfWeek)
                if (startMinutes != null) {
                    val startTime = LocalTime.of(startMinutes / 60, startMinutes % 60)
                    val alarmDateTime = LocalDateTime.of(checkDate, startTime)

                    // Wenn heute und Zeit noch nicht vorbei, oder zukünftiger Tag
                    if (alarmDateTime.isAfter(now)) {
                        Log.d(TAG, "Nächster Alarm: $alarmDateTime")
                        return alarmDateTime
                    }
                }
            }

            return null
        }

        private fun setExactAlarm(context: Context, dateTime: LocalDateTime) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoStartAlarmReceiver::class.java).apply {
                action = ACTION_AUTO_START
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        Log.i(TAG, "Exakter Alarm gesetzt für: $dateTime")
                    } else {
                        // Fallback: Inexakter Alarm
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        Log.i(TAG, "Inexakter Alarm gesetzt für: $dateTime (keine exakte Alarm-Berechtigung)")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "Exakter Alarm gesetzt für: $dateTime")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Keine Berechtigung für exakte Alarme", e)
                // Fallback
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoStartAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.i(TAG, "Alarm abgebrochen")
        }
    }
}

/**
 * BroadcastReceiver für den Auto-Start Alarm
 */
class AutoStartAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoStartAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Auto-Start Alarm empfangen!")

        if (intent.action == AutoStartAlarmManager.ACTION_AUTO_START ||
            intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Führe Auto-Start in Coroutine aus
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    performAutoStart(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Auto-Start: ${e.message}", e)
                } finally {
                    // Plane nächsten Alarm
                    AutoStartAlarmManager.scheduleNextAlarm(context)
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun performAutoStart(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val settings = database.userSettingsDao().getSettings()

        if (settings == null || !settings.autoStartEnabled) {
            Log.d(TAG, "Auto-Start nicht aktiviert")
            return
        }

        val runningTimeTracker = com.arbeitszeit.tracker.autostart.RunningTimeTracker(context)

        // Bereits am Laufen?
        if (runningTimeTracker.isTracking()) {
            Log.d(TAG, "Zeiterfassung läuft bereits")
            return
        }

        // Heute schon Eintrag mit Start-Zeit?
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existingEntry = database.timeEntryDao().getEntryByDate(today)
        if (existingEntry?.startZeit != null) {
            Log.d(TAG, "Heute bereits gestartet")
            return
        }

        // Start-Zeit für heute
        val dayOfWeek = LocalDate.now().dayOfWeek.value
        val startZeitMinutes = settings.getAutoStartZeitForDay(dayOfWeek)
        if (startZeitMinutes == null) {
            Log.d(TAG, "Kein Auto-Start für heute konfiguriert")
            return
        }

        // Geofencing-Prüfung (wenn erforderlich)
        if (settings.autoStartRequiresGeofencing) {
            val geofencingManager = com.arbeitszeit.tracker.geofencing.GeofencingManager(context)

            // Prüfe Hintergrund-Berechtigung
            if (!geofencingManager.hasBackgroundLocationPermission()) {
                Log.w(TAG, "Keine Hintergrund-Standort-Berechtigung")
                showPermissionNotification(context)
                return
            }

            val isAtWorkLocation = geofencingManager.checkActiveLocationAtWork()
            if (!isAtWorkLocation) {
                Log.d(TAG, "Nicht am Arbeitsort - kein Auto-Start")
                return
            }
            Log.d(TAG, "Am Arbeitsort bestätigt!")
        }

        // TimeEntry erstellen falls nicht vorhanden
        var entry = existingEntry
        if (entry == null) {
            Log.d(TAG, "Erstelle neuen TimeEntry für heute")
            val newEntry = com.arbeitszeit.tracker.data.entity.TimeEntry(
                datum = today,
                startZeit = null,
                endeZeit = null,
                pauseMinuten = 0,
                notizen = null,
                istUrlaubstag = false,
                istKrankheitstag = false,
                istFeiertag = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            database.timeEntryDao().insert(newEntry)
            entry = database.timeEntryDao().getEntryByDate(today)
        }

        if (entry == null) {
            Log.e(TAG, "Konnte TimeEntry nicht erstellen")
            return
        }

        // Update TimeEntry mit Start-Zeit
        database.timeEntryDao().update(entry.copy(
            startZeit = startZeitMinutes,
            updatedAt = System.currentTimeMillis()
        ))

        // Starte Tracking
        val startTime = LocalTime.of(startZeitMinutes / 60, startZeitMinutes % 60)
        runningTimeTracker.startTracking(
            startTime = startTime,
            isAutoStart = true,
            date = today
        )

        Log.i(TAG, "Auto-Start erfolgreich: $startTime")
        showSuccessNotification(context, startTime)
    }

    private fun showSuccessNotification(context: Context, startTime: LocalTime) {
        val channelId = "auto_start_channel"
        createNotificationChannel(context, channelId)

        val intent = Intent(context, com.arbeitszeit.tracker.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.arbeitszeit.tracker.R.drawable.ic_notification_time)
            .setContentTitle("Arbeitszeit gestartet")
            .setContentText("Automatisch gestartet um ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(100, notification)
    }

    private fun showPermissionNotification(context: Context) {
        val channelId = "auto_start_channel"
        createNotificationChannel(context, channelId)

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.arbeitszeit.tracker.R.drawable.ic_notification_time)
            .setContentTitle("Auto-Start fehlgeschlagen")
            .setContentText("Bitte Hintergrund-Standort-Berechtigung erteilen")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(101, notification)
    }

    private fun createNotificationChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Auto-Start Benachrichtigungen",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für automatischen Start der Zeiterfassung"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

/**
 * Receiver für Boot-Completed um Alarme nach Neustart wiederherzustellen
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootCompletedReceiver", "Boot completed - plane Auto-Start Alarm")
            AutoStartAlarmManager.scheduleNextAlarm(context)
        }
    }
}
