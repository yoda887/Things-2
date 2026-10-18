package com.example.data.sync

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.room.InvalidationTracker
import com.example.data.backup.DriveAuth
import com.example.data.local.AppDatabase
import com.example.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Когда синхронизироваться самим, пока приложение открыто: сразу при запуске и через [CHANGE_DELAY_MS] после правок
 * (несколько правок подряд — одна синхронизация). Изменения, которые внесла сама синхронизация, поводом не считаются.
 *
 * Пока приложение на экране, оно ещё и забирает правки других устройств: при возвращении в приложение и каждые
 * [FOREGROUND_PULL_MS]. Фоновая синхронизация (раз в 15 минут) на телефонах с жёсткой экономией батареи может
 * не запускаться — тогда открытое приложение показывало бы чужие правки только после перезапуска.
 */
@Singleton
class SyncAutoRun @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val database: AppDatabase,
    private val manager: SyncManager
) {

    private var pending: Job? = null
    private var foregroundPull: Job? = null
    private var visibleActivities = 0

    fun start() {
        (context as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                visibleActivities++
                if (visibleActivities == 1) onForeground()
            }

            override fun onActivityStopped(activity: Activity) {
                visibleActivities = (visibleActivities - 1).coerceAtLeast(0)
                if (visibleActivities == 0) foregroundPull?.cancel()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(SyncTables.ORDER.toTypedArray()) {
            override fun onInvalidated(tables: Set<String>) {
                // Правки самой синхронизации — не повод синхронизироваться снова
                if (System.currentTimeMillis() - manager.lastAppliedAt < OWN_CHANGES_WINDOW_MS) return
                requestSoon()
            }
        })
        run(delayMs = 0L)
    }

    private fun requestSoon() = run(CHANGE_DELAY_MS)

    /** Приложение снова на экране: сразу забрать правки других устройств и дальше — раз в [FOREGROUND_PULL_MS] */
    private fun onForeground() {
        foregroundPull?.cancel()
        foregroundPull = scope.launch {
            while (true) {
                // Своя синхронизация уже ждёт (после правок) — она и заберёт пришедшее
                if (pending?.isActive != true) run(delayMs = 0L)
                delay(FOREGROUND_PULL_MS)
            }
        }
    }

    private fun run(delayMs: Long) {
        if (!manager.state.value.enabled) return
        pending?.cancel()
        pending = scope.launch {
            delay(delayMs)
            val token = DriveAuth.tokenSilently(context) ?: run {
                manager.reportError(SyncManager.Error.ACCESS)
                return@launch
            }
            manager.sync(token)
        }
    }

    private companion object {
        /** Через столько после последней правки — синхронизация */
        const val CHANGE_DELAY_MS = 20_000L

        /** Как часто открытое приложение проверяет правки других устройств (без изменений на Drive — без скачивания) */
        const val FOREGROUND_PULL_MS = 60_000L

        /** Столько после применения пришедших изменений правки базы считаются своими у синхронизации */
        const val OWN_CHANGES_WINDOW_MS = 3_000L
    }
}
