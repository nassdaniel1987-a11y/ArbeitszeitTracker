package com.arbeitszeit.tracker.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Legacy-Stub damit bestehende WorkManager-Einträge nicht crashen.
 * Bricht alte Periodic Work ab und gibt sofort Erfolg zurück.
 * Die echte Planung übernimmt ReminderAlarmManager.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WorkManager.getInstance(applicationContext).apply {
            cancelUniqueWork("morning_reminder")
            cancelUniqueWork("evening_reminder")
            cancelUniqueWork("missing_entries_check")
        }
        return Result.success()
    }

    companion object {
        fun scheduleMorningReminder(context: Context, hour: Int = 7, minute: Int = 30) =
            ReminderAlarmManager.scheduleAll(context)

        fun scheduleEveningReminder(context: Context, hour: Int = 17, minute: Int = 0) = Unit

        fun scheduleMissingEntriesCheck(context: Context, hour: Int = 20, minute: Int = 0) = Unit

        fun cancelAllReminders(context: Context) = ReminderAlarmManager.cancelAll(context)
    }
}

/**
 * Exaktes AlarmManager-basiertes Scheduling für Erinnerungen.
 * Setzt täglich drei Alarme: Morgen (7:30), Abend (17:00), Fehlende Einträge (20:00).
 */
object ReminderAlarmManager {

    private const val TAG = "ReminderAlarmManager"

    const val ACTION_MORNING = "com.arbeitszeit.tracker.REMINDER_MORNING"
    const val ACTION_EVENING = "com.arbeitszeit.tracker.REMINDER_EVENING"
    const val ACTION_MISSING = "com.arbeitszeit.tracker.REMINDER_MISSING"

    private const val RC_MORNING = 3001
    private const val RC_EVENING = 3002
    private const val RC_MISSING = 3003

    private val MORNING_TIME = LocalTime.of(7, 30)
    private val EVENING_TIME = LocalTime.of(17, 0)
    private val MISSING_TIME = LocalTime.of(20, 0)

    fun scheduleAll(context: Context) {
        // Alte WorkManager-Einträge abräumen
        WorkManager.getInstance(context).apply {
            cancelUniqueWork("morning_reminder")
            cancelUniqueWork("evening_reminder")
            cancelUniqueWork("missing_entries_check")
        }
        scheduleAlarm(context, ACTION_MORNING, MORNING_TIME, RC_MORNING)
        scheduleAlarm(context, ACTION_EVENING, EVENING_TIME, RC_EVENING)
        scheduleAlarm(context, ACTION_MISSING, MISSING_TIME, RC_MISSING)
        Log.i(TAG, "Alle Reminder-Alarme geplant")
    }

    fun cancelAll(context: Context) {
        cancelAlarm(context, ACTION_MORNING, RC_MORNING)
        cancelAlarm(context, ACTION_EVENING, RC_EVENING)
        cancelAlarm(context, ACTION_MISSING, RC_MISSING)
        Log.i(TAG, "Alle Reminder-Alarme abgebrochen")
    }

    fun scheduleAlarm(context: Context, action: String, targetTime: LocalTime, requestCode: Int) {
        val now = LocalDateTime.now()
        var alarmDateTime = LocalDateTime.of(now.toLocalDate(), targetTime)
        if (!alarmDateTime.isAfter(now)) {
            alarmDateTime = alarmDateTime.plusDays(1)
        }

        val triggerMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                Log.d(TAG, "Inexakter Alarm (keine SCHEDULE_EXACT_ALARM Berechtigung): $action um $alarmDateTime")
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                Log.d(TAG, "Exakter Alarm: $action um $alarmDateTime")
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            Log.w(TAG, "Fallback-Alarm: $action", e)
        }
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }
}

/**
 * Empfängt Reminder-Alarme und zeigt die entsprechende Benachrichtigung.
 * Plant danach den nächsten Alarm für den Folgetag.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm empfangen: ${intent.action}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderAlarmManager.ACTION_MORNING ->
                        NotificationHelper.showMorningReminder(context)
                    ReminderAlarmManager.ACTION_EVENING ->
                        NotificationHelper.showEveningReminder(context)
                    ReminderAlarmManager.ACTION_MISSING ->
                        handleMissingEntries(context)
                }
            } finally {
                // Alarm für nächsten Tag neu planen
                ReminderAlarmManager.scheduleAll(context)
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMissingEntries(context: Context) {
        if (!NotificationHelper.isReminderNotificationAllowed(context)) return

        val database = AppDatabase.getDatabase(context)
        val today = DateUtils.today()
        val entry = database.timeEntryDao().getEntryByDate(today)

        if (entry == null || !entry.isComplete()) {
            val yesterday = DateUtils.yesterday()
            val incompleteEntries = database.timeEntryDao().getIncompleteEntries(yesterday)
            if (incompleteEntries.isNotEmpty()) {
                NotificationHelper.showMissingEntriesReminder(context, incompleteEntries.size)
            }
        }
    }
}
