package com.example.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Резервная копия базы в Google Drive: «Зберегти зараз», «Відновити з копії» и автоматическая копия раз в день
 * (WorkManager, только с сетью). На Drive — одна копия, каждая запись её заменяет.
 */
@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DriveBackupApi,
    private val database: DatabaseBackup
) {

    /** Состояние для экрана настроек */
    data class State(
        val connected: Boolean = false,
        val autoBackup: Boolean = true,
        val busy: Boolean = false,
        /** Когда сделана копия на Drive (мс); null — копии нет или ещё не узнали */
        val remoteBackupAt: Long? = null,
        val error: Error? = null
    )

    enum class Error { NETWORK, ACCESS, NO_BACKUP, BAD_BACKUP }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        State(
            connected = prefs.getBoolean(KEY_CONNECTED, false),
            autoBackup = prefs.getBoolean(KEY_AUTO, true),
            remoteBackupAt = prefs.getLong(KEY_REMOTE_AT, 0L).takeIf { it > 0 }
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    /** Одна операция с Drive за раз: ручная копия и фоновая не пишут одновременно */
    private val lock = Mutex()

    /** Доступ к Drive получен — копия становится автоматической, если она включена */
    suspend fun onConnected(token: String) {
        prefs.edit().putBoolean(KEY_CONNECTED, true).apply()
        _state.update { it.copy(connected = true, error = null) }
        schedule(_state.value.autoBackup)
        refresh(token)
    }

    /** Отключить: копия на Drive остаётся, автоматическая копия больше не идёт */
    fun disconnect() {
        prefs.edit().putBoolean(KEY_CONNECTED, false).apply()
        _state.update { it.copy(connected = false, error = null) }
        schedule(false)
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        _state.update { it.copy(autoBackup = enabled) }
        schedule(enabled && _state.value.connected)
    }

    /** Узнать, есть ли копия на Drive и когда она сделана */
    suspend fun refresh(token: String) = operation {
        val remote = api.find(token)
        remember(DriveFiles.parseTime(remote?.modifiedTime))
    }

    /** Записать копию базы на Drive (вместо прежней) */
    suspend fun backupNow(token: String): Boolean? = operation {
        val snapshot = database.snapshot()
        try {
            val existing = api.find(token)
            api.upload(token, snapshot, existing?.id)
            remember(DriveFiles.parseTime(api.find(token)?.modifiedTime) ?: System.currentTimeMillis())
            true
        } finally {
            snapshot.delete()
        }
    }

    /**
     * Заменить базу копией с Drive. Копия сначала скачивается и проверяется; нынешняя база сохраняется на телефоне.
     * true — база заменена, приложение нужно перезапустить ([restartApp]).
     */
    suspend fun restore(token: String): Boolean? = operation {
        val remote = api.find(token)
        if (remote == null) {
            _state.update { it.copy(error = Error.NO_BACKUP) }
            return@operation false
        }
        val downloaded = File(context.cacheDir, "restore_${DriveFiles.BACKUP_NAME}")
        try {
            api.download(token, remote.id, downloaded)
            if (!database.isRestorable(downloaded)) {
                _state.update { it.copy(error = Error.BAD_BACKUP) }
                return@operation false
            }
            database.restore(downloaded)
            true
        } finally {
            downloaded.delete()
        }
    }

    /** Перезапуск приложения после восстановления: база закрыта, открыть заново можно только в новом процессе */
    fun restartApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }

    /** Google не дал доступ (отказ, приложение не зарегистрировано в Google Cloud, нет сети) */
    fun reportError(error: Error) = _state.update { it.copy(error = error) }

    /** Ошибка показана — убрать */
    fun clearError() = _state.update { it.copy(error = null) }

    private fun remember(remoteAt: Long?) {
        prefs.edit().putLong(KEY_REMOTE_AT, remoteAt ?: 0L).apply()
        _state.update { it.copy(remoteBackupAt = remoteAt, error = null) }
    }

    /** Операция с Drive: занятость для экрана, ошибки сети и доступа — в состояние, а не исключением */
    private suspend fun <T> operation(block: suspend () -> T): T? = lock.withLock {
        _state.update { it.copy(busy = true, error = null) }
        try {
            block()
        } catch (e: DriveHttpException) {
            _state.update { it.copy(error = if (e.code == 401 || e.code == 403) Error.ACCESS else Error.NETWORK) }
            null
        } catch (e: java.io.IOException) {
            _state.update { it.copy(error = Error.NETWORK) }
            null
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    private fun schedule(enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val PREFS = "drive_backup"
        const val KEY_CONNECTED = "connected"
        const val KEY_AUTO = "auto_backup"
        const val KEY_REMOTE_AT = "remote_backup_at"
        const val WORK_NAME = "drive_backup_daily"
    }
}
