package com.arbeitszeit.tracker.autostart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arbeitszeit.tracker.R
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

/**
 * Scheduler für Auto-Start der Arbeitszeiterfassung
 *
 * Features:
 * - Scheduled AlarmManager für präzises Timing
 * - Vor-Erinnerung (z.B. 5 Min vor Start)
 * - Auto-Start zum konfigurierten Zeitpunkt
 */
class AutoStartScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val database = AppDatabase.getDatabase(context)

    companion object {
        private const val TAG = "AutoStartScheduler"
        private const val REQUEST_CODE_REMINDER = 1001
        private const val REQUEST_CODE_START = 1002

        const val ACTION_REMINDER = "com.arbeitszeit.tracker.AUTO_START_REMINDER"
        const val ACTION_START = "com.arbeitszeit.tracker.AUTO_START"
    }

    /**
     * Scheduled alle Auto-Start Alarme für die nächsten Tage
     *
     * Ruft diese Methode auf:
     * - Beim App-Start
     * - Nach Änderung der Einstellungen
     * - Nach Änderung der Wochenvorlagen
     */
    suspend fun scheduleAutoStarts() {
        try {
            val settings = database.userSettingsDao().getSettings() ?: return

            if (!settings.autoStartEnabled) {
                Log.d(TAG, "Auto-Start deaktiviert, cancelling alle Alarme")
                cancelAllAlarms()
                return
            }

            // Schedule für morgen
            scheduleTomorrow(settings.autoStartReminderMinutes)

            Log.i(TAG, "Auto-Start Alarme erfolgreich geplant")

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Planen der Auto-Start Alarme", e)
        }
    }

    /**
     * Scheduled Auto-Start für morgen
     */
    private suspend fun scheduleTomorrow(reminderMinutes: Int) {
        val autoStartManager = AutoStartManager(context)
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value

        // Lade Wochenvorlage für morgen
        val templates = database.weekTemplateDao().getAllTemplates()
        if (templates.isEmpty()) {
            Log.d(TAG, "Keine Wochenvorlagen vorhanden")
            return
        }

        val activeTemplate = templates.firstOrNull() ?: return
        val entries = database.weekTemplateDao().getEntriesForTemplate(activeTemplate.id)
        val tomorrowEntry = entries.firstOrNull { it.dayOfWeek == tomorrowDayOfWeek }

        if (tomorrowEntry == null || tomorrowEntry.startZeit == null) {
            Log.d(TAG, "Keine Start-Zeit für morgen konfiguriert")
            return
        }

        val startTime = minutesToLocalTime(tomorrowEntry.startZeit)

        // Schedule Reminder (z.B. 5 Min vor Start)
        val reminderTime = startTime.minusMinutes(reminderMinutes.toLong())
        scheduleAlarm(tomorrow, reminderTime, ACTION_REMINDER, REQUEST_CODE_REMINDER)

        // Schedule Auto-Start
        scheduleAlarm(tomorrow, startTime, ACTION_START, REQUEST_CODE_START)

        Log.i(TAG, "Auto-Start für morgen geplant: Reminder=$reminderTime, Start=$startTime")
    }

    /**
     * Scheduled einen einzelnen Alarm
     */
    private fun scheduleAlarm(date: LocalDate, time: LocalTime, action: String, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, AutoStartReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Alarm geplant: $action um ${time.hour}:${time.minute}")
    }

    /**
     * Cancelt alle Auto-Start Alarme
     */
    fun cancelAllAlarms() {
        cancelAlarm(REQUEST_CODE_REMINDER)
        cancelAlarm(REQUEST_CODE_START)
        Log.d(TAG, "Alle Alarme gecancelt")
    }

    /**
     * Cancelt einen einzelnen Alarm
     */
    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, AutoStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Konvertiert Minuten seit Mitternacht zu LocalTime
     */
    private fun minutesToLocalTime(minutes: Int): LocalTime {
        val hours = minutes / 60
        val mins = minutes % 60
        return LocalTime.of(hours, mins)
    }
}

/**
 * BroadcastReceiver für Auto-Start Alarme
 */
class AutoStartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoStartReceiver"
        private const val NOTIFICATION_ID_REMINDER = 2001
        private const val NOTIFICATION_ID_RUNNING = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm empfangen: ${intent.action}")

        when (intent.action) {
            AutoStartScheduler.ACTION_REMINDER -> {
                handleReminder(context)
            }
            AutoStartScheduler.ACTION_START -> {
                handleAutoStart(context)
            }
        }
    }

    /**
     * Zeigt Vor-Erinnerung an
     */
    private fun handleReminder(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            val autoStartManager = AutoStartManager(context)
            val startTime = autoStartManager.getStartTimeForToday()

            if (startTime != null) {
                showReminderNotification(context, startTime)
            }
        }
    }

    /**
     * Führt Auto-Start durch
     */
    private fun handleAutoStart(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val autoStartManager = AutoStartManager(context)

            if (autoStartManager.shouldAutoStart()) {
                val success = autoStartManager.performAutoStart()

                if (success) {
                    // Zeige Benachrichtigung
                    CoroutineScope(Dispatchers.Main).launch {
                        showRunningNotification(context)
                    }

                    // Schedule für morgen
                    val scheduler = AutoStartScheduler(context)
                    scheduler.scheduleAutoStarts()
                }
            } else {
                Log.d(TAG, "Auto-Start abgebrochen (Bedingungen nicht erfüllt)")
            }
        }
    }

    /**
     * Zeigt Vor-Erinnerungs-Benachrichtigung
     */
    private fun showReminderNotification(context: Context, startTime: LocalTime) {
        NotificationHelper.ensureNotificationChannelExists(context)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Auto-Start in Kürze")
            .setContentText("Arbeitszeit startet automatisch um ${startTime.hour}:${String.format("%02d", startTime.minute)} Uhr")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
    }

    /**
     * Zeigt persistente Benachrichtigung für laufende Zeiterfassung
     */
    private fun showRunningNotification(context: Context) {
        NotificationHelper.ensureNotificationChannelExists(context)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Arbeitszeit läuft")
            .setContentText("Automatisch gestartet")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)  // Persistent
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RUNNING, notification)
    }
}
