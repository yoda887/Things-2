package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для создания нового проекта.
 */
class AddProjectUseCase(private val repository: ITaskRepository) {

    /**
     * Создает новый проект (Item с type = 1) и записывает в БД.
     */
    suspend operator fun invoke(name: String, notes: String = "", areaId: String? = null) {
        val project = Item(
            type = 1,
            title = name.ifBlank { "New Project" },
            notes = notes,
            areaId = areaId
        )
        repository.insertProject(project)
    }
}
