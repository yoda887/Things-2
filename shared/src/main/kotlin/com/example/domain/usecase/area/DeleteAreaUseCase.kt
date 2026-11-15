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
     * Задачи и проекты удаляются явно: ключ `areaId` в базе при удалении области лишь обнуляется. Подзаголовки
     * проектов, пункты чек-листов и связи с тегами уходят каскадом базы вслед за своими проектами и задачами.
     * Одной транзакцией: синхронизация не застанет область удалённой наполовину (на десктопе транзакции
     * репозитория нет — там шаги идут подряд).
     */
    suspend operator fun invoke(area: Area) {
        // Читаем до транзакции: поток списка внутри неё ждал бы её же окончания
        val items = repository.observeAllTasks().first().map { it.item }
        val contents = AreaList.contents(area.id, items)
        repository.inTransaction {
            contents
                .filter { it.type == Item.TYPE_TEMPLATE || it.type == Item.TYPE_PROJECT_TEMPLATE }
                .forEach { repository.deleteRecurrenceRule(it.id) }
            repository.deleteTasks(contents)
            repository.deleteArea(area)
        }
    }
}
