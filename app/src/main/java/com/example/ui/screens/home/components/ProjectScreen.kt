package com.example.ui.screens.home.components

import com.example.domain.lists.ProjectList
import com.example.data.model.DayBounds
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.HeadingOrder

/** Строки экрана проекта (правила — в ProjectList). Перенос между заголовками — в TaskDrops. */
object ProjectScreen {

    /**
     * Задачи без заголовка, затем каждый заголовок со своими задачами. Задачи заголовка, который несут
     * под пальцем, убраны — как проекты свёрнутой области. Более поздние задачи ([ProjectList.isLater]) стоят в
     * конце своей части ([ProjectList.order]); если они скрыты ([laterHidden]) — их нет. Когда поздние есть, внизу —
     * переключатель «Показати/Сховати пізніші», как в Things.
     */
    fun rows(
        tasks: List<ItemWithChecklist>,
        headings: List<Item>,
        draggedItemKey: Any?,
        laterHidden: Boolean = false,
        bounds: DayBounds = DayBounds.now()
    ): List<Any> = buildList {
        val hasLater = tasks.any { ProjectList.isLater(it.item, bounds) }
        val shown = if (laterHidden) tasks.filterNot { ProjectList.isLater(it.item, bounds) } else tasks
        if (headings.isEmpty()) {
            addAll(shown)
        } else {
            val headingIds = headings.map { it.id }
            val draggedHeadingId = (draggedItemKey as? String)
                ?.takeIf { it.startsWith(TaskListKeys.HEADING_PREFIX) }
                ?.removePrefix(TaskListKeys.HEADING_PREFIX)
            addAll(HeadingOrder.tasksOf(shown, null, headingIds))
            headings.forEach { heading ->
                add(ProjectHeadingItem(heading))
                if (heading.id != draggedHeadingId) {
                    addAll(HeadingOrder.tasksOf(shown, heading.id, headingIds))
                }
            }
        }
        if (hasLater) add(TaskListKeys.AREA_LATER_TOGGLE)
    }

    /** Сколько более поздних задач в проекте — для переключателя. */
    fun laterCount(tasks: List<ItemWithChecklist>, bounds: DayBounds = DayBounds.now()): Int =
        tasks.count { ProjectList.isLater(it.item, bounds) }

    fun isEmpty(tasks: List<ItemWithChecklist>, headings: List<Item>): Boolean = tasks.isEmpty() && headings.isEmpty()
}
