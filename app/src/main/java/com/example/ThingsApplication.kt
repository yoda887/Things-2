package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Пользовательский класс Application приложения.
 * Инициализируется Hilt для автоматического сопоставления зависимостей на уровне приложения.
 */
@HiltAndroidApp(Application::class)
class ThingsApplication : Hilt_ThingsApplication() {

    override fun onCreate() {
        super.onCreate()
    }
}
