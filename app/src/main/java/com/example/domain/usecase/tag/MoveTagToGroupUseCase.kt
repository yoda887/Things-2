package com.example.domain.usecase.tag

import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для перемещения тега в другую группу.
 */
class MoveTagToGroupUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Изменяет родительский ID выбранного тега в репозитории.
     */
    suspend operator fun invoke(tagId: String, newGroupId: String?) {
        repository.moveTagToGroup(tagId, newGroupId)
    }
}
