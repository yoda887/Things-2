package com.example.domain.usecase.heading

import com.example.data.model.Item
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарии работы с заголовками проектов (Item с типом [Item.TYPE_HEADING]).
 *
 * Заголовок группирует задачи проекта: задача ссылается на него через headingId.
 * Удаление заголовка удаляет и его задачи (внешний ключ headingId — ON DELETE CASCADE),
 * архивирование уносит заголовок и его задачи в Logbook.
 */
class HeadingUseCases @Inject constructor(private val repository: ITaskRepository) {

    fun observe(): Flow<List<Item>> = repository.observeHeadings()

    /** Новый пустой заголовок в конце проекта; название вводят сразу после создания. */
    fun newHeading(projectId: String, existing: List<Item>): Item = Item(
        id = UUID.randomUUID().toString(),
        type = Item.TYPE_HEADING,
        projectId = projectId,
        sortOrder = (existing.filter { it.projectId == projectId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
    )

    suspend fun save(heading: Item) {
        repository.insertTask(heading.copy(modificationDate = System.currentTimeMillis()))
    }

    suspend fun delete(heading: Item) {
        repository.deleteTask(heading)
    }

    suspend fun archive(heading: Item) {
        repository.archiveHeading(heading)
    }

    suspend fun reorder(headings: List<Item>) {
        repository.reorderHeadings(headings)
    }
}
