package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ChecklistItem
import com.example.data.model.Tag
import com.example.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для обновления существующей задачи (или списка задач) в базе данных
 * с динамическим разрешением, созданием и прикреплением тегов.
 */
class UpdateTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Обновляет задачу базовой информацией.
     */
    suspend operator fun invoke(item: Item) {
        repository.inTransaction {
            repository.insertTask(item)
            resolveAndAttachTags(item)
        }
    }

    /**
     * Обновляет задачу вместе с её чек-листом.
     *
     * Все записи — задача, чек-лист с его счётчиками, теги — идут одной транзакцией: иначе списки
     * задач пересобирались бы после каждой из них, прямо во время анимации сворачивания редактора.
     */
    suspend operator fun invoke(item: Item, checklist: List<ChecklistItem>) {
        repository.inTransaction {
            repository.insertTask(item, checklist)
            resolveAndAttachTags(item)
        }
    }

    /**
     * Пакетное обновление задач.
     */
    suspend operator fun invoke(items: List<Item>) {
        repository.insertTasks(items)
    }

    private suspend fun resolveAndAttachTags(item: Item) {
        val cleanTags = item.tags.map { it.trim() }.filter { it.isNotEmpty() }
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
        repository.updateItemTags(item.id, tagObjects)
    }
}
