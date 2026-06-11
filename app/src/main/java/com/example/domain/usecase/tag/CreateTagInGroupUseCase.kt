package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для создания тега в определенной группе.
 */
class CreateTagInGroupUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Создает тег с привязкой к родительской группе.
     */
    suspend operator fun invoke(tagName: String, parentId: String?): Tag {
        return repository.createTagInGroup(tagName, parentId)
    }
}
