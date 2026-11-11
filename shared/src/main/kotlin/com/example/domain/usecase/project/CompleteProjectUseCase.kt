package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для завершения проекта.
 */
class CompleteProjectUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Отмечает проект выполненным, а вместе с ним — его ещё открытые задачи: проект в Logbook
     * не оставляет за собой «висящих» задач. Выполненные и отменённые задачи не трогаются.
     */
    suspend operator fun invoke(project: Item, projectTasks: List<ItemWithChecklist>) {
        val now = System.currentTimeMillis()
        val openTasks = projectTasks
            .map { it.item }
            .filter { it.projectId == project.id && it.status == Item.STATUS_OPEN && !it.isHeading }
            .map { it.copy(status = Item.STATUS_COMPLETED, stopDate = now, modificationDate = now) }
        repository.inTransaction {
            if (openTasks.isNotEmpty()) repository.insertTasks(openTasks)
            repository.insertProject(
                project.copy(status = Item.STATUS_COMPLETED, stopDate = now, modificationDate = now)
            )
        }
    }
}
