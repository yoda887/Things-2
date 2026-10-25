package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist

/**
 * Перетаскивание в днях «Планов», как в Things: задачи и проекты дня — одним списком ([com.example.domain.lists.UpcomingOrder]),
 * строку можно поставить между любыми строками дня или перенести в другой день. Здесь — где встанет строка;
 * сохраняет порядок [UpcomingDragController]. Разделы месяцев сюда не относятся (у задач там свои правила, TaskDrops).
 * Закреплено тестами UpcomingDragTest.
 */
object UpcomingDrag {

    /** Итог переноса: день [dayStart] и id его задач и проектов сверху вниз (с перенесённой строкой) */
    data class Move(val draggedId: String, val dayStart: Long, val order: List<String>)

    private fun idOf(row: Any): String? = when (row) {
        is ItemWithChecklist -> row.item.id
        is Item -> if (row.type == Item.TYPE_PROJECT) row.id else null
        else -> null
    }

    private fun isHeader(row: Any) = row is UpcomingHeaderItem || row is UpcomingMonthHeaderItem

    /**
     * Строка [draggedId] из строк экрана [rows] — на место цели [targetKey] (по ключам [keyOf]); [movingDown] — направление
     * жеста. null — переносить не нужно: цель в разделе месяца, выше первого дня или не строка дня.
     */
    fun plan(rows: List<Any>, draggedId: String, targetKey: Any, movingDown: Boolean, keyOf: (Any) -> Any?): Move? {
        val from = rows.indexOfFirst { idOf(it) == draggedId }
        if (from < 0) return null
        val rest = rows.toMutableList().apply { removeAt(from) }
        val target = rest.indexOfFirst { keyOf(it) == targetKey }
        if (target < 0) return null
        val targetRow = rest[target]
        var insert = when {
            targetRow is UpcomingMonthHeaderItem -> return null
            // На заголовок дня сверху — первой в этом дне (после его событий), снизу — последней в дне выше
            targetRow is UpcomingHeaderItem -> if (movingDown) target + 1 else target
            idOf(targetRow) != null -> if (movingDown) target + 1 else target
            else -> return null
        }
        if (targetRow is UpcomingHeaderItem && movingDown) {
            while (insert < rest.size && rest[insert] is UpcomingEventItem) insert++
        }
        // День — ближайший заголовок выше места; заголовок месяца между ними — это раздел месяца
        val header = rest.subList(0, insert).lastOrNull { isHeader(it) } as? UpcomingHeaderItem ?: return null
        val headerIndex = rest.indexOf(header)
        rest.add(insert, rows[from])
        val dayEnd = (headerIndex + 1 until rest.size).firstOrNull { isHeader(rest[it]) } ?: rest.size
        val order = rest.subList(headerIndex + 1, dayEnd).mapNotNull { idOf(it) }
        return Move(draggedId, header.dateMillis, order)
    }
}
