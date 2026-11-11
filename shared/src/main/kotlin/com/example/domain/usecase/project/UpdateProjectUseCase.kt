package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для обновления информации о существующем проекте.
 */
class UpdateProjectUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Обновляет переданный проект и, как у задачи, пересобирает его связи с тегами по строке тегов.
     */
    suspend operator fun invoke(project: Item) {
        repository.inTransaction {
            repository.insertProject(project)
            repository.setItemTagsByTitles(project.id, project.tags)
        }
    }
}
