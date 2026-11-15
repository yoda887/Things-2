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
     * переключатель «Показати/Сховати пізніші», как в Things. Список отфильтрован по тегам ([filtered]) — как в
     * Things, подзаголовок виден, только если под ним остались задачи.
     */
    fun rows(
        tasks: List<ItemWithChecklist>,
        headings: List<Item>,
        draggedItemKey: Any?,
        laterHidden: Boolean = false,
        bounds: DayBounds = DayBounds.now(),
        filtered: Boolean = false
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
                val under = HeadingOrder.tasksOf(shown, heading.id, headingIds)
                if (filtered && under.isEmpty()) return@forEach
                add(ProjectHeadingItem(heading))
                if (heading.id != draggedHeadingId) addAll(under)
            }
        }
        if (hasLater) add(TaskListKeys.AREA_LATER_TOGGLE)
    }

    /** Сколько более поздних задач в проекте — для переключателя. */
    fun laterCount(tasks: List<ItemWithChecklist>, bounds: DayBounds = DayBounds.now()): Int =
        tasks.count { ProjectList.isLater(it.item, bounds) }

    /** Пусто — ни задач, ни подзаголовков; в отфильтрованном списке подзаголовки без задач не видны */
    fun isEmpty(tasks: List<ItemWithChecklist>, headings: List<Item>, filtered: Boolean = false): Boolean =
        tasks.isEmpty() && (headings.isEmpty() || filtered)
}
