package com.example.di

import android.content.Context
import com.example.data.local.DeviceCalendarDataSource
import com.example.data.local.LocalTaskDataSource
import com.example.data.local.TaskDao
import com.example.data.remote.GoogleTasksService
import com.example.data.remote.RemoteTaskDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideLocalTaskDataSource(taskDao: TaskDao): LocalTaskDataSource {
        return LocalTaskDataSource(taskDao)
    }

    @Provides
    @Singleton
    fun provideRemoteTaskDataSource(api: GoogleTasksService): RemoteTaskDataSource {
        return RemoteTaskDataSource(api)
    }

    @Provides
    @Singleton
    fun provideDeviceCalendarDataSource(@ApplicationContext context: Context): DeviceCalendarDataSource {
        return DeviceCalendarDataSource(context)
    }
}
