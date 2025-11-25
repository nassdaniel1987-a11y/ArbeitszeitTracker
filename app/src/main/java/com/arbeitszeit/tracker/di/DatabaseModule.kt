package com.arbeitszeit.tracker.di

import android.content.Context
import com.arbeitszeit.tracker.data.dao.SollZeitVorlageDao
import com.arbeitszeit.tracker.data.dao.TimeEntryDao
import com.arbeitszeit.tracker.data.dao.UserSettingsDao
import com.arbeitszeit.tracker.data.dao.WeekTemplateDao
import com.arbeitszeit.tracker.data.dao.WorkLocationDao
import com.arbeitszeit.tracker.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module für Database-Injection
 *
 * Stellt Database und DAOs als Singleton zur Verfügung
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Stellt die App-Database als Singleton bereit
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Stellt TimeEntryDao bereit
     */
    @Provides
    @Singleton
    fun provideTimeEntryDao(database: AppDatabase): TimeEntryDao {
        return database.timeEntryDao()
    }

    /**
     * Stellt UserSettingsDao bereit
     */
    @Provides
    @Singleton
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }

    /**
     * Stellt WorkLocationDao bereit
     */
    @Provides
    @Singleton
    fun provideWorkLocationDao(database: AppDatabase): WorkLocationDao {
        return database.workLocationDao()
    }

    /**
     * Stellt WeekTemplateDao bereit
     */
    @Provides
    @Singleton
    fun provideWeekTemplateDao(database: AppDatabase): WeekTemplateDao {
        return database.weekTemplateDao()
    }

    /**
     * Stellt SollZeitVorlageDao bereit
     */
    @Provides
    @Singleton
    fun provideSollZeitVorlageDao(database: AppDatabase): SollZeitVorlageDao {
        return database.sollZeitVorlageDao()
    }
}
