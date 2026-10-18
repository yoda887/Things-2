package com.example.data.sync

import android.content.Context
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
 */
@Singleton
class SyncAutoRun @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val database: AppDatabase,
    private val manager: SyncManager
) {

    private var pending: Job? = null

    fun start() {
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

    private fun run(delayMs: Long) {
        if (!manager.state.value.enabled) return
        pending?.cancel()
        pending = scope.launch {
            delay(delayMs)
            val token = DriveAuth.tokenSilently(context) ?: return@launch
            manager.sync(token)
        }
    }

    private companion object {
        /** Через столько после последней правки — синхронизация */
        const val CHANGE_DELAY_MS = 20_000L

        /** Столько после применения пришедших изменений правки базы считаются своими у синхронизации */
        const val OWN_CHANGES_WINDOW_MS = 3_000L
    }
}
