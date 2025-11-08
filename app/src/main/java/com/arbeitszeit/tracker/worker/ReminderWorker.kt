package com.arbeitszeit.tracker.worker

import android.content.Context
import androidx.work.*
import com.arbeitszeit.tracker.data.database.AppDatabase
import com.arbeitszeit.tracker.utils.DateUtils
import com.arbeitszeit.tracker.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = AppDatabase.getDatabase(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val type = inputData.getString(KEY_REMINDER_TYPE) ?: return@withContext Result.failure()
        
        when (type) {
            TYPE_MORNING -> handleMorningReminder()
            TYPE_EVENING -> handleEveningReminder()
            TYPE_MISSING_ENTRIES -> handleMissingEntriesCheck()
        }
        
        Result.success()
    }
    
    private suspend fun handleMorningReminder() {
        NotificationHelper.showMorningReminder(applicationContext)
    }
    
    private suspend fun handleEveningReminder() {
        NotificationHelper.showEveningReminder(applicationContext)
    }
    
    private suspend fun handleMissingEntriesCheck() {
        val today = DateUtils.today()
        val entry = database.timeEntryDao().getEntryByDate(today)
        
        // Prüfe ob heute Eintrag fehlt oder unvollständig ist
        if (entry == null || !entry.isComplete()) {
            // Zähle alle unvollständigen Einträge der letzten 7 Tage
            val yesterday = DateUtils.yesterday()
            val incompleteEntries = database.timeEntryDao().getIncompleteEntries(yesterday)
            
            if (incompleteEntries.isNotEmpty()) {
                NotificationHelper.showMissingEntriesReminder(
                    applicationContext,
                    incompleteEntries.size
                )
            }
        }
    }
    
    companion object {
        private const val WORK_NAME_MORNING = "morning_reminder"
        private const val WORK_NAME_EVENING = "evening_reminder"
        private const val WORK_NAME_MISSING = "missing_entries_check"
        
        private const val KEY_REMINDER_TYPE = "reminder_type"
        private const val TYPE_MORNING = "morning"
        private const val TYPE_EVENING = "evening"
        private const val TYPE_MISSING_ENTRIES = "missing_entries"
        
        /**
         * Plant tägliche Morgen-Erinnerung
         */
        fun scheduleMorningReminder(context: Context, hour: Int = 7, minute: Int = 30) {
            val currentTime = LocalTime.now()
            val targetTime = LocalTime.of(hour, minute)
            
            val initialDelay = if (currentTime.isBefore(targetTime)) {
                // Heute noch nicht vorbei
                targetTime.toSecondOfDay() - currentTime.toSecondOfDay()
            } else {
                // Morgen
                (24 * 3600) - currentTime.toSecondOfDay() + targetTime.toSecondOfDay()
            }
            
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay.toLong(), TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_REMINDER_TYPE to TYPE_MORNING))
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_MORNING,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
        
        /**
         * Plant tägliche Abend-Erinnerung
         */
        fun scheduleEveningReminder(context: Context, hour: Int = 17, minute: Int = 0) {
            val currentTime = LocalTime.now()
            val targetTime = LocalTime.of(hour, minute)
            
            val initialDelay = if (currentTime.isBefore(targetTime)) {
                targetTime.toSecondOfDay() - currentTime.toSecondOfDay()
            } else {
                (24 * 3600) - currentTime.toSecondOfDay() + targetTime.toSecondOfDay()
            }
            
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay.toLong(), TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_REMINDER_TYPE to TYPE_EVENING))
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_EVENING,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
        
        /**
         * Plant täglichen Check für fehlende Einträge (20:00 Uhr)
         */
        fun scheduleMissingEntriesCheck(context: Context, hour: Int = 20, minute: Int = 0) {
            val currentTime = LocalTime.now()
            val targetTime = LocalTime.of(hour, minute)
            
            val initialDelay = if (currentTime.isBefore(targetTime)) {
                targetTime.toSecondOfDay() - currentTime.toSecondOfDay()
            } else {
                (24 * 3600) - currentTime.toSecondOfDay() + targetTime.toSecondOfDay()
            }
            
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay.toLong(), TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_REMINDER_TYPE to TYPE_MISSING_ENTRIES))
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_MISSING,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
        
        /**
         * Stoppt alle Erinnerungen
         */
        fun cancelAllReminders(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME_MORNING)
            workManager.cancelUniqueWork(WORK_NAME_EVENING)
            workManager.cancelUniqueWork(WORK_NAME_MISSING)
        }
    }
}
