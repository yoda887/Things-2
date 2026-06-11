package com.example.domain.usecase.query

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Сценарий чтения (Use Case) для наблюдения за всеми проектами (тип = 1) в реальном времени.
 */
class ObserveAllProjectsUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Возвращает реактивный поток всех проектов.
     */
    operator fun invoke(): Flow<List<Item>> {
        return repository.observeAllProjects()
    }
}
