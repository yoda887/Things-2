package com.example

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.reminders.ReminderSync
import com.example.repeat.RepeatSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Пользовательский класс Application приложения.
 * Инициализируется Hilt для автоматического сопоставления зависимостей на уровне приложения.
 */
@HiltAndroidApp
class ThingsApplication : Application(), Configuration.Provider {

    /** Фоновые задачи (автоматическая копия в Google Drive) получают зависимости от Hilt */
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** Будильники напоминаний следуют за базой, пока жив процесс */
    @Inject lateinit var reminderSync: ReminderSync

    /** Копии повторяющихся задач создаются по их правилам: при запуске, при изменениях и каждую полночь */
    @Inject lateinit var repeatSync: RepeatSync

    override fun onCreate() {
        super.onCreate()
        reminderSync.start()
        repeatSync.start()
    }
}
