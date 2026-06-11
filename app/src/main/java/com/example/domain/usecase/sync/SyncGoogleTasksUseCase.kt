package com.example.domain.usecase.sync

import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для выполнения двусторонней синхронизации с Google Tasks.
 */
class SyncGoogleTasksUseCase(private val repository: ITaskRepository) {

    /**
     * Вызывает метод синхронизации в репозитории с использованием переданного OAuth-токена.
     */
    suspend operator fun invoke(accessToken: String): Result<Unit> {
        return repository.syncWithGoogleTasks(accessToken)
    }
}
