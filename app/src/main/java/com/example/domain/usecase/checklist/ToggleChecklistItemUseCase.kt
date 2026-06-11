package com.example.domain.usecase.checklist

import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для переключения состояния выполнения пункта чек-листа.
 */
class ToggleChecklistItemUseCase(private val repository: ITaskRepository) {

    /**
     * Находит нужный пункт чек-листа, меняет его состояние на противоположное и обновляет список.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist, itemId: String) {
        val updatedChecklist = wrapper.checklist.map {
            if (it.id == itemId) it.copy(isCompleted = !it.isCompleted) else it
        }
        repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
    }
}
