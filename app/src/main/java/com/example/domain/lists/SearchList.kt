package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Поиск: какие задачи находятся — открытые, выполненные и отменённые, по названию, заметкам и чек-листу;
 * повторяющиеся задачи (шаблоны) и их копии — тоже.
 */
object SearchList {

    /**
     * Подзаголовки проектов по названию — открытые, в открытых неудалённых проектах (и шаблонах повторяющихся
     * проектов). Нажатие ведёт в проект к подзаголовку, как к найденной задаче.
     */
    fun headings(headings: List<Item>, projects: List<Item>, query: String): List<Item> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val openProjects = projects.filter { !it.trashed && !it.isClosed }.associateBy { it.id }
        return headings.filter { heading ->
            heading.isHeading && !heading.trashed && heading.status == Item.STATUS_OPEN &&
                heading.projectId in openProjects && heading.title.contains(q, ignoreCase = true)
        }
    }

    fun includes(task: ItemWithChecklist, query: String): Boolean {
        val q = query.trim()
        val item = task.item
        return q.isNotEmpty() && (item.type == Item.TYPE_TASK || RepeatingList.includes(item)) && !item.trashed && (
            item.title.contains(q, ignoreCase = true) ||
                item.notes.contains(q, ignoreCase = true) ||
                task.checklist.any { it.title.contains(q, ignoreCase = true) }
            )
    }
}
