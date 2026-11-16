package com.example.domain.edits

import com.example.data.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Подзаголовки в множественном выборе: что выбрано, перенос в другой проект, копия, «Поделиться». */
class HeadingBatchTest {

    private val h1 = Item(id = "h1", type = Item.TYPE_HEADING, title = "Этап 1", projectId = "p", sortOrder = 0)
    private val h2 = Item(id = "h2", type = Item.TYPE_HEADING, title = "Этап 2", projectId = "p", sortOrder = 1)
    private val other = Item(id = "o1", type = Item.TYPE_HEADING, title = "Там", projectId = "q", sortOrder = 4)
    private val a = Item(id = "a", title = "А", projectId = "p", headingId = "h1", sortOrder = 0)
    private val b = Item(id = "b", title = "Б", projectId = "p", headingId = "h1", sortOrder = 1)
    private val c = Item(id = "c", title = "В", projectId = "p", headingId = "h2")
    private val loose = Item(id = "d", title = "Г", projectId = "p")
    private val tasks = listOf(a, b, c, loose)
    private val headings = listOf(h2, h1, other)

    @Test
    fun split_tasksOfChosenHeadingGoWithIt() {
        val s = HeadingBatch.split(setOf("h1", "a", "c", "d"), tasks, headings)
        assertEquals(listOf("h1"), s.headings.map { it.id })
        // «А» выбрана, но она под выбранным подзаголовком — идёт с ним, отдельно не считается
        assertEquals(listOf("c", "d"), s.tasks.map { it.id })
        assertEquals(listOf("a", "b"), s.headingTasks["h1"]?.map { it.id })
    }

    @Test
    fun moved_headingsToEndOfTarget_tasksStayUnderThem() {
        val s = HeadingBatch.split(setOf("h2", "h1"), tasks, headings)
        val saved = HeadingBatch.moved(s, projectId = "q", areaId = "ar", targetHeadings = headings, now = 9)
        val byId = saved.associateBy { it.id }
        // В конце подзаголовков проекта q (там последний — 4), в прежнем порядке
        assertEquals(5, byId.getValue("h1").sortOrder)
        assertEquals(6, byId.getValue("h2").sortOrder)
        assertTrue(saved.take(2).all { it.type == Item.TYPE_HEADING && it.projectId == "q" })
        val movedA = byId.getValue("a")
        assertEquals("q", movedA.projectId)
        assertEquals("ar", movedA.areaId)
        assertEquals("h1", movedA.headingId)
        assertEquals("h2", byId.getValue("c").headingId)
        // Задачи без выбранного подзаголовка не трогаются
        assertTrue("d" !in byId)
        // В тот же проект — ничего
        assertTrue(HeadingBatch.moved(s, "p", null, headings).isEmpty())
    }

    @Test
    fun copy_atEndOfItsProject() {
        val copy = HeadingBatch.copy(h1, headings, newId = "n", now = 7)
        assertEquals("n", copy.id)
        assertEquals("Этап 1", copy.title)
        assertEquals("p", copy.projectId)
        assertEquals(2, copy.sortOrder)
    }

    @Test
    fun shareText_headingThenItsTasks_thenOwnTasks() {
        val s = HeadingBatch.split(setOf("h1", "d"), tasks, headings)
        assertEquals("Этап 1\n• А\n• Б\n• Г", HeadingBatch.shareText(s))
    }
}
