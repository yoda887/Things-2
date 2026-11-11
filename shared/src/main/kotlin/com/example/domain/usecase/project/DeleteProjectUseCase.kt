package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для удаления проекта.
 */
class DeleteProjectUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Удаляет проект из репозитория.
     */
    suspend operator fun invoke(project: Item) {
        repository.deleteProject(project)
    }
}
