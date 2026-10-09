package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Поиск: какие задачи находятся — открытые, выполненные и отменённые, по названию, заметкам и чек-листу. */
object SearchList {

    fun includes(task: ItemWithChecklist, query: String): Boolean {
        val q = query.trim()
        val item = task.item
        return q.isNotEmpty() && item.type == Item.TYPE_TASK && !item.trashed && (
            item.title.contains(q, ignoreCase = true) ||
                item.notes.contains(q, ignoreCase = true) ||
                task.checklist.any { it.title.contains(q, ignoreCase = true) }
            )
    }
}
