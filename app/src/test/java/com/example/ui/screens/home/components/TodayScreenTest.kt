package com.example.ui.screens.home.components

import com.example.domain.lists.TodayList
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Сегодня»: день и вечер, строки экрана, пустой экран. */
class TodayScreenTest {

    private fun task(id: String, tonight: Boolean = false) = ItemWithChecklist(Item(id = id, title = id, isTonight = tonight))
    private fun ids(rows: List<Any>) = rows.map { if (it is ItemWithChecklist) it.item.id else it.toString() }

    private val tasks = listOf(task("d1"), task("e1", tonight = true), task("d2"))

    @Test
    fun dayAndEvening_keepOrder() {
        assertEquals(listOf("d1", "d2"), TodayList.day(tasks).map { it.item.id })
        assertEquals(listOf("e1"), TodayList.evening(tasks).map { it.item.id })
    }

    @Test
    fun rows_eveningHeaderOnlyWithEveningTasksOrWhileDragging() {
        val day = TodayList.day(tasks)
        assertEquals(listOf("d1", "d2", TaskListKeys.EVENING_HEADER, "e1"), ids(TodayScreen.rows(day, TodayList.evening(tasks), false)))
        assertEquals(listOf("d1", "d2"), ids(TodayScreen.rows(day, emptyList(), false)))
        assertEquals(listOf("d1", "d2", TaskListKeys.EVENING_HEADER), ids(TodayScreen.rows(day, emptyList(), true)))
    }

    @Test
    fun isEmpty() {
        assertTrue(TodayScreen.isEmpty(emptyList(), emptyList(), false))
        assertFalse(TodayScreen.isEmpty(emptyList(), emptyList(), true))
        assertFalse(TodayScreen.isEmpty(emptyList(), listOf(task("e", true)), false))
    }

    @Test
    fun projects_startedTodayOrEarlierOrDue_onTopOfTheirPart() {
        val bounds = com.example.data.model.DayBounds.now()
        val now = System.currentTimeMillis()
        val dayAgo = now - 86_400_000L
        val weekAhead = now + 7 * 86_400_000L
        val started = Item(id = "p1", type = Item.TYPE_PROJECT, startDate = dayAgo)
        val todayEvening = Item(id = "p2", type = Item.TYPE_PROJECT, start = Item.START_TODAY, isTonight = true)
        val due = Item(id = "p3", type = Item.TYPE_PROJECT, dueDate = now)
        val later = Item(id = "p4", type = Item.TYPE_PROJECT, startDate = weekAhead)
        val someday = Item(id = "p5", type = Item.TYPE_PROJECT, start = Item.START_SOMEDAY, startDate = dayAgo)
        val done = Item(id = "p6", type = Item.TYPE_PROJECT, startDate = dayAgo, status = Item.STATUS_COMPLETED)
        val template = Item(id = "p7", type = Item.TYPE_PROJECT_TEMPLATE, startDate = dayAgo)
        val projects = TodayList.projects(listOf(started, todayEvening, due, later, someday, done, template), bounds)
        assertEquals(listOf("p1", "p3"), projects.first.map { it.id })
        assertEquals(listOf("p2"), projects.second.map { it.id })
        val rows = TodayScreen.rows(listOf(task("d1")), listOf(task("e1", true)), false, projects)
        assertEquals(listOf("p1", "p3", "d1", TaskListKeys.EVENING_HEADER, "p2", "e1"), rows.map { if (it is Item) it.id else if (it is ItemWithChecklist) it.item.id else it.toString() })
        assertFalse(TodayScreen.isEmpty(emptyList(), emptyList(), false, projects))
    }
}
