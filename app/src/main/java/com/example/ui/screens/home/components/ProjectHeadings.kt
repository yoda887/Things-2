package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.HeadingOrder

/** Заголовок внутри проекта в плоском списке экрана проекта */
data class ProjectHeadingItem(val heading: Item) {
    val key: String get() = TaskListKeys.HEADING_PREFIX + heading.id
}

/**
 * Перенос задач и заголовков перетаскиванием на экране проекта. Порядок задач по заголовкам —
 * в [HeadingOrder]. Обычные функции над данными — закреплены тестами ProjectHeadingsTest.
 */
object ProjectHeadings {

    /**
     * Перенос задачи [draggedId] на цель [targetId] — задачу или заголовок ([TaskListKeys.HEADING_PREFIX]).
     *
     * - На задачу — встаёт на её место и в её группу.
     * - Вниз через заголовок — первой задачей этого заголовка.
     * - Вверх через заголовок своей группы — последней задачей предыдущей группы
     *   (предыдущего заголовка либо задач без заголовка).
     *
     * @return новый порядок задач (в порядке экрана) либо null, если перенос не нужен
     */
    fun planDrop(
        list: List<ItemWithChecklist>,
        draggedId: String,
        targetId: String,
        movingDown: Boolean,
        headingIds: List<String>,
    ): List<ItemWithChecklist>? {
        val ordered = HeadingOrder.orderByHeading(list, headingIds) { it.item }
        val fromIndex = ordered.indexOfFirst { it.item.id == draggedId }
        if (fromIndex == -1) return null
        val next = ordered.toMutableList()
        var moved = next.removeAt(fromIndex)
        val movedGroup = HeadingOrder.groupOf(moved.item, headingIds)

        if (targetId.startsWith(TaskListKeys.HEADING_PREFIX)) {
            val headingIndex = headingIds.indexOf(targetId.removePrefix(TaskListKeys.HEADING_PREFIX))
            if (headingIndex == -1) return null
            if (movingDown && movedGroup >= headingIndex) return null
            if (!movingDown && movedGroup != headingIndex) return null
            val destGroup = if (movingDown) headingIndex else headingIndex - 1
            val insertAt = if (movingDown) {
                next.count { HeadingOrder.groupOf(it.item, headingIds) < destGroup }
            } else {
                next.count { HeadingOrder.groupOf(it.item, headingIds) <= destGroup }
            }
            moved = moved.withHeading(headingIds.getOrNull(destGroup))
            next.add(insertAt, moved)
            return next
        }

        val toIndex = ordered.indexOfFirst { it.item.id == targetId }
        if (toIndex == -1 || toIndex == fromIndex) return null
        val hovered = ordered[toIndex].item
        moved = moved.withHeading(hovered.headingId?.takeIf { it in headingIds })
        next.add(toIndex, moved)
        return next
    }

    /** Ставит заголовок [draggedId] на место заголовка [targetId]. */
    fun moveHeading(headings: List<Item>, draggedId: String, targetId: String): List<Item>? {
        val from = headings.indexOfFirst { it.id == draggedId }
        val to = headings.indexOfFirst { it.id == targetId }
        if (from == -1 || to == -1 || from == to) return null
        return headings.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun ItemWithChecklist.withHeading(headingId: String?): ItemWithChecklist =
        if (item.headingId == headingId) this else copy(item = item.copy(headingId = headingId))
}
