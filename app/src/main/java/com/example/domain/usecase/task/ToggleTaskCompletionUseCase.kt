package com.example.domain.usecase.task

import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для переключения статуса завершения задачи.
 */
class ToggleTaskCompletionUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Меняет статус выполнения задачи на противоположный и обновляет её в репозитории.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist) {
        val item = wrapper.item
        val isCompleting = !item.isCompleted
        val updated = item.copy(
            status = if (isCompleting) 3 else 0,
            stopDate = if (isCompleting) System.currentTimeMillis() else null
        )
        repository.insertTask(updated, wrapper.checklist)
    }
}
