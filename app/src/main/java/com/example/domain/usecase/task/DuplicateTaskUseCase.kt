package com.example.domain.usecase.task

import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для дублирования задачи со всеми свойствами, тегами и чек-листом.
 */
class DuplicateTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Дублирует задачу, генерирует новые UUID и заново привязывает теги и пункты чек-листа.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist) {
        val newId = UUID.randomUUID().toString()
        val copiedItem = wrapper.item.copy(
            id = newId,
            googleTaskId = null,
            googleTaskListId = null,
            creationDate = System.currentTimeMillis(),
            modificationDate = System.currentTimeMillis()
        )
        val newChecklist = wrapper.checklist.map {
            it.copy(id = UUID.randomUUID().toString(), itemId = newId)
        }
        repository.inTransaction {
            repository.insertTask(copiedItem, newChecklist)
            repository.setItemTagsByTitles(newId, copiedItem.tags)
        }
    }
}
