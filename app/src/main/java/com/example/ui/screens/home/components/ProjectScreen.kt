package com.example.ui.screens.home.components

import com.example.domain.lists.ProjectList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.HeadingOrder

/** Строки экрана проекта (правила — в ProjectList). Перенос между заголовками — в TaskDrops. */
object ProjectScreen {

    /**
     * Задачи без заголовка, затем каждый заголовок со своими задачами. Задачи заголовка, который несут
     * под пальцем, убраны — как проекты свёрнутой области.
     */
    fun rows(tasks: List<ItemWithChecklist>, headings: List<Item>, draggedItemKey: Any?): List<Any> = buildList {
        if (headings.isEmpty()) {
            addAll(tasks)
            return@buildList
        }
        val headingIds = headings.map { it.id }
        val draggedHeadingId = (draggedItemKey as? String)
            ?.takeIf { it.startsWith(TaskListKeys.HEADING_PREFIX) }
            ?.removePrefix(TaskListKeys.HEADING_PREFIX)
        addAll(HeadingOrder.tasksOf(tasks, null, headingIds))
        headings.forEach { heading ->
            add(ProjectHeadingItem(heading))
            if (heading.id != draggedHeadingId) {
                addAll(HeadingOrder.tasksOf(tasks, heading.id, headingIds))
            }
        }
    }

    fun isEmpty(tasks: List<ItemWithChecklist>, headings: List<Item>): Boolean = tasks.isEmpty() && headings.isEmpty()
}
