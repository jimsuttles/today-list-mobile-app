package com.fourctech.todaylist.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fourctech.todaylist.core.notifications.AndroidNotificationScheduler
import com.fourctech.todaylist.core.notifications.NotificationScheduler
import com.fourctech.todaylist.core.time.ClockProvider
import com.fourctech.todaylist.core.time.SystemClockProvider
import com.fourctech.todaylist.domain.recurrence.DefaultRecurrenceEngine
import com.fourctech.todaylist.domain.recurrence.RecurrenceEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {
    @Binds
    @Singleton
    abstract fun bindClockProvider(impl: SystemClockProvider): ClockProvider

    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(impl: AndroidNotificationScheduler): NotificationScheduler

    @Binds
    @Singleton
    abstract fun bindRecurrenceEngine(impl: DefaultRecurrenceEngine): RecurrenceEngine

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("today_list_settings") },
            )
    }
}
