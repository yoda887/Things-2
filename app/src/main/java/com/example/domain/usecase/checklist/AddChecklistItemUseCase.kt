package com.example.domain.usecase.checklist

import com.example.data.model.ItemWithChecklist
import com.example.data.model.ChecklistItem
import com.example.domain.repository.ITaskRepository

/**
 * Сценарий использования (Use Case) для добавления нового пункта в чек-лист конкретной задачи.
 */
class AddChecklistItemUseCase(private val repository: ITaskRepository) {

    /**
     * Создает пункт чек-листа и обновляет весь чек-лист для переданной задачи.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist, title: String) {
        if (title.isBlank()) return
        val updatedChecklist = wrapper.checklist + ChecklistItem(itemId = wrapper.item.id, title = title)
        repository.updateChecklistItems(wrapper.item.id, updatedChecklist)
    }
}
