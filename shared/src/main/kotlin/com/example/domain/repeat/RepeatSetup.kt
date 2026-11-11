package com.example.domain.repeat

import com.example.data.model.Item

/** Как задача становится повторяющейся. Закреплено тестами RepeatTest. */
object RepeatSetup {

    /**
     * Можно ли сделать задачу повторяющейся: только обычную открытую задачу — не копию повторяющейся
     * (у неё уже есть шаблон, как в Things) и не проект или подзаголовок.
     */
    fun canRepeat(item: Item): Boolean =
        item.type == Item.TYPE_TASK && item.repeatTemplateId == null && !item.isClosed && !item.trashed

    /**
     * Задача становится шаблоном повтора (TYPE_TEMPLATE): из обычных списков уходит, а её копии по правилу
     * появляются в «Сегодня». «Когда» и срок у шаблона снимаются — их задаёт правило; название, заметки,
     * теги, чек-лист и место остаются и переходят в копии; напоминание — [reminderTime] из окна «Повторять».
     */
    fun template(task: Item, reminderTime: Int?, now: Long = System.currentTimeMillis()): Item = task.copy(
        type = Item.TYPE_TEMPLATE,
        start = Item.START_ANYTIME,
        startDate = null,
        dueDate = null,
        isTonight = false,
        reminderTime = reminderTime,
        modificationDate = now
    )
}
