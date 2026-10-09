package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/** Вставка элемента в упорядоченный список. Закреплено тестами EditsTest и FabInsertionTest. */
object SortOrders {

    /** Новый элемент и соседи, чей sortOrder пришлось изменить. */
    data class Insertion(val created: Item, val reordered: List<Item>)

    /**
     * Свободный sortOrder для вставки перед элементом [index] (или в конец): между соседями,
     * если там есть место, иначе null — придётся перенумеровать.
     */
    fun freeSortOrder(orders: List<Int>, index: Int): Int? {
        if (orders.zipWithNext().any { (a, b) -> a > b }) return null
        val i = index.coerceIn(0, orders.size)
        val prev = orders.getOrNull(i - 1)
        val next = orders.getOrNull(i)
        return when {
            prev == null && next == null -> 0
            prev == null -> next!! - 1
            next == null -> prev + 1
            prev + 1 < next -> prev + 1
            else -> null
        }
    }

    /**
     * Вставляет [newItem] в [items] (в порядке списка) перед элементом [index]. Есть место между
     * соседями — меняется только новый элемент, нет — список перенумеровывается, и в [Insertion.reordered]
     * попадают соседи с новым sortOrder.
     */
    fun insert(items: List<Item>, index: Int, newItem: Item): Insertion {
        val at = index.coerceIn(0, items.size)
        freeSortOrder(items.map { it.sortOrder }, at)?.let { return Insertion(newItem.copy(sortOrder = it), emptyList()) }
        val renumbered = items.toMutableList().apply { add(at, newItem) }
            .mapIndexed { i, item -> if (item.sortOrder == i) item else item.copy(sortOrder = i) }
        val previous = items.associate { it.id to it.sortOrder }
        return Insertion(
            created = renumbered.first { it.id == newItem.id },
            reordered = renumbered.filter { it.id != newItem.id && previous[it.id] != it.sortOrder }
        )
    }

    /** Список после перестановки ([updated]) и задачи, которые надо сохранить ([changed]). */
    data class Reordered(val updated: List<ItemWithChecklist>, val changed: List<Item>)

    /**
     * После перетаскивания: sortOrder — по позиции в новом порядке [order]. Сохранять нужно задачи, у
     * которых по сравнению с [before] изменились позиция, вечер, дата старта или подзаголовок — они же
     * получают новую дату изменения.
     */
    fun afterReorder(order: List<ItemWithChecklist>, before: List<ItemWithChecklist>, now: Long = System.currentTimeMillis()): Reordered {
        val previous = before.associateBy { it.item.id }
        fun ItemWithChecklist.changedAt(index: Int): Boolean {
            val original = previous[item.id] ?: return true
            return original.item.sortOrder != index ||
                original.item.isTonight != item.isTonight ||
                original.item.startDate != item.startDate ||
                original.item.headingId != item.headingId
        }
        val updated = order.mapIndexed { index, wrapper ->
            if (wrapper.changedAt(index)) {
                wrapper.copy(item = wrapper.item.copy(sortOrder = index, modificationDate = now))
            } else {
                wrapper.copy(item = wrapper.item.copy(sortOrder = index))
            }
        }
        val changed = updated.filterIndexed { index, wrapper -> order[index].changedAt(index) }.map { it.item }
        return Reordered(updated, changed)
    }
}
