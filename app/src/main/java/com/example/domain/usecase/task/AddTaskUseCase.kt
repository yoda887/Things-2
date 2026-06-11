package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ChecklistItem
import com.example.data.model.Tag
import com.example.data.model.TaskSection
import com.example.data.model.toStartVal
import com.example.domain.repository.ITaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для создания новой задачи с опциональными подпунктами чек-листа и тегами.
 */
class AddTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Создает и добавляет задачу в базу данных.
     */
    suspend operator fun invoke(
        title: String,
        notes: String = "",
        section: TaskSection = TaskSection.INBOX,
        isTonight: Boolean = false,
        startDate: Long? = null,
        tags: List<String> = emptyList(),
        projectId: String? = null,
        checklist: List<ChecklistItem> = emptyList(),
        priority: Int = 0
    ) {
        val itemId = UUID.randomUUID().toString()
        val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }
        val startValue = section.toStartVal()
        val computedStartDate = startDate ?: if (section == TaskSection.TODAY) System.currentTimeMillis() else null
        val item = Item(
            id = itemId,
            type = 0,
            title = title.ifBlank { "Untitled To-Do" },
            notes = notes,
            start = startValue,
            isTonight = isTonight,
            startDate = computedStartDate,
            dueDate = null,
            projectId = projectId,
            priority = priority
        )
        repository.insertTask(item)

        if (cleanTags.isNotEmpty()) {
            val tagList = cleanTags.map { Tag(title = it) }
            for (t in tagList) {
                repository.insertTag(t)
            }
            repository.updateItemTags(itemId, tagList)
        }
        if (checklist.isNotEmpty()) {
            val listEntities = checklist.map { it.copy(itemId = itemId) }
            repository.updateChecklistItems(itemId, listEntities)
        }
    }
}
