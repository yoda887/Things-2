package com.example.ui.screens.home.components

import androidx.compose.runtime.mutableStateMapOf
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Шаблон повтора в «Планах» стоит на дне следующей копии с подставленной датой — перетаскивание её не сохраняет. */
class UpcomingDragControllerTest {

    private val zone = ZoneId.of("UTC")
    private fun day(n: Int) = LocalDate.of(2026, 11, n).atStartOfDay(zone).toInstant().toEpochMilli()
    private val d2 = UpcomingHeaderItem(day(2), "2", "Mon")
    private val d3 = UpcomingHeaderItem(day(3), "3", "Tue")

    // Шаблон в базе — без даты; на экране — на 2-м (день следующей копии)
    private val template = Item(id = "T", title = "T", type = Item.TYPE_TEMPLATE)
    private val shown = ItemWithChecklist(template.copy(startDate = day(2)))
    private val task = ItemWithChecklist(Item(id = "a", title = "a", startDate = day(2)))

    private fun key(row: Any): Any? = when (row) {
        is UpcomingHeaderItem -> "day_${row.dayOfMonth}"
        is ItemWithChecklist -> row.item.id
        else -> null
    }

    private fun controller(): Pair<UpcomingDragController, () -> List<ItemWithChecklist>> {
        var tasks = listOf(shown, task)
        val c = UpcomingDragController(
            rows = { listOf(d2) + tasks.filter { it.item.startDate == day(2) } + d3 + tasks.filter { it.item.startDate == day(3) } },
            keyOf = ::key,
            tasks = { tasks },
            setTasks = { tasks = it },
            projects = { emptyList() },
            projectOverrides = mutableStateMapOf(),
            stored = { id -> if (id == "T") template else null },
            zone = zone
        )
        return c to { tasks }
    }

    @Test
    fun reorderInDay_templateSavedWithOwnDate() {
        val (c, tasks) = controller()
        assertTrue(c.move("a", "T", movingDown = false))
        val saved = c.end(now = 1L).associateBy { it.id }
        assertEquals(null, saved.getValue("T").startDate)
        assertTrue(saved.getValue("a").dayOrder!! < saved.getValue("T").dayOrder!!)
        // На экране шаблон — по-прежнему на дне копии
        assertEquals(day(2), tasks().first { it.item.id == "T" }.item.startDate)
    }

    @Test
    fun templateToOtherDay_notMoved() {
        val (c, _) = controller()
        assertTrue(c.move("T", "day_3", movingDown = true))
        assertFalse(c.hasMoves)
        assertTrue(c.end(now = 1L).isEmpty())
    }
}
