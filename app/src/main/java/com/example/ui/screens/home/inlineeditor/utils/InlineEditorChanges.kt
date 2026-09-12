package com.example.ui.screens.home.inlineeditor.utils

import com.example.data.model.ChecklistItem
import com.example.data.model.ItemWithChecklist
import com.example.data.model.TaskSection

/**
 * Отличается ли то, что сейчас в редакторе, от сохранённой задачи — по тем же полям, что уходят
 * в сохранение. Если нет, сохранять не нужно: запись в базу обновила бы дату изменения и заставила
 * бы список пересобраться прямо во время анимации сворачивания.
 */
internal fun hasUnsavedChanges(
    task: ItemWithChecklist,
    title: String,
    notes: String,
    section: TaskSection,
    isTonight: Boolean,
    startDate: Long?,
    dueDate: Long?,
    tags: List<String>,
    checklist: List<ChecklistItem>,
    priority: Int
): Boolean {
    val item = task.item
    return title != item.title ||
        notes != item.notes ||
        section != item.section ||
        isTonight != item.isTonight ||
        startDate != item.startDate ||
        dueDate != item.dueDate ||
        priority != item.priority ||
        tags != item.tags.filter { it.isNotEmpty() } ||
        checklist != task.checklist
}
