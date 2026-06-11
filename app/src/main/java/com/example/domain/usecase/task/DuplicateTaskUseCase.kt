package com.example.domain.usecase.task

import com.example.data.model.ItemWithChecklist
import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import java.util.UUID

/**
 * Сценарий использования (Use Case) для дублирования задачи со всеми свойствами, тегами и чек-листом.
 */
class DuplicateTaskUseCase(private val repository: ITaskRepository) {

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
        repository.insertTask(copiedItem, newChecklist)
        
        val cleanTags = copiedItem.tags.map { it.trim() }.filter { it.isNotEmpty() }
        val allTagsList = repository.getAllTags()
        val tagObjects = cleanTags.map { title ->
            val existing = allTagsList.firstOrNull { it.title.equals(title, ignoreCase = true) }
            if (existing != null) {
                existing
            } else {
                val newTag = Tag(title = title)
                repository.insertTag(newTag)
                newTag
            }
        }
        repository.updateItemTags(newId, tagObjects)
    }
}
