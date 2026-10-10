package com.example.domain.usecase.task

import com.example.domain.edits.TaskStatuses
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Отмена задачи (или возврат отменённой). Задачу берёт из базы по id, а не из экрана: отменяют её из
 * открытого редактора, и его правки, сохранённые при сворачивании, не должны затереться устаревшей копией.
 */
class ToggleTaskCancelUseCase @Inject constructor(private val repository: ITaskRepository) {

    suspend operator fun invoke(taskId: String) {
        repository.inTransaction {
            val task = repository.getTask(taskId) ?: return@inTransaction
            repository.insertTask(TaskStatuses.toggledCancel(task))
        }
    }
}
