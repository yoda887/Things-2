package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для создания тега в определенной группе.
 */
class CreateTagInGroupUseCase(private val repository: ITaskRepository) {

    /**
     * Создает тег с привязкой к родительской группе.
     */
    suspend operator fun invoke(tagName: String, parentId: String?): Tag {
        return repository.createTagInGroup(tagName, parentId)
    }
}
