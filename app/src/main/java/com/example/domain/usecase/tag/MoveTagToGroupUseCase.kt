package com.example.domain.usecase.tag

import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для перемещения тега в другую группу.
 */
class MoveTagToGroupUseCase(private val repository: ITaskRepository) {

    /**
     * Изменяет родительский ID выбранного тега в репозитории.
     */
    suspend operator fun invoke(tagId: String, newGroupId: String?) {
        repository.moveTagToGroup(tagId, newGroupId)
    }
}
