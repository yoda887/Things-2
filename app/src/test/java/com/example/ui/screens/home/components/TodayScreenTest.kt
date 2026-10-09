package com.example.ui.screens.home.components

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
        assertEquals(listOf("d1", "d2"), TodayScreen.day(tasks).map { it.item.id })
        assertEquals(listOf("e1"), TodayScreen.evening(tasks).map { it.item.id })
    }

    @Test
    fun rows_eveningHeaderOnlyWithEveningTasksOrWhileDragging() {
        val day = TodayScreen.day(tasks)
        assertEquals(listOf("d1", "d2", TaskListKeys.EVENING_HEADER, "e1"), ids(TodayScreen.rows(day, TodayScreen.evening(tasks), false)))
        assertEquals(listOf("d1", "d2"), ids(TodayScreen.rows(day, emptyList(), false)))
        assertEquals(listOf("d1", "d2", TaskListKeys.EVENING_HEADER), ids(TodayScreen.rows(day, emptyList(), true)))
    }

    @Test
    fun isEmpty() {
        assertTrue(TodayScreen.isEmpty(emptyList(), emptyList(), false))
        assertFalse(TodayScreen.isEmpty(emptyList(), emptyList(), true))
        assertFalse(TodayScreen.isEmpty(emptyList(), listOf(task("e", true)), false))
    }
}
