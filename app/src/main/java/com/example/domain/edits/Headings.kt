package com.example.domain.edits

import com.example.data.model.Item
import java.util.UUID

/** Подзаголовки проекта: новый в конце или на месте, перенос задач под заголовок. Закреплено тестами EditsTest. */
object Headings {

    /** Новый пустой подзаголовок после всех заголовков проекта. */
    fun newAtEnd(projectId: String, headings: List<Item>, id: String = UUID.randomUUID().toString()): Item =
        Item(id = id, type = Item.TYPE_HEADING, projectId = projectId, sortOrder = (headings.maxOfOrNull { it.sortOrder } ?: -1) + 1)

    /** Новый пустой подзаголовок; место задаётся вставкой ([insertAt]). */
    fun newHeading(projectId: String, id: String = UUID.randomUUID().toString()): Item =
        Item(id = id, type = Item.TYPE_HEADING, projectId = projectId, sortOrder = -1)

    /** Заголовки с [heading] на месте [index] — перенумерованы по порядку. */
    fun insertAt(headings: List<Item>, heading: Item, index: Int): List<Item> =
        headings.toMutableList()
            .apply { add(index.coerceIn(0, size), heading) }
            .mapIndexed { i, h -> if (h.sortOrder == i) h else h.copy(sortOrder = i) }

    fun rename(heading: Item, title: String): Item = heading.copy(title = title)

    /** Задачи под заголовком [headingId] (null — без заголовка). */
    fun assign(tasks: List<Item>, headingId: String?, now: Long = System.currentTimeMillis()): List<Item> =
        tasks.map { it.copy(headingId = headingId, modificationDate = now) }

    /** Подзаголовки после переноса [draggedId] на место [targetId]; null — переносить нечего. */
    fun move(headings: List<Item>, draggedId: String, targetId: String): List<Item>? {
        val from = headings.indexOfFirst { it.id == draggedId }
        val to = headings.indexOfFirst { it.id == targetId }
        if (from == -1 || to == -1 || from == to) return null
        return headings.toMutableList().apply { add(to, removeAt(from)) }
    }
}
