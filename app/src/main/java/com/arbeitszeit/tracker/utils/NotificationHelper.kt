package com.arbeitszeit.tracker.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arbeitszeit.tracker.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID_REMINDERS = "arbeitszeit_reminders"
    private const val CHANNEL_ID_EXPORT = "arbeitszeit_export"

    const val NOTIFICATION_ID_MORNING = 1001
    const val NOTIFICATION_ID_EVENING = 1002
    const val NOTIFICATION_ID_MISSING = 1003
    const val NOTIFICATION_ID_EXPORT = 2001
    const val NOTIFICATION_ID_IMPORT = 2002

    /**
     * Erstellt alle Notification Channels
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Erinnerungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erinnerungen zum Stempeln"
                enableVibration(true)
            }

            val exportChannel = NotificationChannel(
                CHANNEL_ID_EXPORT,
                "Excel-Export",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen über Excel-Exporte"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(exportChannel)
        }
    }

    /**
     * Zeigt Morgen-Erinnerung mit Action Buttons
     */
    fun showMorningReminder(context: Context) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "stamp_start")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Jetzt Stempeln
        val stampStartIntent = Intent(context, com.arbeitszeit.tracker.notification.NotificationActionReceiver::class.java).apply {
            action = com.arbeitszeit.tracker.notification.NotificationActionReceiver.ACTION_STAMP_START
        }
        val stampStartPendingIntent = PendingIntent.getBroadcast(
            context, 1, stampStartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Später
        val dismissIntent = Intent(context, com.arbeitszeit.tracker.notification.NotificationActionReceiver::class.java).apply {
            action = com.arbeitszeit.tracker.notification.NotificationActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Guten Morgen! ☀️")
            .setContentText("Zeit zum Einstempeln")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Nicht vergessen einzustempeln!\nTippe auf 'JETZT STEMPELN' für schnelles Einstempeln."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_upload,
                "JETZT STEMPELN",
                stampStartPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "SPÄTER",
                dismissPendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
    }

    /**
     * Zeigt Abend-Erinnerung mit Restzeit-Info und Action Button
     */
    suspend fun showEveningReminder(context: Context) {
        if (!hasNotificationPermission(context)) return

        // Get current entry to show remaining time
        val database = com.arbeitszeit.tracker.data.database.AppDatabase.getDatabase(context)
        val today = DateUtils.today()
        val entry = database.timeEntryDao().getEntryByDate(today)

        val infoText = if (entry != null && entry.startZeit != null) {
            val istMinuten = entry.getIstMinuten()
            val sollMinuten = entry.sollMinuten
            val differenz = istMinuten - sollMinuten

            when {
                differenz >= 0 -> {
                    val diffText = TimeUtils.formatDifferenz(differenz)
                    "Du hast dein Soll erreicht! Überstunden: $diffText ✓"
                }
                else -> {
                    val remainingMinutes = -differenz
                    val remainingText = TimeUtils.minutesToHoursMinutes(remainingMinutes)
                    "Noch $remainingText bis zum Soll"
                }
            }
        } else {
            "Jetzt ausstempeln! 🏠"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "stamp_end")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Jetzt Ausstempeln
        val stampEndIntent = Intent(context, com.arbeitszeit.tracker.notification.NotificationActionReceiver::class.java).apply {
            action = com.arbeitszeit.tracker.notification.NotificationActionReceiver.ACTION_STAMP_END
        }
        val stampEndPendingIntent = PendingIntent.getBroadcast(
            context, 3, stampEndIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Feierabend? 🏠")
            .setContentText(infoText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(infoText + "\n\nTippe auf 'JETZT AUSSTEMPELN' für schnelles Ausstempeln."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "JETZT AUSSTEMPELN",
                stampEndPendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
    }

    /**
     * Zeigt Erinnerung für fehlende Einträge
     */
    fun showMissingEntriesReminder(context: Context, count: Int) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "show_calendar")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Fehlende Einträge")
            .setContentText("Du hast $count unvollständige Einträge")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSING, notification)
    }

    /**
     * Zeigt Export-Erfolg Benachrichtigung
     */
    fun showExportSuccess(context: Context, fileName: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Excel exportiert")
            .setContentText("Datei: $fileName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPORT, notification)
    }

    /**
     * Zeigt Import-Erfolg Benachrichtigung
     */
    fun showImportSuccess(context: Context, entriesCount: Int) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EXPORT)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Excel importiert")
            .setContentText("$entriesCount Zeiteinträge importiert")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT, notification)
    }

    /**
     * Prüft ob Notification-Permission gewährt wurde
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Löscht alle Notifications
     */
    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}