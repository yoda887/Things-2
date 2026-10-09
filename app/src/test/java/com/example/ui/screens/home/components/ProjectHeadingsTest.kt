package com.example.ui.screens.home.components

import com.example.domain.lists.HeadingOrder
import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectHeadingsTest {

    private fun task(id: String, heading: String? = null) =
        ItemWithChecklist(item = Item(id = id, title = id, projectId = "p", headingId = heading), checklist = emptyList())

    private fun heading(id: String) = Item(id = id, title = id, type = Item.TYPE_HEADING, projectId = "p")

    private fun hdr(id: String) = TaskListKeys.HEADING_PREFIX + id

    private fun ids(list: List<ItemWithChecklist>?) = list!!.map { it.item.id }
    private fun headingOf(list: List<ItemWithChecklist>?, id: String) = list!!.first { it.item.id == id }.item.headingId

    // t1 | A: a1 a2 | B: b1
    private val headings = listOf("A", "B")
    private val tasks = listOf(task("t1"), task("a1", "A"), task("a2", "A"), task("b1", "B"))

    @Test
    fun orderByHeading_putsLooseTasksFirstAndKeepsOrderInsideGroups() {
        val mixed = listOf(task("b1", "B"), task("a1", "A"), task("t1"), task("a2", "A"), task("x", "archived"))
        assertEquals(listOf("t1", "x", "a1", "a2", "b1"), ids(HeadingOrder.orderByHeading(mixed, headings) { it.item }))
    }

    @Test
    fun drop_onTask_takesItsPlaceAndHeading() {
        val result = ProjectHeadings.planDrop(tasks, "t1", "a1", movingDown = true, headingIds = headings)
        assertEquals(listOf("a1", "t1", "a2", "b1"), ids(result))
        assertEquals("A", headingOf(result, "t1"))
    }

    @Test
    fun drop_downAcrossHeading_becomesFirstOfIt() {
        val result = ProjectHeadings.planDrop(tasks, "a2", hdr("B"), movingDown = true, headingIds = headings)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(result))
        assertEquals("B", headingOf(result, "a2"))
    }

    @Test
    fun drop_upAcrossOwnHeading_becomesLastOfPreviousGroup() {
        val toA = ProjectHeadings.planDrop(tasks, "b1", hdr("B"), movingDown = false, headingIds = headings)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(toA))
        assertEquals("A", headingOf(toA, "b1"))

        val toLoose = ProjectHeadings.planDrop(tasks, "a1", hdr("A"), movingDown = false, headingIds = headings)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(toLoose))
        assertNull(headingOf(toLoose, "a1"))
    }

    @Test
    fun drop_intoEmptyHeading() {
        val withEmpty = listOf("A", "B", "C")
        val result = ProjectHeadings.planDrop(tasks, "b1", hdr("C"), movingDown = true, headingIds = withEmpty)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(result))
        assertEquals("C", headingOf(result, "b1"))
    }

    @Test
    fun drop_acrossHeadingInWrongDirection_isRejected() {
        assertNull(ProjectHeadings.planDrop(tasks, "b1", hdr("A"), movingDown = true, headingIds = headings))
        assertNull(ProjectHeadings.planDrop(tasks, "t1", hdr("A"), movingDown = false, headingIds = headings))
    }

    @Test
    fun drop_onLooseTaskWithArchivedHeading_clearsHeading() {
        val list = listOf(task("x", "archived"), task("a1", "A"))
        val result = ProjectHeadings.planDrop(list, "a1", "x", movingDown = false, headingIds = headings)
        assertNull(headingOf(result, "a1"))
    }

    @Test
    fun moveHeading_swapsOrder() {
        val hs = listOf(heading("A"), heading("B"), heading("C"))
        assertEquals(listOf("C", "A", "B"), ProjectHeadings.moveHeading(hs, "C", "A")!!.map { it.id })
        assertNull(ProjectHeadings.moveHeading(hs, "A", "A"))
    }
}
