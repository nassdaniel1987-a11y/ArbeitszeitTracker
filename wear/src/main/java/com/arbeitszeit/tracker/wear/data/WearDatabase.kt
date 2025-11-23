package com.arbeitszeit.tracker.wear.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arbeitszeit.tracker.data.dao.TimeEntryDao
import com.arbeitszeit.tracker.data.dao.UserSettingsDao
import com.arbeitszeit.tracker.data.entity.TimeEntry
import com.arbeitszeit.tracker.data.entity.UserSettings

/**
 * Wear OS version of the database - shares schema with mobile app
 * This allows seamless data synchronization between devices
 */
@Database(
    entities = [
        UserSettings::class,
        TimeEntry::class
    ],
    version = 15,
    exportSchema = false
)
abstract class WearDatabase : RoomDatabase() {

    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun timeEntryDao(): TimeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: WearDatabase? = null

        fun getDatabase(context: Context): WearDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WearDatabase::class.java,
                    "arbeitszeit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
