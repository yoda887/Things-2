package com.example.domain.usecase.query

import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow

/**
 * Сценарий чтения (Use Case) для наблюдения за всеми задачами и их чек-листами в реальном времени.
 */
class ObserveAllTasksUseCase(private val repository: ITaskRepository) {

    /**
     * Возвращает реактивный поток всех задач.
     */
    operator fun invoke(): Flow<List<ItemWithChecklist>> {
        return repository.observeAllTasks()
    }
}
