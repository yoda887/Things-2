package com.example.domain.usecase.project

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для дублирования проекта.
 */
class DuplicateProjectUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Создаёт копию проекта с его заголовками и задачами (с чек-листами и тегами) под новыми UUID.
     * Задачи копии остаются под копиями своих заголовков. Копия встаёт сразу после исходного проекта.
     */
    suspend operator fun invoke(project: Item, headings: List<Item>, projectTasks: List<ItemWithChecklist>) {
        val now = System.currentTimeMillis()
        fun Item.fresh(id: String) = copy(
            id = id,
            googleTaskId = null,
            googleTaskListId = null,
            creationDate = now,
            modificationDate = now
        )
        val newProjectId = UUID.randomUUID().toString()
        val newProject = project.fresh(newProjectId).copy(sortOrder = project.sortOrder + 1)
        val headingIds = headings.filter { it.projectId == project.id }.associate { it.id to UUID.randomUUID().toString() }
        val newHeadings = headings.filter { it.id in headingIds }.map { heading ->
            heading.fresh(headingIds.getValue(heading.id)).copy(projectId = newProjectId)
        }
        repository.inTransaction {
            repository.insertProject(newProject)
            repository.setItemTagsByTitles(newProjectId, newProject.tags)
            if (newHeadings.isNotEmpty()) repository.insertTasks(newHeadings)
            projectTasks.filter { it.item.projectId == project.id }.forEach { wrapper ->
                val newId = UUID.randomUUID().toString()
                val copied = wrapper.item.fresh(newId).copy(
                    projectId = newProjectId,
                    headingId = wrapper.item.headingId?.let { headingIds[it] }
                )
                val checklist = wrapper.checklist.map { it.copy(id = UUID.randomUUID().toString(), itemId = newId) }
                repository.insertTask(copied, checklist)
                repository.setItemTagsByTitles(newId, copied.tags)
            }
        }
    }
}
