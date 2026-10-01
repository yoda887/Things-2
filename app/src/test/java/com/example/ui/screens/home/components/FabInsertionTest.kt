package com.example.ui.screens.home.components

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FabInsertionTest {

    private fun task(id: String, heading: String? = null, tonight: Boolean = false) =
        ItemWithChecklist(item = Item(id = id, title = id, headingId = heading, isTonight = tonight), checklist = emptyList())

    private fun heading(id: String) = ProjectHeadingItem(Item(id = id, title = id, type = Item.TYPE_HEADING))

    // t1 | A: a1 a2 | B: b1
    private val t1 = task("t1")
    private val a1 = task("a1", "A")
    private val a2 = task("a2", "A")
    private val b1 = task("b1", "B")
    private val project: List<Any> = listOf(t1, heading("A"), a1, a2, heading("B"), b1)
    private val tasks = listOf(t1, a1, a2, b1)

    @Test
    fun slotAt_beforeRowWhoseMiddleIsBelowFinger() {
        val rows = listOf(FabRow(0, 0f, 100f), FabRow(1, 100f, 100f), FabRow(2, 200f, 100f))
        assertEquals(0, FabInsertion.slotAt(10f, rows, false)!!.index)
        assertEquals(1, FabInsertion.slotAt(60f, rows, false)!!.index)
        assertEquals(3, FabInsertion.slotAt(280f, rows, false)!!.index)
        assertNull(FabInsertion.slotAt(10f, emptyList(), false))
    }

    @Test
    fun taskPlacement_insideHeadingGroup() {
        // между a1 и a2
        val p = FabInsertion.taskPlacement(project, 3, tasks)
        assertEquals(2, p.taskIndex)
        assertEquals("A", p.headingId)
        assertFalse(p.isTonight)
    }

    @Test
    fun taskPlacement_rightAfterHeading_belongsToIt() {
        val p = FabInsertion.taskPlacement(project, 5, tasks)
        assertEquals(3, p.taskIndex)
        assertEquals("B", p.headingId)
    }

    @Test
    fun taskPlacement_beforeFirstHeading_hasNoHeading() {
        val p = FabInsertion.taskPlacement(project, 1, tasks)
        assertEquals(1, p.taskIndex)
        assertNull(p.headingId)
    }

    @Test
    fun taskPlacement_atEnd() {
        val p = FabInsertion.taskPlacement(project, project.size, tasks)
        assertEquals(4, p.taskIndex)
        assertEquals("B", p.headingId)
    }

    @Test
    fun taskPlacement_afterEveningHeader_isTonight() {
        val day = task("d")
        val eve = task("e", tonight = true)
        val today: List<Any> = listOf(day, TaskListKeys.EVENING_HEADER, eve)
        assertTrue(FabInsertion.taskPlacement(today, 2, listOf(day, eve)).isTonight)
        assertFalse(FabInsertion.taskPlacement(today, 1, listOf(day, eve)).isTonight)
    }

    @Test
    fun headingPlacement_splitsGroup() {
        // новый заголовок между a1 и a2: a2 уходит под него, заголовок — второй
        val p = FabInsertion.headingPlacement(project, 3)
        assertEquals(1, p.headingIndex)
        assertEquals(listOf("a2"), p.movedTaskIds)

        // перед первым заголовком: под него уходят задачи без заголовка ниже — их нет
        val top = FabInsertion.headingPlacement(project, 1)
        assertEquals(0, top.headingIndex)
        assertEquals(emptyList<String>(), top.movedTaskIds)

        // в самом начале: под него уходит t1
        assertEquals(listOf("t1"), FabInsertion.headingPlacement(project, 0).movedTaskIds)
    }

    @Test
    fun withGap_insertsPlaceholder() {
        val gapped = FabInsertion.withGap(project, FabSlot(2, asHeading = true))
        assertEquals(FabGapItem(true), gapped[2])
        assertEquals(project.size + 1, gapped.size)
    }

    @Test
    fun headingsWith_renumbers() {
        val hs = listOf(Item(id = "A", type = 2, sortOrder = 0), Item(id = "B", type = 2, sortOrder = 1))
        val result = FabInsertion.headingsWith(hs, Item(id = "N", type = 2, sortOrder = -1), 1)
        assertEquals(listOf("A", "N", "B"), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }
}
