package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.edits.BatchDuplicates
import com.example.domain.repository.ITaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для дублирования задачи со всеми свойствами, тегами и чек-листом;
 * копия встаёт в списке сразу под оригиналом; при дублировании выбранных задач место задаёт [place]
 * ([BatchDuplicates]).
 */
class DuplicateTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Дублирует задачу, генерирует новые UUID и заново привязывает теги и пункты чек-листа.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist, place: BatchDuplicates.Place? = null) {
        val newId = UUID.randomUUID().toString()
        // Дубликат повторяющейся задачи (шаблона или копии) — обычная задача, без повтора
        val copiedItem = wrapper.item.withoutRepeat().copy(
            id = newId,
            googleTaskId = null,
            googleTaskListId = null,
            creationDate = place?.creationDate ?: duplicateCreationDate(wrapper.item.creationDate),
            sortOrder = place?.sortOrder ?: wrapper.item.sortOrder,
            dayOrder = if (place != null) place.dayOrder else wrapper.item.dayOrder,
            modificationDate = System.currentTimeMillis()
        )
        val newChecklist = wrapper.checklist.map {
            it.copy(id = UUID.randomUUID().toString(), itemId = newId)
        }
        repository.inTransaction {
            repository.insertTask(copiedItem, newChecklist)
            repository.setItemTagsByTitles(newId, copiedItem.tags)
        }
    }
}

/**
 * Копия одной задачи встаёт сразу под оригиналом, как в Things: место в списке то же, а при равном месте раньше
 * идёт более новая задача — копия «создана» на миг раньше оригинала
 */
internal fun duplicateCreationDate(originalCreationDate: Long): Long = originalCreationDate - 1

/**
 * Обычная задача или проект вместо повторяющихся: шаблон задачи — задача, шаблон проекта — проект, задача внутри
 * шаблона проекта — задача, у копии повтора — без ссылки на шаблон. Так дубликат не повторяется и не считается
 * копией повтора. Остальное не меняется.
 */
internal fun Item.withoutRepeat(): Item = when (type) {
    Item.TYPE_TEMPLATE -> copy(type = Item.TYPE_TASK, repeatTemplateId = null)
    Item.TYPE_PROJECT_TEMPLATE -> copy(type = Item.TYPE_PROJECT, repeatTemplateId = null)
    else -> if (repeatTemplateId != null) copy(repeatTemplateId = null) else this
}
