package com.example.domain.edits

import com.example.data.model.Item

/** Отмена задачи и возврат отменённой. Закреплено тестами EditsTest. */
object TaskStatuses {

    /** Отменить открытую задачу (в «Журнал», как отменённую) или вернуть отменённую в открытые. */
    fun toggledCancel(task: Item, now: Long = System.currentTimeMillis()): Item =
        if (task.status == Item.STATUS_CANCELLED) {
            task.copy(status = Item.STATUS_OPEN, stopDate = null, modificationDate = now)
        } else {
            task.copy(status = Item.STATUS_CANCELLED, stopDate = now, modificationDate = now)
        }
}
