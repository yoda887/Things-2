package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.backup.DriveBackupApi
import com.example.data.backup.DriveFiles
import com.example.data.backup.DriveHttpException
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
 * Синхронизация устройств через скрытую папку приложения на Google Drive: общий файл [DriveFiles.SYNC_NAME] со всеми
 * записями и журналом удалений. Один проход: найти файл, скачать (если он изменился с прошлого раза), слить со своим
 * состоянием ([SyncMerge]), применить у себя новое с других устройств и записать файл, если в нём не хватает своего.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DriveBackupApi,
    private val database: SyncDatabase
) {

    data class State(
        val enabled: Boolean = false,
        val busy: Boolean = false,
        /** Когда синхронизация последний раз прошла успешно (мс); null — ещё не было */
        val lastSyncAt: Long? = null,
        val error: Error? = null
    )

    /** NETWORK — нет интернета; SERVER — Google временно недоступен; ACCESS — доступ отозван; BAD_FILE — файл из новой версии */
    enum class Error { NETWORK, SERVER, ACCESS, BAD_FILE }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        State(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            lastSyncAt = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    private val lock = Mutex()

    /** Последнее известное общее состояние — чтобы не скачивать файл, если он не менялся */
    private val cacheFile get() = File(context.filesDir, CACHE_NAME)

    /** Когда синхронизация сама изменила базу: эти изменения не повод синхронизироваться снова */
    @Volatile var lastAppliedAt: Long = 0L
        private set

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.update { it.copy(enabled = enabled, error = null) }
        schedule(enabled)
    }

    fun reportError(error: Error) = _state.update { it.copy(error = error) }
    fun clearError() = _state.update { it.copy(error = null) }

    /** Один проход синхронизации; true — прошёл успешно */
    suspend fun sync(token: String): Boolean = lock.withLock {
        _state.update { it.copy(busy = true, error = null) }
        try {
            // Общее состояние прошлого обмена — по нему запись, правленная на обоих устройствах, сливается по полям
            val base = if (cacheFile.exists()) SyncCodec.decode(cacheFile.readBytes()) else null
            var attempt = 0
            var synced = false
            while (!synced) {
                attempt++
                val remoteFile = api.find(token, DriveFiles.SYNC_NAME)
                val cachedMd5 = prefs.getString(KEY_REMOTE_MD5, null)
                val remote: SyncMerge.State = when {
                    remoteFile == null -> SyncMerge.State.EMPTY
                    remoteFile.md5 != null && remoteFile.md5 == cachedMd5 && cacheFile.exists() ->
                        SyncCodec.decode(cacheFile.readBytes()) ?: download(token, remoteFile.id)
                    else -> download(token, remoteFile.id)
                } ?: run {
                    _state.update { it.copy(error = Error.BAD_FILE) }
                    return@withLock false
                }

                val now = System.currentTimeMillis()
                val result = SyncMerge.merge(database.read(), remote, base)
                val merged = SyncMerge.pruneDeletions(result.merged, now)
                if (result.applyLocally.isNotEmpty() || result.deleteLocally.isNotEmpty()) {
                    lastAppliedAt = now
                    database.apply(result.applyLocally, result.deleteLocally, pruneBefore = now - SyncMerge.DELETION_KEEP_MS)
                }

                val bytes = SyncCodec.encode(merged)
                if (result.remoteChanged || remoteFile == null) {
                    // Пока сливали, другое устройство могло записать общий файл — тогда слить заново с его версией:
                    // иначе эта выгрузка затёрла бы его правки (Drive не умеет «записать, только если не менялся»)
                    if (attempt < SYNC_ATTEMPTS && api.find(token, DriveFiles.SYNC_NAME)?.md5 != remoteFile?.md5) continue
                    val upload = File(context.cacheDir, DriveFiles.SYNC_NAME)
                    try {
                        upload.writeBytes(bytes)
                        api.upload(token, upload, remoteFile?.id, DriveFiles.SYNC_NAME)
                    } finally {
                        upload.delete()
                    }
                }
                // Запоминаем общее состояние и его отпечаток — в следующий раз без скачивания, если его никто не менял
                cacheFile.writeBytes(bytes)
                prefs.edit()
                    .putString(KEY_REMOTE_MD5, api.find(token, DriveFiles.SYNC_NAME)?.md5)
                    .putLong(KEY_LAST_SYNC, now)
                    .apply()
                _state.update { it.copy(lastSyncAt = now) }
                synced = true
            }
            true
        } catch (e: DriveHttpException) {
            _state.update { it.copy(error = if (e.isAccess) Error.ACCESS else Error.SERVER) }
            false
        } catch (e: java.io.IOException) {
            _state.update { it.copy(error = Error.NETWORK) }
            false
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    private suspend fun download(token: String, id: String): SyncMerge.State? {
        val target = File(context.cacheDir, "download_${DriveFiles.SYNC_NAME}")
        return try {
            api.download(token, id, target)
            SyncCodec.decode(target.readBytes())
        } finally {
            target.delete()
        }
    }

    /** Фоновая синхронизация каждые 15 минут (меньше WorkManager не позволяет), только с сетью */
    private fun schedule(enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val PREFS = "drive_sync"
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_REMOTE_MD5 = "remote_md5"
        const val CACHE_NAME = "sync_state_cache.json.gz"
        const val WORK_NAME = "drive_sync"
        /** Сколько раз слить заново, если общий файл меняли, пока сливали */
        const val SYNC_ATTEMPTS = 3
    }
}
