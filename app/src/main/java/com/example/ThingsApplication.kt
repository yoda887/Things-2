package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Пользовательский класс Application приложения.
 * Инициализируется Hilt для автоматического сопоставления зависимостей на уровне приложения.
 */
@HiltAndroidApp
class ThingsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
