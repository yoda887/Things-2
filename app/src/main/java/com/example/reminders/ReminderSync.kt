package com.example.reminders

import com.example.data.local.TaskDao
import com.example.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Держит будильники напоминаний в согласии с базой: пока жив процесс, любое изменение задач
 * пересчитывает их ([ReminderScheduler.sync]). Запускается при старте приложения.
 */
@Singleton
class ReminderSync @Inject constructor(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            dao.getAllItems().collect { items -> scheduler.sync(items) }
        }
    }

    /** Один пересчёт по текущей базе — после перезагрузки телефона или смены времени. */
    suspend fun syncNow() {
        scheduler.sync(dao.getAllItems().first())
    }
}
