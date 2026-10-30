package com.example.ui.screens.home.components

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.RepeatingList
import com.example.domain.lists.UpcomingOrder
import java.time.Instant
import java.time.ZoneId

/**
 * Перетаскивание задач и проектов в днях «Планов» ([UpcomingDrag]): пока тянут — порядок дня и день строки меняются
 * в списке на экране (задачи — [setTasks], проекты — [projectOverrides]), по отпусканию ([end]) — что сохранить:
 * перенумерованные дни (UpcomingOrder.renumber) и новый день перенесённой строки.
 */
class UpcomingDragController(
    private val rows: () -> List<Any>,
    private val keyOf: (Any) -> Any?,
    private val tasks: () -> List<ItemWithChecklist>,
    private val setTasks: (List<ItemWithChecklist>) -> Unit,
    private val projects: () -> List<Item>,
    val projectOverrides: SnapshotStateMap<String, Item>,
    // Запись в базе по id: шаблон повтора стоит в «Планах» на дне следующей копии с подставленной датой —
    // сохраняется он со своей датой, а не с подставленной
    private val stored: (String) -> Item? = { null },
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    /** Дни, где менялся порядок, — их нумеруют заново */
    private val touchedDays = LinkedHashSet<Long>()

    /** Был ли перенос в этом жесте — сохранять есть что */
    val hasMoves: Boolean get() = touchedDays.isNotEmpty()

    private fun dayOf(millis: Long?) = millis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    /** Строка [draggedId] — к цели [targetKey]. false — это не перенос в днях (раздел месяца, не строка дня) */
    fun move(draggedId: String, targetKey: Any, movingDown: Boolean): Boolean {
        val move = UpcomingDrag.plan(rows(), draggedId, targetKey, movingDown, keyOf) ?: return false
        // Шаблон повтора — только в своём дне: его день задаёт правило повтора, а не перетаскивание
        val dragged = tasks().firstOrNull { it.item.id == draggedId }?.item
        if (dragged != null && RepeatingList.includes(dragged) && dayOf(dragged.startDate) != dayOf(move.dayStart)) return true
        touchedDays += move.dayStart
        // Порядок дня — пока тянут, просто по месту (0, 1, 2…); при отпускании нумеруется заново
        val index = move.order.withIndex().associate { it.value to it.index.toLong() }
        fun Item.placed(): Item {
            val order = index[id] ?: return this
            val start = if (id == draggedId && dayOf(startDate) != dayOf(move.dayStart)) move.dayStart else startDate
            return copy(dayOrder = order, startDate = start)
        }
        setTasks(UpcomingOrder.orderTasks(tasks().map { w -> if (w.item.id in index) w.copy(item = w.item.placed()) else w }))
        projects().filter { it.id in index }.forEach { projectOverrides[it.id] = it.placed() }
        return true
    }

    /** Отпустили: задачи и проекты перенумерованных дней (с новым днём перенесённой строки) — сохранить */
    fun end(now: Long = System.currentTimeMillis()): List<Item> {
        if (touchedDays.isEmpty()) return emptyList()
        val current = rows()
        val taskById = tasks().associateBy { it.item.id }
        val projectById = projects().associateBy { it.id }
        val saved = touchedDays.flatMap { dayStart ->
            val header = current.indexOfFirst { it is UpcomingHeaderItem && it.dateMillis == dayStart }
            if (header < 0) return@flatMap emptyList()
            val dayRows = current.drop(header + 1).takeWhile { it !is UpcomingHeaderItem && it !is UpcomingMonthHeaderItem }
            val items = dayRows.mapNotNull { row ->
                when (row) {
                    is ItemWithChecklist -> taskById[row.item.id]?.item
                    is Item -> projectById[row.id]?.takeIf { it.type == Item.TYPE_PROJECT }
                    else -> null
                }
            }
            UpcomingOrder.renumber(items, now).map { item ->
                val own = if (RepeatingList.includes(item)) stored(item.id)?.let { item.copy(startDate = it.startDate) } ?: item else item
                own.copy(modificationDate = now)
            }
        }
        touchedDays.clear()
        // На экране — уже сохраняемые значения: список не перескакивает, пока база не вернёт их
        val byId = saved.associateBy { it.id }
        // Шаблон на экране остаётся на дне копии — с подставленной датой
        val shownDate = tasks().associate { it.item.id to it.item.startDate }
        setTasks(tasks().map { w -> byId[w.item.id]?.let { w.copy(item = it.copy(startDate = shownDate[it.id] ?: it.startDate)) } ?: w })
        saved.filter { it.type == Item.TYPE_PROJECT }.forEach { projectOverrides[it.id] = it }
        return saved
    }

    /** База вернула проекты — сохранённые замены не нужны */
    fun onProjectsLoaded(loaded: List<Item>) {
        val byId = loaded.associateBy { it.id }
        projectOverrides.keys.toList().forEach { id ->
            val o = projectOverrides[id] ?: return@forEach
            val p = byId[id]
            if (p == null || (p.dayOrder == o.dayOrder && p.startDate == o.startDate)) projectOverrides.remove(id)
        }
    }
}
