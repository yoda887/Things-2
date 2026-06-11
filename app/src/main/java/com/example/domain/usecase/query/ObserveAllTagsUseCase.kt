package com.example.domain.usecase.query

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow

/**
 * Сценарий чтения (Use Case) для наблюдения за всеми тегами в реальном времени.
 */
class ObserveAllTagsUseCase(private val repository: ITaskRepository) {

    /**
     * Возвращает реактивный поток всех тегов.
     */
    operator fun invoke(): Flow<List<Tag>> {
        return repository.getAllTagsFlow()
    }
}
