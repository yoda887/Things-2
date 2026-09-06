package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для удаления задачи из системы.
 */
class DeleteTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Удаляет переданную задачу из БД.
     */
    suspend operator fun invoke(item: Item) {
        repository.deleteTask(item)
    }

    /**
     * Пакетно удаляет список задач из БД.
     */
    suspend operator fun invoke(items: List<Item>) {
        repository.deleteTasks(items)
    }
}
