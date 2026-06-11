package com.example.domain.usecase.checklist

import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для удаления определенного пункта чек-листа задачи.
 */
class DeleteChecklistItemUseCase(private val repository: ITaskRepository) {

    /**
     * Удаляет пункт чек-листа по ID и обновляет список в БД.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist, itemId: String) {
        val updatedChecklist = wrapper.checklist.filter { it.id != itemId }
        repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
    }
}
