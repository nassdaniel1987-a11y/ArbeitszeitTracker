package com.arbeitszeit.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Empfängt Aktionen aus Notifications und führt direkte Zeitstempel aus
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STAMP_START = "com.arbeitszeit.tracker.ACTION_STAMP_START"
        const val ACTION_STAMP_END = "com.arbeitszeit.tracker.ACTION_STAMP_END"
        const val ACTION_DISMISS = "com.arbeitszeit.tracker.ACTION_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STAMP_START -> {
                handleStartStamp(context)
            }
            ACTION_STAMP_END -> {
                handleEndStamp(context)
            }
            ACTION_DISMISS -> {
                // Notification wird automatisch geschlossen
            }
        }
    }

    private fun handleStartStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                    // Show success toast on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        val timeString = TimeUtils.minutesToTimeString(currentTime)
                        Toast.makeText(
                            context,
                            "✓ Eingestempelt um $timeString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Fehler beim Stempeln",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleEndStamp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                    // Calculate and show worked time
                    val workedMinutes = entry.getIstMinuten()
                    val workedTime = TimeUtils.minutesToHoursMinutes(workedMinutes)

                    CoroutineScope(Dispatchers.Main).launch {
                        val timeString = TimeUtils.minutesToTimeString(currentTime)
                        Toast.makeText(
                            context,
                            "✓ Ausgestempelt um $timeString\nGearbeitet: $workedTime",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Fehler beim Stempeln",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
