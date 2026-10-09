package com.example.domain.edits

import com.example.data.model.Item
import com.example.data.model.ItemWithChecklist
import com.example.domain.lists.HeadingOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Перенос в проекте (по подзаголовкам), перенос подзаголовков и группа задач вслед за ведущей. */
class TaskDropsTest {

    private fun task(id: String, heading: String? = null) =
        ItemWithChecklist(item = Item(id = id, title = id, projectId = "p", headingId = heading), checklist = emptyList())

    private fun heading(id: String) = Item(id = id, title = id, type = Item.TYPE_HEADING, projectId = "p")

    private fun ids(list: List<ItemWithChecklist>?) = list!!.map { it.item.id }
    private fun headingOf(list: List<ItemWithChecklist>?, id: String) = list!!.first { it.item.id == id }.item.headingId

    // t1 | A: a1 a2 | B: b1
    private val headings = listOf("A", "B")
    private val project = DropList.Project(headings)
    private val tasks = listOf(task("t1"), task("a1", "A"), task("a2", "A"), task("b1", "B"))

    private fun drop(list: List<ItemWithChecklist>, dragged: String, target: DropTarget, down: Boolean, dropList: DropList = project) =
        TaskDrops.plan(list, dragged, target, dropList, down)

    @Test
    fun orderByHeading_putsLooseTasksFirstAndKeepsOrderInsideGroups() {
        val mixed = listOf(task("b1", "B"), task("a1", "A"), task("t1"), task("a2", "A"), task("x", "archived"))
        assertEquals(listOf("t1", "x", "a1", "a2", "b1"), ids(HeadingOrder.orderByHeading(mixed, headings) { it.item }))
    }

    @Test
    fun drop_onTask_takesItsPlaceAndHeading() {
        val result = drop(tasks, "t1", DropTarget.Task("a1"), down = true)
        assertEquals(listOf("a1", "t1", "a2", "b1"), ids(result))
        assertEquals("A", headingOf(result, "t1"))
    }

    @Test
    fun drop_downAcrossHeading_becomesFirstOfIt() {
        val result = drop(tasks, "a2", DropTarget.Heading("B"), down = true)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(result))
        assertEquals("B", headingOf(result, "a2"))
    }

    @Test
    fun drop_upAcrossOwnHeading_becomesLastOfPreviousGroup() {
        val toA = drop(tasks, "b1", DropTarget.Heading("B"), down = false)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(toA))
        assertEquals("A", headingOf(toA, "b1"))

        val toLoose = drop(tasks, "a1", DropTarget.Heading("A"), down = false)
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(toLoose))
        assertNull(headingOf(toLoose, "a1"))
    }

    @Test
    fun drop_intoEmptyHeading() {
        val result = drop(tasks, "b1", DropTarget.Heading("C"), down = true, dropList = DropList.Project(listOf("A", "B", "C")))
        assertEquals(listOf("t1", "a1", "a2", "b1"), ids(result))
        assertEquals("C", headingOf(result, "b1"))
    }

    @Test
    fun drop_acrossHeadingInWrongDirection_isRejected() {
        assertNull(drop(tasks, "b1", DropTarget.Heading("A"), down = true))
        assertNull(drop(tasks, "t1", DropTarget.Heading("A"), down = false))
    }

    @Test
    fun drop_onLooseTaskWithArchivedHeading_clearsHeading() {
        val list = listOf(task("x", "archived"), task("a1", "A"))
        val result = drop(list, "a1", DropTarget.Task("x"), down = false)
        assertNull(headingOf(result, "a1"))
    }

    @Test
    fun headingTargets_outsideProject_areRejected() {
        assertNull(drop(tasks, "t1", DropTarget.Heading("A"), down = true, dropList = DropList.Plain))
        assertNull(drop(tasks, "t1", DropTarget.DayHeader(0L), down = true, dropList = DropList.Today))
        assertNull(drop(tasks, "t1", DropTarget.EveningHeader, down = true))
    }

    @Test
    fun moveHeading_swapsOrder() {
        val hs = listOf(heading("A"), heading("B"), heading("C"))
        assertEquals(listOf("C", "A", "B"), Headings.move(hs, "C", "A")!!.map { it.id })
        assertNull(Headings.move(hs, "A", "A"))
    }

    @Test
    fun follower_takesLeadersEveningDateAndHeadingOnlyInProject() {
        val leader = Item(id = "l", isTonight = true, startDate = 1000L, headingId = "A")
        val other = task("o", "B")
        val inProject = TaskDrops.follower(other, leader, inProject = true).item
        assertEquals("A", inProject.headingId)
        assertEquals(true, inProject.isTonight)
        assertEquals(1000L, inProject.startDate)
        val elsewhere = TaskDrops.follower(other, leader, inProject = false).item
        assertEquals("B", elsewhere.headingId)
        assertEquals(1000L, elsewhere.startDate)
        assertFalse(TaskDrops.follower(other, leader.copy(isTonight = false), inProject = false).item.isTonight)
    }
}
