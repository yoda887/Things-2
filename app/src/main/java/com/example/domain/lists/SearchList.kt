package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Поиск: какие задачи находятся — открытые, выполненные и отменённые, по названию, заметкам и чек-листу;
 * повторяющиеся задачи (шаблоны) и их копии, задачи внутри повторяющихся проектов — тоже.
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

    /**
     * Что ищется среди задач: обычные задачи и копии повторов, шаблоны повторяющихся задач и задачи внутри шаблонов
     * повторяющихся проектов — их тоже ищут по словам, хотя копий проекта ещё нет
     */
    fun searchable(item: Item): Boolean =
        item.type == Item.TYPE_TASK || RepeatingList.includes(item) || item.isTemplatePart

    fun includes(task: ItemWithChecklist, query: String): Boolean {
        val q = query.trim()
        val item = task.item
        return q.isNotEmpty() && searchable(item) && !item.trashed && (
            item.title.contains(q, ignoreCase = true) ||
                item.notes.contains(q, ignoreCase = true) ||
                task.checklist.any { it.title.contains(q, ignoreCase = true) }
            )
    }
}
