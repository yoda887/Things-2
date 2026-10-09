package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Экран проекта — его правила в одном месте: какие задачи на нём, его открытые заголовки, порядок задач
 * по заголовкам, строки списка и признак пустого экрана. Перенос между заголовками — в ProjectHeadings.
 * Закреплено тестами ProjectScreenTest.
 */
object ProjectScreen {

    fun includes(item: Item, projectId: String?): Boolean =
        projectId != null && item.projectId == projectId && !item.isCompleted

    /** Открытые заголовки проекта по порядку. */
    fun headings(all: List<Item>, projectId: String?): List<Item> =
        all.filter { projectId != null && it.projectId == projectId && it.status == Item.STATUS_OPEN && !it.trashed }
            .sortedBy { it.sortOrder }

    /** Задачи в порядке экрана: сначала без заголовка, затем под каждым заголовком. */
    fun order(tasks: List<ItemWithChecklist>, headings: List<Item>): List<ItemWithChecklist> =
        if (headings.isEmpty()) tasks else ProjectHeadings.orderByHeading(tasks, headings.map { it.id }) { it.item }

    /**
     * Строки экрана: задачи без заголовка, затем каждый заголовок со своими задачами. Задачи заголовка,
     * который несут под пальцем, убраны — как проекты свёрнутой области.
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
        addAll(ProjectHeadings.tasksOf(tasks, null, headingIds))
        headings.forEach { heading ->
            add(ProjectHeadingItem(heading))
            if (heading.id != draggedHeadingId) {
                addAll(ProjectHeadings.tasksOf(tasks, heading.id, headingIds))
            }
        }
    }

    fun isEmpty(tasks: List<ItemWithChecklist>, headings: List<Item>): Boolean = tasks.isEmpty() && headings.isEmpty()
}
