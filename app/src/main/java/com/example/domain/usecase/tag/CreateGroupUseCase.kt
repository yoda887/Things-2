package com.example.domain.usecase.tag

import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для создания корневой группы тегов.
 */
class CreateGroupUseCase(private val repository: ITaskRepository) {

    /**
     * Создает группу с указанным именем в репозитории.
     */
    suspend operator fun invoke(groupName: String): Tag {
        return repository.createGroup(groupName)
    }
}
