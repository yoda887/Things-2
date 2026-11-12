package com.example.domain.usecase.area

import com.example.data.model.Area
import com.example.data.model.Item
import com.example.domain.lists.AreaList
import com.example.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для удаления сферы (области) жизни.
 */
class DeleteAreaUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Удаляет область вместе с её задачами и проектами (и всем внутри проектов) — как в Things.
     * Содержимое удаляется явно, а не каскадом базы: в базе десктопа каскадных ключей нет, а
     * ключ `areaId` и в базе телефона лишь обнуляется.
     */
    suspend operator fun invoke(area: Area) {
        val items = repository.observeAllTasks().first().map { it.item }
        val contents = AreaList.contents(area.id, items)
        contents
            .filter { it.type == Item.TYPE_TEMPLATE || it.type == Item.TYPE_PROJECT_TEMPLATE }
            .forEach { repository.deleteRecurrenceRule(it.id) }
        repository.deleteTasks(contents)
        repository.deleteArea(area)
    }
}
