package com.example.domain.usecase.sync

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий чтения (Use Case) для получения событий из локального системного календаря.
 */
class FetchLocalCalendarEventsUseCase(private val repository: ITaskRepository) {

    /**
     * Извлекает локальные события календаря и возвращает в виде списка Item, обернутого в Result.
     */
    suspend operator fun invoke(): Result<List<Item>> {
        return repository.fetchLocalCalendarEvents()
    }
}
