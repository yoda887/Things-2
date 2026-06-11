package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для обновления информации о существующем проекте.
 */
class UpdateProjectUseCase(private val repository: ITaskRepository) {

    /**
     * Обновляет переданный проект.
     */
    suspend operator fun invoke(project: Item) {
        repository.insertProject(project)
    }
}
