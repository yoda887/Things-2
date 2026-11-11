package com.example.domain.usecase.task

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.repository.ITaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Сценарий использования (Use Case) для дублирования задачи со всеми свойствами, тегами и чек-листом;
 * копия встаёт в списке сразу под оригиналом.
 */
class DuplicateTaskUseCase @Inject constructor(private val repository: ITaskRepository) {

    /**
     * Дублирует задачу, генерирует новые UUID и заново привязывает теги и пункты чек-листа.
     */
    suspend operator fun invoke(wrapper: ItemWithChecklist) {
        val newId = UUID.randomUUID().toString()
        // Дубликат повторяющейся задачи (шаблона или копии) — обычная задача, без повтора
        val copiedItem = wrapper.item.withoutRepeat().copy(
            id = newId,
            googleTaskId = null,
            googleTaskListId = null,
            // Копия встаёт сразу под оригиналом, как в Things: место в списке то же, а при равном месте раньше
            // идёт более новая задача — копия «создана» на миг раньше оригинала
            creationDate = wrapper.item.creationDate - 1,
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
 * Обычная задача или проект вместо повторяющихся: шаблон задачи — задача, шаблон проекта — проект, задача внутри
 * шаблона проекта — задача, у копии повтора — без ссылки на шаблон. Так дубликат не повторяется и не считается
 * копией повтора. Остальное не меняется.
 */
internal fun Item.withoutRepeat(): Item = when (type) {
    Item.TYPE_TEMPLATE -> copy(type = Item.TYPE_TASK, repeatTemplateId = null)
    Item.TYPE_PROJECT_TEMPLATE -> copy(type = Item.TYPE_PROJECT, repeatTemplateId = null)
    else -> if (repeatTemplateId != null) copy(repeatTemplateId = null) else this
}
