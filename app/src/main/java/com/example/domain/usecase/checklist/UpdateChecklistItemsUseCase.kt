package com.example.domain.usecase.checklist

import com.example.data.model.ChecklistItem
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для обновления списка подпунктов чек-листа для конкретной задачи.
 */
class UpdateChecklistItemsUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Обновляет пункты чек-листа задачи в репозитории.
     */
    suspend operator fun invoke(itemId: String, list: List<ChecklistItem>) {
        repository.updateChecklistItems(itemId, list)
    }
}
