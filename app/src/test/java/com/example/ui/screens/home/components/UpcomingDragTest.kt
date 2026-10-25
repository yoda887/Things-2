package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Перетаскивание в днях «Планов»: задачи и проекты вперемешку, перенос в другой день. */
class UpcomingDragTest {

    private val d26 = UpcomingHeaderItem(26L, "26", "Tomorrow")
    private val d27 = UpcomingHeaderItem(27L, "27", "Tuesday")
    private val month = UpcomingMonthHeaderItem(100L, "November")
    private fun task(id: String) = ItemWithChecklist(item = Item(id = id, title = id), checklist = emptyList())
    private fun project(id: String) = Item(id = id, title = id, type = Item.TYPE_PROJECT)
    private fun key(row: Any): Any? = when (row) {
        is ItemWithChecklist -> row.item.id
        is Item -> row.id
        is UpcomingHeaderItem -> "day_${row.dateMillis}"
        is UpcomingMonthHeaderItem -> "month_${row.monthMillis}"
        else -> null
    }

    private val rows = listOf(d26, project("P"), task("a"), task("b"), d27, task("c"), project("Q"), month, task("m"))

    @Test
    fun projectBetweenTasks_sameDay() {
        val move = UpcomingDrag.plan(rows, "P", "a", movingDown = true, keyOf = ::key)!!
        assertEquals(26L, move.dayStart)
        assertEquals(listOf("a", "P", "b"), move.order)
    }

    @Test
    fun taskAboveProject() {
        val move = UpcomingDrag.plan(rows, "a", "P", movingDown = false, keyOf = ::key)!!
        assertEquals(listOf("a", "P", "b"), move.order)
    }

    @Test
    fun projectToNextDay_viaHeader_goesFirst() {
        val move = UpcomingDrag.plan(rows, "P", "day_27", movingDown = true, keyOf = ::key)!!
        assertEquals(27L, move.dayStart)
        assertEquals(listOf("P", "c", "Q"), move.order)
    }

    @Test
    fun upThroughHeader_lastInPreviousDay() {
        val move = UpcomingDrag.plan(rows, "c", "day_27", movingDown = false, keyOf = ::key)!!
        assertEquals(26L, move.dayStart)
        assertEquals(listOf("P", "a", "b", "c"), move.order)
    }

    @Test
    fun monthSection_andAboveFirstDay_notHandled() {
        assertNull(UpcomingDrag.plan(rows, "Q", "month_100", movingDown = true, keyOf = ::key))
        assertNull(UpcomingDrag.plan(rows, "Q", "m", movingDown = true, keyOf = ::key))
        assertNull(UpcomingDrag.plan(rows, "P", "day_26", movingDown = false, keyOf = ::key))
    }
}
