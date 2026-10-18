package com.example.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Автоматическая копия базы на Drive раз в день. Маркер доступа берётся без окон; если Google просит согласие
 * заново (доступ отозван), копия пропускается до следующего раза — экран настроек покажет «Підключити».
 */
@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val manager: DriveBackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!manager.state.value.connected) return Result.success()
        val token = DriveAuth.tokenSilently(applicationContext) ?: return Result.success()
        return if (manager.backupNow(token) == true) Result.success() else Result.retry()
    }
}
