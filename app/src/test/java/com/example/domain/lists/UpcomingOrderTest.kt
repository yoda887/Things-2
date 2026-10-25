package com.example.domain.lists

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Порядок внутри дня в «Планах»: задачи и проекты вперемешку, перенесённое — первым. */
class UpcomingOrderTest {

    private val zone = ZoneId.of("Europe/Kiev")
    private fun day(d: Int) = LocalDate.of(2026, 10, d).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun task(id: String, start: Long?, order: Long? = null) =
        ItemWithChecklist(item = Item(id = id, title = id, startDate = start, dayOrder = order), checklist = emptyList())
    private fun project(id: String, start: Long?, order: Long? = null) =
        Item(id = id, title = id, type = Item.TYPE_PROJECT, startDate = start, dayOrder = order)
    private fun ids(rows: List<Any>) = rows.map { (it as? ItemWithChecklist)?.item?.id ?: (it as Item).id }

    @Test
    fun withoutOrder_projectsFirst_asBefore() {
        val rows = UpcomingOrder.merge(listOf(project("P", day(26))), listOf(task("a", day(26)), task("b", day(26))))
        assertEquals(listOf("P", "a", "b"), ids(rows))
    }

    @Test
    fun mixedByDayOrder() {
        val rows = UpcomingOrder.merge(
            listOf(project("P1", day(26), 2), project("P2", day(26), 5)),
            listOf(task("a", day(26), 1), task("b", day(26), 3), task("c", day(26), 6))
        )
        assertEquals(listOf("a", "P1", "b", "P2", "c"), ids(rows))
    }

    @Test
    fun movedToAnotherDay_goesTop_sameDayKeepsOrder() {
        val old = Item(id = "a", startDate = day(26), dayOrder = 7)
        val moved = UpcomingOrder.rescheduled(old, old.copy(startDate = day(27)), zone, now = 1_000L)
        assertEquals(UpcomingOrder.top(1_000L), moved.dayOrder)
        // Тот же день (другое время) — порядок прежний
        assertEquals(7L, UpcomingOrder.rescheduled(old, old.copy(startDate = day(26) + 3600_000L), zone).dayOrder)
        // Порядок задан заново (перетаскивание) — не трогаем
        assertEquals(42L, UpcomingOrder.rescheduled(old, old.copy(startDate = day(27), dayOrder = 42), zone).dayOrder)
        // Новая запись и без даты — как есть
        assertNull(UpcomingOrder.rescheduled(null, Item(id = "n", startDate = day(27)), zone).dayOrder)
        assertEquals(7L, UpcomingOrder.rescheduled(old, old.copy(startDate = null), zone).dayOrder)
    }

    @Test
    fun laterMoveIsAboveEarlierMoveAndRenumberedDay() {
        val renumbered = UpcomingOrder.renumber(listOf(Item(id = "x"), Item(id = "y")), now = 1_000L)
        val later = UpcomingOrder.top(1_001L)
        assertTrue(renumbered.all { later < it.dayOrder!! })
        assertTrue(UpcomingOrder.top(2_000L) < UpcomingOrder.top(1_000L))
    }

    @Test
    fun orderTasks_byDayOrderThenListOrder() {
        val tasks = listOf(task("a", day(26)), task("b", day(26), 5), task("c", day(26)), task("d", day(26), 1))
        assertEquals(listOf("d", "b", "a", "c"), UpcomingOrder.orderTasks(tasks).map { it.item.id })
    }

    @Test
    fun afterDrag_renumbersOnlyChangedDays_projectsKeepPlaces() {
        val p = project("P", day(26), 2)
        val tasks = listOf(task("b", day(26), 3), task("a", day(26), 1), task("z", day(27), 9))
        val result = UpcomingOrder.afterDrag(
            tasks, changed = listOf(tasks[0].item),
            days = listOf(UpcomingOrder.DayRows(day(26), listOf(p)), UpcomingOrder.DayRows(day(27), emptyList())),
            zone = zone, now = 1_000L
        ).associateBy { it.id }
        // День 26 — заново: задачи в порядке перетаскивания, проект перед первой задачей с ключом больше своего
        assertEquals(setOf("b", "a", "P"), result.keys)
        val order = result.values.sortedBy { it.dayOrder }.map { it.id }
        assertEquals(listOf("P", "b", "a"), order)
    }

    @Test
    fun monthSections_byDayThenOrder() {
        val rows = UpcomingOrder.mergeByDay(
            listOf(project("P", day(28)), project("Q", day(27), 1)),
            listOf(task("a", day(27), 2), task("b", day(28))),
            zone
        )
        assertEquals(listOf("Q", "a", "P", "b"), ids(rows))
    }
}
