package com.example.domain.usecase.sync

import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для выполнения двусторонней синхронизации с Google Tasks.
 */
class SyncGoogleTasksUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Вызывает метод синхронизации в репозитории с использованием переданного OAuth-токена.
     */
    suspend operator fun invoke(accessToken: String): Result<Unit> {
        return repository.syncWithGoogleTasks(accessToken)
    }
}
