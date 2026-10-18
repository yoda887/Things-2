package com.example.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.backup.DriveAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Фоновая синхронизация устройств. Маркер — без окон; Google просит согласие заново — пропуск до следующего раза. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val manager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!manager.state.value.enabled) return Result.success()
        val token = DriveAuth.tokenSilently(applicationContext) ?: return Result.success()
        return if (manager.sync(token)) Result.success() else Result.retry()
    }
}
