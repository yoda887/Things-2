package com.example

import android.app.Application
import com.example.di.AppContainer

/**
 * Пользовательский класс Application приложения.
 * Инициализирует и удерживает долговечный контейнер зависимостей [AppContainer] в течение всего жизненного цикла процесса.
 */
class ThingsApplication : Application() {

    /**
     * Контейнер зависимостей приложения.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
