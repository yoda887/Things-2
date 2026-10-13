package com.example

import android.app.Application
import com.example.reminders.ReminderSync
import com.example.repeat.RepeatSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Пользовательский класс Application приложения.
 * Инициализируется Hilt для автоматического сопоставления зависимостей на уровне приложения.
 */
@HiltAndroidApp
class ThingsApplication : Application() {

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
